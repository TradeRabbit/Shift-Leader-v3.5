package br.com.crearesistemas.shift_leader.service.volley

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import br.com.crearesistemas.shift_leader.db_service.model.AppointmentSaved
import br.com.crearesistemas.shift_leader.db_service.model.MaintenanceTask
import br.com.crearesistemas.shift_leader.db_service.viewmodel.*
import br.com.crearesistemas.shift_leader.dto.DataCollectorDto
import br.com.crearesistemas.shift_leader.dto.UserRequestDto
import br.com.crearesistemas.shift_leader.singleton.MessageSingleton
import com.google.gson.Gson

/**
 * Responsável por gerenciar conexões de dados do tipo appointment
 * @author Isaias Alves
 */
class ShiftLeaderVolleyService(var application: Application) : VolleyService()
{

    private var userRequest = UserRequestDto("master", "TddUBRjP5UXBZafrgR")
    private var serviceApi  = ShiftLeaderApiService()
    private lateinit var token : String

    /**
     *
     */
    companion object
    {
        private val TAG = ShiftLeaderVolleyService::class.java.simpleName
    }

    fun syncToCloud(context: Context)
    {
        initContext(application)

        Log.d(TAG, "################################################################################################")
        Log.d(TAG, "START SYNC TO CLOUD")

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.bindProcessToNetwork(null)

        if( this.cloudLogin() ) {
            if (!this.sendAllDataToCloud()) return
            if (!this.clearIfNecessary()) return
            if(!this.getMachineList()) return
            if(!this.getUserList()) return
            if(!this.getAllDataFromCloud()) return
        } else {
            return
        }

    }

    /**
     * Efetua o login na cloud
     */
    private fun cloudLogin(): Boolean
    {
        var loginResponse = this.serviceApi.api.login(this.userRequest).execute()
        if (!loginResponse.isSuccessful) {
            logMessage("LOGIN FAILED INTO API")
            return false
        }

        var loginBody = loginResponse.body()
        var jsonReceived = loginBody.toString().trim()

        if (jsonReceived.isNullOrEmpty()) {
            logMessage("LOGIN FAILED: EMPTY CONTENT")
            return false
        }

        this.token = loginBody!!.token.toString().trim()
        if(this.token.isNullOrEmpty()) {
            logMessage("LOGIN FAILED: INVALID TOKEN")
            return false
        }
        logMessage("API LOGIN SUCESS")
        logMessage("--------------------------------------------")
        return true
    }

    /**
     * Busca a lista de maquinas na cloud
     */
    private fun getMachineList(): Boolean
    {
        logMessage("GET MACHINE LIST", false)

        var machines = this.serviceApi.api.machineGetAll(this.token).execute()

        if (machines.body().isNullOrEmpty()) {
            logMessage("MACHINE LIST BODY IS NULL")
            return false
        }

        val machineService = MachineViewModel(application)
        logMessage("MACHINE LIST SIZE: "+ machines.body()!!.size,false)

        machines.body()!!.forEach {
            machineService.save(it)
        }

        logMessage("MACHINE LIST RECEIVED: "+ machines.body()!!.size)

        return true
    }

    /**
     * Busca a lista de maquinas na cloud
     */
    private fun getUserList(): Boolean
    {
        logMessage("GET USER LIST", false)

        var users = this.serviceApi.api.userGetAll(this.token).execute()

        if (users.body().isNullOrEmpty()) {
            logMessage("USER LIST BODY IS NULL")
            return false
        }

        val userService = UserViewModel(application)
        logMessage("USER LIST SIZE: "+ users.body()!!.size,false)

        users.body()!!.forEach {
            userService.save(it)
        }

        logMessage("USER LIST RECEIVED: "+ users.body()!!.size)

        return true
    }

    private fun logMessage(message:String) {
        logMessage(message, true)
    }

    private fun logMessage(message:String, addToPanel: Boolean) {
        Log.d(TAG, message)
        if (addToPanel) {
            MessageSingleton.messages.add(message+"\n")
        }
    }

