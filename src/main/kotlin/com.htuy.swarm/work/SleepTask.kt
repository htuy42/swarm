package com.htuy.swarm.work

import com.htuy.swarm.coordination.QueueDomain
import com.htuy.swarm.data.Id
import java.io.Serializable

class SleepTask(override val domain: QueueDomain, val sleepFor : Long) : Task() {
    override val depedencies: List<Id> = listOf()
    override var yield: Map<Id, Serializable> = mapOf()
    override var childTasks: List<Task> =  listOf()


    override fun execute() {
        Thread.sleep(sleepFor)
    }

}