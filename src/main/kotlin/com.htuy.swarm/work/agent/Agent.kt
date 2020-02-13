package com.htuy.swarm.work.agent

import com.htuy.common.Address
import com.htuy.common.Logging
import com.htuy.netlib.sockets.Socket
import com.htuy.swarm.coordination.QueueDomain
import com.htuy.swarm.coordination.QueueConnector
import com.htuy.swarm.coordination.QueueItem
import com.htuy.swarm.coordination.StringDomain
import com.htuy.swarm.coordination.singlequeue.SingleQueueClient
import com.htuy.swarm.data.KeyValueStore
import com.htuy.swarm.data.singlestore.SingleStoreClient
import com.htuy.swarm.management.King
import com.htuy.swarm.management.Service
import com.htuy.swarm.work.Task
import kotlinx.coroutines.experimental.runBlocking
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.concurrent.thread


// note there is no special requirement that these domains have the underscores or anything, its just to try to ensure
// that's not a name anyone will accidentally reuse. Perhaps there ought to be some global registry of these domains
// to ensure that can never happen, but for the moment there isn't
// todo ^
val DEFAULT_WORK_DOMAIN = StringDomain("___SIMPLE_AGENT_DEFAULT_DOMAIN___")

interface Agent {
    val store: KeyValueStore
    val queue: QueueConnector
    val domains: List<QueueDomain>

    fun prepareTask(task: Task)
    fun executeTask(task: Task)
    fun storeTask(task: Task)

    val currentYield : MutableMap<UUID,List<QueueItem>>

    fun start() = runBlocking{
        val pool = Executors.newFixedThreadPool(4)
        outer@ while (true) {
            Logging.getLogger().debug { "Running a task poll loop" }
            // 2 message trips for poll
            val tasks = queue.bulkFinishAndPoll(domains,currentYield)
            currentYield.clear()
            Logging.getLogger().debug { "Did the poll: $tasks" }
            if (tasks.size == 0) {
                Logging.getLogger().debug { "Didn't get any items from the queue, sleeping and then trying again" }
                Thread.sleep(1000)
                Logging.getLogger().debug { "Awoke from sleeping, polling again" }
                continue@outer
            }
            Logging.getLogger().debug { "Got some items. executing $tasks" }

            // allows batching of all the task puts at the end. Potentially look into doing a similar thing
            // for other elements (dependencies and results)
            val tasksFutures = tasks.map {
                val local = it
                if(local !is Task){
                    Logging.getLogger().error{"Non-task item $it in the task queue we were serving. Ignoring it, but this" +
                            "is dangerous: it will pollute the queue forever, and its probably not what you want anyways"}
                    null
                } else {
                    pool.submit {
                        Logging.getLogger().debug { "Preparing $local" }
                        // 0 message trips assuming no fetches needed
                        prepareTask(local)
                        Logging.getLogger().debug { "Executing $local" }
                        // 0 message trips
                        executeTask(local)
                        Logging.getLogger().debug { "Storing $local" }
                        // 0 message trips we need to wait for. Messages pushed rather than gotten
                        storeTask(local)
                        Logging.getLogger().debug { "Stored $local" }
                    }
                }
            }
            Logging.getLogger().debug { "Trying to join the futures: $tasksFutures" }
            tasksFutures.forEach { it?.get() }
            Logging.getLogger().debug { "Joined the futures" }
        }
    }

}


internal class SimpleAgent(val king: King) : Agent, Service {
    override lateinit var socketAddress: Address

    override var done: Boolean = false
    override val currentYield = ConcurrentHashMap<UUID,List<QueueItem>>()


    override val store: KeyValueStore = SingleStoreClient(king)
    override val queue: QueueConnector = SingleQueueClient(king)
    override val domains: MutableList<QueueDomain> = ArrayList()

    override fun startService(){
        domains.add(DEFAULT_WORK_DOMAIN)
        thread{
            start()
        }
    }

    override fun register(socket: Socket) {
        socket.registerTypeListener(DomainServeRequest::class.java) {
            domains.clear()
            domains.addAll(it.domains)
            null
        }
    }

    override fun prepareTask(task: Task) = runBlocking {
        task.registerHandles(store,queue,king)
        task.prepare(task.depedencies.associate { it to store.get(it) })
    }

    override fun executeTask(task: Task) {
        task.execute()
    }

    override fun storeTask(task: Task) = runBlocking {
        task.yield.keys.forEach {
            store.put(it, task.yield[it]!!)
        }
        currentYield[task.id] = task.childTasks
        task.afterSuccess()
    }

}