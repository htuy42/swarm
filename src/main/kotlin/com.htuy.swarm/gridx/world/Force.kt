package com.htuy.swarm.gridx.world

import java.io.Serializable

interface Force : Serializable, BlockItem, Tickable{
    override val properties : Map<String,Serializable>
}