package com.htuy.swarm.extending.api

import com.htuy.common.Address
import com.htuy.common.ObjectSerializer
import com.htuy.swarm.coordination.QueueConnector
import com.htuy.swarm.coordination.singlequeue.SingleQueueClient
import com.htuy.swarm.data.Id
import com.htuy.swarm.data.KeyValueStore
import com.htuy.swarm.data.StringId
import com.htuy.swarm.data.singlestore.SingleStoreClient
import com.htuy.swarm.management.AGENT_SERVICE_TYPE
import com.htuy.swarm.management.DIRECTIVE_SERVICE_TYPE
import com.htuy.swarm.management.King
import com.htuy.swarm.management.onetorule.Leader
import com.htuy.swarm.management.onetorule.SingleKingClient
import com.htuy.swarm.management.onetorule.SingleKingServer
import com.htuy.swarm.processor.Directive
import com.htuy.swarm.synchro.ConcurrencyManager
import com.htuy.swarm.work.Task
import kotlinx.coroutines.experimental.runBlocking
import java.io.Serializable

class SwarmApi {

    // this ought to be sufficient but I can add more features as needed.

    lateinit var king : King
    var kingServer : SingleKingServer? = null
    lateinit var queue: QueueConnector
    lateinit var store : KeyValueStore
    lateinit var synch : ConcurrencyManager

    private fun checkKing(){
        if (!::king.isInitialized){
            throw IllegalAccessException("We need a king before we can do that. Try connect or host!")
        }
    }

    private fun checkServers(){
        if(!::store.isInitialized){
            checkKing()
            store = SingleStoreClient(king)
            queue = SingleQueueClient(king)
        }
    }

    fun kill(){
        kingServer?.kill()
    }

    fun connect(addr : Address){
        king = SingleKingClient(addr)
    }

    fun doDirective(directive : Directive,count : Int) = runBlocking{
        checkServers()
        val directiveString = ObjectSerializer.objectToString(directive)
        store.put(StringId(directive.id.toString()),directiveString)
        king.startService(DIRECTIVE_SERVICE_TYPE,count,directive.id.toString())
    }

    fun host(childName : String){
        kingServer = SingleKingServer(childName = childName)
        kingServer!!.start()
        king = SingleKingClient(kingServer!!.addr)
    }

    fun requestTaskAgents(count : Int){
        checkKing()
        king.startService(AGENT_SERVICE_TYPE,count,"")
    }

    fun putTasks(tasks : List<Task>) = runBlocking{
        checkServers()
        queue.put(tasks)
    }

    // note, in theory you want to call this before putting a task that might set the id, since it could perfectly well be
    // the case that the task finishes so fast the id has already been set before a watch is placed otherwise.
    fun watchId(id : Id, callback : (Serializable) -> Unit) = runBlocking{
        checkServers()
        store.watch(id,callback)
    }

    fun silence(){
        kingServer?.silenceSubs()
    }
}