    private fun sendAppointmentsToCloud() {

        try {
            var appointmentSavedService = AppointmentSavedViewModel(application)

            var appointments = appointmentSavedService.getAllNotSent()
            var appointmentsPartial = ArrayList<AppointmentSaved>()

            var i =0
            appointments.forEach {

                appointmentsPartial.add(it)

                if ( i >= 100 ) {

                    // Primeiro Grupo
                    if (sendAppointmentGroup(appointmentsPartial,appointmentSavedService )) {
                        // Sucesso ao salvar grupo de apontamentos (100)
                        logMessage("PARTIAL APPOINMENT SEND: $i ", false)
                    }

                    appointmentsPartial = ArrayList<AppointmentSaved>()
                    i = 0
                }
                i++
            }

            // Ultimo grupo
            if (!appointmentsPartial.isNullOrEmpty()) {
                if (sendAppointmentGroup(appointmentsPartial,appointmentSavedService )) {
                    // Sucesso ao salvar grupo de apontamentos appointmentsPartial.size
                    logMessage("PARTIAL SEND LAST success", false)
                }
            }

        } catch (e:Exception) {
            logMessage("[FAIL] APPOINTMENT SEND ERROR " + e.message)
            e.printStackTrace()

        }
    }

    private fun sendAppointmentGroup(appointmentsPartial : ArrayList<AppointmentSaved>,
                                   appointmentSavedService:AppointmentSavedViewModel) : Boolean {
        var successPost = false

        var dataCollectorPartial = DataCollectorDto()
        dataCollectorPartial.appointmentSavedList = appointmentsPartial

        var jsonToPostPartial = Gson().toJson(dataCollectorPartial)

        postToCloud("data-collector", jsonToPostPartial, token, object : VolleyCallback {
            override fun onSuccessResponse(result: String?) {
                successPost = true
            }
        },"AppointmentGroup_Save")

        if (successPost) {
            appointmentsPartial.forEach { itAppointment ->
                itAppointment.sentToCloud = true
                appointmentSavedService.save(itAppointment)
            }
        }

        return successPost
    }



