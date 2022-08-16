package br.com.crearesistemas.shift_leader.ui.datasync


import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import br.com.crearesistemas.shift_leader.MainActivity
import br.com.crearesistemas.shift_leader.R
import br.com.crearesistemas.shift_leader.singleton.MessageSingleton
import br.com.crearesistemas.shift_leader.wifi_service.WifiService
import br.com.crearesistemas.socket.client.service.MachineStatusSingleton
import java.util.*

/**
 * Adapter, responsável por montar a lista de conexões wifi
 */
class DataSyncListAdapter(
    private val context: Activity,
    private val names: ArrayList<String>,
    private val dates: ArrayList<String>,
    private val onOff: ArrayList<Int>,
    private val status: ArrayList<Boolean>,
    private val machineSignalList: ArrayList<Int>,
    private val wifiService: WifiService

)

    : ArrayAdapter<String>(context, R.layout.list_datasync, names) {


    override fun getView(position: Int, view: View?, parent: ViewGroup): View {

        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.list_datasync, null, true)
        val nameText = rowView.findViewById(R.id.name) as TextView
        val imageViewWifi = rowView.findViewById(R.id.icon_wifi) as ImageView
        val iconOnOff = rowView.findViewById(R.id.icon_onoff) as ImageView
        val dateText = rowView.findViewById(R.id.date) as TextView
        val switchConnection = rowView.findViewById<Switch>(R.id.switch_connection) as Switch
        val machineName = names[position]

        val circleProgressBar =
            rowView.findViewById<ProgressBar>(R.id.circle_progress_bar) as ProgressBar

        if (MachineStatusSingleton.currentMachineName != machineName) {

            if (MachineStatusSingleton.isConnectingStatus.containsKey(machineName)
                && MachineStatusSingleton.isConnectingStatus[machineName]!!
            ) {
                circleProgressBar.visibility = View.VISIBLE
            } else {
                circleProgressBar.visibility = View.INVISIBLE
            }

        }

        switchConnection.isChecked = MachineStatusSingleton.activationStatus[machineName] == true


        switchConnection.setOnClickListener {


            MachineStatusSingleton.activationStatus[machineName] = switchConnection.isChecked
            // Ja conecta na hora de ativar
            if (MachineStatusSingleton.selecToggleName != "" && MachineStatusSingleton.selecToggleName != machineName){
                Toast.makeText(context, "You are Connected to another machine. Please disconnect before.", Toast.LENGTH_LONG).show()
                switchConnection.isChecked = false
                MachineStatusSingleton.activationStatus[machineName] = false
            }
            else{
                if (switchConnection.isChecked) {
                    MachineStatusSingleton.selecToggleName = machineName
                    circleProgressBar.visibility = View.VISIBLE
                    MachineStatusSingleton.isConnectingStatus[machineName] = true
                    var actualPosition = machineSignalList[position]
                    // Quando cliquei no botao para conectar (switch), ele ja conecta na hora
                    wifiService.connectToMachine(machineName, true)

                } else {
                    MachineStatusSingleton.selecToggleName = ""
                    circleProgressBar.visibility = View.INVISIBLE
                    MachineStatusSingleton.isConnectingStatus[machineName] = false
                }
            }
        }

        nameText.text = names[position]
        imageViewWifi.setImageResource(machineSignalList[position])

        if (dates[position] == "01/01/2021 00:00") {
            dateText.text = "-"
        } else {
            dateText.text = dates[position]
        }


        iconOnOff.setImageResource(onOff[position])

        return rowView
    }

    /**
     * Durante testes foi verificado a necessidade de um force reload nas conexoes wifis
     * Obs: Nao estamos mais usando esse comportamento
     */
    private fun hardReloadConnection(context: Context, message: String) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent!!.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        mainIntent.putExtra("message", message)
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }

    /**
     * Reload mais simples menos invasivo apenas recarregando activity principal
     * Obs: Nao estamos mais usando esse comportamento
     */
    fun tinyReloadConnection(message: String) {
        val intent = Intent(getContext(), MainActivity::class.java)
        intent.putExtra("message", message)
        context.startActivity(intent)
    }

    private fun switchOpenModal() {
        val builder = AlertDialog.Builder(context)
        val layoutInflater = LayoutInflater.from(context)
        val inflateView = layoutInflater.inflate(R.layout.feature_status_window, null)
        val message = inflateView.findViewById<TextView>(R.id.menssagerDialog)
        val progress = inflateView.findViewById<ProgressBar>(R.id.progressBar)
        val uncheck = inflateView.findViewById<ImageView>(R.id.uncheckcomm)
        val check = inflateView.findViewById<ImageView>(R.id.checkcomm)

        val handler = Handler()

        MessageSingleton.messages.clear()
        MessageSingleton.messages.add("STARTED CONNECTION.\n")

        val refreshList: Runnable = object : Runnable {
            override fun run() {
                var messages = ""
                MessageSingleton.messages.forEach {
                    messages = "$messages$it"
                    message.text = messages

                    if (message.text.indexOf("FINISHED SUCESSFULL") > -1) {
                        progress.visibility = View.INVISIBLE
                        uncheck.visibility = View.INVISIBLE
                        check.visibility = View.VISIBLE
                    } else if (message.text.indexOf("FAILED") > -1) {
                        uncheck.visibility = View.VISIBLE
                        check.visibility = View.INVISIBLE
                        progress.visibility = View.INVISIBLE
                    }
                }
                handler.postDelayed(this, (3 * 1000).toLong())
            }
        }
        handler.postDelayed(refreshList, (1 * 1000).toLong())

        builder.setView(inflateView)
        builder.setCancelable(true)

        try {
            val dialog = builder.create()
            dialog.show()
        } catch (e: Exception) {
        }

    }
}