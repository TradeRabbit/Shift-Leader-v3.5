package br.com.crearesistemas.shift_leader.db_service.repository
import android.app.Application
import android.os.AsyncTask
import br.com.crearesistemas.shift_leader.db_service.AppDatabase
import br.com.crearesistemas.shift_leader.db_service.model.TelemetryMonitoring
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.ExecutionException
class TelemetryMonitoringRepository(application: Application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db!!.telemetryMonitoringDao()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault())


    fun save(vararg values: TelemetryMonitoring) = runBlocking {
        values.forEach {
            it.collectedAt = dateFormat.format(Date())
        }
        dao.save(*values)
    }

    fun deleteById(pgn: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            dao.deleteById(pgn)
        }
    }

    fun deleteAll(value: List<TelemetryMonitoring>) {
        CoroutineScope(Dispatchers.IO).launch {
            dao.deleteAll(value)
        }
    }

    fun getById(pgn: Long): TelemetryMonitoring? {

        try {
            return object : AsyncTask<Any?, Any?, Any?>() {
                override fun doInBackground(objects: Array<Any?>): Any? {
                    return dao.getById(pgn)
                }
            }.execute().get() as TelemetryMonitoring?
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        }
        return null
    }

    fun getAll(): List<TelemetryMonitoring> {

        try {
            return object : AsyncTask<Any?, Any?, Any?>() {
                override fun doInBackground(objects: Array<Any?>): Any? {
                    return dao.getAll()
                }
            }.execute().get() as List<TelemetryMonitoring>
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        }
        return emptyList()
    }

}