    private fun sendAllDataToCloud() : Boolean {
        //enviando tudo para cloud
        try {

            // Salva os appointments prioritariamente
            sendAppointmentsToCloud();

            var jsonToPost = ""
            var stepLocation = ""

            /** Salvando appointmentSemiMechanized  ***/
            Log.d(TAG,"**** AppointmentSemiMechanized")
            var dataCollector = DataCollectorDto()
            var semiMechanizedService = AppointmentSemiMechanizedViewModel(application)
            dataCollector.appointmentSemiMechanizedList = semiMechanizedService.getAllNotSent()
            logMessage("SENDING APPOINT. SEMI. MECH.: ${dataCollector.appointmentSemiMechanizedList.size} .")
            jsonToPost = Gson().toJson(dataCollector)
            stepLocation = "Checklist"
            postJson(jsonToPost,"AppointmentSemiMec_Save")

            /** Salvando checklist  ***/
            Log.d(TAG,"**** Salvando checklist")
            dataCollector = DataCollectorDto()
            var checklistSavedService = ChecklistSavedViewModel(application)
            dataCollector.checklistSavedList = checklistSavedService.getAllNotSent()
            stepLocation = "Checklist"
            logMessage("SENDING CHECKLIST: ${dataCollector.checklistSavedList.size} .")
            jsonToPost = Gson().toJson(dataCollector)
            postJson(jsonToPost,"Checklist_Save")

            /** Salvando driver login  ***/
            Log.d(TAG,"**** Salvando driver login")
            dataCollector = DataCollectorDto()
            var driverLoginService = DriverLoginViewModel(application)
            dataCollector.driverLoginList = driverLoginService.getAllNotSent()
            logMessage("SENDING DRIVER LOGINS: ${dataCollector.driverLoginList.size} .")
            jsonToPost = Gson().toJson(dataCollector)
            postJson(jsonToPost,"DriverLogin_Save")

            /** Salvando setup  ***/
            Log.d(TAG,"**** Salvando setup")
            dataCollector = DataCollectorDto()
            var machineSetupService = MachineSetupViewModel(application)
            dataCollector.machineSetupList = machineSetupService.getAllNotSent()
            logMessage("SENDING MACHINE SETUP: ${dataCollector.machineSetupList.size} .")
            jsonToPost = Gson().toJson(dataCollector)
            postJson(jsonToPost,"Setup_Save")

            /** Salvando production  ***/
            Log.d(TAG,"**** Salvando production")
            dataCollector = DataCollectorDto()
            var productionService = ProductionHarvesterViewModel(application)
            dataCollector.productionHarvesterList = productionService.getAllNotSent()
            logMessage("SENDING PRODUCTION: ${dataCollector.productionHarvesterList.size} .")
            jsonToPost = Gson().toJson(dataCollector)
            postJson(jsonToPost,"Production_Save")

            /** Salvando telemetry list  ***/
            Log.d(TAG,"**** Salvando telemetry list")
            dataCollector = DataCollectorDto()
            var telemetryService = TelemetryViewModel(application)
            dataCollector.telemetryList = telemetryService.getAllNotSent()
            logMessage("SENDING TELEMETRY LIST: ${dataCollector.telemetryList.size} .")
            jsonToPost = Gson().toJson(dataCollector)
            postJson(jsonToPost,"Telemetry_Save")

            /** Salvando telemetry alert  ***/
            Log.d(TAG,"**** Salvando telemetry alert")
            dataCollector = DataCollectorDto()
            var telemetryAlertService = TelemetryAlertViewModel(application)
            dataCollector.telemetryAlertList = telemetryAlertService.getAllNotSent()
            logMessage("SENDING TELEMETRY ALERT: ${dataCollector.telemetryAlertList.size} .")
            jsonToPost = Gson().toJson(dataCollector)
            postJson(jsonToPost,"TelemetryAlert_Save")

            logMessage("ALL DATA SENT.")
            logMessage("--------------------------------------------")
            logMessage("FINISHED SUCCESSFUL.")
            MessageSingleton.finished = true

            return true
        } catch (e:Exception) {
            logMessage("SYNC FAILED.")
            e.printStackTrace()
            return false
        }
    }

    private fun postJson(jsonToPost:String, taskName:String) {
        postToCloud("data-collector", jsonToPost, token, object : VolleyCallback {
            override fun onSuccessResponse(result: String?) {
                Log.d(TAG,"**** SUCCESS POST")
            }
        }, taskName)
    }

