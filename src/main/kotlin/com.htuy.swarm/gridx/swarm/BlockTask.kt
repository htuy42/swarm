package com.htuy.swarm.gridx.swarm

import com.htuy.swarm.coordination.QueueDomain
import com.htuy.swarm.data.Id
import com.htuy.swarm.gridx.world.Block
import com.htuy.swarm.gridx.world.Message
import com.htuy.swarm.gridx.world.MessageList
import com.htuy.swarm.work.Task
import java.awt.Point
import java.io.Serializable
import java.util.*

class BlockTask(val gridId: SwarmBlockId) : Task() {
    override val depedencies: List<SwarmId>
    override lateinit var yield: Map<Id, Serializable>
    override lateinit var childTasks: List<Task>
    override val domain: QueueDomain = gridId.getDomain()

    lateinit var messages : List<Message>
    lateinit var block : Block
    val createdMessages : MutableList<Message> = mutableListOf()



    init{
        val depList = ArrayList<SwarmId>()
        depList.add(gridId)
        val point = gridId.location
        for(x in -1..1){
            for(y in -1..1){
                depList.add(SwarmMessagePacketId(Point(point.x + x, point.y + y),point,gridId.generation))
            }
        }
        depedencies = depList
    }

    override fun prepare(fetchedItems: Map<Id, Serializable?>) {
        val fetchdMessages = ArrayList<Message>()
        for(elt in depedencies){
            val fetchd = fetchedItems[elt]
            if(elt is SwarmMessagePacketId){
                if(fetchd != null && fetchd is MessageList){
                    fetchdMessages.addAll(fetchd.messages)
                }
            } else if(elt is SwarmBlockId){
                if(fetchd != null && fetchd is Block){
                    block = fetchd
                } else {
                    block = Block.emptyBlock(elt)
                }
            }
        }
    }

    override fun execute() {
        for(elt in messages){
            elt.execute(block)
        }
        for(elt in block.forces){
            elt.onTick(block)
        }
        for(elt in block.entities){
            elt.onTick(block)
        }
        this.yield = createdMessages.associate { it.to to it }
    }

    override fun afterSuccess() {
        // pass
    }
}