package br.com.crearesistemas.shift_leader.service.volley

import android.content.Context
import android.util.Log
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import java.util.*
import android.widget.Toast
import br.com.crearesistemas.shift_leader.singleton.ConfigSingleton
import br.com.crearesistemas.shift_leader.singleton.MessageSingleton
import br.com.crearesistemas.shift_leader.singleton.VolleySingleton
import com.android.volley.AuthFailureError
import org.json.JSONObject
import java.text.SimpleDateFormat
import com.android.volley.VolleyError

import com.android.volley.RetryPolicy
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.lang.RuntimeException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


/**
 * Classe responsável por ter as funcionalidades basicas
 * para comunicação rest usando api Volley do android
 * Porque usar Volley? Pois é nativa do android com isso evitamos problemas com
 * mudanças no comportamento da API e tambem pelo fato de não travar o app enquanto
 * uma conexão ocorre, a API volley deixa o gerenciamento de conectividade a critério
 * do android o que melhora a performance geral das conectividades do app
 *
 * @author Isaias Alves <isaias@wswork.com.br>
 */
open class VolleyService constructor() {

    val url =  ConfigSingleton.machineTabletAddress ?:
//    "http://192.168.0.129:8087" // TESTES
    "http://10.0.0.5:8087" // VALENDO

    //private val toCloudUrl = "http://192.168.200.233:8095/api/shift-leader/"
     private val toCloudUrl = "https://april.crearecloud.com.br/api/shift-leader/"
     // private val toCloudUrl = "https://creare.wswork.com.br/api/shift-leader/"

    val logTag = "VolleyService"
    var sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")

    var context : Context? = null

    open fun initContext(context: Context) {
        this.context = context
    }

    /**
     * Envia dadosutilizando funções de callback api volley
     */
    open fun post(
        endpoint: String,
        postJson: String,
        callback: VolleyCallback
    ) {
        Log.d(logTag,"Init post from VolleyService at: ${sdf.format(Date())} to url $url${endpoint}")

        //val queue = VolleySingleton.getInstance(context)?.requestQueue
        val request: StringRequest = object : StringRequest(

            Method.POST, "$url${endpoint}",

            Response.Listener {
                    response -> callback.onSuccessResponse(response)
                Log.d(logTag,"SUCCESS --> $url${endpoint}  at: ${sdf.format(Date())}")
                //Toast.makeText(context, "SUCCESS --> $url${endpoint}  at: ${sdf.format(Date())}", Toast.LENGTH_LONG).show()

                MessageSingleton.messages.add("SUCCESS --> $url${endpoint}  at: ${sdf.format(Date())}")
            },

            Response.ErrorListener { e ->
                e.printStackTrace()
                Log.d(logTag,"Error post from VolleyService at: ${sdf.format(Date())}")
                //Toast.makeText(context, "Validating post, retrying. in $url${endpoint} At: ${sdf.format(Date())} - ${e.message}", Toast.LENGTH_LONG).show()

                MessageSingleton.messages.add("Validating post, retrying. in $url${endpoint} At: ${sdf.format(Date())} - ${e.message}")

                // SE DER ERRO VAI TENTAR DA MANEIRA ANTIGA
                legacyPost(postJson, url, endpoint)

            }) {

            // set headers
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                // TODO teremos auth via token?
                //params["Authorization: Basic"] = TOKEN
                params["Accept-Encoding"] = "gzip"
                return params
            }

            override fun getBodyContentType(): String {
                return "application/json";
            }

            @Throws(AuthFailureError::class)
            override fun getBody(): ByteArray {
                var httpPostBody = postJson

                return httpPostBody.toByteArray()
            }
        }

        request.retryPolicy = object : RetryPolicy {
            override fun getCurrentTimeout(): Int {
                return 120000
            }

            override fun getCurrentRetryCount(): Int {
                return 120000
            }

            @Throws(VolleyError::class)
            override fun retry(error: VolleyError) {
            }
        }

