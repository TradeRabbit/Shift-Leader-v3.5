package br.com.crearesistemas.shift_leader

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import java.util.*
import android.app.Activity
import android.content.res.Configuration
import android.content.res.Resources
import br.com.crearesistemas.shift_leader.db_service.model.ShiftLeaderConfig
import br.com.crearesistemas.shift_leader.db_service.model.User
import br.com.crearesistemas.shift_leader.db_service.viewmodel.MachineViewModel
import br.com.crearesistemas.shift_leader.db_service.viewmodel.ShiftLeaderConfigMachineViewModel
import br.com.crearesistemas.shift_leader.db_service.viewmodel.ShiftLeaderConfigViewModel
import br.com.crearesistemas.shift_leader.db_service.viewmodel.UserViewModel
import br.com.crearesistemas.shift_leader.singleton.ConfigSingleton
import br.com.crearesistemas.shift_leader.singleton.LoginSingleton
import br.com.crearesistemas.shift_leader.ui.SplashScreem
import br.com.crearesistemas.socket.client.service.MachineStatusSingleton
import java.text.SimpleDateFormat

/**
 *
 */
class LoginActivity : AppCompatActivity()
{

    private val loginMaxAttempts by lazy { 5 }
    private val loginMaxAttemptsTimeout = 60 * 1000 //Proximo login apenas apos 1 minuto
    private val sdf = SimpleDateFormat("dd/MM/yyyy")

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        bindEvents()

        val versionapp = findViewById<TextView>(R.id.versionApp)
        versionapp.setText("v"+ BuildConfig.VERSION_NAME)

        try {

            val machineService = MachineViewModel(application)
            val allMachines = machineService.getAll()
            allMachines.forEach {
                val machineConfigService = ShiftLeaderConfigMachineViewModel(application)
                val machineConfig = machineConfigService.getBySsid(it.hotspotSsid!!)

                if (machineConfig!=null) {
                    var timestamp = sdf.parse("01/01/2021").time

                    if (machineConfig?.lastSyncTimestamp!=null && machineConfig.lastSyncTimestamp!="null") {
                        timestamp = machineConfig?.lastSyncTimestamp.toString().toLong()
                    }

                    MachineStatusSingleton.lastReadTimestamp[it.hotspotSsid!!] = timestamp
                    MachineStatusSingleton.activationStatus[it.hotspotSsid!!] = machineConfig?.isConnected!!
                    MachineStatusSingleton.isConnectingStatus[it.hotspotSsid!!] = machineConfig?.isConnected!!
                }
            }

            val globalConfigService = ShiftLeaderConfigViewModel(application)

            val globalConfig = globalConfigService.getById(1)
            if (globalConfig?.lastSyncTime != null) {
                MachineStatusSingleton.lastSyncCloudTimestamp = globalConfig.lastSyncTime.toString().toLong()
                MachineStatusSingleton.greenStatusTimeout = globalConfig.limitYellow ?: 10800 * 1000
                MachineStatusSingleton.lastSyncCloudTimestamp = (globalConfig.limitRed ?: 18000 * 1000).toLong()
                ConfigSingleton.cloudAddress = globalConfig.cloudAddress
                ConfigSingleton.machineTabletAddress = globalConfig.machineTabletAddress
                ConfigSingleton.wifiDefaultPassword = globalConfig.wifiDefaultPassword
            } else {

                val config = ShiftLeaderConfig()
                config.id = 1
                config.limitRed = 18000 * 1000
                config.limitYellow = 10800 * 1000
                globalConfigService.save(config)
            }

        }catch (e:Exception) {
            e.printStackTrace()
        }


        val b = intent.extras
        var value = ""
        if (b != null)  {
            value = b.get("message").toString()
        }
        if (value == "connect") {
            startActivityForResult( Intent( this,
                MainActivity::class.java ), 1 )
        }

    }

    private fun setLocale(activity: Activity, languageCode: String?) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources: Resources = activity.resources
        val config: Configuration = resources.getConfiguration()
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.getDisplayMetrics())
    }

    private fun bindEvents() {

        var userNameTtxt = findViewById<EditText>(R.id.user_name)
        //var userPwdTtxt = findViewById<EditText>(R.id.user_password)
        var userPwdTtxt = findViewById<EditText>(R.id.user_password)

        // bind de botao login
        val buttonLogin = findViewById<Button>( R.id.btn_login )
        buttonLogin.setOnClickListener {

            var userInput = User()
            userInput.username = userNameTtxt.text.toString()
            userInput.password = userPwdTtxt.text.toString()

            when {
                /*LoginSingleton.loginAttempts >= loginMaxAttempts && (
                        Date().time <=
                                (LoginSingleton.loginFailTimestamp+loginMaxAttemptsTimeout)
                                && LoginSingleton.loginFailTimestamp > 0L ) -> {

                    Toast.makeText(this,
                        resources.getString(R.string.msg_login_attempts_fail), Toast.LENGTH_SHORT).show()
                }*/

                checkLogin( userInput!! ) -> {

                    LoginSingleton.loginAttempts = 0
                    LoginSingleton.loginFailTimestamp = 0
                    LoginSingleton.isAdmin = false
                    LoginSingleton.statusLogin = true

                    startActivityForResult(
                        Intent(
                            this,
                            MainActivity::class.java
                        ), 1
                    )
                }
                else -> {
                    LoginSingleton.isAdmin = false
                    LoginSingleton.statusLogin = false
                    LoginSingleton.loginAttempts++
                    LoginSingleton.loginFailTimestamp = Date().time
                    Toast.makeText(this,
                        "Invalid login.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val buttonEn = findViewById<ImageButton>( R.id.lang_en )
        buttonEn.setOnClickListener {
            setLocale(this, "")
            resources.getString(R.string.msg_change_login)
            startActivityForResult(
                Intent(
                    this,
                    LoginActivity::class.java
                ), 1
            )
        }

        val buttonIn = findViewById<ImageButton>( R.id.lang_in )
        buttonIn.setOnClickListener {
            setLocale(this, "in")
            Toast.makeText(this, resources.getString(R.string.msg_change_login) , Toast.LENGTH_SHORT).show()
            startActivityForResult(
                Intent(
                    this,
                    LoginActivity::class.java
                ), 1
            )
        }
    }

    private fun checkLogin(userInputData: User) : Boolean {
        // Super admin temporario
        if (userInputData.username == "admuser" && userInputData.password == "inpm7099") return true;
        if (userInputData.username == "creare"  && userInputData.password == "creare4455") return true;


        val userService = UserViewModel(application)
        val loginFound = userService.login( userInputData.username!!, userInputData.password!! )
        val authenticated = loginFound != null
        return authenticated
    }

}
