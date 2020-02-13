package com.htuy.swarm.management.onetorule

import com.htuy.common.Address
import com.htuy.swarm.management.ServiceType
import java.io.Serializable
import java.util.*

data class ServiceRequest(val type : ServiceType) : Serializable

data class ServiceError(val type : ServiceType, val host : Address) : Serializable

data class ServiceRegister(val type : ServiceType, val address : Address) : Serializable

data class ServiceResponse(val type : ServiceType, val address: Address) : Serializable

data class MakeServiceRequest(val type : ServiceType, val count : Int, val args : String = "") : Serializable

data class ServiceExistenceRequest(val services : List<ServiceType>) : Serializable

interface Ping : Serializable{
    val confirmed : Boolean
}

data class FollowerRegister(val type : ServiceType, val id : String) : Serializable

data class ServicePing(val service : ServiceType, override val confirmed : Boolean) : Ping

data class LeadServicePing(override val confirmed : Boolean) : Ping

class ServiceRegisterResponse : Serializable

class ServiceNotYetReady : Serializable

class ServiceDeregister : Serializable

data class SendToLeadServicesRequest(val type: ServiceType, val request : Serializable) : Serializable

class LeadServiceFinish : Serializable