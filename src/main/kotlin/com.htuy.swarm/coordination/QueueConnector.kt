package com.htuy.swarm.coordination

import com.htuy.swarm.coordination.singlequeue.DEFAULT_ITEMS_AT_A_TIME
import com.htuy.swarm.work.Task
import java.util.*

interface QueueConnector{

    fun put(items : List<QueueItem>)

    fun put(item : QueueItem){
        put(listOf(item))
    }

    fun yield(items : List<QueueItem>, yieldFrom : UUID)

    fun poll(domains : List<QueueDomain>, desiredItemCount : Int = DEFAULT_ITEMS_AT_A_TIME) : List<QueueItem>

    fun bulkFinishAndPoll(domains : List<QueueDomain>, finishedItems : Map<UUID,List<QueueItem>>,desiredItemCount: Int = DEFAULT_ITEMS_AT_A_TIME) : List<QueueItem>

    fun configureDomains(domains : List<QueueDomain>, config : QueueConfig)

    fun finish(items : List<UUID>)

    fun getSize() : Int
}