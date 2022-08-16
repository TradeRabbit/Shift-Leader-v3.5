package br.com.crearesistemas.shift_leader.db_service.model

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import java.time.OffsetDateTime

/**
 * transmission direction:
 * creare_cloud -> api_shift_leader -> shift_leader -> tablet_machine
 */
@Entity(tableName = "farm_sector")
class FarmSector {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    var id: Long? = null

    @ColumnInfo(name = "origin")
    var origin: String? = null

    @ColumnInfo(name = "description")
    var description: String? = null

    @ColumnInfo(name = "collected_at")
    var collectedAt: String? = null
}