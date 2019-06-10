package com.example.eamm

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager

import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.telephony.SmsManager
import kotlinx.android.synthetic.main.activity_main.*

import android.hardware.usb.UsbDevice.getDeviceId
import android.os.Build
import android.content.Context.TELEPHONY_SERVICE
import android.os.AsyncTask
import android.os.Build.VERSION.SDK_INT
import android.support.annotation.RequiresApi
import android.support.annotation.UiThread
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.TextView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PanelUpdate("Iniciando")
        var Nserie: String = ""; Nserie = Build.SERIAL; // solo para la version 23 android 6.0
        var deviceInfo: String = "" + Build.MANUFACTURER + "" + Build.MODEL + "" + Nserie
        PanelUpdate("Dispositivo "+deviceInfo)

        btnIniciar.onClick {
            val pausa = launch {
                repeat(3) { i ->
                    //                    toast("Iniciando | Esperando $i ...")
                    PanelUpdate("Iniciando..."+i.toString())
                    delay(3000)
                }
            }

            var c = 1
            while (c >= 1) {
                pausa.run {
                    repeat(10) { i ->
                        //                        toast("Esperando > $i ...")
                        PanelUpdate("Esperando > "+ i)
                        delay(3000)
                    }
                }
                PanelUpdate("-----------------------------------")
                CicloDeTrabajo("", "", c.toString())
                pausa.cancel() // cancels the job
                pausa.join() // waits for job's completion

                c++
            }

        }
    }



    fun PanelUpdate(Texto:String){
        val date = getCurrentDateTime()
        val FechaHora= date.toString("yyyy/MM/dd HH:mm:ss")


        var Panel = findViewById<TextView>(R.id.Panel)as TextView//<- panel de la app
        var Panel0 = Panel.text
        Panel.text = ""+FechaHora+"-"+Texto+"\n"+Panel0
    }

    fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
    }

    fun getCurrentDateTime(): Date {
        return Calendar.getInstance().time
    }


    //---------------------------------------funciones ===========


    fun EnviarSMS(numero: String, mensaje: String) = doAsync {
        //        toast("Init")
        var error: Exception? = null
        try { //ejecutamos la tarea
            val Aleatorio = (Math.random() * (30000 + 1 - 20000)).toInt() //entre medio y 1 minuto
            Thread.sleep(Aleatorio.toLong()) //espera variable 60000
            val sms = SmsManager.getDefault()
            sms.sendTextMessage(numero, null, mensaje, null, null)
        } catch (e: Exception) { //capturamos el error
            error = e
        }
        //mandamos el resultado
        uiThread {
            if (error != null) {
                // Show the exception message.
                toast("Error al enviar sms " + error)
            } else {
                // Update the UI with the result
                toast("Mensaje enviado con exito a " + numero)

                Thread.sleep(10000)

            }
        }

    }





    inner class LeeServidor : AsyncTask<String, String, String>() {
        override fun onPreExecute() {}
        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpURLConnection? = null

            try {
                val url = URL(urls[0])
                urlConnection = url.openConnection() as HttpURLConnection; urlConnection.connectTimeout = 60000; urlConnection.readTimeout = 60000
                var inString = streamToString(urlConnection.inputStream); publishProgress(inString)
//                toast("OK1")
                //PanelUpdate("Coneccion  exitosa: "+urls[0])
            } catch (ex: Exception) {
                //PanelUpdate("Error en coneccion al enviar sms: " + ex +"\n->"+urls[0])
            } finally {if (urlConnection != null) {urlConnection.disconnect()}}
            return " "
        }
        //        @RequiresApi(Build.VERSION_CODES.DONUT)
        override fun onProgressUpdate(vararg values: String?) {
            try {
                values[0]?.let { toast(it) }
                toast("OK" + values[0])
                var datosjson = JSONObject(values[0])
                val  id = datosjson.get("id")
                var celular = datosjson.get("celular")
                val mensaje = datosjson.get("mensaje")



                val valor = when {
                    celular=="X1" -> {
//                        toast("Dispositivo No Autorizado. Comunicarse al Dpto. de Informatica")
                        PanelUpdate("ERROR: DISPOSITIVO NO AUTORIZADO")
                        CicloDeTrabajo(id.toString(),"S No autorizado","") // le contesta a la plataforma
                    }
                    celular=="0" -> {
                        //sin trabajo actualmente
//                        toast("Sin Trabajo actualmente...")
                        PanelUpdate("Sin Trabajo")
                        CicloDeTrabajo(id.toString(),"S Sin trabajo","") // le contesta a la plataforma
                    }
                    celular=="OK" -> {
                        //respuesta del informe al servidor
//                        toast("Respuesta del la plataforma: OK")
                        PanelUpdate("OK")
                        CicloDeTrabajo(id.toString(),"S Solicitando","") // le contesta a la plataforma
                    }

                    else -> {
//                        toast("Enviando mensaje a " + celular)
                        PanelUpdate("Enviando mensaje a "+ celular)
                        EnviarSMS(celular as String, mensaje as String)
                        CicloDeTrabajo(id.toString(),"R Enviado correctamente","") // le contesta a la plataforma
                    }
                }


            } catch (ex: Exception) {
                toast("Error Servidor: " + ex)
//                Thread.sleep(30000)
                CicloDeTrabajo("","E" + ex.toString(),"")
            }
        }





        override fun onPostExecute(result: String?) { }



    }



    fun streamToString(inputStream: InputStream): String {

        val bufferReader = BufferedReader(InputStreamReader(inputStream))
        var line: String
        var result = ""

        try {
            do {
                line = bufferReader.readLine()
                if (line != null) {
                    result += line
                }
            } while (line != null)
            inputStream.close()
        } catch (ex: Exception) {

        }

        return result
    }






    fun CicloDeTrabajo(IdSMS:String, Info:String, C:String) {
        toast("Iniciando Ciclo " + C)
        //checamos permisos
        var permissionCheck =
            ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.READ_PHONE_STATE
            ) // var para permiso
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                225
            ) //<-- solicita permiso
        } else {

            //obtenemos el imei y android id para crear el idi
            //        val telephonyManager = this@MainActivity.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager; //para la version 7.1

            var myIMEI = ""; val mTelephony = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager //solo para la Ver. api23 android 6
            if (mTelephony.deviceId != null) {myIMEI = mTelephony.deviceId } // lo almacenamos aqui

            var DispositivoIMEI = myIMEI

            var DispositivoID =  Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)


            //Checamos los permisos para enviar sms
            var permissionCheck2 = ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.SEND_SMS
            ) // var para permiso
            if (permissionCheck2 != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.SEND_SMS),
                    225
                ) //<-- solicita permiso
            } else {// :D ya tiene permiso, obtenemos la informacion

                var permissionCheck3 = ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.INTERNET
                ) // var para permiso
                if (permissionCheck3 != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.INTERNET),
                        225
                    ) //<-- solicita permiso
                } else {
                    //EN ESTE PUNTO YA TENEMOS PERMISO DE INTERNET, READPHONE Y SMS


                    var IDI: String = DispositivoID + "" + DispositivoIMEI
                    var Nserie: String = ""; Nserie = Build.SERIAL; // solo para la version 23 android 6.0
                    var deviceInfo: String = "" + Build.MANUFACTURER + "" + Build.MODEL + "" + Nserie
                    toast("->> Soy "+deviceInfo)




                    val StrURL = "https://plataformaitavu.tamaulipas.gob.mx/eamm/sms_ws.php?idi=" + DispositivoIMEI + DispositivoID + "&ext=" + IdSMS + "&r="+Info + deviceInfo
                    toast("URL:"+StrURL)
                    PanelUpdate("URL: "+ StrURL)
                    LeeServidor().execute(StrURL, "", C)
//                CicloDeTrabajo()



                }
            }


        }

        PanelUpdate("Fin de Ciclo "+C)

    }

}
