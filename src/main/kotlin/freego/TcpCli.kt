package freego

import java.net.*
import java.io.*

class TcpClient {
    var clientSocket: Socket? = null
    var out_buff: PrintWriter? = null
    var in_buff: BufferedReader? = null
 
    fun open( ip: String, port: Int ) {
        clientSocket = Socket(ip, port)
        out_buff = PrintWriter(clientSocket?.getOutputStream(), true)
        in_buff = BufferedReader( InputStreamReader(clientSocket?.getInputStream()) )
    }
 
    fun send( msg: String): String? {
        out_buff?.println(msg)
        val resp = in_buff?.readLine()
        return resp
    }
 
    fun close() {
        in_buff?.close()
        out_buff?.close()
        clientSocket?.close()
    }
}