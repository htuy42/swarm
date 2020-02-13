package com.htuy.swarm.coordination

import java.io.Serializable

interface QueueDomain : Serializable

data class StringDomain(val name : String) : QueueDomain

val DEFAULT_DOMAIN = StringDomain("___NO_DOMAIN___")