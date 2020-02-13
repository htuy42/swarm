package com.htuy.swarm.work

import com.htuy.swarm.coordination.QueueDomain
import com.htuy.swarm.data.Id
import java.io.Serializable

class LinkTasks(val tasks : List<Task>) : Task (){
    override val depedencies: List<Id> = tasks[0].depedencies
    override var yield: Map<Id, Serializable> = tasks[0].yield
    override var childTasks: List<Task> = listOf()
    override val domain: QueueDomain = tasks[0].domain

    override fun prepare(fetchedItems: Map<Id, Serializable?>) {
        tasks[0].prepare(fetchedItems)
    }

    override fun execute() {
        tasks[0].execute()
        setupChildTasks()
    }

    fun setupChildTasks(){
        val newTaskList = mutableListOf<Task>()
        newTaskList.addAll(tasks[0].childTasks)
        if(tasks.size > 2){
            newTaskList.add(LinkTasks(tasks.drop(1)))
        } else if(tasks.size == 2){
            newTaskList.add(tasks[1])
        }
        childTasks = newTaskList
    }


    override fun afterSuccess() {
        tasks[0].afterSuccess()
    }
}