package com.htuy.swarm.coordination

import java.io.Serializable
import java.util.*

interface QueueItem : Serializable{
    val domain : QueueDomain
    var id : UUID
}