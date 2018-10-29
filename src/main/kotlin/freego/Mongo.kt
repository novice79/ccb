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

import java.util.concurrent.TimeUnit

class Mdb{
    val client = KMongo.createClient( MongoClientURI(mongo_url) )
    val database: MongoDatabase by lazy {
        client.getDatabase(mongodb_name)
    }
    init{
        val col = database.getCollection<Pending>() 
        col.ensureIndex(Pending::createdAt, indexOptions = IndexOptions().expireAfter(3600, TimeUnit.SECONDS))
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
}