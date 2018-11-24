package freego

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import freemarker.cache.*
import io.ktor.freemarker.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.features.*
import io.ktor.http.content.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.html.*
import io.ktor.sessions.*
import io.ktor.util.*
import io.ktor.websocket.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame

import kotlinx.coroutines.channels.*
import kotlin.system.*
import kotlin.math.round
import kotlinx.html.*
import kotlinx.coroutines.*

import com.fasterxml.jackson.core.util.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.*
import com.fasterxml.jackson.module.kotlin.*

import java.util.*
import java.util.concurrent.*
import java.io.*
import java.time.*
import java.time.format.*
import java.security.*
import java.net.*
import javax.xml.bind.DatatypeConverter

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*


val mdb = Mdb()
val ws = WsDealer()
val json_mapper = jacksonObjectMapper()
fun Application.main() {
     launch {
        while(true){
            delay(1000)
            // println("--Launch-- bg job : ${Thread.currentThread().getName()}")
        }       
    }
    install(Sessions) {
        cookie<IDSession>("IDSession", storage = SessionStorageMemory()){
            cookie.path = "/"
            cookie.duration = Duration.ofDays(3) // Specify duration here
        }
    } 
    install(ForwardedHeaderSupport)
    install(XForwardedHeaderSupport)
    install(DefaultHeaders)
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }
    install(WebSockets)
    install(Compression)
    // install(CallLogging)
    install(PartialContent)
    install(ContentNegotiation) {
        jackson {
            configure(SerializationFeature.INDENT_OUTPUT, true)
            setDefaultPrettyPrinter(DefaultPrettyPrinter().apply {
                indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
                indentObjectsWith(DefaultIndenter("  ", "\n"))
            })
            registerModule(JavaTimeModule())  // support java.time.* types
        }
    }
    routing {
        // post("/notify") marker@{
        post("/notify") {
            var qs = call.request.queryString()
            logger.info("ccb notify callback query string = [$qs]")
            ///////////////////////////////////////////
            val rsa = RSASig()
            rsa.setPublicKey(ccb_pub);
            val qsPara = call.request.queryParameters
            val sign = qsPara["SIGN"]
            val qsList = qsPara.filter( true, {k, _-> k != "SIGN" }).flattenEntries()            
            qs = qsList.formUrlEncode()
    
            if( rsa.verifySigature( sign, qs) ){
                // println("验签成功")
            } else{
                sendMail("建行通知包验签失败", qs)
                logger.error("验签失败")
                call.respond( "failed" )
                // be carefull return from lambda
                return@post
                // or return@marker
            }
            ///////////////////////////////////////////
            try{
                // val data = call.receive<String>()
                // logger.info("ccb notify callback [$data]")                
                val order_id = call.request.queryParameters["ORDERID"]!!
                val o = mdb.find_po_by_oid(order_id)
                if(o != null){
                    val finish_order = Order(
                        o.out_trade_no, o.total_amount, o.body, 
                        format_instant(o.createdAt), format_now(),
                        o.id, o.id_type )
                    mdb.insert_success_order(finish_order)
                    if(o.cli_id != null){ 
                        ws.notify_pay_success(o.cli_id, finish_order)                            
                    }              
                    if(o.notify_url != null){ 
                        post_order( o.notify_url, finish_order)                   
                    }
                    post_wx_msg(finish_order)
                    post_wx_msg(finish_order.copy(id="o2GzG1ENhxSmIBe4wwpLTVJTU2GM", id_type="openid") )
                    // update doc in pending collection, that doc will be auto removed after expired
                    mdb.update_to_paid(order_id)      
                }
                call.respond( "success")
            }
            catch(e:Exception){
                logger.info("throw exception while handling ccb notify : ${e.toString()}")
                call.respond( "failed: ${e.toString()}" )
            }
        }
        post("/qr") {
            try{
                val data = call.receive<Pending>()
                logger.info("Request[$data]")
                var qr_url = ccb_req_qr(data)
                qr_url = affixed_qr_url(call, qr_url, data.out_trade_no)
                println(qr_url)
                
                val rd = mapOf(
                        "ret" to 0,
                        "qr_url" to qr_url
                    )
                logger.info("Respond[$rd]")                
                call.respond( rd)
                mdb.insert_pending_order(data)
            }
            catch(e:Exception){
                val rd = mapOf(
                        "ret" to -1,
                        "msg" to e.toString()
                    )  
                logger.info("Respond[$rd]")
                call.respond( rd )
            }
        }
        post("/test") {
            post_wx_msg( Order(
                "20181124224837983", 1, "中心景区门票(1)", 
                "2018-11-24 22:55:14", "2018-11-24 22:56:14",
                "o2GzG1ENhxSmIBe4wwpLTVJTU2GM", "openid"
                ) 
            )
            // val rsa = RSASig()
            // rsa.setPublicKey(ccb_pub);
            // val qsPara = call.request.queryParameters
            // val sign = qsPara["SIGN"]
            // val qsList = qsPara.filter( true, {k, _-> k != "SIGN" }).flattenEntries()            
            // val qs = qsList.formUrlEncode()
    
            // if( rsa.verifySigature( sign, qs) ){
            //     println("验签成功")
            // } else{
            //     println("验签失败")
            //     sendMail("建行通知包验签失败", "入侵警告")
            // }
            //call.request.queryString()
            // println("in post /test $qs")
            // println("in post /test ${qs}")            
            // post_order( "http://localhost:30001", Order("aaa", 1, "bbb"), 1)
            // val tcp = TcpClient()
            // tcp.open("127.0.0.1", 8888)
            // val ret = tcp.send("send from david")
            // tcp.close()
            // sendMail("告警测试", "入侵警告")
            // val data = call.receive<Pending>()
            // val paid = mdb.find_pending_paid(data.cli_id!!)
            // paid.forEach { println(it) }
            // val a = "${call.request.origin.scheme}://${call.request.origin.host}:${call.request.origin.port}"
            call.respond( "${format_now()}: ${my_url(call)}" )
            // call.aaaa()
            // try{
            //     val data = call.receive<ReqData>()
            //     // col.insertOne(data)
            //     val ret = col.findOneAndUpdate("{out_trade_no:'${data.name}'}", "{$set: {body: '${data.body}'}}")
            //     // val ret = col.findOne("{out_trade_no: '3333'")
            //     call.respond( if(ret == null) Ret(-1, "failed") else ret)
            // }
            // catch(e:Exception){
            //     call.respond( Ret(-1, "invalid parameters") )
            // }
            // slow a bit
            // val res = async {
            //     try{
            //         val data = call.receive<ReqData>()
            //         // col.insertOne(data)
            //         val ret = col.findOneAndUpdate("{out_trade_no:'${data.name}'}", "{$set: {body: '${data.body}'}}")
            //         // val ret = col.findOne("{out_trade_no: '3333'")
            //         if(ret == null) Ret(-1, "failed") else ret
            //     }
            //     catch(e:Exception){
            //         Ret(-1, "invalid parameters") 
            //     }
            // }
            // call.respond(res.await())
            // val res = async {
            //     val client = HttpClient() {
            //         install(JsonFeature) {
            //             serializer = JacksonSerializer()
            //         }
            //     }
            //     println("Requesting test...")
            //     val aaa = client.post<Item>(port = 8080, path = "/json")
            //     println("Fetching data = '${aaa.ret}'...")
            //     aaa
            // }
            // call.respond(res.await())
        }
        webSocket("/ws") { // this: WebSocketSession ->
            // println("onConnect")
            try {
                incoming.consumeEach { frame ->
                    // Frames can be [Text], [Binary], [Ping], [Pong], [Close].
                    if (frame is Frame.Text) {
                        try{
                            val json = frame.readText()
                            logger.info("websocket recieved: ${json}")
                            val data = json_mapper.readValue<Command>(json)
                            ws.handle_msg(data, this, call)
                        }                        
                        catch(e:Exception){                           
                            logger.info("websocket parse json failed ${e.toString()}")
                        }
                    }
                }
            } finally {
                // Either if there was an error, of it the connection was closed gracefully.
                println("$this offlined")
                ws.cli_offline(this)
            }
        }
        get("/relay") {
            call.relay()
        }
        get("/tpl") {
            call.respond(
                FreeMarkerContent("index.ftl", mapOf(
                    "name" to "david",
                    "age" to "40"
                ), "e")
            )
        }
        static("/") {
            // these for server outside dir
            // staticRootFolder = File("public")
            // files(".")
            // default("index.html")

            // serve page in resources
            staticBasePackage = ("public")
            resources(".")
            defaultResource("index.html")
        }
        get("/return") {
            val qs = call.request.queryString()
            logger.info("in /return querystring = $qs")
            if(call.request.queryParameters["ORDERID"] == null){
                call.respond( "invalid entrance" )
                return@get
            }
            val order_id = call.request.queryParameters["ORDERID"]!!           
            val o = mdb.find_po_by_oid(order_id)
            if(o != null){
                val order = mapOf(
                    "out_trade_no" to o.out_trade_no,
                    "body" to o.body,
                    "total_amount" to o.total_amount.toString()
                )
                val para = order.toList().formUrlEncode()  
                logger.info("return para=${para}")
                if(o.return_url != null){
                    val rurl = "${o.return_url}?$para"
                    logger.info("redirect to $rurl")
                    call.respondRedirect(rurl)
                } else {
                    call.respondRedirect("/ccb/#/finish?$para", permanent = true)
                }               
            } else{
                call.respond( "can not found pending order" )
            }            
            // POSID=026820746&BRANCHID=430000000&ORDERID=20181105230332034&PAYMENT=.01&CURCODE=01&REMARK1=E58D97E5B2B3E997A8E7A5A8&REMARK2=&SUCCESS=Y&TYPE=1&REFERER=&CLIENTIP=113.247.56.20&SIGN=95338e87c26610d36d23fe6690744b7cd60d444d212f6582c024d13152e08cb6332d1681465b54abce6cd7cdbd6de74e870239beefd8df6e56706da1e767a3093b6765a4a254a17e88e3ee7a8b96d85edaf49703e7226262e11723f457d9a5117c44ce989b62a3ece5066b55faf8b19a5cfbd9543e1f325e2967d93261d932ee
        }
    }    
}

fun main(args: Array<String>) {
    println( "Hello david" )
    
}