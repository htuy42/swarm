package com.htuy.swarm.management.onetorule

import com.htuy.common.Address
import com.htuy.common.Configurator
import com.htuy.common.Logging
import com.htuy.constant.pathToLocalModule
import com.htuy.machines.MachineCollection
import com.htuy.netlib.sockets.InternetSockets
import com.htuy.netlib.sockets.Socket
import com.htuy.swarm.SinglePointOfFailure
import com.htuy.swarm.management.ServiceType
import java.util.*


// is a single point of failure. Also points to only a single service of each type.

class SingleKingServer(
    val waitForServerToBoot: Long = 15000,
    val port: Int? = null,
    val childName: String = "child_server"
) : SinglePointOfFailure {
    val addr: Address
    val leader: Leader = Leader(this)

    init {
        if (port != null) {
            addr = Address.withPortLocal(port)
        } else {
            addr = Address.anyPortLocal()
        }
        Logging.getLogger().warn { "Running on address $addr" }
        println("Running on address $addr")
        leader.start()
    }

    val services = HashMap<ServiceType, Address>()

    val waitingFor = HashMap<ServiceType, Long>()

    val machines = MachineCollection()

    var silent = false


    fun start() {
        // there are definitely problems where we will keep getting broken services for a few tries before a new one gets booted,
        // but equilibrium should eventually be restored
        fun onSocketGotten(it: Socket) {
            Logging.getLogger().warn { "Got socket from ${it.connectedTo}, registering listeners" }
            val to = it.connectedTo
            it.registerTypeListener(ServiceRequest::class.java) {
                Logging.getLogger().trace { "Got request for ${it.type} from $to" }
                val service = services[it.type]
                if (service != null) {
                    Logging.getLogger().trace { "Had the requested service, it was at $service" }
                    ServiceResponse(it.type, service)
                } else {
                    Logging.getLogger().debug { "Didn't have the service, making it" }
                    startServerForService(it.type)
                    ServiceNotYetReady()
                }
            }
            it.registerTypeListener(SendToLeadServicesRequest::class.java) {
                leader.sendToFollowersFor(it.type, it.request)
                null
            }
            it.registerTypeListener(ServiceError::class.java) {
                Logging.getLogger().warn { "Got service error for ${it.type}, ${it.host}, from $to" }

                val current = services[it.type]

                if (current == it.host) {

                    val pingRes = current.tryGetSynch(ServicePing(it.type, false)) {
                        null
                    }
                    if (pingRes !is ServicePing || !pingRes.confirmed) {
                        Logging.getLogger().warn { "The service was actually down for ${it.type}" }
                        synchronized(services) {
                            if (services[it.type] == it.host) {
                                services.remove(it.type)
                            }
                        }
                    } else {
                        Logging.getLogger().error { "$to reported ${it.type} as down, but it wasn't. Malicious node??" }
                    }
                } else {
                    Logging.getLogger().debug { "The server was just outdated for ${it.type}" }
                }
                null
            }
            it.registerTypeListener(ServiceRegister::class.java) {
                Logging.getLogger().debug { "Got service register for ${it.type} from ${it.address}" }

                synchronized(services) {
                    if (services[it.type] != null) {
                        Logging.getLogger()
                            .debug { "We already had a service for ${it.type}, telling ${it.address} to shutdown" }
                        ServiceDeregister()
                    } else {
                        services[it.type] = it.address
                        null
                    }
                }
                ServiceRegisterResponse()
            }
            it.registerTypeListener(ServicePing::class.java) {
                Logging.getLogger().trace { "Got service ping, responding positively" }
                it
            }

            it.registerTypeListener(LeadServicePing::class.java) {
                Logging.getLogger().trace { "Got lead service ping, responding positively" }
                it
            }

            it.registerTypeListener(ServiceExistenceRequest::class.java) {
                for (elt in it.services) {
                    if (elt !in services) {
                        startServerForService(elt)
                    }
                }
                null
            }

            it.registerTypeListener(MakeServiceRequest::class.java) {
                handleServiceRequest(it)

                null
            }

        }
        InternetSockets().listenOn(addr, ::onSocketGotten)
    }

    fun handleServiceRequest(request: MakeServiceRequest, countOverride: Int = request.count) {
        Logging.getLogger().debug { "Asked to make $countOverride copies of ${request.type} with args ${request.args}" }
        for (x in 0 until countOverride) {
            val id = UUID.randomUUID().toString()
            leader.notifyNewService(id, request)
            startServerForService(request.type, request.args, leader.leaderAddress, id)
        }
        Logging.getLogger().debug { "finished making $countOverride copies of ${request.type}" }
    }


    fun startServerForService(service: ServiceType, args: String = "", leader: Address? = null, id: String? = null) {
        synchronized(waitingFor) {
            if (leader == null) {
                val waitTill = waitingFor[service]
                if (waitTill != null && System.currentTimeMillis() < waitTill) {
                    return
                }
            }
            val machine = machines.getMachinesLoadManaged(MachineCollection.INFINITE_LOAD, 1).first()
            Logging.getLogger().debug { "Requesting $service from $machine" }
            val leaderString = if (leader == null) {
                "none none"
            } else {
                "${leader.address} ${leader.port}"
            }
            val idString = if (id == null) {
                "none"
            } else {
                id.toString()
            }

            val command = "java -jar ${pathToLocalModule(childName)} " +
                    "$service ${addr.address} ${addr.port} $leaderString $idString $args"
            Logging.getLogger().debug { "The command is $command" }
            machine.sendSshCommand(command, silent)
            if (leader == null) {
                waitingFor[service] = System.currentTimeMillis() + waitForServerToBoot
            }
        }
    }

    fun kill() {
        for (elt in services) {
            elt.value.tryPush(ServiceDeregister()) {}
        }
    }

    fun silenceSubs() {
        silent = true
    }
}

fun main(args: Array<String>) {
    Configurator().run()
    var port: Int? = null
    var childName: String = "child_server"
    if (args.size >= 1) {
        port = args[0].toInt()
        if (args.size >= 2) {
            childName = args[1]
        }
    }
    SingleKingServer(port = port, childName = childName).start()
    Thread.currentThread().join()
}