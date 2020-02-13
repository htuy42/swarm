package com.htuy.swarm.data.singlestore

import com.htuy.common.Address
import com.htuy.common.Logging
import com.htuy.netlib.sockets.InternetSockets
import com.htuy.swarm.data.Id
import com.htuy.swarm.data.KeyValueStore
import com.htuy.swarm.management.King
import com.htuy.swarm.management.STORE_SERVICE_TYPE
import com.htuy.swarm.management.ServiceType
import kotlinx.coroutines.experimental.launch
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap

class SingleStoreClient(private val king: King) : KeyValueStore {


    private val localMap = ConcurrentHashMap<Id, Serializable>()

    private val ownAddress = Address.anyPortLocal()

    private val watches = ConcurrentHashMap<Id, (Serializable) -> Unit>()

    init {
        InternetSockets().listenOn(ownAddress) {
            it.registerTypeListener(RequestValueMessage::class.java) {
                Logging.getLogger().debug { "Value requested from me!" }
                val fromMap = localMap[it.id]
                if (fromMap != null) {
                    Logging.getLogger().debug { "I found the value locally: ${it.id}" }
                    ValueMessage(it.id, fromMap)
                } else {
                    Logging.getLogger().warn { "I didn't have the value someone asked for: ${it.id}" }
                    MissingMessage(it.id)
                }
            }
            it.registerTypeListener(WatchTriggerMessage::class.java) {
                Logging.getLogger().debug { "Got a watch trigger for ${it.id}" }
                val watch = watches[it.id]
                if (watch != null) {
                    launch {
                        val value = get(it.id)
                        Logging.getLogger().debug{"Triggering the callback for ${it.id}, with value $value"}
                        watch.invoke(value)
                    }
                } else {
                    Logging.getLogger().warn {
                        "Didn't actually have a callback for ${it.id}"
                    }
                }
                null
            }
        }
    }

    override suspend fun watch(key: Id, callback: (Serializable) -> Unit) {
        watches[key] = callback
        king.tryPushToServiceRetry(STORE_SERVICE_TYPE, WatchRegisterMessage(key,ownAddress))
        Logging.getLogger().debug { "Locally registered callback for key $key" }
    }

    private suspend fun getKeyHost(key: Id): Address? {
        Logging.getLogger().debug { "Getting host for key $key" }
        return (king.tryGetSynchToServiceRetry(STORE_SERVICE_TYPE, RequestIdMessage(key)) as IdHostMessage).host
    }

    override suspend fun get(key: Id): Serializable {
        Logging.getLogger().debug { "Looking up $key" }
        val fromMap = localMap[key]
        if (fromMap != null) {
            return fromMap
        }
        val host = getKeyHost(key) ?: return key.fallthru()
        val res = host.tryGetSynch(RequestValueMessage(key)) {
            null
        } ?: return key.fallthru()
        if (res is MissingMessage) {
            Logging.getLogger().warn { "Remote host didn't have it, using fallthru" }
            return key.fallthru()
        }
        Logging.getLogger().debug { "Got the result: $res" }
        res as ValueMessage
        return res.value
    }

    override suspend fun put(key: Id, item: Serializable) {
        Logging.getLogger().debug { "Attempting put for $key" }
        localMap[key] = item
        king.tryGetSynchToServiceRetry(STORE_SERVICE_TYPE, PutIdMessage(key, ownAddress))
    }

    override suspend fun unput(key: Id) {
        Logging.getLogger().debug { "Attempting unput for $key" }
        localMap.remove(key)
        king.tryPushToServiceRetry(STORE_SERVICE_TYPE, UnputIdMessage(key))
    }
}