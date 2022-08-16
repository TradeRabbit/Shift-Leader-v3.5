package br.com.crearesistemas.shift_leader.db_service.dao

import androidx.room.*
import br.com.crearesistemas.shift_leader.db_service.model.AppointmentGroup

@Dao
interface AppointmentGroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(vararg values: AppointmentGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAll(value: List<AppointmentGroup>)

    @Query("DELETE FROM appointment_groups WHERE code = :code")
    suspend fun deleteById(code: Long)

    @Query("DELETE FROM appointment_groups ")
    suspend fun deleteAll()

    @Query("SELECT * FROM appointment_groups tb WHERE tb.code = :code LIMIT 1")
    fun getById(code: Long): AppointmentGroup?

    @Query("SELECT * FROM appointment_groups")
    fun getAll(): List<AppointmentGroup>

}