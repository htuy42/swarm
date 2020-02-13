package com.htuy.swarm.coordination.singlequeue

import com.htuy.swarm.coordination.QueueConfig
import com.htuy.swarm.coordination.QueueDomain
import com.htuy.swarm.coordination.QueueItem
import com.htuy.swarm.work.Task
import java.io.Serializable
import java.util.*

data class QueueAddMessage(val items: List<QueueItem>) : Serializable

// the items should only be added if we are the first person to report finishing yieldFrom
data class QueueYieldAddMessage(val items: List<QueueItem>, val yieldFrom: UUID) : Serializable

data class QueuePollMessage(val domains: List<QueueDomain>, val desiredItemCount: Int) : Serializable

data class QueueFinishMessage(val ids: List<UUID>) : Serializable

data class QueueBulkYieldMessage(
    val yield: Map<UUID, List<QueueItem>>,
    val domainsToPoll: List<QueueDomain>? = null,
    val desiredItemCount: Int? = null
) : Serializable

data class QueueResponseMessage(val items: List<QueueItem>) : Serializable

class TasksLeftRequest : Serializable

data class TasksLeftResponse(val count: Int) : Serializable

data class QueueConfigMessage(val domains: List<QueueDomain>, val config: QueueConfig) : Serializable