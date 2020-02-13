package com.htuy.swarm.management

import com.htuy.common.Address
import com.htuy.common.Logging
import com.htuy.netlib.sockets.Socket
import com.htuy.swarm.work.agent.DomainServeRequest
import java.io.Serializable
import java.util.*


interface King{

    suspend fun getHostForService(service : ServiceType) : Address

    fun serviceError(service : ServiceType)

    fun registerSelfAsService(service : ServiceType, address: Address)

    fun startService(service : ServiceType, count : Int, args : String)

    fun sendRequestToHostLeadServices(type : ServiceType, request : Serializable)

    fun requestServiceExistence(services : List<ServiceType>)

    suspend fun tryGetSynchToServiceRetry(service : ServiceType, message : Serializable) : Serializable?{
        Logging.getLogger().debug{"Trying with retry to get $service to send $message!"}
        while(true){
            val res = tryGetSynchToService(service,message){
                null
            }
            if(res != null){
                Logging.getLogger().debug{"Successfully got $res"}
                return res
            }
            Logging.getLogger().debug{"Something went wrong, retrying"}
        }
    }

    suspend fun tryGetSynchToService(service : ServiceType, message : Serializable, handler: () -> Serializable?) : Serializable?{
        Logging.getLogger().debug{"Trying to get from $service"}
        val serviceHost = getHostForService(service)
        Logging.getLogger().debug{"Got the service $service @ $serviceHost. Trying to send"}
        return serviceHost.tryGetSynch(message){
            Logging.getLogger().debug{"Failed to get from $service, executing fallback"}
            serviceError(service)
            handler()
        }
    }

    suspend fun tryPushToServiceRetry(service : ServiceType, message : Serializable){
        Logging.getLogger().debug { "Trying to push $message to $service with retry" }
        while(true){
            var succ = true
            tryPushToService(service,message){
                Logging.getLogger().warn{"Problem pushing to service! retrying"}
                succ = false
            }
            if(succ){
                Logging.getLogger().debug{"Sucessfully pushed $message"}
                return
            }
        }
    }

    suspend fun tryPushToService(service : ServiceType, message : Serializable, handler: () -> Unit){
        Logging.getLogger().debug{"Trying to push $message to $service"}
        getHostForService(service).tryPush(message){
            Logging.getLogger().warn{"Problem pushing to $service, executing handler"}
            serviceError(service)
            handler()
        }
    }
}