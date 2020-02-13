package com.htuy.swarm.management.onetorule

import com.htuy.common.Address
import com.htuy.common.Logging
import com.htuy.swarm.management.King
import com.htuy.swarm.management.ServiceType
import kotlinx.coroutines.experimental.delay
import java.io.Serializable
import java.util.*

class SingleKingClient(val host : Address, val notReadyDelay : Long = 1000, val retryTimes : Int = 50) : King {

    override fun requestServiceExistence(services: List<ServiceType>) {
        host.tryPush(ServiceExistenceRequest(services)){
            requestServiceExistence(services)
        }
    }

    override fun sendRequestToHostLeadServices(type: ServiceType, request: Serializable) {
        Logging.getLogger().debug{"Sending $request to all host lead services of type $type"}
        host.tryPush(SendToLeadServicesRequest(type,request)){
            Logging.getLogger().warn{"Failed to send request to host for lead services, retrying"}
            sendRequestToHostLeadServices(type,request)
        }
    }

    override fun startService(service: ServiceType, count: Int, args: String) {
        Logging.getLogger().debug { "Starting service $service : $count : $args" }
        host.tryPush(MakeServiceRequest(service,count,args)){
            Logging.getLogger().warn{"Failed startservice request, retrying"}
            startService(service,count,args)
        }
    }

    private val cachedServices = HashMap<ServiceType,Address>()

    override suspend fun getHostForService(service: ServiceType): Address  {
        Logging.getLogger().trace{"Getting host for $service"}
        val serviceAddr = cachedServices[service]
        if(serviceAddr != null){
            Logging.getLogger().trace{"Had host already locally"}
            return serviceAddr
        } else {
            Logging.getLogger().trace { "Didn't have local host for $service, asking king" }
            var retries = 0
            while(retries++ < retryTimes) {
                Logging.getLogger().trace{"$retries attempt to get a host"}
                val newAddr = host.tryGetSynch(ServiceRequest(service)) {
                    throw InternalError("Connection to king failed. No further progress can be made. King was $host")
                }
                if (newAddr is ServiceNotYetReady) {
                    Logging.getLogger().trace{"Got service not ready yet. Sleeping $notReadyDelay and then trying again"}
                    delay(notReadyDelay)
                    continue
                } else {
                    newAddr as ServiceResponse
                    Logging.getLogger().debug{"Successfully got response from king, ${newAddr.address} is our host for ${newAddr.type}"}
                    cachedServices[service] = newAddr.address
                    return newAddr.address
                }
            }
            throw InternalError("King didn't manage to create a service after ${retryTimes * notReadyDelay / 1000.0} seconds :/")
        }
    }

    override  fun serviceError(service: ServiceType) {
        Logging.getLogger().warn{"Service error on $service"}
        val prev = cachedServices[service]
        if(prev != null){
            host.tryPush(ServiceError(service,prev)){
                throw InternalError("Connection to king failed. No further progress can be made. King was $host")
            }
            // id like to delay here but I'm lazy so I won't, probably worth fixing eventually
            cachedServices.remove(service)
        }
    }

    override fun registerSelfAsService(service: ServiceType, address:Address) {
        Logging.getLogger().warn{
            "registering self as host for $service at $address"
        }
        host.tryPush(ServiceRegister(service,address)){
            throw InternalError("Connection to king failed. No further progress can be made. King was $host")
        }
    }

}