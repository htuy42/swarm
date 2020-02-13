package com.htuy.swarm.management

import com.htuy.common.Address
import com.htuy.swarm.work.agent.SimpleAgent
import com.htuy.swarm.coordination.singlequeue.SingleQueueServer
import com.htuy.swarm.data.singlestore.SingleStoreServer
import com.htuy.swarm.processor.DirectiveServer
import com.htuy.swarm.synchro.singlesynchro.SingleSynchroServer
import java.lang.IllegalArgumentException

typealias ServiceType = String

val QUEUE_SERVICE_TYPE = "QUEUE"
val STORE_SERVICE_TYPE = "STORE"
val AGENT_SERVICE_TYPE = "AGENT"
val SYNCHRO_SERVICE_TYPE = "SYNCHRO"
val DIRECTIVE_SERVICE_TYPE = "DIRECTIVE"

interface ServiceStarters{
    fun makeService(serviceType : String, king : King, leader : Address?) : Service
}

class SimpleServiceStarters : ServiceStarters{
    private val services = HashMap<ServiceType,(King,Address?) -> Service>()
    override fun makeService(serviceType : String, king : King, leader: Address?) : Service {

        return services[serviceType]?.invoke(king,leader) ?: when(serviceType){
            QUEUE_SERVICE_TYPE -> SingleQueueServer()
            STORE_SERVICE_TYPE -> SingleStoreServer()
            AGENT_SERVICE_TYPE -> SimpleAgent(king)
            SYNCHRO_SERVICE_TYPE -> SingleSynchroServer()
            DIRECTIVE_SERVICE_TYPE -> DirectiveServer(king)
            else -> throw IllegalArgumentException("Not a valid service type that this service starter recognizes")
        }
    }

    fun addService(type : ServiceType, creator : (King, Address?) -> Service){
        services.put(type,creator)
    }
}