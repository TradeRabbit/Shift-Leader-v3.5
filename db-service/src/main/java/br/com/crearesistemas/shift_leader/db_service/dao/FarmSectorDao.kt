package br.com.crearesistemas.shift_leader.db_service.dao

import androidx.room.*
import br.com.crearesistemas.shift_leader.db_service.model.FarmSector

@Dao
interface FarmSectorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(vararg values: FarmSector)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAll(value: List<FarmSector>)

    @Query("DELETE FROM farm_sector WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM farm_sector ")
    suspend fun deleteAll()

    @Query("SELECT * FROM farm_sector tb WHERE tb.id = :id LIMIT 1")
    fun getById(id: Long): FarmSector?

    @Query("SELECT * FROM farm_sector")
    fun getAll(): List<FarmSector>

}