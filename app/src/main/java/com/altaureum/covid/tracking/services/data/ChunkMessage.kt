package com.altaureum.covid.tracking.services.data

class ChunkMessage {
    var packets:Int=0
    var chunks=ArrayList<String>()

    override fun toString():String{
        val buffer = StringBuffer()
        for(chunk in chunks){
            buffer.append(chunk)
        }
        return buffer.toString()
    }
}