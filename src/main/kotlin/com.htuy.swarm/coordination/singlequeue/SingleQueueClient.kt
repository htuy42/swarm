package com.htuy.swarm.coordination.singlequeue

import com.htuy.common.Logging
import com.htuy.swarm.coordination.QueueConfig
import com.htuy.swarm.coordination.QueueDomain
import com.htuy.swarm.coordination.QueueConnector
import com.htuy.swarm.coordination.QueueItem
import com.htuy.swarm.management.King
import com.htuy.swarm.management.QUEUE_SERVICE_TYPE
import kotlinx.coroutines.experimental.runBlocking
import java.util.*

class SingleQueueClient(val king : King) : QueueConnector{
    override fun bulkFinishAndPoll(
        domains: List<QueueDomain>,
        finishedItems: Map<UUID, List<QueueItem>>,
        desiredItemCount: Int
    ): List<QueueItem> = runBlocking{
        return@runBlocking (king.tryGetSynchToServiceRetry(QUEUE_SERVICE_TYPE,QueueBulkYieldMessage(finishedItems,domains,desiredItemCount))as QueueResponseMessage).items
    }

    override fun configureDomains(domains: List<QueueDomain>, config: QueueConfig) = runBlocking{
        king.tryPushToServiceRetry(QUEUE_SERVICE_TYPE,QueueConfigMessage(domains,config))
    }

    override fun yield(items: List<QueueItem>, yieldFrom: UUID) = runBlocking{
        king.tryPushToServiceRetry(QUEUE_SERVICE_TYPE,QueueYieldAddMessage(items,yieldFrom))
    }

    override fun finish(items: List<UUID>) = runBlocking {
        king.tryPushToServiceRetry(QUEUE_SERVICE_TYPE,QueueFinishMessage(items))
    }

    override fun getSize() : Int = runBlocking{
        val left = king.tryGetSynchToServiceRetry(QUEUE_SERVICE_TYPE,TasksLeftRequest())
        left as TasksLeftResponse
        return@runBlocking left.count
    }

    override fun put(items: List<QueueItem>) = runBlocking{
        Logging.getLogger().debug { "Putting items: $items" }
        king.tryPushToServiceRetry(QUEUE_SERVICE_TYPE,QueueAddMessage(items))
    }

    override fun poll(domains: List<QueueDomain>, desiredItemCount: Int): List<QueueItem> = runBlocking{
        Logging.getLogger().debug{"Polling domains: ${QueuePollMessage(domains,desiredItemCount)}"}
        return@runBlocking (king.tryGetSynchToServiceRetry(QUEUE_SERVICE_TYPE,QueuePollMessage(domains,desiredItemCount)) as QueueResponseMessage).items
    }
}