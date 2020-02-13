package com.htuy.swarm.management

import com.htuy.common.Address
import com.htuy.netlib.sockets.Socket

interface Service{
    fun register(socket : Socket)
    fun passArgs(args: Array<String>){}
    fun startService(){}
    var done : Boolean
    var socketAddress : Address
}