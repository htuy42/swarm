package com.htuy.swarm.extending.shell

import com.htuy.common.Address
import com.htuy.shell.KShell
import com.htuy.shell.ShellCommand
import com.htuy.shell.ShellModule
import com.htuy.swarm.management.King
import com.htuy.swarm.management.onetorule.Leader
import com.htuy.swarm.management.onetorule.SingleKingClient
import com.htuy.swarm.management.onetorule.SingleKingServer

internal class ClientShell(val swarmSubs: Map<String, SwarmModule>) : ShellModule{
    override val submodules: Map<String, ShellModule> = mapOf()
    internal var kingHost : SingleKingServer? = null
    override val name: String = "swarm_client"
    override fun getCommands(): List<ShellCommand> = listOf(SwarmAddCommand(),LaunchLocalCommand(),ConnectCommand(),SwarmListCommand())
    internal val kShell : KShell = KShell()
    lateinit var king : King
    init{
        kShell.registerModule("swarm_client",this)
    }
    inner class SwarmAddCommand: ShellCommand() {
        override val aliases: List<String> = listOf("sadd")
        override val description: String = "Add a swarm module, given its [name]. Note that this must be used instead of madd for swarm modules, and that connecting must happen first. To add all available modules, use sadd all"
        override val name: String = "swarm_add"
        override val argListSize: Int = 1

        override suspend fun execute(input: List<String>): String {
            if(input[0] == "all"){
                for(mod in swarmSubs.values){
                    mod.setup(king)
                    kShell.registerModule(mod.mainShellModule.name, mod.mainShellModule)
                }
                return "Added all available modules"
            }
            val module = swarmSubs[input[0]]

            if(module == null){
                return "Couldnt find a module named ${input[0]}. Had ${submodules.keys}"
            } else {
                module.setup(king)
                kShell.registerModule(module.mainShellModule.name, module.mainShellModule)
            }
            return "Successfully added ${input[0]} module."
        }
    }
    inner class SwarmListCommand : ShellCommand(){
        override val description: String = "List the swarm modules that are available."
        override val name: String = "swarm_list"
        override val argListSize: Int = 0

        override suspend fun execute(input: List<String>): String {
            return "Swarm Modules: ${swarmSubs.keys.joinToString(separator = ", ")}"
        }
    }
    inner class LaunchLocalCommand : ShellCommand(){
        override val description: String = "Launch and connect to a local king host. Takes [port] and [child_server_name] (optional, defaults to child_server"
        override val name: String = "launch_local"

        override suspend fun execute(input: List<String>): String {
            if(input.size < 1){
                return "Not long enough input: $description"
            }
            val port = input[0].toInt()
            var childServerName = "child_server"
            if(input.size >= 2){
                childServerName = input[1]
            }
            val server = SingleKingServer(port = port, childName =  childServerName)
            server.start()
            kingHost = server
            king = SingleKingClient(Address.withPortLocal(port))
            return "Started host on $port and connected to it. Using child server name $childServerName."
        }

    }
    inner class ConnectCommand() : ShellCommand(){
        override val description: String = "Connect to a remote king host. Takes [addr] and [port]"
        override val name: String = "connect"
        override val argListSize: Int = 2
        override suspend fun execute(input: List<String>): String {
            king = SingleKingClient(Address(input[0],input[1].toInt()))
            return "Made a king to ${input[0]} : ${input[1]}"
        }
    }
}





