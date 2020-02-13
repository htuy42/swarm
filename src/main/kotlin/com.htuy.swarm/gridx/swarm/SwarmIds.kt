package com.htuy.swarm.gridx.swarm

import com.htuy.swarm.coordination.QueueDomain
import com.htuy.swarm.data.Id
import com.htuy.swarm.gridx.WORLD_MAJOR_STEP_SIZE
import com.htuy.swarm.gridx.world.Block
import com.htuy.swarm.gridx.world.MessageList
import java.awt.Point
import java.io.Serializable

interface SwarmId : Id

data class SwarmDomain(val i : Int) : QueueDomain{
    //todo
}

data class SwarmBlockId(val location : Point, val generation : Long) : SwarmId{
    fun getDomain() : QueueDomain{
        return SwarmDomain(1)
    }

    override fun fallback(): Id? {
        if(generation % WORLD_MAJOR_STEP_SIZE != 0L){
            return SwarmBlockId(location, generation - 1)
        } else {
            return null
        }
    }

    override fun fallthru(): Serializable {
        return Block.emptyBlock(this)
    }
}

data class SwarmMessagePacketId(val from : Point, val to : Point, val generation : Long) : SwarmId{
    override fun fallback() : Id?{
        return null
    }

    override fun fallthru(): Serializable {
        return MessageList(listOf())
    }
}