    /**
     *
     */
    private fun getAllDataFromCloud() : Boolean
    {
        var allDatas = serviceApi.api.receiveData(token).execute()
        var allData = allDatas.body()

        val appointmentModel        = AppointmentViewModel(application)
        val appointmentGroupModel   = AppointmentGroupViewModel(application)
        val appointmentDataModel = AppointmentDataViewModel(application)
        val checkListModel          = ChecklistViewModel(application)
        val driverModel             = DriverViewModel(application)
        val farmCompartmentModel    = FarmCompartmentViewModel(application)
        val farmSectorModel         = FarmSectorViewModel(application)
        val activityTypeModel       = MachineActivityTypeViewModel(application)
        val machineEquipmentModel   = MachineEquipmentViewModel(application)
        val mechanizationTypeModel  = MachineMechanizationTypeViewModel(application)
        val telemetryMonitoringModel= TelemetryMonitoringViewModel(application)
        val appointmentMachineTypeModel = AppointmentMachineTypeViewModel(application)
        val maintenanceTaskViewModel = MaintenanceTaskViewModel(application)

        var statusMessage = ""
        var size = 0

        Log.d(TAG, "SAVE APPOINMENT DATA: "+ allData?.appointmentDataList?.size)
        allData?.appointmentDataList?.forEach{appointmentDataModel.save(it)}
        size = appointmentDataModel.getAll().size!!

        Log.d(TAG,"TOTAL APPOINTMENT DATA LIST: $size" )
        statusMessage += "APPOINTMENT DATA RECEIVED: $size \n"

        Log.d(TAG, "SAVE APPOINTMENT MACHINE TYPE: "+ allData?.appointmentMachineType?.size)
        allData?.appointmentMachineType?.forEach { appointmentMachineTypeModel.save(it) }
        size = appointmentMachineTypeModel.getAll().size

        Log.d(TAG, "TOTAL APPOINTMENT MACHINE TYPE LIST: $size" )
        statusMessage += "APPOINTMENT MACHINE TYPE RECEIVED: $size \n"

        Log.d(TAG, "SAVE APPOINTMENT GROUP LIST: "+ allData?.appointmentGroupList?.size )
        allData?.appointmentGroupList?.forEach { appointmentGroupModel.save(it) }
        size = appointmentGroupModel.getAll().size!!

        Log.d(TAG, "TOTAL APPOINTMENT GROUP LIST: $size " )
        statusMessage += "APPOINTMENT RECEIVED: $size \n"

        Log.d(TAG, "SAVE APPOINTMENT LIST: "+ allData?.appointmentList?.size )
        allData?.appointmentList?.forEach { appointmentModel.save(it) }
        size = appointmentModel.getAll().size!!
        Log.d(TAG, "TOTAL APPOINTMENT LIST: $size \n" )
        statusMessage += "APPOINTMENT LIST RECEIVED: $size \n"

        Log.d(TAG, "SAVE CHECKLIST ITEM LIST: "+ allData?.checklistList?.size )
        allData?.checklistList?.forEach { checkListModel.save(it) }
        size = checkListModel.getAll().size
        Log.d(TAG, "TOTAL CHECKLIST ITEM LIST: $size")
        statusMessage += "CHECKLIST RECEIVED: $size \n"

        Log.d(TAG, "SAVE DRIVER LIST: "+ allData?.driverList?.size )
        allData?.driverList?.forEach { driverModel.save(it) }
        size =  driverModel.getAll().size
        Log.d(TAG, "TOTAL DRIVER LIST: $size " )
        statusMessage += "DRIVER RECEIVED: $size \n"

        Log.d(TAG, "SAVE FARM SECTOR LIST: "+ allData?.farmSectorList?.size )
        allData?.farmSectorList?.forEach { farmSectorModel.save(it) }
        size = farmSectorModel.getAll().size
        Log.d(TAG, "TOTAL FARM SECTOR LIST: $size " )
        statusMessage += "FARM SECTOR RECEIVED: $size \n"

        Log.d(TAG, "SAVE FARM COMPARTMENT LIST: "+ allData?.farmCompartmentList?.size )
        allData?.farmCompartmentList?.forEach { farmCompartmentModel.save(it) }
        size = farmCompartmentModel.getAll().size
        Log.d(TAG, "TOTAL FARM COMPARTMENT LIST: $size " )
        statusMessage += "FARM COMPARTMENT RECEIVED: $size \n"

        Log.d(TAG, "SAVE ACTIVITY TYPE LIST: "+ allData?.machineActivityTypeList?.size )
        allData?.machineActivityTypeList?.forEach { activityTypeModel.save(it) }
        size = activityTypeModel.getAll().size
        Log.d(TAG, "TOTAL ACTIVITY TYPE LIST: $size " )
        statusMessage += "ACTIVITY TYPE RECEIVED: $size \n"

        Log.d(TAG, "SAVE EQUIPMENT LIST: "+ allData?.machineEquipmentList?.size )
        allData?.machineEquipmentList?.forEach { machineEquipmentModel.save(it) }
        size = machineEquipmentModel.getAll().size
        Log.d(TAG, "TOTAL EQUIPMENT LIST: $size " )
        statusMessage += "EQUIPMENT RECEIVED: $size \n"

        Log.d(TAG, "SAVE MECHANIZATION TYPE LIST: "+ allData?.machineMechanizationTypeList?.size )
        allData?.machineMechanizationTypeList?.forEach { mechanizationTypeModel.save(it) }
        size = mechanizationTypeModel.getAll().size
        Log.d(TAG, "TOTAL MECHANIZATION TYPE LIST: $size ")
        statusMessage += "MECHANIZATION TYPE RECEIVED: $size \n"

        Log.d(TAG, "SAVE TELEMETRY MONITORING LIST: "+ allData?.telemetryMonitoringList?.size )
        allData?.telemetryMonitoringList?.forEach { telemetryMonitoringModel.save(it) }
        size = telemetryMonitoringModel.getAll().size
        Log.d( TAG, "TOTAL TELEMETRY MONITORING LIST: $size " )
        statusMessage += "TELEMETRY RECEIVED: $size \n"

        Log.d(TAG, "SAVE MAINTENANCE TASK LIST: "+ allData?.maintenanceTasks?.size )
        allData?.maintenanceTasks?.forEach { maintenanceTaskViewModel.save(it) }
        size = maintenanceTaskViewModel.getAll().size
        Log.d( TAG, "TOTAL MAINTENANCE TASK LIST: $size " )
        statusMessage += "MAINTENANCE TASK RECEIVED: $size \n"

        statusMessage += "-------------------------------------------- \n"

        MessageSingleton.messages.add(statusMessage)

        return true
    }

