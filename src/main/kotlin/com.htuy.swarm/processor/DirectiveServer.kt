package com.htuy.swarm.processor

import com.htuy.common.Address
import com.htuy.common.ObjectSerializer
import com.htuy.netlib.sockets.Socket
import com.htuy.swarm.coordination.singlequeue.SingleQueueClient
import com.htuy.swarm.coordination.singlequeue.SingleQueueServer
import com.htuy.swarm.data.StringId
import com.htuy.swarm.data.singlestore.SingleStoreClient
import com.htuy.swarm.management.King
import com.htuy.swarm.management.Service
import com.htuy.swarm.synchro.singlesynchro.SingleSynchroClient
import kotlinx.coroutines.experimental.runBlocking
import java.io.FileReader

class DirectiveServer(val king : King) : Service {
    override lateinit var socketAddress: Address

    override var done: Boolean = false

    lateinit var directive : Directive

    override fun passArgs(args: Array<String>) = runBlocking{
        val queue = SingleQueueClient(king)
        val store = SingleStoreClient(king)
        val synchro = SingleSynchroClient(king)
        val directiveString = store.get(StringId(args[0])) as String
        directive = ObjectSerializer.stringToObject(directiveString) as Directive
        directive.queue = queue
        directive.store = store
        directive.synchro = synchro
    }

    override fun startService() {
        directive.perform()
        done = true
    }

    override fun register(socket: Socket) {
        // we don't actually send any information to anyone or expect to receive any. All we do is run whatever directive we were given, once, and then quit.
    }
}