package com.htuy.swarm.synchro.singlesynchro

import com.htuy.common.Address
import com.htuy.common.Logging
import com.htuy.netlib.sockets.Socket
import com.htuy.swarm.SinglePointOfFailure
import com.htuy.swarm.data.Id
import com.htuy.swarm.management.Service
import java.io.Serializable


class SingleSynchroServer : Service, SinglePointOfFailure {
    override lateinit var socketAddress: Address

    override var done: Boolean = false

    val longValuedIds = HashMap<Id, Long>()
    val setValuedIds = HashMap<Id, MutableSet<Serializable>>()
    val barriers = HashMap<Id, MutableSet<Address>>()
    val triggeredTriggers = HashSet<Id>()
    val triggerWaiters = HashMap<Id,MutableSet<Address>>()

    fun modifyGetLong(id : Id, toDo: (Long) -> Long) : Long{
        synchronized(longValuedIds) {
            val fromIds = longValuedIds.getOrPut(id) {0L}
            longValuedIds[id] = toDo(fromIds)
            return longValuedIds[id]!!
        }
    }

    override fun register(socket: Socket) {
        socket.registerTypeListener(GetAndIncrRequest::class.java) {
            AtomicLongResponse(modifyGetLong(it.id){it + 1})
        }
        socket.registerTypeListener(GetAndIncrSetRequest::class.java){ request ->
            lateinit var set : MutableSet<Serializable>
            synchronized(setValuedIds){
                set = setValuedIds.getOrPut(request.id) {HashSet()}
            }
            synchronized(set){
                set.addAll(request.toAdd)
                GetAndIncrSetResponse(set)
            }
        }
        socket.registerTypeListener(GetAndSetRequest::class.java){ request ->
            AtomicLongResponse(modifyGetLong(request.id){request.setTo})
        }
        socket.registerTypeListener(GetAndMaxRequest::class.java){request ->
            AtomicLongResponse(modifyGetLong(request.id){java.lang.Long.max(it,request.toMaxWith)})
        }
        socket.registerTypeListener(BarrierWaitRequest::class.java){request ->
            lateinit var set : MutableSet<Address>
            synchronized(barriers){
                set = barriers.getOrPut(request.id) {HashSet()}
            }
            synchronized(set){
                if(set.size == request.waitForCount - 1){
                    triggerBarrier(set,request)
                    BarrierWaitResponse(request.id,true,request.waitForCount)
                } else {
                    set.add(request.listeningAddress)
                    BarrierWaitResponse(request.id,false,null)
                }
            }
        }
        socket.registerTypeListener(TriggerRequest::class.java){request ->
            if(request.isTriggerActivation){
                synchronized(triggeredTriggers){
                    val needsToPerformTrigger = triggeredTriggers.contains(request.id)
                    triggeredTriggers.add(request.id)
                    if(needsToPerformTrigger){
                        triggerTrigger(request.id)
                        null
                    }
                }
                null
            } else {
                synchronized(triggeredTriggers){
                    if(request.id in triggeredTriggers){
                        TriggerResponse(request.id,true)
                    } else {
                        lateinit var set : MutableSet<Address>
                        synchronized(triggerWaiters){
                            set = triggerWaiters.getOrPut(request.id) {HashSet()}
                        }
                        synchronized(set){
                            set.add(request.ownAddress!!)
                        }
                        TriggerResponse(request.id,false)
                    }
                }
            }
        }
    }

    private fun triggerTrigger(id: Id) {
        for(elt in triggerWaiters[id]?:return){
            elt.tryPush(TriggerResponse(id,true)){
                Logging.getLogger().warn { "Tried to send trigger update to a node but couldn't reach it" }
            }
        }
    }

    fun triggerBarrier(barrierSet : Set<Address>, lastRequest : BarrierWaitRequest) {
        for((index, address) in barrierSet.withIndex()){
            address.tryPush(BarrierWaitResponse(lastRequest.id,true,index)){
                Logging.getLogger().error { "One of the barrier waiters was unable to accept our push!" }
            }
        }
        barriers.remove(lastRequest.id)
    }
}