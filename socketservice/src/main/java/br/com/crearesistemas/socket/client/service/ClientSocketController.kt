package br.com.crearesistemas.socket.client.service


import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import br.com.crearesistemas.shift_leader.db_service.model.*
import java.io.OutputStream
import java.net.Socket
import java.nio.charset.Charset
import java.util.*
import br.com.crearesistemas.shift_leader.db_service.viewmodel.*
import com.google.gson.Gson
import kotlin.random.Random.Default.nextInt

/**
 *      CREARE SISTEMAS 2021 - ORBCOMM COMMUNICATOR SERVICE
 *
 *      @author: Luiz Gregory
 *      @email: luiz.gregory@crearesistemas.com.br
 *
 */
open class ClientSocketController(val application: Application)
{
    /**
     *
     */
    private var runThread: RunThread? = null

    private lateinit var appointmentsService: AppointmentViewModel
    private lateinit var appointmentsGroupService: AppointmentGroupViewModel
    private lateinit var checklistService: ChecklistViewModel
    private lateinit var driverService: DriverViewModel
    private lateinit var mechanizationService: MachineMechanizationTypeViewModel
    private lateinit var farmSectorsService: FarmSectorViewModel
    private lateinit var farmCompartmentService: FarmCompartmentViewModel
    private lateinit var activityTypeService: MachineActivityTypeViewModel
    private lateinit var equipmentService: MachineEquipmentViewModel
    private lateinit var appointmentDataService: AppointmentDataViewModel
    private lateinit var appointmentMachineTypeService: AppointmentMachineTypeViewModel
    private lateinit var maintenanceTaskService: MaintenanceTaskViewModel

    private lateinit var telemetryModel: TelemetryViewModel
    private lateinit var machineSetupModel: MachineSetupViewModel
    private lateinit var machineChecklistModel: ChecklistSavedViewModel
    private lateinit var machineAppoinmentModel: AppointmentSavedViewModel
    private lateinit var driverLoginViewModel: DriverLoginViewModel
    private lateinit var productionHarvesterViewModel: ProductionHarvesterViewModel




    val SERVER_ADDR = "10.0.0.5"
//    val SERVER_ADDR = "192.168.0.129"

    val PORT = 6589

    /**
     *
     */
    companion object
    {
        private val TAG = ClientSocketController::class.java.simpleName
    }


    /**
     *
     */
    fun start()
    {
        if (this.runThread != null) {
            return
        }

        if (!this.connect()) {
            Log.e(TAG, "FAIL TO OPEN SOCKET SERVER")
            return
        }
    }


    /**
     *
     */
    private fun connect(): Boolean
    {
        this.appointmentsService = AppointmentViewModel(application)
        this.appointmentsGroupService = AppointmentGroupViewModel(application)
        this.checklistService = ChecklistViewModel(application)
        this.driverService = DriverViewModel(application)
        this.mechanizationService = MachineMechanizationTypeViewModel(application)
        this.farmSectorsService = FarmSectorViewModel(application)
        this.farmCompartmentService = FarmCompartmentViewModel(application)
        this.activityTypeService = MachineActivityTypeViewModel(application)
        this.equipmentService = MachineEquipmentViewModel(application)
        this.appointmentDataService = AppointmentDataViewModel(application)
        this.appointmentMachineTypeService = AppointmentMachineTypeViewModel(application)
        this.maintenanceTaskService = MaintenanceTaskViewModel(application)

        this.telemetryModel = TelemetryViewModel(application)
        this.machineSetupModel = MachineSetupViewModel(application)
        this.machineChecklistModel = ChecklistSavedViewModel(application)
        this.machineAppoinmentModel = AppointmentSavedViewModel(application)
        this.driverLoginViewModel = DriverLoginViewModel(application)
        this.productionHarvesterViewModel = ProductionHarvesterViewModel(application)



        Log.d(TAG,"connect()::#################################################################################################################")
        Log.d(TAG, "connect()::PORT:: $SERVER_ADDR:$PORT")
        MachineStatusSingleton.messages.add("CONNECTING $SERVER_ADDR:$PORT \n")

        synchronized(this) {
            this.runThread?.interrupted = true
            this.runThread?.interrupt()
            this.runThread = null
            this.runThread = RunThread()
            this.runThread?.start()
        }

        return true
    }


    /**
     *
     */
    fun onDestroy()
    {
        Log.d(TAG, "disconnect()")
        MachineStatusSingleton.messages.add("DISCONNECTING. \n")
        synchronized(this) {
            this.runThread?.interrupted = true
            this.runThread?.interrupt()
            this.runThread = null
        }
    }


