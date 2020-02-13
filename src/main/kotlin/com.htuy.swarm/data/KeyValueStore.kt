package com.htuy.swarm.data

import java.io.Serializable

interface KeyValueStore{
    suspend fun get(key : Id) : Serializable?

    suspend fun put(key : Id, item : Serializable)

    suspend fun watch(key : Id, callback : (Serializable) -> Unit)

    suspend fun unput(key : Id)
}