package com.htuy.swarm.management.onetorule

import com.htuy.common.Address
import com.htuy.concurrent.TimeoutMap
import com.htuy.netlib.sockets.InternetSockets
import com.htuy.netlib.sockets.Socket
import com.htuy.swarm.management.ServiceType
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap


class Leader(val king: SingleKingServer, expectedPingFrequency: Long = 30000L) {

    val requestedServices = TimeoutMap(king.waitForServerToBoot, handler = ::serviceTimedOut)

    val aliveServices = TimeoutMap(expectedPingFrequency, handler = ::serviceTimedOut)

    val leaderAddress: Address = Address.anyPortLocal()

    val followers = ConcurrentHashMap<ServiceType, MutableList<Socket>>()

    fun start() {
        InternetSockets().listenOn(leaderAddress) {
            val sock = it
            var id: String? = null
            var request: MakeServiceRequest? = null
            it.registerTypeListener(FollowerRegister::class.java) {
                val lst = followers.getOrPut(it.type) {
                    ArrayList()
                }
                synchronized(lst) {
                    lst.add(sock)
                }
                id = it.id
                synchronized(this) {
                    if (id in requestedServices) {
                        request = requestedServices[it.id]!!
                    }
                }
                idRegister(it.id)
            }
            it.registerTypeListener(LeadServicePing::class.java) {
                if (id == null || request == null) {
                    return@registerTypeListener LeadServicePing(false)
                }
                synchronized(this) {
                    if(id in requestedServices){
                        requestedServices.remove(id!!)
                    } else {
                        return@registerTypeListener LeadServicePing(false)
                    }
                    aliveServices.put(id!!, request!!)

                }
                it
            }
            it.registerTypeListener(LeadServiceFinish::class.java) {
                synchronized(this) {
                    aliveServices.remove(id)
                    requestedServices.remove(id)
                }
            }
        }
    }

    fun serviceTimedOut(id: String, request: MakeServiceRequest) {
        synchronized(this) {
            requestedServices.remove(id)
            aliveServices.remove(id)
        }
        king.handleServiceRequest(request, 1)
    }

    // ensure we actually get the services
    fun notifyNewService(id: String, request: MakeServiceRequest) {
        synchronized(this) {
            requestedServices[id] = request
        }
    }

    fun sendToFollowersFor(type: ServiceType, request: Serializable) {
        val lst = followers[type] ?: return
        synchronized(this) {
            val iter = lst.iterator()
            iter.forEach {
                if (it.isOpen()) {
                    it.tryPush(request) {
                        iter.remove()
                    }
                } else {
                    iter.remove()
                }
            }
        }
    }

    fun idRegister(id: String): Boolean {
        synchronized(this) {
            return if (id !in requestedServices) {
                false
            } else {
                aliveServices[id] = requestedServices[id]!!
                requestedServices.remove(id)
                true
            }
        }
    }
}