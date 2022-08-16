package br.com.crearesistemas.shift_leader.ui.datasync

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.wifi.WifiManager
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.crearesistemas.shift_leader.R
import br.com.crearesistemas.shift_leader.db_service.model.ShiftLeaderConfigMachine
import br.com.crearesistemas.shift_leader.db_service.viewmodel.ShiftLeaderConfigMachineViewModel
import br.com.crearesistemas.shift_leader.service.datasync.DataSyncService
import br.com.crearesistemas.shift_leader.singleton.ConfigSingleton
import br.com.crearesistemas.shift_leader.singleton.MessageSingleton
import br.com.crearesistemas.shift_leader.wifi_service.DataSync
import br.com.crearesistemas.shift_leader.wifi_service.WifiService
import br.com.crearesistemas.socket.client.service.ClientSocketService
import br.com.crearesistemas.socket.client.service.MachineStatusSingleton
import java.text.SimpleDateFormat
import java.util.*


/**
 * Este fragmento é responsável por gerenciar a interface de data sync
 */
class DataSyncFragment : Fragment() {

    private lateinit var dataSyncViewModel: DataSyncViewModel
    private val TAG: String = "Shift_Leader_DataSyncFragment"
    private val SDF: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")

    private fun hardReloadConnection(context: Context, message: String) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent!!.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        mainIntent.putExtra("message", message)
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        val handler = Handler()

        dataSyncViewModel =
            ViewModelProvider(this).get(DataSyncViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_datasync, container, false)

        val wifiManager = context?.getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager

        var wifiService =
            WifiService(requireContext(), wifiManager, ConfigSingleton.wifiDefaultPassword)
        var wifiList = ArrayList<DataSync>()
        var dataSyncListMachines = getDataSyncList(wifiManager, wifiList, wifiService)

        loadItems(wifiManager, dataSyncListMachines, root, wifiService)

