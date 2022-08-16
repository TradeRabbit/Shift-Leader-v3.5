package br.com.crearesistemas.shift_leader.db_service.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import br.com.crearesistemas.shift_leader.db_service.model.MachineActivityType
import br.com.crearesistemas.shift_leader.db_service.repository.MachineActivityTypeRepository

class MachineActivityTypeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MachineActivityTypeRepository(application)

    fun save(vararg values: MachineActivityType) = repository.save(*values)

    fun deleteById(id: Long) = repository.deleteById(id)

    fun deleteAll() = repository.deleteAll()

    fun getById(id: Long) = repository.getById(id)

    fun getAll() = repository.getAll()

}