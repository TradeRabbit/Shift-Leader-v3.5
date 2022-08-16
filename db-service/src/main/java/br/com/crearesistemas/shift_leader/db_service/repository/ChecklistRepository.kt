package br.com.crearesistemas.shift_leader.db_service.repository

import android.app.Application
import android.os.AsyncTask
import androidx.room.Transaction
import br.com.crearesistemas.shift_leader.db_service.AppDatabase
import br.com.crearesistemas.shift_leader.db_service.model.Checklist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.ExecutionException

class ChecklistRepository(application: Application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db!!.checkListDao()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault())

    @Transaction
    fun save(vararg values: Checklist) = runBlocking {
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

    fun getById(id: Long): Checklist? {

        try {
            return object : AsyncTask<Any?, Any?, Any?>() {
                override fun doInBackground(objects: Array<Any?>): Any? {
                    return dao.getById(id)
                }
            }.execute().get() as Checklist?
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        }
        return null
    }

    fun getAll(): List<Checklist> {

        try {
            return object : AsyncTask<Any?, Any?, Any?>() {
                override fun doInBackground(objects: Array<Any?>): Any? {
                    return dao.getAll()
                }
            }.execute().get() as List<Checklist>
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        }
        return emptyList()
    }

}