    private fun clearIfNecessary() : Boolean {

        Log.d(TAG, "CLEAR TASK STEP 1 \n")

        var allDatas = serviceApi.api.receiveData(token).execute()
        var allData = allDatas.body()

        allData?.maintenanceTasks?.forEach {
            Log.d(TAG, "CLEAR TASK STEP 2 \n")
            if ( it.version != null ) {

                executeMaintenance(it)

                MessageSingleton.messages.add("CLEAR TASK DETECTED - " + it.version + " \n")

                Log.d(TAG, "CLEAR TASK DETECTED - " + it.version +" \n")

            }
        }
        Log.d(TAG, "CLEAR TASK STEP 3 \n")

        return true
    }

    private fun executeMaintenance(maintenanceTask: MaintenanceTask) {

        val maintenanceTaskViewModel = MaintenanceTaskViewModel(application)
        val driverModel             = DriverViewModel(application)
        val mechanizationTypeModel  = MachineMechanizationTypeViewModel(application)
        val activityTypeModel       = MachineActivityTypeViewModel(application)
        val machineEquipmentModel   = MachineEquipmentViewModel(application)
        val checkListModel          = ChecklistViewModel(application)
        val appointmentGroupModel   = AppointmentGroupViewModel(application)
        val appointmentModel        = AppointmentViewModel(application)
        val appointmentDataModel = AppointmentDataViewModel(application)
        val appointmentMachineTypeModel = AppointmentMachineTypeViewModel(application)
        val farmSectorModel         = FarmSectorViewModel(application)
        val farmCompartmentModel    = FarmCompartmentViewModel(application)

        var lastVersion = maintenanceTaskViewModel.getLastVersion()

        if (lastVersion==null) {
            lastVersion = 0
        }

        if (maintenanceTask.version!=null && lastVersion!! < maintenanceTask.version!!) {
            when (maintenanceTask.type) {
                "CLEAR_DATA" -> {
                    driverModel.deleteAll()
                    mechanizationTypeModel.deleteAll()
                    activityTypeModel.deleteAll()
                    machineEquipmentModel.deleteAll()
                    checkListModel.deleteAll()
                    appointmentGroupModel.deleteAll()
                    appointmentModel.deleteAll()
                    appointmentDataModel.deleteAll()
                    appointmentMachineTypeModel.deleteAll()
                    farmSectorModel.deleteAll()
                    farmCompartmentModel.deleteAll()
                }
            }
        }
    }

}