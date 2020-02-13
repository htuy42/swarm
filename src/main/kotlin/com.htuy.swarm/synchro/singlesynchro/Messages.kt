package com.htuy.swarm.synchro.singlesynchro

import com.htuy.common.Address
import com.htuy.swarm.data.Id
import java.io.Serializable

data class GetAndIncrRequest(val id : Id) : Serializable

data class AtomicLongResponse(val value : Long) : Serializable

data class GetAndIncrSetRequest(val id : Id, val toAdd : Set<Serializable>) : Serializable

data class GetAndIncrSetResponse(val value : Set<Serializable>) : Serializable

data class GetAndSetRequest(val id : Id, val setTo : Long) : Serializable

data class BarrierWaitRequest(val id : Id, val waitForCount : Int, val listeningAddress : Address) : Serializable

data class BarrierWaitResponse(val id : Id, val ready : Boolean, val returnId : Int?) : Serializable

data class TriggerRequest(val id : Id, val isTriggerActivation : Boolean, val ownAddress : Address? = null) : Serializable

data class TriggerResponse(val id : Id, val hasTriggered : Boolean) : Serializable

data class GetAndMaxRequest(val id : Id, val toMaxWith : Long) : Serializable