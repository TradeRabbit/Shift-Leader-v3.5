package br.com.crearesistemas.shift_leader.db_service.repository
import android.app.Application
import android.os.AsyncTask
import br.com.crearesistemas.shift_leader.db_service.AppDatabase
import br.com.crearesistemas.shift_leader.db_service.model.FarmSector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.ExecutionException
class FarmSectorRepository(application: Application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db!!.farmSectorDao()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault())

    fun save(vararg values: FarmSector) = runBlocking {
        values.forEach {
            it.collectedAt = dateFormat.format(Date())
        }
        dao.save(*values)
    }

    fun deleteById(id: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            dao.deleteById(id)
        }
    }

    fun deleteAll() = runBlocking {
        dao.deleteAll()
    }

    fun getById(id: Long): FarmSector? {

        try {
            return object : AsyncTask<Any?, Any?, Any?>() {
                override fun doInBackground(objects: Array<Any?>): Any? {
                    return dao.getById(id)
                }
            }.execute().get() as FarmSector?
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        }
        return null
    }

    fun getAll(): List<FarmSector> {

        try {
            return object : AsyncTask<Any?, Any?, Any?>() {
                override fun doInBackground(objects: Array<Any?>): Any? {
                    return dao.getAll()
                }
            }.execute().get() as List<FarmSector>
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        }
        return emptyList()
    }


}