package br.com.crearesistemas.shift_leader.dto

import br.com.crearesistemas.shift_leader.db_service.model.*

class AllDataDTO {

    /**
     * Dados para enviar da maquina ao shift leader
     */
    var appointmentsSaved : List<AppointmentSaved> = emptyList()
    var setups : List<MachineSetup> = emptyList()
    var checklistsSaved : List<ChecklistSaved> = emptyList()
    var telemetries : List<Telemetry> = emptyList()
    var productions : List<ProductionHarvester> = emptyList()
    var telemetryAlerts : List<TelemetryAlert> = emptyList()
    var telemetryMonitorings : List<TelemetryMonitoring> = emptyList()
    var driverLogins : List<DriverLogin> = emptyList()

    /**
     * Dados recebidos pela maquina a partir do shift leader
     */

    //a
    var appointments : List<Appointment> = emptyList()
    var appointmentGroups : List<AppointmentGroup> = emptyList()
    var checklists : List<Checklist> = emptyList()

    //b
    var drivers : List<Driver> = emptyList()
    var farmSectors : List<FarmSector> = emptyList()
    var farmCompartments : List<FarmCompartment> = emptyList()
    var activityTypes : List<MachineActivityType> = emptyList()
    var mechanizations : List<MachineMechanizationType> = emptyList()
    var equipments : List<MachineEquipment> = emptyList()
}