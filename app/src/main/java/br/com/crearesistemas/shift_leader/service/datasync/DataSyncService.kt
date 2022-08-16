package br.com.crearesistemas.shift_leader.service.datasync

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import br.com.crearesistemas.shift_leader.db_service.viewmodel.MachineViewModel
import br.com.crearesistemas.shift_leader.dto.Machine
import br.com.crearesistemas.shift_leader.service.volley.ShiftLeaderVolleyService
import br.com.crearesistemas.shift_leader.singleton.MessageSingleton
import br.com.crearesistemas.socket.client.service.MachineStatusSingleton
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


/**
 * Service especifico para funcionalidades do DataSync
 */
class DataSyncService {

    private val sdf: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")

    var list = ArrayList<Machine>()

    fun  connectToCloudRest(context: Context, application: Application, callback: (() -> Unit?)? = null,) {
        try {
            var volleyService = ShiftLeaderVolleyService(application)
            volleyService.syncToCloud(context)
        } catch (e: Exception){

            e.printStackTrace()

            if (e!=null && !e.message.isNullOrEmpty()) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            } else {
                Log.d("FATAL_ERROR","Error to log connection message.")
            }

            callback?.let { it() }
        }
    }





    private fun toastMessage(context: Context, message: String) {
        try {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.d("DataSyncService", "Erro ao logar mensagem")
        }
    }

    fun listMachines(application:Application) : ArrayList<Machine> {

        val machineService = MachineViewModel(application)
        machineService.getAll().forEach {
            if (it.hotspotSsid!=null) {
                list.add(buildItem(it.hotspotSsid!!))
                MachineStatusSingleton.passwords[it.hotspotSsid!!] = it.hotspotPassword!!
            }
        }

        return list
    }

    private fun buildItem(machineName: String): Machine {

        if (MachineStatusSingleton.lastReadTimestamp.containsKey(machineName)) {
            return Machine(machineName, Date(MachineStatusSingleton.lastReadTimestamp[machineName]!!),"")
        }

        return Machine(machineName, sdf.parse("01/01/2021 00:00"),"")

    }

}