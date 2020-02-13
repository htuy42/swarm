package com.htuy.swarm.data

import java.io.Serializable


interface Id : Serializable{
    // MUST DEFINE EQUALS MANUALLY!!!!!!!!!!

    fun fallback() : Id?
    fun fallthru() : Serializable

}

data class StringId(val idStr : String) : Id{
    override fun fallback(): Id? {
        return null
    }

    override fun fallthru(): Serializable {
        return idStr
    }
}

