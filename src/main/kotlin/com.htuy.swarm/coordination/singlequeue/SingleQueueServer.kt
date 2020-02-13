package com.htuy.swarm.coordination.singlequeue

import com.htuy.common.Address
import com.htuy.common.Logging
import com.htuy.common.ThreadSafeTimeTrigger
import com.htuy.netlib.sockets.Socket
import com.htuy.swarm.SinglePointOfFailure
import com.htuy.swarm.coordination.QueueConfig
import com.htuy.swarm.coordination.QueueDomain
import com.htuy.swarm.coordination.QueueItem
import com.htuy.swarm.management.Service
import java.lang.Integer.max
import java.lang.Integer.min
import java.lang.Math.ceil
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.LongAdder
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

// note that this uses an at least once setup. It'll keep reassigning items until someone finishes them.
// duplicate items might double finish or something like that

// Note that currently there is no danger of sending an individual agent duplicate work items, because
// they serve a full set of items before asking for more, but if that behavior is changed then a check for that
// might need to be implemented
class SingleQueueServer : Service, SinglePointOfFailure {

    override lateinit var socketAddress: Address
    override var done = false
    // todo mechanism to try to track not only time *out* for tasks, but also time *in* (ie before it ever gets to sent to
    // any agents. Might want some way to try to deal with this or at least log it.

    val reconfigFrequency = 5000L

    private data class QueueItemWrapper(val item: QueueItem, var timesOut: Int = 0, var consumed: Boolean = false) {
        var initialTimeOut: Long = 0
        val timeAddedToQueue = System.currentTimeMillis()
    }


    private inner class Domain(val name: QueueDomain) {

        var config: QueueConfig = QueueConfig.default()

        val averagesLock = ReentrantReadWriteLock()


        // our lock for config, since config itself is a variable
        val configLock = ""

        // "packetsize" is just a euphemism for how many items we treat as a single item when dumping things out of the queue.
        // if config doesn't specify it, we will customize it to try to move towards configs specified desired packet time.
        // the reason to do this is to minimize networking time without having to hand tune parameters every time
        var packetSize: Int = config.packetSize ?: 1

        val queue: PriorityBlockingQueue<QueueItemWrapper> =
            PriorityBlockingQueue(5, object : Comparator<QueueItemWrapper> {
                override fun compare(o1: QueueItemWrapper, o2: QueueItemWrapper): Int {
                    return o1.timesOut.compareTo(o2.timesOut)
                }
            })


        // this can't overflow in practice. I promise, I though about it and its one of those things where the number
        // is so big you would be more likely to fill the universe with winning lottery tickets made of grains of sand
        val summedTimeSinceLastConfig = LongAdder()

        val numItemsSinceLastConfig = LongAdder()

        val configurer = ThreadSafeTimeTrigger(reconfigFrequency) {
            Logging.getLogger().trace { "Reconfiguring for domain $name" }
            // todo try the at-most-1-fail lock and see if its enough faster than the rw to be worth using

            // if we've been given a packet size, don't compute one

            // note this will probably never reach a real equilibrium, just oscillate around one, but that's fine as long
            // as it works acceptably.

            //todo test how well this works
            if (config.packetSize == null && config.desiredTimePer > 0) {
                var time = 0L
                var count = 0L
                averagesLock.write {
                    time = summedTimeSinceLastConfig.sumThenReset()
                    count = summedTimeSinceLastConfig.sumThenReset()
                }
                val averageTime = time / count
                // how long it took "per item" last time
                var computedTimePer = averageTime / packetSize
                if (computedTimePer == 0L) {
                    Logging.getLogger().warn { "computed time per was 0. Attempting to resolve by bumping size" }
                    computedTimePer++
                }
                packetSize = (config.desiredTimePer / computedTimePer).toInt()
                Logging.getLogger()
                    .debug { "${name} reconfiguring packet size. Computed underlying time per item as $computedTimePer. Last time we moved this to $averageTime with a packet size of $packetSize, targeting ${config.desiredTimePer}" }

            }
        }

        fun addItem(item: QueueItem) {
            queue.add(QueueItemWrapper(item))
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (other is Domain) {
                return name == other.name
            }
            return false
        }

        // returns how many items it considers itself as having given us.
        fun poll(
            items: MutableCollection<QueueItem>,
            dirtyItems: MutableCollection<QueueItem>,
            desiredItems: Int
        ): Int {
            // todo there's an issue here, I think. Items that go out to an agent which dies will theoretically eventually
            // get served, but if new stuff keeps coming in they might in practice sit in the queue for a very long
            // time / until the flow of items stops entirely, which might not be desirable.
            // for the moment we are considering this to be acceptable, but if we don't any longer at some point in the future
            // the fix would be to occasionally reset the timesOut score on things that have been in the queue for a long time
            var givenItems = 0
            synchronized(queue) {
                val takenOut = ArrayList<QueueItemWrapper>(min(queue.size, desiredItems * packetSize))
                while (queue.size > 0 && givenItems / packetSize < desiredItems) {
                    val polled = queue.poll() ?: continue
                    synchronized(polled) {
                        if (!polled.consumed) {
                            items.add(polled.item)
                            givenItems++
                        } else if (config.resendOutItems) {
                            dirtyItems.add(polled.item)
                            // we don't increment given items. It wouldn't mess up this queue: we would always use all
                            // clean items first. However, other queues beneath us would potentially get underserved,
                            // since this changes the size of our return value which is used in the outer poll method

                            // unfortunately this has the effect of always treating *dirty* objects as individuals,
                            // and never packeting them.

                            // todo fix this ^, but its not very high priority because the fix is kind of messy and
                            // the problem isn't that big of a deal
                        }
                        polled.timesOut += 1
                        takenOut.add(polled)
                    }
                }
                queue.addAll(takenOut)
            }
            // round it up, rather than rounding down as simple division would do. We don't want to report a non-empty packet
            // as 0 elements. Technically this isn't a big deal, though.
            return ceil(givenItems.toDouble() / packetSize.toDouble()).toInt()
        }

        fun finish(fromMap: QueueItemWrapper) {
            configurer.check()
            // read and write cases are sort of inverted here, since the underlying objects are already synchronous
            // we track this even if packet size is given, in case that changes
            averagesLock.read {
                numItemsSinceLastConfig.increment()
                summedTimeSinceLastConfig.add(System.currentTimeMillis() - fromMap.initialTimeOut)
            }
        }

        fun changeConfig(config: QueueConfig) {
            synchronized(configLock) {
                this.config = config
            }
            packetSize = config.packetSize ?: packetSize
        }
    }


