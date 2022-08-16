package br.com.crearesistemas.shift_leader.db_service.dao

import androidx.room.*
import br.com.crearesistemas.shift_leader.db_service.model.MachineEquipment

@Dao
interface MachineEquipmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(vararg values: MachineEquipment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAll(value: List<MachineEquipment>)

    @Query("DELETE FROM machine_equipment WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM machine_equipment ")
    suspend fun deleteAll()

    @Query("SELECT * FROM machine_equipment tb WHERE tb.id = :id LIMIT 1")
    fun getById(id: Long): MachineEquipment?

    @Query("SELECT * FROM machine_equipment")
    fun getAll(): List<MachineEquipment>

}