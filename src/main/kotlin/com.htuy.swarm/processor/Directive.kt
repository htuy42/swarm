package com.htuy.swarm.processor

import com.htuy.swarm.coordination.QueueConnector
import com.htuy.swarm.data.KeyValueStore
import com.htuy.swarm.synchro.ConcurrencyManager
import java.io.Serializable
import java.util.*

interface Directive : Serializable{

    var store : KeyValueStore
    var queue : QueueConnector
    var synchro : ConcurrencyManager
    val id : UUID

    fun perform()
}

abstract class AbstractDirective : Directive{
    override lateinit var store: KeyValueStore
    override lateinit var queue: QueueConnector
    override lateinit var synchro: ConcurrencyManager
    override val id: UUID = UUID.randomUUID()
}