package br.com.crearesistemas.shift_leader.dto

import br.com.crearesistemas.shift_leader.db_service.model.*

data class DataCollectorDto(

    var appointmentSavedList: List<AppointmentSaved> = emptyList(),

    var appointmentSemiMechanizedList: List<AppointmentSemiMechanized> = emptyList(),

    var appointmentDataList : List<AppointmentData> = emptyList(),

    var checklistSavedList: List<ChecklistSaved> = emptyList(),

    var driverLoginList: List<DriverLogin> = emptyList(),

    var machineSetupList: List<MachineSetup> = emptyList(),

    var productionHarvesterList: List<ProductionHarvester> = emptyList(),

    var telemetryList: List<Telemetry> = emptyList(),

    var telemetryAlertList: List<TelemetryAlert> = emptyList(),

    var machineActivityTypeList: List<MachineActivityType> = emptyList(),

    var maintenanceTaskList: List<MaintenanceTask> = emptyList()

    )