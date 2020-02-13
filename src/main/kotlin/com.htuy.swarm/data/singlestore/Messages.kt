package com.htuy.swarm.data.singlestore

import com.htuy.common.Address
import com.htuy.swarm.data.Id
import java.io.Serializable

data class PutIdMessage(val id: Id, val host: Address) : Serializable

data class RequestIdMessage(val id: Id) : Serializable

data class UnputIdMessage(val id: Id) : Serializable

data class IdHostMessage(val id: Id, val host: Address?) : Serializable

data class RequestValueMessage(val id : Id) : Serializable

data class ValueMessage(val id : Id, val value : Serializable) : Serializable

data class MissingMessage(val id : Id) : Serializable

data class WatchRegisterMessage(val id : Id, val callbackAddres : Address) : Serializable

data class WatchTriggerMessage(val id : Id) : Serializable