    // a map from domains to a queue of items ordered by how many agents currently have them
    private val domainMap = ConcurrentHashMap<QueueDomain, Domain>()
    private val itemsMap = HashMap<UUID, QueueItemWrapper>()
    //    val outOrder = ConcurrentLinkedQueue<UUID>()
    var queueSize = AtomicInteger(0)

    fun put(item: QueueItem) {
        Logging.getLogger().debug { "Putting QueueItem $item" }
        var domain = domainMap[item.domain]
        if (domain == null) {
            synchronized(domainMap) {
                Logging.getLogger().debug { "Queue was null, making new one" }
                domain = domainMap[item.domain]
                if (domain == null) {
                    domainMap[item.domain] = Domain(item.domain)
                }
                domain = domainMap[item.domain]
                Logging.getLogger().debug { "Performed actual put of item $item" }
            }
        }
        domain!!.addItem(item)
        queueSize.incrementAndGet()
        Logging.getLogger().debug { "Finishing put" }
    }

    fun poll(domains: List<QueueDomain>, desiredItems: Int): List<QueueItem> {

        val items = ArrayList<QueueItem>(desiredItems)
        // dirty items are ones that at least one agent has already been sent. We don't send them out unless there aren't
        // enough clean items
        val dirtyItems = ArrayList<QueueItem>()
        val iter = domains.listIterator()
        Logging.getLogger().debug { "Polling for items: $domains" }
        var gottenItems = 0
        while (items.size < desiredItems && iter.hasNext()) {
            gottenItems += domainMap[iter.next()]?.poll(items, dirtyItems, desiredItems) ?: 0
        }
        // add the dirty items
        if (gottenItems < desiredItems && dirtyItems.size > 0) {
            dirtyItems.shuffle()
            for (i in 0 until min(desiredItems - gottenItems, dirtyItems.size)) {
                items.add(dirtyItems[i])
                // here, we could technically correct the scores of the items we didn't wind up using. The problem
                // is that they are all back in a pq, and so doing that would entail pulling them out of the pq and then putting
                // them back in. pq.remove is potentially expensive afaik. This is another thing we are letting slide

                // todo fix this if we want to ^
            }
        }
        Logging.getLogger().debug { "Finished task polling" }
        return items
    }

    // returns true if it actually removed the item, false if it wasn't present (anymore, or yet)
    fun finish(id: UUID): Boolean {
        // we assume
        val fromMap = itemsMap[id] ?: return false
        synchronized(fromMap) {
            if (!fromMap.consumed) {
                fromMap.consumed = true
                itemsMap.remove(id)
                domainMap[fromMap.item.domain]!!.finish(fromMap)
                return true
            }
            return false
        }
    }

    override fun register(socket: Socket) {
        Logging.getLogger().debug { "registering socket that connected to the server" }
        socket.registerTypeListener(QueueAddMessage::class.java) {
            for (elt in it.items) {
                put(elt)
            }
            null
        }
        socket.registerTypeListener(QueuePollMessage::class.java) {
            QueueResponseMessage(poll(it.domains, it.desiredItemCount))
        }
        socket.registerTypeListener(TasksLeftRequest::class.java) {
            TasksLeftResponse(queueSize.get())
        }
        socket.registerTypeListener(QueueYieldAddMessage::class.java) {
            if (finish(it.yieldFrom)) {
                for (elt in it.items) {
                    put(elt)
                }
            }
            null
        }
        socket.registerTypeListener(QueueConfigMessage::class.java) {
            for (domainName in it.domains) {
                synchronized(domainMap) {
                    if (domainName !in domainMap) {
                        domainMap[domainName] = Domain(domainName)
                    }
                }
                val domain = domainMap[domainName]!!
                domain.changeConfig(it.config.copy())
            }
            null
        }
        socket.registerTypeListener(QueueBulkYieldMessage::class.java) {
            it.yield.forEach { t, u ->
                if (finish(t)) {
                    for (elt in u) {
                        put(elt)
                    }
                }
            }
            if (it.domainsToPoll != null) {
                QueueResponseMessage(poll(it.domainsToPoll, it.desiredItemCount!!))
            } else {
                null
            }
        }
    }
}
