package com.htuy.swarm.synchro.singlesynchro

import com.htuy.common.Address
import com.htuy.netlib.sockets.InternetSockets
import com.htuy.swarm.data.Id
import com.htuy.swarm.management.King
import com.htuy.swarm.management.SYNCHRO_SERVICE_TYPE
import com.htuy.swarm.synchro.ConcurrencyManager
import kotlinx.coroutines.experimental.runBlocking
import nl.komponents.kovenant.Deferred
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import java.io.Serializable

class SingleSynchroClient(val king : King) : ConcurrencyManager {


    val localListeningAddress = Address.anyPortLocal()

    val barrierWaits = HashMap<Id,Deferred<Int,Exception>>()

    val triggerWaits = HashMap<Id,Deferred<Unit,java.lang.Exception>>()

    // where BarrierWaitRequest responses should go in the case that they appear after our intial request and before
    // we register a wait for them
    val barrierEarlies = HashMap<Id,Int>()

    val triggerEarlies = HashSet<Id>()

    init{
        InternetSockets().listenOn(localListeningAddress){
            it.registerTypeListener(BarrierWaitResponse::class.java){
                synchronized(localListeningAddress){
                    val wait = barrierWaits[it.id]
                    if(wait != null){
                        wait.resolve(it.returnId!!)
                        barrierWaits.remove(it.id)
                    } else {
                        barrierEarlies[it.id] = it.returnId!!
                    }
                    null
                }
            }
            it.registerTypeListener(TriggerResponse::class.java){
                synchronized(localListeningAddress){
                    val trigger = triggerWaits[it.id]
                    if(trigger != null){
                        trigger.resolve(Unit)
                        barrierWaits.remove(it.id)
                    } else {
                        triggerEarlies.add(it.id)
                    }
                    null
                }
            }
        }
    }

    override fun barrierAtWithId(forId: Id, waitForCount: Int): Promise<Int, Exception> = runBlocking{
        val response = king.tryGetSynchToServiceRetry(SYNCHRO_SERVICE_TYPE,BarrierWaitRequest(forId,waitForCount,localListeningAddress)) as BarrierWaitResponse
        val promise = deferred<Int,Exception>()
        synchronized(localListeningAddress) {
            if (response.ready) {
                promise.resolve(response.returnId!!)
            } else if(forId in barrierEarlies) {
                promise.resolve(barrierEarlies[forId]!!)
                barrierEarlies.remove(forId)
            } else {
                barrierWaits[forId] = promise
            }
        }

        return@runBlocking promise.promise
    }

    override fun triggerTrigger(triggerId: Id) = runBlocking{
        king.tryPushToServiceRetry(SYNCHRO_SERVICE_TYPE,TriggerRequest(triggerId,true))
    }

    override fun waitForTrigger(triggerId: Id): Promise<Unit, java.lang.Exception> = runBlocking{
        val promise = deferred<Unit,java.lang.Exception>()
        val response = king.tryGetSynchToServiceRetry(SYNCHRO_SERVICE_TYPE,TriggerRequest(triggerId,false)) as TriggerResponse
        if(response.hasTriggered){
        }
        synchronized(localListeningAddress){
            if (response.hasTriggered) {
                promise.resolve(Unit)
            } else if(triggerId in triggerEarlies) {
                promise.resolve(Unit)
                triggerEarlies.remove(triggerId)
            } else {
                triggerWaits[triggerId] = promise
            }
        }
        return@runBlocking promise.promise
    }

    override fun atomicGetAndIncrement(forId: Id): Long = runBlocking{
        return@runBlocking (king.tryGetSynchToServiceRetry(SYNCHRO_SERVICE_TYPE,GetAndIncrRequest(forId)) as AtomicLongResponse).value
    }

    override fun atomicSetGetAndAdd(forId: Id, toAdd: Set<Serializable>): Set<Serializable> = runBlocking{
        return@runBlocking (king.tryGetSynchToServiceRetry(SYNCHRO_SERVICE_TYPE,GetAndIncrSetRequest(forId,toAdd)) as GetAndIncrSetResponse).value
    }

    override fun atomicGetAndSet(forId : Id, setTo : Long) : Long = runBlocking{
        return@runBlocking (king.tryGetSynchToServiceRetry(SYNCHRO_SERVICE_TYPE,GetAndSetRequest(forId,setTo)) as AtomicLongResponse).value
    }

    override fun atomicGetAndMax(forId: Id, testVal: Long): Long = runBlocking{
        return@runBlocking (king.tryGetSynchToServiceRetry(SYNCHRO_SERVICE_TYPE,GetAndMaxRequest(forId,testVal)) as AtomicLongResponse).value

    }

}