package com.htuy.swarm.work.agent

import com.htuy.swarm.coordination.QueueDomain
import java.io.Serializable

data class DomainServeRequest(val domains : List<QueueDomain>) : Serializable