package com.htuy.swarm.gridx.world

import com.htuy.swarm.gridx.swarm.SwarmBlockId
import java.awt.Point
import java.io.Serializable

data class Block(val entities: List<Entity>,
                 val cells: Map<Point, Cell>,
                 val forces: List<Force>,
                 val location: SwarmBlockId) : Serializable {
    companion object {

        fun emptyBlock(id: SwarmBlockId): Block {
            return Block(listOf(), mapOf(), listOf(), id)
        }
    }
}