    /**
     * Thread que controla a comunicação seria com o terminal satelital
     */
    protected inner class RunThread : Thread()
    {

        var interrupted: Boolean = false
        private var connection: Socket? = null
        private var reader: Scanner? = null
        private var writer: OutputStream? = null
        private var readContent: String = ""
        private var timeout: Int = 5
        private var frameArray: List<String>? = null
        private var code: Long? = 0
        private var command: String? = ""
        private var content: String? = ""


        /**
         * Método principal
         */
        override fun run()
        {
            do {
                try {
                    if (!this.socketConnect()) {
                        continue
                    }

                    Log.d(TAG,"###########################################################################################################################")
                    Log.d(TAG, "# SOCKET RECEIVE DATA : START ")
                    this.getList("getLogin",        driverLoginViewModel.getMaxDate( this.getCurrentWiFiSSID()!! ))
                    this.getList("getTelemetry",    telemetryModel.getMaxDate( this.getCurrentWiFiSSID()!! ))
                    this.getList("getSetup",        machineSetupModel.getMaxDate( this.getCurrentWiFiSSID()!! ))
                    this.getList("getChecklist",    machineChecklistModel.getMaxDate( this.getCurrentWiFiSSID()!! ))
                    this.getList("getAppointment",  machineAppoinmentModel.getMaxDate( this.getCurrentWiFiSSID()!! ))
                    this.getList("getProduction",   productionHarvesterViewModel.getMaxDate( this.getCurrentWiFiSSID()!! ))


                    Log.d(TAG, "# SOCKET RECEIVE DATA : FINAL ")
                    Log.d(TAG,"###########################################################################################################################")
                    Log.d(TAG, "# SOCKET SEND DATA : START ")

                    val maintenanceTaskList = maintenanceTaskService.getAll()
                    this.sendListItem("saveMaintenanceTask", maintenanceTaskList )
                    MachineStatusSingleton.messagesSocket.add("SENDING MAINTENANCE TASK ${maintenanceTaskList.size}. \n")

                    val driverList = driverService.getAll()
                    this.sendListItem("saveDriver", driverList)
                    MachineStatusSingleton.messagesSocket.add("SENDING DRIVER ${driverList.size}. \n")

                    val mechanizationList = mechanizationService.getAll()
                    this.sendListItem("saveMechanizations", mechanizationList)
                    MachineStatusSingleton.messagesSocket.add("SENDING MECHANIZATION ${mechanizationList.size}. \n")

                    val activityTypeList = activityTypeService.getAll()
                    this.sendListItem("saveActivityType", activityTypeList)
                    MachineStatusSingleton.messagesSocket.add("SENDING ACTIVITY TYPE ${activityTypeList.size}. \n")

                    val equipmentList = equipmentService.getAll()
                    this.sendListItem("saveEquipment", equipmentList)
                    MachineStatusSingleton.messagesSocket.add("SENDING EQUIPMENTS ${equipmentList.size}. \n")

                    val checklistList = checklistService.getAll()
                    this.sendListItem("saveChecklist", checklistList)
                    MachineStatusSingleton.messagesSocket.add("SENDING CHECKLISTS ${checklistList.size}. \n")

                    val appointmentsGroupList = appointmentsGroupService.getAll()
                    this.sendListItem("saveAppointmentsGroup", appointmentsGroupList)
                    MachineStatusSingleton.messagesSocket.add("SENDING APPOINTMENTS GROUP ${appointmentsGroupList.size}. \n")

                    val appointmentDataList = appointmentDataService.getAll()
                    this.sendListItem("saveAppointmentData", appointmentDataList)
                    MachineStatusSingleton.messagesSocket.add("SENDING APPOINTMENTS DATA ${appointmentDataList.size}. \n")

                    val appointmentMachineTypeList = appointmentMachineTypeService.getAll()
                    this.sendListItem("saveAppointmentMachineType", appointmentMachineTypeList)
                    MachineStatusSingleton.messagesSocket.add("SENDING APPOINTMENTS MACHINE TYPE ${appointmentMachineTypeList.size}. \n")

                    val appointmentList = appointmentsService.getAll()
                    this.sendListItem("saveAppointments", appointmentList)
                    MachineStatusSingleton.messagesSocket.add("SENDING APPOINTMENTS ${appointmentList.size}. \n")

                    val farmSectorList = farmSectorsService.getAll()
                    this.sendListItem("saveFarmSectors", farmSectorList)
                    MachineStatusSingleton.messagesSocket.add("SENDING FARM SECTORS ${farmSectorList.size}. \n")

                    val farmCompartmentList = farmCompartmentService.getAll()
                    this.sendListItem("saveFarmCompartment", farmCompartmentList )
                    MachineStatusSingleton.messagesSocket.add("SENDING FARM COMPARTMENTS ${farmCompartmentList.size}. \n")

                    Log.d(TAG, "# SOCKET SEND DATA : FINAL ")

                    MachineStatusSingleton.messagesSocket.add("ALL DATA SENT. \n")
                    MachineStatusSingleton.messagesSocket.add("FINISHED SUCCESSFUL")

                    // Marca o status verde para a maquina atual (sinaliza que a transferencia de dados foi sucedida)
                    if (MachineStatusSingleton.currentMachineName !=null) {
                        MachineStatusSingleton.lastReadTimestamp[MachineStatusSingleton.currentMachineName!!] = Date().time
                    }

                    Log.d(TAG,"###########################################################################################################################")

                    return this.socketDisconnect()
                    //sleep(1000 * 60 )
                }
                catch (ex: Exception) {
                    println(ex.printStackTrace())
                    MachineStatusSingleton.messagesSocket.add("DATA SEND FAILED ${ex.message}. \n")
                    return this.socketDisconnect()
                }
            }
            while (!this.interrupted)
        }


        /**
         *
         */
        private fun sendListItem(method: String, list: List<*>): Boolean
        {
            var total = list.size
            Log.d(TAG, "sendListItem: $method : $total")
            if (total == 0) {
                this.sendFinish(total.toLong())
                return this.waitingAck(total.toLong())
            }

            var attempts = 0
            var i = 0
            while (i < total) {
                var jItem = Gson().toJson(list[i])
                this.write("CS|$method|$i|$jItem|")
                if (!this.waitingAck(i.toLong())) {
                    if (attempts++ >= 10) return false
                    continue
                }
                i++
            }

            this.sendFinish(total.toLong())
            if (!this.waitingAck(total.toLong())) {
                return false
            }
            return true
        }


        /**
         *
         */
        private fun getList(method: String, maxDate: String): Boolean
        {
            MachineStatusSingleton.messagesSocket.add("RECEIVED: $method; ")

            var i = (0..100).random()
            this.write("CS|$method|$i|$maxDate|")
            if (!this.waitingAck(i.toLong())) {
                MachineStatusSingleton.messagesSocket.add("FAIL! \n")
                return false
            }
            var t = 0;

            while (!this.waitingCommand().isNullOrEmpty()) {
                try {
                    when (this.command!!) {
                         "saveLogin"        -> driverLoginViewModel.save( Gson().fromJson( this.content!!, DriverLogin::class.java ) )
                         "saveTelemetry"    -> telemetryModel.save( Gson().fromJson( this.content!!, Telemetry::class.java ) )
                         "saveSetup"        -> machineSetupModel.save( Gson().fromJson( this.content!!, MachineSetup::class.java ) )
                         "saveChecklist"    -> machineChecklistModel.save( Gson().fromJson( this.content!!, ChecklistSaved::class.java ) )
                         "saveAppointment"  -> machineAppoinmentModel.save( Gson().fromJson( this.content!!, AppointmentSaved::class.java ) )
                         "saveProduction"   -> productionHarvesterViewModel.save( Gson().fromJson( this.content!!, ProductionHarvester::class.java ) )
                         "finish"           -> {
                             MachineStatusSingleton.messagesSocket.add("SUCCESS! TOTAL: $t \n")
                             return true
                         }
                    }
                    t++
                    Log.d(TAG, "${this.command}: SUCESS ")
                }
                catch (e: Exception) {
                    e.printStackTrace()
                    return false
                }
            }

            return false
        }


        /**
         *
         */
        private fun waitingCommand(): String?
        {
            var content = this.readLine()
            if (content.isNullOrEmpty()) {
                return null
            }
            if (!this.checkFrame(content)) {
                this.sendNack(0, "InvalidFrame")
                return null
            }
            if (!this.checkCRC(content)) {
                this.sendNack(0, "InvalidCRC")
                return null
            }

            this.code = this.frameArray!![2].toLong()
            this.command = this.frameArray!![1]
            this.content = this.frameArray!![3]

            if (this.command in arrayOf("ACK", "NACK")) {
                return null
            }

            this.sendAck(this.code!!, this.command!!)
            return this.command
        }


        /**
         *  Valida de a estrutura basica do frame é válida
         */
        private fun checkFrame(frame: String): Boolean
        {
            val pattern = Regex("^CS\\|[a-zA-Z0-9\\-]{1,30}\\|.*\\|[0-9]{1,6}\$")
            val ans: MatchResult? = pattern.find(frame)
            if (ans?.value.isNullOrEmpty()) {
                Log.d(TAG, "INVALID FRAME: $frame")
                return false
            }
            return true
        }


        /**
         *
         */
        private fun sendAck(code: Long, command: String)
        {
            this.write("CS|ACK|$code|$command|")
        }


        /**
         *
         */
        private fun sendFinish(code: Long)
        {
            this.write("CS|finish|$code|")
        }

        /**
         *
         */
        private fun sendNack(code: Long, reason: String)
        {
            this.write("CS|NACK|$code|$reason|")
        }


        /**
         *
         */
        private fun write(message: String)
        {
            try {
                sleep(35)
                val crc = this.getCRC(message)
                val complete = "$message$crc"
                this.writer?.write("$complete\n".toByteArray(Charset.defaultCharset()))
                Log.d(TAG, " -> write( $complete )")
            } catch (e: Exception) {
            }
        }


        /**
         * CRC-16/CCITT-FALSE
         */
        private fun getCRC(frame: String): Int {
            var crc = 0xFFFF
            var charArray = frame.toByteArray(Charsets.UTF_8)
            for (element in charArray) {
                var x = ((crc shr 8) xor element.toInt()) and 0xFF
                x = x xor (x shr 4)
                crc = ((crc shl 8) xor (x shl 12) xor (x shl 5) xor x) and 0xFFFF
            }
            return crc
        }


        /**
         *
         */
        private fun waitingAck(commandId: Long): Boolean
        {
            val ackFrame = this.readLine()
            if (ackFrame.isEmpty()) {
                return false
            }
            if (!this.checkFrame(ackFrame)) {
                return false
            }
            if (!this.checkCRC(ackFrame)) {
                return false
            }
            if (this.frameArray!![1] !in arrayOf("ACK")) {
                return false
            }
            if (this.frameArray!![2].toLong() != commandId) {
                return false
            }

            return true
        }


        /**
         *
         */
        private fun checkCRC(frame: String): Boolean {
            this.frameArray = frame.split("|")
            val frameCRC = this.frameArray!!.last().trim().replace(".0", "")
            val pattern = Regex("[0-9.]{1,8}$")
            val nframe = frame.replace(pattern, "")
            val crcCalc = getCRC(nframe)
            return crcCalc == frameCRC.toInt()
        }


        /**
         *
         */
        private fun readLine(): String {

            val startRead = Date()
            do {
                sleep(10)
            }
            while ((this.differenceInSeconds( startRead, Date() ) <= this.timeout) && this.readContent.isEmpty() )


            if (!this.readContent.isNullOrEmpty()) {
                Log.d(TAG, " <- read: ${this.readContent}")
            }

            val res = this.readContent
            this.readContent = ""
            return res
        }


        /**
         *
         */
        inner class ReadThread : Thread()
        {
            override fun run() {
                while (true) {
                    try {
                        readContent = reader?.nextLine().toString()
                        sleep(10)
                    }
                    catch (e: Exception) {
                    }
                }
            }
        }


        /**
         *
         */
        private fun differenceInSeconds(startDate: Date, endDate: Date): Long {
            return (endDate.time - startDate.time) / 1000
        }


        /**
         *
         */
        private fun socketConnect(): Boolean {
            if (this.connection == null || !this.connection!!.isConnected) {
                try {
                    Log.d(TAG,"############################################################################################")
                    Log.d(TAG, "# Socket Connection...")
                    this.connection = Socket(SERVER_ADDR, PORT)
                    this.reader = Scanner(this.connection!!.getInputStream())
                    this.writer = this.connection!!.getOutputStream()
                    ReadThread().start()
                    Log.d(TAG, "# Socket Connection: OK!! ")
                    sleep(1000)
                } catch (e: Exception) {
                    Log.e(TAG, "Socket Connection Fail!")
                    MachineStatusSingleton.messages.add("SOCKET CONNECTION FAILED. \n")
                    sleep(5000)
                    return false
                }
            }
            return true
        }


        /**
         *
         */
        private fun socketDisconnect() {
            try {
                this.write("CS|disconnect|0|")
                sleep(100)
                ReadThread().interrupt()
                this.reader?.close()
                this.writer?.close()
                this.connection?.close()
                this.connection = null
                onDestroy()

            } catch (e: Exception) {
            }
        }


        /**
         *
         */
        private fun getCurrentWiFiSSID(): String? {
            val wifiManager =
                application.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            return wifiManager.connectionInfo.ssid.replace("\"", "")
        }

    }

}