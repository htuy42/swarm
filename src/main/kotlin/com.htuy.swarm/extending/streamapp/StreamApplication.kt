// currently wip

//package com.htuy.swarm.extending.streamapp
//
//import com.htuy.common.Idd
//import com.htuy.swarm.coordination.QueueDomain
//import com.htuy.swarm.data.Id
//import com.htuy.swarm.extending.api.SwarmApi
//import com.htuy.swarm.work.Task
//import java.io.Serializable
//import java.util.*
//import java.util.concurrent.LinkedBlockingQueue
//import javax.xml.transform.stream.StreamSource
//
//
//interface StreamElement {
//    val id: UUID
//}
//
//interface StreamProducer<T> : StreamElement {
//    val feedsTo: MutableList<StreamConsumer<T>>
//    fun produceNext(): T
//    fun hasNext(): Boolean
//}
//
//// produce should return null when there is nothing further to be gotten from the stream, ie no next element
//class Produce<T>(val _produce: () -> T?) : StreamProducer<T>, Idd() {
//    override val feedsTo: MutableList<StreamConsumer<T>> = ArrayList()
//    var nxt = _produce()
//    override fun produceNext(): T {
//        val prev = nxt
//        nxt = _produce()
//        return prev!!
//    }
//    override fun hasNext(): Boolean {
//        return nxt != null
//    }
//
//}
//
//class CollectionProduce<T>(collection: Collection<T>) : StreamProducer<T>, Idd() {
//    override val feedsTo: MutableList<StreamConsumer<T>> = ArrayList()
//    val iter = collection.iterator()
//    override fun produceNext(): T {
//        return iter.next()
//    }
//    override fun hasNext(): Boolean {
//        return iter.hasNext()
//    }
//}
//
//interface StreamConsumer<T> : StreamElement {
//    var feedsFrom: StreamProducer<T>
//    fun useFeed(feed: StreamProducer<T>) {
//        feedsFrom = feed
//        feed.feedsTo.add(this)
//    }
//
//    fun consumeNext(next: T)
//}
//
//class Consume<T>(override var feedsFrom: StreamProducer<T>, val _consume: (T) -> Unit) : StreamConsumer<T>, Idd() {
//    init {
//        useFeed(feedsFrom)
//    }
//
//    override fun consumeNext(next: T) {
//        _consume(next)
//    }
//}
//
//interface StreamTransformer<I, O> : StreamConsumer<I>, StreamProducer<O> {
//    val buffer: LinkedBlockingQueue<I>
//    override fun consumeNext(next: I) {
//        buffer.add(next)
//    }
//
//    override fun produceNext(): O {
//        return transform(buffer.take())
//    }
//
//    fun transform(input: I): O
//}
//
//class Transform<I, O>(override var feedsFrom: StreamProducer<I>, val _transform: (I) -> O) : StreamTransformer<I, O>, Idd() {
//    // it never has next in the sense that it will never create new data
//    override fun hasNext(): Boolean = false
//    override val buffer: LinkedBlockingQueue<I> = LinkedBlockingQueue()
//
//    override val feedsTo: MutableList<StreamConsumer<O>> = ArrayList()
//
//    init {
//        useFeed(feedsFrom)
//    }
//
//    override fun transform(input: I): O {
//        return _transform(input)
//    }
//
//}
//
//
//class TransformTask<I,O>(val input : I,override val domain: QueueDomain, val outputDomain : QueueDomain, val transform : (I) -> O) : Task() {
//    override val depedencies: List<Id> = listOf()
//    override var yield: Map<Id, Serializable> = mapOf()
//    override lateinit var childTasks: List<Task>
//    override fun execute() {
//
//    }
//}
//
///**
// * A simplified representation of a stream application, which involves getting data from sources, moving it around,
// * applying transforms to it, and then dumping it to sinkuses. Its like reactive streams plus kafka minus being developed
// * by a team of professionals
// */
//
//
//// this looks at the api to see if it has been initialized with a host. If it hasn't, it goes ahead and tries to
//// host local, and if that fails obviously it doesn't work
//fun runStreamApplication(api: SwarmApi, streamApp : List<StreamConsumer<*>>) {
//    val sources = HashSet<StreamSource>()
//    fun getSource(consumer : StreamConsumer<*>){
//        var next : Any = consumer
//        while(next is StreamConsumer<*>){
//            next = next.feedsFrom
//        }
//        if(next !is StreamSource){
//            throw IllegalArgumentException("Root of a consumer was not a source. Not sure what to do from here :(")
//        }
//        sources.add(next)
//    }
//    for(elt in streamApp){
//        getSource(elt)
//    }
//
//
//
//}
//
//fun apiDemos() {
//
//    val allStrings = LinkedList(listOf("dog", "cat", "bag", "sadadasd"))
//
//    val appStream1 = Produce { allStrings.poll() }
//    val finalOutflow1 = Consume(appStream1) { println(it) }
//
//    runStreamApplication(finalOutflow1)
//
////    val appStream1 = streamSource{"GET STUFF FROM A FILE"}
////    val finalOutflow1 = streamConsumer(appStream1){"DUMP THE STUFF TO THE FILE"}
////    run(finalOutflow1)
//
//
//    val appStream2 = CollectionProduce(listOf(1,2,3,4,5,6))
//    val appStreamTransformed2 = Transform(appStream2) { it.toString() }
//    val finalOutflow2 = Consume(appStreamTransformed2)
//    run(finalOutflow2)
//
//
////    // todo on this part, since it probably requires some changes to how the queue works / maybe the implementation of something
////    // new on the synchro service
////    val appStream3 = streamSource { "GET STUFF FROM A DATABASE" }
////    // how do we know when we're done? Do we just assume we need to clear everything to the collapse?
////    val appStreamCollapsed =
////        streamMerge("FUNCTION TO SUM ITEMS FROM appStream3", "FUNCTION TO SUM RESULTS FROM appStreamCollapsed")
//
//}