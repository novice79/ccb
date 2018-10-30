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
        post("/notify") {
            try{
                //or some other format
                val data = call.receive<StrMap>()
                logger.info("ccb notify callback [$data]")
                val order_id = data["ORDERID"]!!
                val o = mdb.find_po_by_oid(order_id)
                if(o != null){
                    val finish_order = Order(o.out_trade_no, o.total_amount, o.body, format_instant(o.createdAt), format_now() )
                    mdb.insert_success_order(finish_order)
                    if(o.cli_id != null){ 
                        if( ws.notify_pay_success(o.cli_id, finish_order) )
                            mdb.del_po_by_oid(order_id) 
                        else 
                            mdb.update_to_paid(order_id)
                    }                    
                }
                call.respond( "success")
            }
            catch(e:Exception){
                call.respond( "failed" )
            }
        }
        post("/qr") {
            try{
                val data = call.receive<Pending>()
                logger.info("Request[$data]")
                val qr_url = ccb_req_qr(data)
                println(qr_url)
                
                val rd = mapOf(
                        "ret" to 0,
                        "qr_url" to qr_url
                    )
                logger.info("Respond[$rd]")
                call.respond( rd)
                // col.insertOne(data)
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
            
            val data = call.receive<Pending>()
            val paid = mdb.find_pending_paid(data.cli_id!!)
            paid.forEach { println(it) }
            call.respond( format_now() )
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
                            ws.handle_msg(data, this)
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
            call.respondText("HELLO WORLD")
        }
    }    
}

fun main(args: Array<String>) {
    println( "Hello david" )
    
}