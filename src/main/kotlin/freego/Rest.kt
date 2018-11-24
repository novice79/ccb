package freego

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.freemarker.*
import io.ktor.sessions.*

import io.ktor.http.*

import java.util.*
import java.util.concurrent.*

// for test use java class
// class A() : MyClass(){
//     override fun do_thing() {
//         super.do_thing()
//         println("in kotlin do_thing()")
//     }
// }


val peers = ConcurrentHashMap<String, String>()

suspend fun ApplicationCall.aaaa() {
    val data = receive<Order>()
    println(data)
    peers[data.body] = data.out_trade_no
    respond( peers)
}
// http://t1.me/ccb/relay?tourl=http://baidu.com&out_trade_no=111
suspend fun ApplicationCall.relay() {
    val tourl = request.queryParameters["tourl"]
    val user_id = request.queryParameters["user_id"]
    val session_id = request.queryParameters["session_id"]
    val openid = request.queryParameters["openid"]
    val out_trade_no = request.queryParameters["out_trade_no"]
    if(tourl != null && out_trade_no != null){
        sessions.set( IDSession(out_trade_no, tourl) )
        val qs = listOf( "rurl" to "${my_url(this)}/relay" ).formUrlEncode()
        // println("qs=$qs")
        respondRedirect("$id_url?$qs")
    } else {
        val ids = sessions.get<IDSession>()
        var id = ""
        var id_type = ""
        if(openid != null) {
            id = openid
            id_type = "openid"
        } else if(session_id != null) {
            id = session_id
            id_type = "session_id"
        } else if(user_id != null) {
            id = user_id
            id_type = "user_id"
        } 
        logger.info("id=$id; id_type=$id_type")
        if(ids != null ){
            mdb.set_po_uid(ids.out_trade_no, id, id_type)
            respondRedirect(ids.tourl)
        } else {
            respond( "invalid entrance")
        }       
    }
    
}