package freego

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.freemarker.*


import java.util.*
import java.util.concurrent.*


val peers = ConcurrentHashMap<String, String>()

suspend fun ApplicationCall.aaaa() {
    val data = receive<Order>()
    println(data)
    peers[data.body] = data.out_trade_no
    respond( peers)
}