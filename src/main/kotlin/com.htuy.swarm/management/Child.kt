package com.htuy.swarm.management

import com.htuy.common.Address
import com.htuy.common.Configurator
import com.htuy.common.Logging
import com.htuy.common.SystemUtilies
import com.htuy.netlib.sockets.InternetSockets
import com.htuy.netlib.sockets.Socket
import com.htuy.swarm.management.onetorule.*
import sun.java2d.pipe.SpanShapeRenderer
import kotlin.concurrent.thread

// The average amount of time a child will live for before randomly dying. If set to -1, the child will never
// die on purpose. The point here is that it will simulate system crashes so we can be sure a system works good under
// crash circumstances. Note that I can only barely almost do math, so its almost certain that this number isn't used
// precisely. I need to be using an exponential distribution to calculate the correct death probability here, and I
// haven't actually done so. The point is that this value is used to determine death time, but I doubt it works
// exactly as its supposed to.
val EXPECTED_CHILD_LIFESPAN_SECONDS : Long = 900 // 15 minutes


class Child(val starter: ServiceStarters = SimpleServiceStarters(), val pingFrequency: Long = 15000L) {
    fun start(serviceType: ServiceType, kingAddr: Address, leaderAddr: Address?, otherArgs: Array<String>, id : String?) {
        val king = SingleKingClient(kingAddr)
        val service = starter.makeService(serviceType, king, leaderAddr)
        val serviceAddr = Address.anyPortLocal()
        // how likely we would be to die randomly, if we had this feature enabled
        val deathProbability = (pingFrequency / 1000) / EXPECTED_CHILD_LIFESPAN_SECONDS * 2
        Logging.getLogger()
            .warn { "Starting running child service for $serviceType with host at $kingAddr and own host $serviceAddr" }
        service.socketAddress = serviceAddr
        service.passArgs(otherArgs)


        fun onSocketGotten(socket: Socket) {
            Logging.getLogger().debug { "Got a client connection, service registering!" }
            service.register(socket)
            socket.registerTypeListener(ServiceDeregister::class.java) {
                Logging.getLogger().error { "Asked to shutdown and stopping!" }
                System.exit(0)
                null
            }
            socket.registerTypeListener(ServicePing::class.java) {
                Logging.getLogger().debug { "Responding to service ping for $serviceType" }
                ServicePing(serviceType, true)
            }
            val sock = socket
            socket.registerTypeListener(ServiceRegisterResponse::class.java) {
                sock.registerOnClose {
                    Logging.getLogger().error { "Socket to closed, shutting down" }
                    System.exit(0)
                }
                null
            }
        }
        InternetSockets().listenOn(serviceAddr) { onSocketGotten(it) }
        service.startService()
        var socketToLeader : Socket? = null
        if (leaderAddr == null) {
            kingAddr.tryPush(ServiceRegister(serviceType, serviceAddr)) {
                Logging.getLogger().error { "Couldn't connect to our king. Quitting!" }
                System.exit(0)
            }
        } else {
            socketToLeader = leaderAddr.socketTo()
            if (socketToLeader != null) {
                onSocketGotten(socketToLeader)
                socketToLeader.getSynch(FollowerRegister(serviceType,id!!))
            } else {
                Logging.getLogger().error { "Couldn't connect to our leader. Quitting!" }
                System.exit(0)
            }
        }


        // really, these ought to be a coroutine rather than a thread, or at least it just as well could be, but that kept
        // causing issues for some unknown reason, so threads they are

        // extra bonus failsafe. If the netcode hangs, this will *still* kill the thing. If nothing else executes for 10 *
        // ping frequency, the system will always stop.
        var deathTime: Long = System.currentTimeMillis() + pingFrequency * 10
        thread {
            while (true) {
                val sleepTime = deathTime - System.currentTimeMillis()
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime)
                } else {
                    Logging.getLogger().error { "Shutting down due to timeout" }
                    System.exit(0)
                }
            }
        }
        thread {
            while (true) {
                Thread.sleep(pingFrequency)
                Logging.getLogger().debug { "I am alive! $serviceType" }
                if(service.done){
                    socketToLeader?.tryPush(LeadServiceFinish()){
                        // if it failed we don't have time to do anything, we are killing ourself *yesterday*
                    }
                    System.exit(0)
                }
                // if we want to die randomly. Which sounds thrilling, but the system can't currently deal with that
//                if(Math.random() < deathProbability){
//                    Logging.getLogger().error { "It killed me, larry :( (random child suicide triggered)" }
//                    System.exit(0)
//                }
            }
        }


        thread {
            var failedLast = false
            while (true) {
                Logging.getLogger().debug {
                    "Start of ping loop"
                }
                Thread.sleep(pingFrequency)
                Logging.getLogger().debug { "did delay" }
                val ping = if (leaderAddr == null) {
                    kingAddr.tryGetSynch(ServicePing(serviceType, true)) {
                        Logging.getLogger().warn { "Ping didn't get returned at all" }
                        ServicePing(serviceType, false)
                    }
                } else {
                    leaderAddr.tryGetSynch(LeadServicePing(true)) {
                        Logging.getLogger().warn { "Ping didn't get returned at all" }
                        LeadServicePing(false)
                    }
                }
                Logging.getLogger().debug { "got the ping back $ping" }
                if (ping is Ping) {
                    Logging.getLogger().debug { "Ping was ping" }
                    if (!(ping.confirmed)) {
                        Logging.getLogger().warn { "Missed a ping. Might shut down soon!" }
                        if (failedLast) {
                            Logging.getLogger().error { "Missed too many pings, shutting down" }
                            System.exit(0)
                        } else {
                            failedLast = true
                        }
                    } else {
                        Logging.getLogger().debug { "Ping was good boi" }
                        failedLast = false
                        deathTime = System.currentTimeMillis() + 10 * pingFrequency
                    }
                } else {
                    Logging.getLogger().error { "It wasn't a ping: $ping" }
                }
            }
        }

    }
}

fun innerMain(args : Array<String>, extraServices : Map<ServiceType,(King,Address?) -> Service>){
    val service = args[0]
    Configurator().run(service)

    val kingAddr = Address(args[1], args[2].toInt())

    val leaderAddr = if (args[3] == "none") {
        null
    } else {
        Address(args[3], args[4].toInt())
    }
    val id = args[5]
    val otherArgs = if (args.size > 6) {
        args.sliceArray(6..args.size)
    } else {
        Array(0) { "" }
    }
    val starter = SimpleServiceStarters()
    extraServices.forEach { t, u ->
        starter.addService(t,u)
    }
    Child(starter).start(service, kingAddr, leaderAddr, otherArgs,id)
    SystemUtilies.waitForSystemExit()
}

fun main(args: Array<String>) {
    innerMain(args,mapOf())
}