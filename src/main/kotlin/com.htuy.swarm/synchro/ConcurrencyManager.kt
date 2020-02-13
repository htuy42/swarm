package com.htuy.swarm.synchro

import com.htuy.swarm.data.Id
import nl.komponents.kovenant.Promise
import java.io.Serializable
import java.lang.Exception

interface ConcurrencyManager{

    fun atomicGetAndIncrement(forId : Id) : Long

    fun atomicSetGetAndAdd(forId : Id, toAdd : Set<Serializable>) : Set<Serializable>

    fun atomicGetAndSet(forId: Id, setTo: Long): Long

    fun atomicGetAndMax(forId : Id, testVal : Long) : Long

    /**
     * Wait on a barrier of the given id until at least count other mans are waiting there.
     * Then, go, and get a unique id (no other man at the barrier will have the same one).
     * As soon as the barrier releases, it can be reused
     */
    fun barrierAtWithId(forId : Id, waitForCount : Int) : Promise<Int,Exception>

    /**
     * Wait for the trigger to be triggered. A trigger cannot safely be reset: it may be triggered only once
     */
    fun waitForTrigger(triggerId : Id) : Promise<Unit,Exception>

    fun triggerTrigger(triggerId : Id)

}