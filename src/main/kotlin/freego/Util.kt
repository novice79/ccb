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
import kotlinx.coroutines.experimental.*

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
// URLEncoder.encode(url, "UTF-8") 
suspend fun ccb_req_qr(data: Pending): String{
    val order = mutableMapOf(
    "MERCHANTID" to MERCHANTID, 
    "POSID" to POSID, 
    "BRANCHID" to BRANCHID,
    "ORDERID" to "111",
    "PAYMENT" to "0.01",
    "CURCODE" to "01",
    "TXCODE" to "530550",
    "REMARK1" to "",
    "REMARK2" to "",
    "RETURNTYPE" to "3",
    "TIMEOUT" to ""
    )
    // var m1 = data.body
    // m1 = URLEncoder.encode(m1, "UTF-8")
    // m1 = Base64.getEncoder().encode("大庙门票".toByteArray()).toString()
    // val charset = Charsets.UTF_8
    // m1 = DatatypeConverter.printHexBinary(m1.toByteArray(charset)) 
    // println(m1)
    // val om1 = DatatypeConverter.parseHexBinary(m1)
    // println( om1.toString(charset) )
    // order["REMARK1"] = m1 
    order["ORDERID"] = data.out_trade_no 
    order["PAYMENT"] = "%.2f".format( data.total_amount / 100.0) 
    // println(order)
    val para = order.toList().formUrlEncode()            

    val md = MessageDigest.getInstance("MD5")
    md.update("$para&PUB=$ccb_pub30".toByteArray() )
    val digest = md.digest()
    val mac = DatatypeConverter.printHexBinary(digest).toLowerCase();
    val url = "$ccb_url&${para}&MAC=${mac}"
    val client = HttpClient() {
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
    }
    // println(url)
    val json = client.post<String>{
        url(URL(url))
        contentType(ContentType.Application.Json)
    }
    println(json)
    
    val outer_ret = json_mapper.readValue<StrMap>(json)
    val inner_ret = client.post<String>( outer_ret["PAYURL"]!! )
    // println(inner_ret)
    val qr_res = json_mapper.readValue<StrMap>(inner_ret)
    val qr_url = URLDecoder.decode(qr_res["QRURL"]!!, "UTF-8")
    return qr_url
}
fun format_now(): String{
    val instant = Instant.now()
    return format_instant(instant)
}
fun format_instant(instant: Instant): String{
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    //  .withLocale( Locale.SIMPLIFIED_CHINESE )
                     .withZone( ZoneId.of( "Asia/Shanghai" ) )
    val output = formatter.format( instant )
    return output
}