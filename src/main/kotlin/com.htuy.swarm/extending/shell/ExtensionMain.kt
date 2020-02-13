package com.htuy.swarm.extending.shell

import com.htuy.common.Configurator
import com.htuy.swarm.extending.shell.defaultextensions.QueueInteractor
import com.htuy.swarm.extending.shell.defaultextensions.StoreInteractor
import kotlinx.coroutines.experimental.runBlocking

fun extensionMain(modules: List<SwarmModule>) = runBlocking {
    Configurator().run()
    val allModules = HashMap<String, SwarmModule>()
    val qi = QueueInteractor()
    allModules.put(qi.mainShellModule.name, qi)
    val si = StoreInteractor()
    allModules.put(si.mainShellModule.name, si)
    modules.forEach {
        allModules.put(it.mainShellModule.name,it)
    }
    ClientShell(allModules).kShell.run()
}