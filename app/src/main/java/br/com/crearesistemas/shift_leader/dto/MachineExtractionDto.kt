package br.com.crearesistemas.shift_leader.dto

import com.google.gson.Gson

class MachineExtractionDto {

    var sizeVolIsAPreset: Float = 0.0f

    fun toJson() : String {
        return Gson().toJson( this ).toString()
    }

}