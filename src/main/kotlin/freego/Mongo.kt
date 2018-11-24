package freego

import org.litote.kmongo.*
import org.litote.kmongo.MongoOperator.set
import org.litote.kmongo.MongoOperator.setOnInsert
import com.mongodb.MongoClientURI
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument.AFTER

import com.mongodb.Block
import com.mongodb.Function
import com.mongodb.ServerAddress
import com.mongodb.ServerCursor
import com.mongodb.client.MongoCursor
import com.mongodb.client.MongoIterable
import com.mongodb.lang.Nullable

import java.util.concurrent.TimeUnit

class Mdb{
    val client = KMongo.createClient( MongoClientURI(mongo_url) )
    val database: MongoDatabase by lazy {
        client.getDatabase(mongodb_name)
    }
    init{
        val col = database.getCollection<Pending>() 
        col.ensureIndex(Pending::createdAt, indexOptions = IndexOptions().expireAfter(1800, TimeUnit.SECONDS))
    }
    fun insert_pending_order(data: Pending){
        val col = database.getCollection<Pending>() 
        col.insertOne(data)
    }
    fun insert_success_order(data: Order){
        val col = database.getCollection<Order>() 
        col.insertOne(data)
    }
    fun find_po_by_oid(out_trade_no: String): Pending?{
        val col = database.getCollection<Pending>() 
        val o : Pending? = col.findOne(Pending::out_trade_no eq out_trade_no)
        return o
    }
    fun del_po_by_oid(out_trade_no: String){
        val col = database.getCollection<Pending>() 
        col.deleteOne("{out_trade_no:'${out_trade_no}'}")
        // col.deleteOne(Pending::out_trade_no eq out_trade_no)
    }
    fun update_to_paid(out_trade_no: String){
        val col = database.getCollection<Pending>() 
        val now_str = format_now()
        col.findOneAndUpdate("{out_trade_no:'${out_trade_no}'}", "{$set: {status: 'paid', time_end: '$now_str'}}")
    }
    fun set_po_uid(out_trade_no: String, id: String, id_type: String){
        val col = database.getCollection<Pending>() 
        col.findOneAndUpdate("{out_trade_no:'${out_trade_no}'}", "{$set: {id: '$id', id_type: '$id_type'}}")
    }
    fun find_pending_paid(cli_id: String): List<Pending>{
        val col = database.getCollection<Pending>() 
        val cli_paid = col.find("{cli_id:'${cli_id}', status:'paid'}")
        val paid_o = mutableListOf<Pending>()
        cli_paid.forEach { paid_o.add(it) }
        return paid_o
    }
}