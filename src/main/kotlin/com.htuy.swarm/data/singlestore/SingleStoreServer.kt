package com.htuy.swarm.data.singlestore

import com.htuy.common.Address
import com.htuy.common.Logging
import com.htuy.netlib.sockets.InternetSockets
import com.htuy.netlib.sockets.Socket
import com.htuy.swarm.SinglePointOfFailure
import com.htuy.swarm.data.Id
import com.htuy.swarm.management.King
import com.htuy.swarm.management.Service
import com.htuy.swarm.management.ServiceType
import java.util.concurrent.ConcurrentHashMap

class SingleStoreServer : Service, SinglePointOfFailure {
    override lateinit var socketAddress: Address

    override var done: Boolean = false
    private val map = ConcurrentHashMap<Id, Address>()
    private val watches = ConcurrentHashMap<Id,MutableSet<Address>>()
    override fun register(socket: Socket) {
        Logging.getLogger().debug{"Registering Store server"}
        socket.registerTypeListener(PutIdMessage::class.java) {
            Logging.getLogger().debug { "Putting ${it.id} @ ${it.host}" }
            map[it.id] = it.host
            val set = watches[it.id]
            if(set != null){
                synchronized(set){
                    for(elt in set){
                        elt.tryPush(WatchTriggerMessage(it.id)){
                            Logging.getLogger().warn{"Wasn't able to callback $elt on callback for ${it.id}"}
                        }
                    }
                }
            }
            // just send back the message, as confirmation
            it
        }
        socket.registerTypeListener(WatchRegisterMessage::class.java){
            Logging.getLogger().debug{"Registering watch on ${it.id}"}
            val set = watches.getOrPut(it.id){HashSet<Address>()}
            synchronized(set){
                set.add(it.callbackAddres)
            }
            null
        }
        socket.registerTypeListener(RequestIdMessage::class.java) {
            Logging.getLogger().debug { "Getting ${it.id}" }
            var res = map[it.id]
            var fallback = it.id.fallback()
            while (res == null) {
                Logging.getLogger().debug { "Have to use fallback of $fallback" }
                if (fallback != null) {
                    res = map[fallback]
                    fallback = fallback.fallback()
                } else {
                    return@registerTypeListener IdHostMessage(it.id, null)
                }
            }
            Logging.getLogger().debug{"Returning $res for ${it.id}"}
            IdHostMessage(it.id, res)
        }
        socket.registerTypeListener(UnputIdMessage::class.java) {
            Logging.getLogger().debug{"Unputting ${it.id}"}
            map.remove(it.id)
        }
    }
}