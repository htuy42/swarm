package com.htuy.swarm.coordination

data class QueueConfig(val allowWrapping : Boolean, var packetSize : Int?, val resendOutItems: Boolean, val desiredTimePer : Int){

    companion object {
        fun default() : QueueConfig{
            return QueueConfig(false,null,true,-1)
        }
    }

    fun copy() : QueueConfig{
        return QueueConfig(allowWrapping,packetSize,resendOutItems,desiredTimePer)
    }
}