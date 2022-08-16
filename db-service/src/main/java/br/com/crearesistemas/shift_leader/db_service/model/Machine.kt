package br.com.crearesistemas.shift_leader.db_service.model

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import java.time.OffsetDateTime

/**
 * transmission direction
 * creare_cloud -> api_shift_leader -> shift_leader
 */
@Entity(tableName = "machine_list", primaryKeys = ["id"])
class Machine {

    @NonNull
    @ColumnInfo(name = "id")
    var id: Long? = null

    @ColumnInfo(name = "description")
    var description: String? = null

    @ColumnInfo(name = "hotspot_ssid")
    var hotspotSsid: String? = null

    @ColumnInfo(name = "hotspot_password")
    var hotspotPassword: String? = null

    @ColumnInfo(name = "collected_at")
    var collectedAt: String? = null

}