package br.com.crearesistemas.shift_leader.db_service.dao

import androidx.room.*
import br.com.crearesistemas.shift_leader.db_service.model.Machine

@Dao
interface MachineDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(vararg values: Machine)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAll(value: List<Machine>)

    @Query("DELETE FROM machine_list WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Delete
    suspend fun deleteAll(value: List<Machine>)

    @Query("SELECT * FROM machine_list tb WHERE tb.id = :id LIMIT 1")
    fun getById(id: Long): Machine?

    @Query("SELECT * FROM machine_list order by description")
    fun getAll(): List<Machine>

}