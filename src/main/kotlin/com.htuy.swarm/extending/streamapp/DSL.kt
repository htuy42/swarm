package com.htuy.swarm.extending.streamapp

import com.htuy.swarm.coordination.QueueDomain
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue


// this is kind of cool, but the problem I encountered with it is that I don't really want tree shaped streams a lot
// of the time, and doing non-treeshaped data with the dsl was proving difficult to express well.

// as such its not currently in use




//interface Outflowable<T> {
//    val make: () -> T
//    val producesTo: List<Inflowable<T>>
//    val id : UUID
//}
//
//data class OutflowableImpl<T>(override val make: () -> T, override val producesTo: List<Inflowable<T>>) : Outflowable<T>{
//    override val id: UUID = UUID.randomUUID()
//}
//
//interface Inflowable<T> {
//    val consume: (T) -> Unit
//    var from: Outflowable<T>
//    val id : UUID
//}
//
//data class InflowableImpl<T>(override val consume: (T) -> Unit) : Inflowable<T> {
//    override lateinit var from: Outflowable<T>
//    override val id: UUID = UUID.randomUUID()
//}
//
//interface IOAble<I, O> : Inflowable<I>, Outflowable<O>
//
//class IOAbleImpl<I, O>(val transform: (I) -> O, override val producesTo: List<Inflowable<O>>) : IOAble<I, O> {
//    override lateinit var from: Outflowable<I>
//    override val id: UUID = UUID.randomUUID()
//    val buffer = LinkedBlockingQueue<I>()
//    override val consume: (I) -> Unit = {
//        buffer.add(it)
//    }
//    override val make: () -> O = {
//        transform(buffer.take())
//    }
//
//}
//
//class StreamApplicationBuilder {
//    private val outs: MutableList<Outflowable<*>> = ArrayList()
//    fun build(): StreamApplication {
//        fun <T> updateProducer(out: Outflowable<T>) {
//            for (elt in out.producesTo) {
//                elt.from = out
//                if (elt is Outflowable<*>) {
//                    updateProducer(elt)
//                }
//            }
//        }
//        outs.forEach { updateProducer(it) }
//        return StreamApplication(outs)
//    }
//
//    fun <T> source(block: OutBuilder<T>.() -> Unit) {
//        outs.add(OutBuilder<T>().apply(block).build())
//    }
//
//    fun <T> source(producer: () -> T, block: OutBuilder<T>.() -> Unit = {}) {
//        val builder = OutBuilder<T>()
//        builder.producer(producer)
//        outs.add(builder.apply(block).build())
//    }
//}
//
//open class OutflowBuilder<T> {
//    protected val outTos = ArrayList<Inflowable<T>>()
//    fun <O> transform(block: IOBuilder<T, O>.() -> Unit) {
//        outTos.add(IOBuilder<T, O>().apply(block).build())
//    }
//
//    fun <O> transform(transform: (T) -> O, block: OutflowBuilder<O>.() -> Unit = {}) {
//        val builder = IOBuilder<T, O>()
//        builder.transformer(transform)
//        outTos.add(builder.apply(block).build())
//    }
//
//    fun dump(consumer: (T) -> Unit) {
//        outTos.add(InflowableImpl(consumer))
//    }
//}
//
//class OutBuilder<T> : OutflowBuilder<T>() {
//    internal lateinit var _producer: () -> T
//
//    fun producer(toUse: () -> T) {
//        _producer = toUse
//    }
//
//    fun build(): Outflowable<T> {
//        return OutflowableImpl(_producer, outTos)
//    }
//}
//
//class IOBuilder<I, O> : OutflowBuilder<O>() {
//    internal lateinit var _transformer: (I) -> O
//    fun transformer(toUse: (I) -> O) {
//        _transformer = toUse
//    }
//
//    fun build(): Inflowable<I> {
//        return IOAbleImpl(_transformer, outTos)
//    }
//}
//
//data class StreamApplication(val sources: List<Outflowable<*>>)
//
//fun streamApp(block: StreamApplicationBuilder.() -> Unit): StreamApplication =
//    StreamApplicationBuilder().apply(block).build()
//
//
