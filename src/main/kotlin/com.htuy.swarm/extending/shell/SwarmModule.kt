package com.htuy.swarm.extending.shell
import com.htuy.shell.ShellModule
import com.htuy.swarm.management.King
import com.htuy.swarm.management.onetorule.Leader

abstract class SwarmModule{
    lateinit var king : King
    open fun setup(king : King){
        this.king = king
    }
    abstract val mainShellModule : ShellModule
}