        var lastSyncLabel = root.findViewById<TextView>(R.id.btn_last_sync_to_cloud)
        try {
            if (MachineStatusSingleton.lastSyncCloudTimestamp != null) {
                lastSyncLabel.text = resources.getText(R.string.btn_last_sync_to_cloud)
                    .toString() + " " + SDF.format(Date(MachineStatusSingleton.lastSyncCloudTimestamp!!))
            } else {
                lastSyncLabel.text = "No Sync Date"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Nome da maquina conectada
        if (wifiService.isConnected()) {
            val wifiConnectedName = wifiManager.connectionInfo?.ssid.toString()
            var machineNameEscapped = wifiConnectedName.replace("\"", "")
            MachineStatusSingleton.currentMachineName = machineNameEscapped
        }

        var btnToCloud = root.findViewById<Button>(R.id.btn_last_sync_cloud)
        btnToCloud.setOnClickListener {

            if (wifiService.isConnected()) {
                try {

                    val openModalLog = openModalLog()

                    val refreshList1: Runnable = object : Runnable {
                        override fun run() {
                            DataSyncService().connectToCloudRest(
                                requireActivity(),
                                requireActivity().application
                            ) { openModalLog?.dismiss() }
                        }
                    }
                    // Auto refresh na lista a cada 25s
                    handler.postDelayed(refreshList1, (25 * 1000).toLong())
                } catch (e: Exception) {

                    e.message?.let { it1 -> toastMessage(it1) }
                }
            }
        }

        var btnSyncAut = root.findViewById<Button>(R.id.btn_sync_aut)

        btnSyncAut.setOnClickListener {

            val alertDialog = AlertDialog.Builder(requireActivity()).create()
            alertDialog.setTitle("CLOUD MODE MUST BE INTERNET")
            alertDialog.setMessage("Please, disable all iOT connections and provide network with internet connection before Sync to cloud. ")
            alertDialog.setButton(
                AlertDialog.BUTTON_NEUTRAL, "Yes, i have internet connection."
            ) { dialog, which ->
                dialog.dismiss()

                Thread.sleep(10000)

                hardReloadConnection(requireActivity(), "msg")
            }
            alertDialog.show()

        }

        var btnConnectMachine = root.findViewById<Button>(R.id.btn_connect_machine)
        btnConnectMachine.setOnClickListener {

            openModalLogSocket()

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // START SERVICES
            val socketIntent = Intent(requireActivity(), ClientSocketService::class.java)
            socketIntent.putExtra("machineTabletAddress", 0)
            requireActivity().startService(socketIntent)
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        }

        wifiList = wifiService.getWifiList()

        val refreshListWifi: Runnable = object : Runnable {
            override fun run() {

                if (!wifiService.isConnected()) {
                    System.out.println("Tentando acessar lista.")
                    wifiList = wifiService.getWifiList()
                }

                handler.postDelayed(this, (120 * 1000).toLong())
            }
        }

        // Auto refresh na lista a cada 120s
        handler.postDelayed(refreshListWifi, (120 * 1000).toLong())

        val refreshList: Runnable = object : Runnable {
            override fun run() {

                try {

                    if (wifiService.isConnected()) {
                        val wifiConnectedName = wifiManager.connectionInfo?.ssid.toString()
                        var machineNameEscapped = wifiConnectedName.replace("\"", "")

                        if (MachineStatusSingleton.isConnectingStatus.containsKey(
                                machineNameEscapped
                            )
                        ) {
                            if (MachineStatusSingleton.isConnectingStatus[machineNameEscapped]!!) {
                                /*DataSyncService().connectToMachineRest(
                                    requireActivity(),
                                    wifiConnectedName, requireActivity().application
                                )*/
                            }
                        }

                        MachineStatusSingleton.currentMachineName = machineNameEscapped

                        updateColorFromMachineRule()
                    }

                } catch (e: Exception) {
                    var externalConnectionError = "External connection failed.  ${e.message}"
                    toastMessage(externalConnectionError)
                }

                dataSyncListMachines = getDataSyncList(wifiManager, wifiList, wifiService)
                loadItems(wifiManager, dataSyncListMachines, root, wifiService)

                // Auto refresh na lista a cada 25s
                handler.postDelayed(this, (25 * 1000).toLong())
            }
        }
        // Auto refresh na lista a cada 25s
        handler.postDelayed(refreshList, (25 * 1000).toLong())

        // Sincronia com DB
        AsyncTask.execute {

            val refreshListAsync: Runnable = object : Runnable {
                override fun run() {

                    try {
                        // Atualiza as configs de cada maquina exibida na lista
                        dataSyncListMachines.forEach {
                            try {

                                // Sync com base de config local
                                val machineConfigService =
                                    ShiftLeaderConfigMachineViewModel(requireActivity().application)
                                var machineConfig = machineConfigService.getBySsid(it.machineName)

                                if (machineConfig == null) {
                                    machineConfig = ShiftLeaderConfigMachine()
                                }

                                if (MachineStatusSingleton.activationStatus.containsKey(it.machineName) &&
                                    MachineStatusSingleton.activationStatus[it.machineName] != null
                                ) {
                                    machineConfig.ssid = it.machineName
                                    machineConfig.isConnected =
                                        MachineStatusSingleton.activationStatus[it.machineName]
                                    machineConfig.lastSyncTimestamp =
                                        MachineStatusSingleton.lastReadTimestamp[it.machineName].toString()
                                    machineConfigService.save(machineConfig)
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    try {
                        if (MachineStatusSingleton.lastSyncCloudTimestamp != null) {
                            lastSyncLabel.text = resources.getText(R.string.btn_last_sync_to_cloud)
                                .toString() + " " + SDF.format(
                                Date(
                                    MachineStatusSingleton.lastSyncCloudTimestamp!!
                                )
                            )
                        } else {
                            lastSyncLabel.text = "No Sync Date"
                        }
                    } catch (e: Exception) {
                        //e.printStackTrace()
                        lastSyncLabel.text = "No Sync Date"
                    }

                    handler.postDelayed(this, (10 * 1000).toLong())
                }
            }
            // Auto runner na lista a cada 30s
            handler.postDelayed(refreshListAsync, (10 * 1000).toLong())
        }

        return root
    }

//    fun teste(){
//        val wifiManager = context?.getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager
//
//        val wifiService = WifiService(requireContext(), wifiManager, ConfigSingleton.wifiDefaultPassword)
//
//
//        val dataSyncListMachines = getDataSyncList(wifiManager, wifiService.getWifiList(), wifiService)
//        loadItems(wifiManager, dataSyncListMachines, root, wifiService)
//    }

    private fun openModalLog(): AlertDialog? {

        val builder = AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog_Alert)
        val dialogView = layoutInflater.inflate(R.layout.feature_status_window, null)
        val message = dialogView.findViewById<TextView>(R.id.menssagerDialog)
        val progress = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val uncheck = dialogView.findViewById<ImageView>(R.id.uncheckcomm)
        val check = dialogView.findViewById<ImageView>(R.id.checkcomm)

        val handler = Handler()

        MessageSingleton.messages.clear()
        MessageSingleton.messages.add("STARTED COMMUNICATION TO CLOUD.\n")

        val refreshList: Runnable = object : Runnable {
            override fun run() {

                try {
                    var messages = ""
                    MessageSingleton.messages.forEach {
                        messages = "$messages$it"
                        message.text = messages

                        if (message.text.indexOf("FINISHED SUCCESSFUL") > -1) {
                            progress.visibility = View.INVISIBLE
                            uncheck.visibility = View.INVISIBLE
                            check.visibility = View.VISIBLE
                        } else if (message.text.indexOf("FAILED") > -1) {
                            uncheck.visibility = View.VISIBLE
                            check.visibility = View.INVISIBLE
                            progress.visibility = View.INVISIBLE
                        }
                    }
                } catch (e:Exception) {
                    e.printStackTrace()
                }

                handler.postDelayed(this, (3 * 1000).toLong())
            }
        }
        handler.postDelayed(refreshList, (1 * 1000).toLong())

        builder.setView(dialogView)
        builder.setCancelable(true)

        try {
            val dialog = builder.create()
            dialog.show()
            return dialog
        } catch (e: Exception) {

            e.printStackTrace()
        }
        return null
    }

    private fun openModalLogSocket() {

        val builder = AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog_Alert)
        val dialogView = layoutInflater.inflate(R.layout.feature_status_window, null)
        val message = dialogView.findViewById<TextView>(R.id.menssagerDialog)
        val progress = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val uncheck = dialogView.findViewById<ImageView>(R.id.uncheckcomm)
        val check = dialogView.findViewById<ImageView>(R.id.checkcomm)

        val handler = Handler()

        MachineStatusSingleton.messagesSocket.clear()
        MachineStatusSingleton.messagesSocket.add("STARTED COMMUNICATION TO MACHINE.\n")

        val refreshList: Runnable = object : Runnable {
            override fun run() {

                try {
                    var messages = ""
                    MachineStatusSingleton.messagesSocket.forEach {
                        messages = "$messages$it"
                        message.text = messages

                        if (message.text.indexOf("FINISHED SUCCESSFUL") > -1) {
                            progress.visibility = View.INVISIBLE
                            uncheck.visibility = View.INVISIBLE
                            check.visibility = View.VISIBLE
                        } else if (message.text.indexOf("FAILED") > -1) {
                            uncheck.visibility = View.VISIBLE
                            check.visibility = View.INVISIBLE
                            progress.visibility = View.INVISIBLE
                        }
                    }
                } catch (e:Exception) {
                    e.printStackTrace()
                }

                handler.postDelayed(this, (3 * 1000).toLong())
            }
        }
        handler.postDelayed(refreshList, (1 * 1000).toLong())

        builder.setView(dialogView)
        builder.setCancelable(true)
        builder.show()

        try {
            val dialog = builder.create()
            dialog.show()
        } catch (e: Exception) {
        }
    }

    /**
     * Popula as listas com conteudo
     */
    private fun loadItems(
        wifiManager: WifiManager,
        dataSyncListMachines: ArrayList<DataSync>,
        root: View,
        wifiService: WifiService
    ) {
        try {

            var machineList = ArrayList<String>()
            var machineDateList = ArrayList<String>()
            var machineSignalList = ArrayList<Int>()
            var machineStatusList = ArrayList<Int>()
            var connectedList = ArrayList<Boolean>()

            dataSyncListMachines
                .sortedWith(
                    compareBy(
                        { it.connected },
                        { it.status },
                        { -it.signal },
                        { it.machineName })
                )
                .forEach {
                    machineList.add(it.machineName)
                    machineDateList.add(it.strDate)
                    machineStatusList.add(it.status)
                    connectedList.add(it.connected)
                    machineSignalList.add(it.signal)
                }

            var dataSyncListAdapter = DataSyncListAdapter(
                context as Activity,
                machineList,
                machineDateList,
                machineStatusList,
                connectedList,
                machineSignalList,
                wifiService
            )

            val listView: ListView = root.findViewById(R.id.listview_datasync_machines)
            listView.adapter = dataSyncListAdapter

            listView.setOnItemClickListener { adapterView, view, position, id ->
                val itemAtPos = adapterView.getItemAtPosition(position)
                val itemIdAtPos = adapterView.getItemIdAtPosition(position)
                val show: Any = Toast.makeText(
                    requireActivity(),
                    "Selecionado $itemAtPos um item id $itemIdAtPos",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {
            Log.d(TAG, "Erro ao montar lista ${e.message} ")
        }

    }

    private fun getDataSyncList(
        wifiManager: WifiManager,
        wifiList: List<DataSync>,
        wifiService: WifiService
    ): ArrayList<DataSync> {

        var dataSyncList = ArrayList<DataSync>()

        try {

            var dataSyncMachineList = DataSyncService().listMachines(requireActivity().application)

            dataSyncMachineList.forEach {

                var dataSync = DataSync()

                dataSync.strDate = SDF.format(it.lastOnDate)
                dataSync.machineName = it.name
                dataSync.connected = false
                dataSync.signal = R.drawable.wifi_empty

                dataSync.status = buildDataSyncStatus(it.name)

                val wifiConnectedName = wifiManager.connectionInfo?.ssid.toString()
                var machineNameEscapped = wifiConnectedName.replace("\"", "")

                if (machineNameEscapped == it.name) {

                    dataSync.signal =
                        br.com.crearesistemas.shift_leader.wifi_service.R.drawable.wifi_half

                } else {
                    wifiList.forEach { _itWifi ->
                        if (_itWifi.machineName == it.name) {
                            dataSync.signal = _itWifi.signal
                        }
                    }
                }

                if (!wifiService.isConnected()) {
                    wifiService.connectToMachine(it.name, false)
                    updateColorFromMachineRule()
                }

                dataSyncList.add(dataSync)
            }

        } catch (e: Exception) {
            Log.d(TAG, "Erro ao montar listagem ${e.message}.")
        }

        return dataSyncList
    }

    private fun updateColorFromMachineRule() {

        try {
            val readTimeoutMap = MachineStatusSingleton.lastReadTimestamp
            val currentTime = Date().time
            readTimeoutMap.keys.forEach { key ->
                val lastTimeOfMachine = readTimeoutMap[key]
                val comparisionTime = (currentTime - lastTimeOfMachine!!)
                if (lastTimeOfMachine > 0L && comparisionTime < MachineStatusSingleton.greenStatusTimeout) {
                    MachineStatusSingleton.colorStatus[key] = 1
                } else if (comparisionTime > MachineStatusSingleton.greenStatusTimeout
                    && comparisionTime < MachineStatusSingleton.yellowStatusTimeout
                ) {
                    MachineStatusSingleton.colorStatus[key] = 2
                } else {
                    MachineStatusSingleton.colorStatus[key] = 3
                }
            }
        } catch (e: Exception) {
            var messageError = e.message
            e.printStackTrace()
            Log.d(TAG, "Erro ao atualizar cores do datasync list")
        }

    }

    private fun buildDataSyncStatus(machineName: String): Int {
        try {
            var machineMap = MachineStatusSingleton.colorStatus
            if (machineMap.containsKey(machineName)) {
                when (machineMap[machineName]) {
                    1 -> {
                        return R.drawable.status_green
                    }
                    2 -> {
                        return R.drawable.status_yellow
                    }
                    3 -> {
                        return R.drawable.status_red
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "Erro ao atualizar cores do datasync list")
        }
        return R.drawable.status_red
    }

    private fun toastMessage(message: String) {
        try {
            Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.d(TAG, "Erro ao logar mensagem")
        }
    }

    // TODO isaias Need refactory?
    private var remoteClientServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
//            boundToService = false
//            serverService = null
        }

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.d(TAG, "remoteClientServiceConnection::onServiceConnected")

//            if(!boundToService) {
//                boundService = binder as SocketAbstractController?
//                serverService = boundService?.service as ServerSocketService
//                serverService?.registerListener(_SocketServiceBindable)
//                boundToService = true
//            }
        }
    }

}