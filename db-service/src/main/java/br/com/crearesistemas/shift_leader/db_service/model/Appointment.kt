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
@Entity(tableName = "appointment_list")
class Appointment {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "code")
    var code: Long? = null

    /**
     * origin = CLOUD
     */
    @ColumnInfo(name = "origin")
    var origin: String? = null

    @ColumnInfo(name = "group_code")
    var groupCode: String? = null

    @ColumnInfo(name = "description_primary")
    var descriptionPrimary: String? = null

    @ColumnInfo(name = "description_secondary")
    var descriptionSecondary: String? = null

    @ColumnInfo(name = "repetitive")
    var repetitive: Boolean? = null

    @ColumnInfo(name = "collected_at")
    var collectedAt: String? = null

    @ColumnInfo(name = "machine_id")
    var machineId: String? = null

}