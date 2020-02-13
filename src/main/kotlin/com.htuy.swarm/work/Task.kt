package com.htuy.swarm.work

import com.htuy.swarm.coordination.QueueDomain
import com.htuy.swarm.coordination.QueueConnector
import com.htuy.swarm.coordination.QueueItem
import com.htuy.swarm.data.Id
import com.htuy.swarm.data.KeyValueStore
import com.htuy.swarm.management.King
import java.io.Serializable
import java.util.*

// todo parent goes in the put message, not the items themselves.
abstract class Task() : Serializable, QueueItem{
    abstract val depedencies: List<Id>
    abstract var yield: Map<Id, Serializable>
    abstract var childTasks : List<Task>
    override var id : UUID = UUID.randomUUID()
    open fun prepare(fetchedItems: Map<Id, Serializable?>){}
    open fun execute(){}
    open fun afterSuccess(){}
    open fun registerHandles(keyValueStore: KeyValueStore, queueConnector: QueueConnector, king : King){
        _king = king
        _store = keyValueStore
        _queue = queueConnector
    }

    lateinit var _king : King
    lateinit var _store : KeyValueStore
    lateinit var _queue : QueueConnector

}