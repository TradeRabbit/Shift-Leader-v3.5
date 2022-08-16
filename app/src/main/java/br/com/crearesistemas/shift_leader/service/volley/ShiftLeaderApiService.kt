package br.com.crearesistemas.shift_leader.service.volley

import android.annotation.SuppressLint
import br.com.crearesistemas.shift_leader.db_service.model.User
import br.com.crearesistemas.shift_leader.dto.CloudDataDto
import br.com.crearesistemas.shift_leader.dto.DataCollectorDto
import br.com.crearesistemas.shift_leader.db_service.model.Machine
import br.com.crearesistemas.shift_leader.dto.AuthResponseDto
import br.com.crearesistemas.shift_leader.dto.UserRequestDto
import br.com.crearesistemas.shift_leader.singleton.ConfigSingleton
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Para se conectar na cloud iremos usar retrofit
 *
 * @author Isaias Alves <isaias@wswork.com.br>
 */
class ShiftLeaderApiService {

    private val createBaseUrl = run {
         var configUrl = "https://april.crearecloud.com.br/"
        //var configUrl = "https://creare.wswork.com.br/"
        if (!configUrl.startsWith("http")) configUrl = "http://$configUrl"
        if (!configUrl.endsWith("/")) configUrl = "$configUrl/"
        "${configUrl}api/shift-leader/"
    }

    private fun client(): OkHttpClient {
        return OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
    }

    private fun gson(): Gson = GsonBuilder().create()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(createBaseUrl)
        .client(client())
        .addConverterFactory(GsonConverterFactory.create(gson()))
        .build()

    val api: ApiService = retrofit.create(ApiService::class.java)

    interface ApiService {

        @POST("auth/login")
        fun login(
            @Body userRequest: UserRequestDto
        ): Call<AuthResponseDto>

        @POST("data-collector")
        fun sendData(
            @Header("Authorization") authorization: String,
            @Body data: DataCollectorDto
        ): Call<Void>

        @GET("cloud-data")
        fun receiveData(
            @Header("Authorization") authorization: String,
        ): Call<CloudDataDto>

        @GET("user")
        fun userGetAll(
            @Header("Authorization") authorization: String,
        ): Call<List<User>>

        @GET("machine")
        fun machineGetAll(
            @Header("Authorization") authorization: String,
        ): Call<List<Machine>>

    }

}