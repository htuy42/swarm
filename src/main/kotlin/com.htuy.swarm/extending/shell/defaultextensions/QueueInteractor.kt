package com.htuy.swarm.extending.shell.defaultextensions

import com.htuy.shell.ShellCommand
import com.htuy.shell.ShellModule
import com.htuy.swarm.work.agent.DomainServeRequest
import com.htuy.swarm.coordination.StringDomain
import com.htuy.swarm.coordination.QueueConnector
import com.htuy.swarm.coordination.singlequeue.SingleQueueClient
import com.htuy.swarm.extending.shell.SwarmModule
import com.htuy.swarm.management.AGENT_SERVICE_TYPE
import com.htuy.swarm.management.King
import com.htuy.swarm.management.onetorule.Leader


internal class QueueInteractor() : SwarmModule(){
    lateinit var queue : QueueConnector
    override val mainShellModule: ShellModule = object : ShellModule {
    override val name: String = "QueueInteractor"
        override val submodules: Map<String, ShellModule> = mapOf()
        override fun getCommands(): List<ShellCommand> = listOf(QueueSizeCommand(),MakeAgentsCommand(),WorkDomainsCommand())
    }

    override fun setup(king: King) {
        super.setup(king)
        queue = SingleQueueClient(king)
    }

    inner class QueueSizeCommand : ShellCommand(){
        override val description: String = "Gets the number of items currently being worked"
        override val name: String = "queue_size"
        override val aliases: List<String> = listOf("q_size")
        override val argListSize: Int = 0
        override suspend fun execute(input: List<String>): String {
            return "the size of the queue is for ${queue.getSize()}"
        }
    }

    inner class MakeAgentsCommand : ShellCommand(){
        override val description: String = "Request [count] simple agents to begin servicing the queue. Note that it is necessary to use work_domains to get them to actually start doing anything."
        override val name: String = "make_agents"
        override val argListSize: Int = 1
        override suspend fun execute(input: List<String>): String {
            king.startService(AGENT_SERVICE_TYPE,input[0].toInt(),"")
            return "Requested the creation of ${input[0]} agents."
        }
    }

    inner class WorkDomainsCommand : ShellCommand(){
        override val description: String = "Request all of our agents to start working the given [domain]'s. Note that they will *only* work these domains, not any others, until instructed otherwises. IE its substituting not adding."
        override val name: String = "work_domains"
        override suspend fun execute(input: List<String>): String {
            king.sendRequestToHostLeadServices(AGENT_SERVICE_TYPE,DomainServeRequest(input.map{ StringDomain(it) }))
            return "Sent request to serve $input to all the connected agents"
        }
    }
}