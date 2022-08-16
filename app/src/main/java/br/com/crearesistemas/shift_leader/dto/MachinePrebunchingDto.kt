package br.com.crearesistemas.shift_leader.dto

import com.google.gson.Gson

class MachinePrebunchingDto {

    var location: String = ""
    var areaSize: Float = 0.0f
    var actualSizePerformed: Float = 0.0f

    fun toJson() : String {
        return Gson().toJson( this ).toString()
    }
}