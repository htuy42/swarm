package com.htuy.swarm.work

import com.htuy.swarm.coordination.QueueConnector
import com.htuy.swarm.data.Id
import com.htuy.swarm.data.KeyValueStore
import com.htuy.swarm.management.King
import kotlinx.coroutines.experimental.runBlocking

abstract class CancellableTask(
    val cancelId : Id
) : Task(){

    var cancelled : Boolean = false

    override fun registerHandles(keyValueStore: KeyValueStore, queueConnector: QueueConnector, king : King) = runBlocking{
        super.registerHandles(keyValueStore, queueConnector, king)
        val cancelledFromStore = keyValueStore.get(cancelId)
        if(cancelledFromStore is Boolean){
            synchronized(cancelled) {
                cancelled = cancelledFromStore
            }
        }
        keyValueStore.watch(cancelId){
            synchronized(cancelled) {
                it as Boolean
                cancelled = true
            }
        }
    }

    fun cancelSelf() = runBlocking{
        cancelled = true
        _store.put(cancelId,true)
    }

}