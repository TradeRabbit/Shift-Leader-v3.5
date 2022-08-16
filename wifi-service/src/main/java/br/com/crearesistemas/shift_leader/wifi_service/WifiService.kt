package br.com.crearesistemas.shift_leader.wifi_service


import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.thanosfisherman.wifiutils.WifiUtils
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionErrorCode
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionSuccessListener

/**
 * Essa classe é responsável por gerenciar o serviço de WIFI dentro do App,
 * o controle deve ser feito por aqui e tarefas como inicio, conexão, desconexão
 * e descoberta serão tratados aqui
 */
class WifiService(
    private val context: Context,
    private val wifiManager: WifiManager,
    private val wifiDefaultPassword: String?
) {

    private val TAG: String = "Shift_Leader_WifiService"

    object DataSyncWifiList {
        init {
        }

        var running = false
        var hasNetworks = false
        var wifiList: ArrayList<DataSync> = ArrayList<DataSync>()
    }

    fun getWifiList(): ArrayList<DataSync> {
        //if (!DataSyncWifiList.running) {
        initService()
        //wifiManager.startScan()
        //}

        return DataSyncWifiList.wifiList
    }

    private fun connect(ssid: String, pass: String) {
        connect(ssid, pass, true)
    }

    private fun connectAgain(ssid: String, pass: String) {
        connect(ssid, pass, false)
    }

    private fun connect(ssid: String, pass: String, recursive: Boolean) =
        WifiUtils.withContext(context)
            .connectWith(ssid, pass)
            .setTimeout(40000)
            .onConnectionResult(object : ConnectionSuccessListener {
                override fun success() {
                    Toast.makeText(context, "SUCCESS connected to $ssid.", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun failed(errorCode: ConnectionErrorCode) {

                    var errorMessage = errorCode.toString()

                    var fatalErrorMessage = ""

                    fatalErrorMessage = if (errorMessage == "COULD_NOT_SCAN") {
                        "Network $ssid reject your connection."
                    } else {
                        "|$ssid| error: "
                    }

                    Toast.makeText(
                        context,
                        "$fatalErrorMessage: $errorMessage",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            })
            .start()


    private fun getScanResults(results: List<ScanResult>) {
        if (results.isEmpty()) {
            Log.i(TAG, "SCAN RESULTS IT'S EMPTY")
            return
        }
        Log.i(TAG, "GOT SCAN RESULTS " + results)

        DataSyncWifiList.wifiList = ArrayList<DataSync>()

        results.forEach {
            val level = WifiManager.calculateSignalLevel(it.level, 4)

            var wifiInfo = wifiManager.connectionInfo
            var connected = wifiInfo.bssid == it.BSSID

            var ds = DataSync()

            ds.machineName = it.SSID
            ds.strDate = ""
            ds.signal = level

            // Define o icone da intensidade do sinal
            when (level) {
                0 -> ds.signal = R.drawable.wifi_empty
                1 -> ds.signal = R.drawable.wifi_low
                2 -> ds.signal = R.drawable.wifi_half
                3 -> ds.signal = R.drawable.wifi_full
                //4 -> ds.signal = R.drawable.wifi_level_4
            }

            if (level > 0) {
                DataSyncWifiList.hasNetworks = true
            }

            ds.connected = connected

            DataSyncWifiList.wifiList.add(ds)
        }
    }

    /**
     * Inicia a execução do serviço de wifi com a criação de um objeto BroadcastReceiver
     * Quem fará o controle de acesso a lista de WIFI será o próprio Android.
     */
    private fun initService() {

        DataSyncWifiList.running = true

        WifiUtils.withContext(context).scanWifi(this::getScanResults).start()

    }

    /**
     * Obtém a config wifi de um determinado ssid
     */
    fun getWiFiConfig(ssid: String): WifiConfiguration? {

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            for (item in wifiManager.configuredNetworks) {
                if (item.SSID != null && item.SSID.equals(ssid)) {
                    return item
                }
            }
        }

        return null
    }

    /**
     * Faz a criação de um profile WPA visando
     * gerenciar corretamente o estoque de
     * informações de segurança da wifi WPA
     */
    fun createWPAProfile(ssid: String, pass: String) {
        Log.d(TAG, "Saving SSID: $ssid")

        val conf = WifiConfiguration()
        conf.SSID = ssid
        conf.preSharedKey = pass

        wifiManager.addNetwork(conf)

        Log.d(TAG, "Saved SSID $ssid to WiFiManger")
    }

    /**
     * Verifica se um determinado ssid esta conectado neste momento
     */
    fun isConnected(): Boolean {
        return WifiUtils.withContext(context).isWifiConnected
    }

    /**
     * Essa função fará a conexão com a maquina recebida por parametro
     */
    fun connectToMachine(machine: String, showMessage: Boolean) {

        if (showMessage) {
            try {
                Toast.makeText(context, "Connecting ... ", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.d("WifiUtil", "Erro ao logar mensagem")
            }
        }

        try {

            var machineActivationStatus =
                br.com.crearesistemas.socket.client.service.MachineStatusSingleton.activationStatus
            if (machineActivationStatus.containsKey(machine)) {
                var activationStatus = machineActivationStatus[machine]
                if (activationStatus == true) {

                    var pass = wifiDefaultPassword ?: "teste123"
                    if (br.com.crearesistemas.socket.client.service.MachineStatusSingleton.passwords.containsKey(
                            machine
                        )
                    ) {
                        pass =
                            br.com.crearesistemas.socket.client.service.MachineStatusSingleton.passwords[machine].toString()
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        //    wifiSuggestion(machine, pass)
                    }
                    connect(machine, pass)

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val message = "Erro ao tentar conectar a maquina $machine"
            Log.d("WifiUtil", message)
        }

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun wifiSuggestion(network: String, pass: String) {

        /* val suggestion2 = WifiNetworkSuggestion.Builder()
             .setSsid("RANSONWARE_2G")
             .setWpa2Passphrase("inpm7099")
             .setIsAppInteractionRequired(true) // Optional (Needs location permission)
             .build()*/

        val suggestion1 = WifiNetworkSuggestion.Builder()
            .setSsid(network)
            .setWpa2Passphrase(pass)
            .setIsAppInteractionRequired(true) // Optional (Needs location permission)
            .build()

        val suggestionsList = listOf(suggestion1)

        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val status = wifiManager.addNetworkSuggestions(suggestionsList)
        if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            // do error handling here
        }

        // Optional (Wait for post connection broadcast to one of your suggestions)
        val intentFilter = IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)

        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!intent.action.equals(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                    return
                }
                // do post connect processing here
            }
        }
        context.registerReceiver(broadcastReceiver, intentFilter)
    }

}