        VolleySingleton.getInstance(context)?.addToRequestQueue(request)
    }

    /**
     * Envia dadosutilizando funções de callback api volley
     */
    open fun postToCloud(
        endpoint: String,
        postJson: String,
        token:String,
        callback: VolleyCallback,
        taskName:String
    ) {
        Log.d(logTag,"Init post to cloud at: ${sdf.format(Date())} to url $toCloudUrl${endpoint}")

        //val queue = VolleySingleton.getInstance(context)?.requestQueue
        val request: StringRequest = object : StringRequest(

            Method.POST, "$toCloudUrl${endpoint}",

            Response.Listener {
                    response -> callback.onSuccessResponse(response)
                Log.d(logTag,"SUCCESS --> $toCloudUrl${endpoint}  at: ${sdf.format(Date())}")
                //Toast.makeText(context, "SUCCESS --> $toCloudUrl${endpoint}  at: ${sdf.format(Date())}", Toast.LENGTH_LONG).show()
            },

            Response.ErrorListener { e ->
                e.printStackTrace()
                Log.d(logTag,"Error post from VolleyService at: ${toCloudUrl.format(Date())}")
                //Toast.makeText(context, "Validating post, retrying. in $toCloudUrl${endpoint} At: ${sdf.format(Date())} - ${e.message}", Toast.LENGTH_LONG).show()
                // SE DER ERRO VAI TENTAR DA MANEIRA ANTIGA
                legacyPost(postJson, toCloudUrl, endpoint)

            }) {

            // set headers
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                // TODO teremos auth via token?
                params["Authorization"] = token
                //params["Accept-Encoding"] = "gzip"
                return params
            }

            override fun getBodyContentType(): String {
                return "application/json";
            }

            @Throws(AuthFailureError::class)
            override fun getBody(): ByteArray {
                var httpPostBody = postJson

                return httpPostBody.toByteArray()
            }
        }

        request.retryPolicy = object : RetryPolicy {
            override fun getCurrentTimeout(): Int {
                return 120000
            }

            override fun getCurrentRetryCount(): Int {
                return 120000
            }

            @Throws(VolleyError::class)
            override fun retry(error: VolleyError) {
            }
        }

        VolleySingleton.getInstance(context)?.addToRequestQueue(request)
    }

    fun legacyPost(input:String, url:String, endpoint:String) {
        legacyPost(input, url, endpoint, null)
    }

    fun legacyPost(input:String, url:String, endpoint:String, token:String?) {

        try {
            val url = URL(
                "$url${endpoint}"
            )

            MessageSingleton.messages.add("Posting using fallback strategy $url ")

            val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
            conn.setDoOutput(true)
            conn.setRequestMethod("POST")
            conn.setRequestProperty("Content-Type", "application/json")
            if (token!=null) {
                conn.setRequestProperty("Authorization", token)
            }
            val os: OutputStream = conn.outputStream
            os.write(input.toByteArray())
            os.flush()

            //try {
            //    toastMessage( context!!,"Post status using legacy strategy ${conn.responseCode}")
            //} catch (e:Exception){}

            val br = BufferedReader(
                InputStreamReader(
                    (conn.getInputStream())
                )
            )
            var output: String?
            println("Output from Server .... \n")
            //try {
            //    Toast.makeText(context, "SUCCESS posting, retrying. in $url${endpoint} At: ${sdf.format(Date())} ", Toast.LENGTH_LONG).show()
            //
            //} catch (e:Exception){}

            while ((br.readLine().also { output = it }) != null) {
                println(output)
            }

            MessageSingleton.messages.add("Sucess posting using fallback $url ")

            conn.disconnect()
        } catch (e: Exception) {
            Toast.makeText(context, "Validating legacy posting, retrying. in $url${endpoint} At: ${sdf.format(Date())} - ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    /**
     * Envia request para buscar dados
     */
    open fun find(
        endpoint: String,
        timestamp: Long,
        callback: VolleyCallback
    ) {

        Log.d(logTag,"Init request from VolleyService at: ${sdf.format(Date())}")

        //val queue = VolleySingleton.getInstance(context)?.requestQueue
        val request: StringRequest = object : StringRequest(
            Method.GET, "$url${endpoint}",
            Response.Listener {
                response -> callback.onSuccessResponse(response)
                Log.d(logTag,"Done request from VolleyService at: ${sdf.format(Date())}")
            },
            Response.ErrorListener { e ->
                e.printStackTrace()
                Log.d(logTag,"Error request from VolleyService at: ${sdf.format(Date())}")
                //Toast.makeText(context, e.toString() + "error", Toast.LENGTH_LONG).show()
            }) {
            // set headers
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                // TODO teremos auth via token?
                //params["Authorization: Basic"] = TOKEN
                return params
            }
        }

        request.retryPolicy = object : RetryPolicy {
            override fun getCurrentTimeout(): Int {
                return 120000
            }

            override fun getCurrentRetryCount(): Int {
                return 120000
            }

            @Throws(VolleyError::class)
            override fun retry(error: VolleyError) {
            }
        }

        VolleySingleton.getInstance(context)?.addToRequestQueue(request)
    }

    private fun toastMessage(context: Context, message: String) {
        try {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.d("DataSyncService", "Erro ao logar mensagem")
        }
    }

}