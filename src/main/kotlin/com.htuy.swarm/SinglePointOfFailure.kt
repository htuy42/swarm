package com.htuy.swarm

// doesn't do anything, but indicates that if the single server running this goes down, the system probably can't
// recover / the data stored there will be lost / something is expected to go wrong
interface SinglePointOfFailure