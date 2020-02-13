package com.htuy.swarm.extending.shell.defaultextensions

import com.htuy.shell.ShellCommand
import com.htuy.shell.ShellModule
import com.htuy.swarm.data.KeyValueStore
import com.htuy.swarm.data.StringId
import com.htuy.swarm.data.singlestore.SingleStoreClient
import com.htuy.swarm.extending.shell.SwarmModule
import com.htuy.swarm.management.King
import com.htuy.swarm.management.onetorule.Leader

internal class StoreInteractor() : SwarmModule(){
    lateinit var store : KeyValueStore
    override val mainShellModule: ShellModule = object : ShellModule{
        override val name: String = "StoreInteractor"
        override val submodules: Map<String, ShellModule> = mapOf()
        override fun getCommands(): List<ShellCommand> = listOf(GetIdCommand(),SetIdCommand())
    }

    override fun setup(king: King) {
        super.setup(king)
        store = SingleStoreClient(king)
    }

    inner class GetIdCommand() : ShellCommand(){
        override val description: String = "Goes to the global store and fetches a the value for the given [id]. Assumes the id is a string."
        override val name: String = "get_id"
        override val argListSize: Int = 1
        override suspend fun execute(input: List<String>): String {
            return "the value for ${input[0]} is ${store.get(StringId(input[0]))}"
        }
    }

    inner class SetIdCommand() : ShellCommand(){
        override val description: String = "Goes to the global store and set the given [id] to the given [value]. Assumes both are strings and treats them as such."
        override val name: String = "set_id"
        override val argListSize: Int = 2
        override suspend fun execute(input: List<String>): String {
            store.put(StringId(input[0]),input[1])
            return "Successfully set the value for ${input[0]} to ${input[1]}. Note that both values were treated as strings."
        }
    }
}