package com.htuy.swarm.gridx.world

import java.awt.Point
import java.io.Serializable

data class Cell(override val properties : Map<String,Serializable>, val location : Point) : Serializable, BlockItem