package com.htuy.swarm.gridx.world

import java.awt.Point
import java.io.Serializable

interface Entity:Serializable, BlockItem, Tickable{
    override val properties : Map<String,Serializable>
    val location : Point
    fun inCell(cell : Point): Boolean
}