package freego

import com.fasterxml.jackson.core.util.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.*
import com.fasterxml.jackson.module.kotlin.*

import io.ktor.application.*

import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.*
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*


typealias WsHandler = WsDealer.(StrMap) -> Unit
class WsDealer {
    
    val sock2id = ConcurrentHashMap<WebSocketSession, String>()
    val id2sock = ConcurrentHashMap<String, WebSocketSession>()
    suspend fun cli_online(id: String, socket: WebSocketSession) {
        if(id2sock[id] != null){
            val old_sock = id2sock[id]
            old_sock!!.close(CloseReason(CloseReason.Codes.NORMAL, "Same client online"))
            sock2id.remove( old_sock )
        }
        id2sock[id] = socket
        sock2id[socket] = id
        logger.info("cli_online  id2sock.size=${id2sock.size} && sock2id.size=${sock2id.size}")
    }
    suspend fun cli_offline(socket: WebSocketSession) {
        if( sock2id[socket] != null ){
            val cli_id = sock2id[socket]
            // logger.info("$cli_id offline")
            id2sock.remove( cli_id )
            sock2id.remove( socket )
        } else {
            // println("no contain $socket")
        }
        logger.info("cli_offline  id2sock.size=${id2sock.size} && sock2id.size=${sock2id.size}")
    }
    suspend fun handle_msg(req: Command, sock: WebSocketSession, call: ApplicationCall) {
        if(req.cmd == "reg_cli_id"){
            cli_online(req.data, sock)
            val paids = mdb.find_pending_paid(req.data)
            paids.forEach { 
                val order = Order(it.out_trade_no, it.total_amount, it.body, format_instant(it.createdAt), it.time_end )
                if( notify_pay_success(it.cli_id!!, order) ){
                    mdb.del_po_by_oid(it.out_trade_no) 
                }
            }
        } else if(req.cmd == "req_qr"){
            try{
                val data = json_mapper.readValue<Pending>(req.data)
                var qr_url = ccb_req_qr(data)
                qr_url = affixed_qr_url(call, qr_url, data.out_trade_no)
                val res_data = json_mapper.writeValueAsString(
                    mapOf(
                        "ret" to 0,
                        "cmd" to req.cmd,
                        "data" to req.data,
                        "qr_url" to qr_url
                    )
                ) 
                logger.info("websocket return: ${res_data}")
                mdb.insert_pending_order(data)
                sock.send(Frame.Text(res_data))
            }catch(e: Exception){
                val res_data = json_mapper.writeValueAsString(
                    mapOf(
                        "ret" to -1,
                        "cmd" to req.cmd,
                        "msg" to e.toString()
                    )
                ) 
                logger.info("websocket return: ${res_data}")
                sock.send(Frame.Text(res_data))
            }
            
        }
    }
    suspend fun notify_pay_success(cli_id: String, o: Order): Boolean{
        val t_sock = id2sock[cli_id]
        if(t_sock != null){
            val res_data = json_mapper.writeValueAsString(
                mapOf(
                    "cmd" to "pay_success",
                    "data" to o
                )
            ) 
            logger.info("websocket notify $cli_id: ${res_data}")
            t_sock.send(Frame.Text(res_data))
            return true
        }
        return false        
    }
    // suspend fun memberJoin(id: String, socket: WebSocketSession) {

    //     // Associates this socket to the member id.
    //     // Since iteration is likely to happen more frequently than adding new items,
    //     // we use a `CopyOnWriteArrayList`.
    //     // We could also control how many sockets we would allow per client here before appending it.
    //     // But since this is a sample we are not doing it.
    //     val list = members.computeIfAbsent(member) { CopyOnWriteArrayList<WebSocketSession>() }
    //     list.add(socket)

    //     // Only when joining the first socket for a member notifies the rest of the users.
    //     if (list.size == 1) {
    //         broadcast("server", "Member joined: $name.")
    //     }

    //     // Sends the user the latest messages from this server to let the member have a bit context.
    //     val messages = synchronized(lastMessages) { lastMessages.toList() }
    //     for (message in messages) {
    //         socket.send(Frame.Text(message))
    //     }
    // }

    // /**
    //  * Handles a [member] idenitified by its session id renaming [to] a specific name.
    //  */
    // suspend fun memberRenamed(member: String, to: String) {
    //     // Re-sets the member name.
    //     val oldName = memberNames.put(member, to) ?: member
    //     // Notifies everyone in the server about this change.
    //     broadcast("server", "Member renamed from $oldName to $to")
    // }

    // /**
    //  * Handles that a [member] with a specific [socket] left the server.
    //  */
    // suspend fun memberLeft(member: String, socket: WebSocketSession) {
    //     // Removes the socket connection for this member
    //     val connections = members[member]
    //     connections?.remove(socket)

    //     // If no more sockets are connected for this member, let's remove it from the server
    //     // and notify the rest of the users about this event.
    //     if (connections != null && connections.isEmpty()) {
    //         val name = memberNames.remove(member) ?: member
    //         broadcast("server", "Member left: $name.")
    //     }
    // }

    // /**
    //  * Handles the 'who' command by sending the member a list of all all members names in the server.
    //  */
    // suspend fun who(sender: String) {
    //     members[sender]?.send(Frame.Text(memberNames.values.joinToString(prefix = "[server::who] ")))
    // }

    // /**
    //  * Handles the 'help' command by sending the member a list of available commands.
    //  */
    // suspend fun help(sender: String) {
    //     members[sender]?.send(Frame.Text("[server::help] Possible commands are: /user, /help and /who"))
    // }

    // /**
    //  * Handles sending to a [recipient] from a [sender] a [message].
    //  *
    //  * Both [recipient] and [sender] are identified by its session-id.
    //  */
    // suspend fun sendTo(recipient: String, sender: String, message: String) {
    //     members[recipient]?.send(Frame.Text("[$sender] $message"))
    // }

    // /**
    //  * Handles a [message] sent from a [sender] by notifying the rest of the users.
    //  */
    // suspend fun message(sender: String, message: String) {
    //     // Pre-format the message to be send, to prevent doing it for all the users or connected sockets.
    //     val name = memberNames[sender] ?: sender
    //     val formatted = "[$name] $message"

    //     // Sends this pre-formatted message to all the members in the server.
    //     broadcast(formatted)

    //     // Appends the message to the list of [lastMessages] and caps that collection to 100 items to prevent
    //     // growing too much.
    //     synchronized(lastMessages) {
    //         lastMessages.add(formatted)
    //         if (lastMessages.size > 100) {
    //             lastMessages.removeFirst()
    //         }
    //     }
    // }

    // /**
    //  * Sends a [message] to all the members in the server, including all the connections per member.
    //  */
    // private suspend fun broadcast(message: String) {
    //     members.values.forEach { socket ->
    //         socket.send(Frame.Text(message))
    //     }
    // }

    // /**
    //  * Sends a [message] coming from a [sender] to all the members in the server, including all the connections per member.
    //  */
    // private suspend fun broadcast(sender: String, message: String) {
    //     val name = memberNames[sender] ?: sender
    //     broadcast("[$name] $message")
    // }

    // /**
    //  * Sends a [message] to a list of [this] [WebSocketSession].
    //  */
    // suspend fun List<WebSocketSession>.send(frame: Frame) {
    //     forEach {
    //         try {
    //             it.send(frame.copy())
    //         } catch (t: Throwable) {
    //             try {
    //                 it.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, ""))
    //             } catch (ignore: ClosedSendChannelException) {
    //                 // at some point it will get closed
    //             }
    //         }
    //     }
    // }
}
