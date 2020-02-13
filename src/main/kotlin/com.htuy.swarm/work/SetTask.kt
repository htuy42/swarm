package com.htuy.swarm.work

import com.htuy.swarm.coordination.QueueDomain
import com.htuy.swarm.data.Id
import java.io.Serializable

class SetTask(val key : Id, val value : Serializable, override val domain : QueueDomain) : Task(){
    override val depedencies: List<Id> = listOf()
    override var yield: Map<Id, Serializable> = mapOf(key to value)
    override var childTasks: List<Task> = listOf()


}