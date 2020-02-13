package com.htuy.swarm.gridx.world

import com.htuy.swarm.gridx.swarm.SwarmBlockId
import java.io.Serializable

interface Message:Serializable{
    fun execute(block : Block)

    val from : SwarmBlockId
    val to : SwarmBlockId
}

data class MessageList(val messages : List<Message>) : Serializable