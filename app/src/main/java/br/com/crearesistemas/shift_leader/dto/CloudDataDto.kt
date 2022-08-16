package br.com.crearesistemas.shift_leader.dto

import br.com.crearesistemas.shift_leader.db_service.model.*

data class CloudDataDto(

    val appointmentList: List<Appointment> = emptyList(),

    val appointmentGroupList: List<AppointmentGroup> = emptyList(),

    val appointmentDataList: List<AppointmentData> = emptyList(),

    val checklistList: List<Checklist> = emptyList(),

    val driverList: List<Driver> = emptyList(),

    val farmCompartmentList: List<FarmCompartment> = emptyList(),

    val farmSectorList: List<FarmSector> = emptyList(),

    val machineActivityTypeList: List<MachineActivityType> = emptyList(),

    val machineEquipmentList: List<MachineEquipment> = emptyList(),

    val machineMechanizationTypeList: List<MachineMechanizationType> = emptyList(),

    val telemetryMonitoringList: List<TelemetryMonitoring> = emptyList(),

    val appointmentMachineType: List<AppointmentMachineType> = emptyList(),

    val maintenanceTasks : List<MaintenanceTask> = emptyList()



    // ****** ESPECIFICAÇÃO ****** --> Entregar sexta dia 3/12 funcionando

    // ATIV 1
    // Setup,
    // MachineSetup --> Vem do TabletMachine, é uma das primeiras telas preenchidas no tablet machine
    // ela é preenchida sempre que o usuario abre o app, esses dados serão estocados dentro da base
    // do shift leader e depois postados na nuvem, como ele é (machineSetup). ---> Nao tem Saved

    // ATIV 2
    // Checklist, Checklist vem da cloud e vai carregado dentro do shift leader e posteriormente vai
    // popular a base do Tablet Machine com dados na Tabela Checklist, quando o usuario do tablet machine
    // selecina um item da tabela checklist, ele posteriormente popula um objeto ChecklistSaved, indicando
    // que ele relacionou o checklist que veio da nuvem com algo que foi efetivamente realizado pelo
    // usuario do machine tablet

    // ATIV 3
    // Apontamentos, Os apontamentos vem da cloud e vao ser carregados dentro da base do shift leader,
    // depois eles serão carregados para dentro do tabler machine, que irá usar esses dados para popular
    // a lista de atividades (new activity) com os apontamentos disponiveis para serem feitos, apos o usuario
    // colocar uma atividade essa será uma instancia de activitySaved que relaciona o apontamento original e
    // depois esse dado irá trafegar devolta ao shift leader, que irá carrega-lo na cloud.
    //
    // ATIV 4
    // Telemetria e produção - Telemetria e produção: Terá um modulo que o gregory irá implementar dentro do
    // Machine Tablet, esse modulo do Greg irá inserir dentro das tabelas do BD local do machine tablet, nossa
    // função é pegar esses dados dessas tabelas de telemetria e produção e fazer com que esses dados sejam
    // transportados para o shift leader e posteriormente entrarem na cloud

)
