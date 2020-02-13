package com.htuy.swarm.extending

import com.htuy.common.Address
import com.htuy.swarm.management.*

fun extensionChildMain(args : Array<String>, extraServices : Map<ServiceType,(King, Address?) -> Service> = mapOf()){
    innerMain(args,extraServices)
}

