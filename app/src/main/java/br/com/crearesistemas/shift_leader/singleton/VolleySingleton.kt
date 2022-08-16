package br.com.crearesistemas.shift_leader.singleton

import android.annotation.SuppressLint
import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

/**
 * Singleton utilizado para suportar as chamadas request da api volley
 */
class VolleySingleton private constructor(private var context: Context) {

    private var mRequestQueue: RequestQueue?

    val requestQueue: RequestQueue?
        get() {
            if (mRequestQueue == null) {
                mRequestQueue = Volley.newRequestQueue(context.applicationContext)
            }
            return mRequestQueue
        }

    fun <T> addToRequestQueue(req: Request<T>?) {
        requestQueue!!.add(req)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var mInstance: VolleySingleton? = null
        @Synchronized
        fun getInstance(context: Context?): VolleySingleton? {
            if (mInstance == null) {
                mInstance = VolleySingleton(context!!)
            }
            return mInstance
        }
    }

    init {
        mRequestQueue = requestQueue
    }
}