package freego
import org.bson.types.ObjectId
import java.time.Instant

data class Order(
    val out_trade_no: String, 
    val total_amount: Int, 
    val body: String,
    val time_begin: String? = null,
    val time_end: String? = null
)
data class Pending(
    val out_trade_no: String, 
    val total_amount: Int, 
    val body: String, 
    val cli_id: String? = null,
    val notify_url: String? = null,
    val createdAt: Instant = Instant.now(),
    val status: String = "unpaid"
)

// data class Model(val name: String, val items: List<Item>, val date: LocalDate = LocalDate.of(2018, 4, 13))
data class Command(val cmd: String, val data: String)

typealias StrMap = Map<String, String>
