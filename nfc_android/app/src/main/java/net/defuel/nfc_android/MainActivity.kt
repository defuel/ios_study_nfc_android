package net.defuel.nfc_android

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.graphics.Color
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetAddress
import java.net.Socket
import java.util.*

class MainActivity : Activity() {

    lateinit var mNfcAdapter : NfcAdapter
    lateinit var mPendingIntent: PendingIntent

    private var connectJob : Job? = null

    lateinit var text : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout : LinearLayout = LinearLayout(this)
        layout.setBackgroundColor(Color.BLUE)
        text = TextView(this)
        text.text = "android_run_app start_bluetooth wait_nfc"
        text.setBackgroundColor(Color.RED)
        text.id = R.id.status_text
        layout.addView(text)
        setContentView(layout!!)

        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)

        val intent:Intent = Intent(this, javaClass)
        intent.flags =  Intent.FLAG_ACTIVITY_SINGLE_TOP

        mPendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
    }

    override fun onResume() {
        super.onResume()

        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()

        mNfcAdapter.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val uid = intent!!.getByteArrayExtra(NfcAdapter.EXTRA_ID)
        Log.d("nfc_android", getNfcUidString(uid))
        sendData(getNfcUidString(uid))
    }

    private fun getNfcUidString(array: ByteArray) : String{
        val sb = StringBuilder()
        for(b in array){
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }

    private fun sendData(data : String){
        connectJob = launch {
            Log.d("nfc_android", "connect server")
            val socket = Socket(InetAddress.getByName("192.168.0.12"), 12345)
            Log.d("nfc_android", "success connection")
            val output = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
            output.print(data)
            output.flush()
            output.close()
            socket.close()
            Log.d("nfc_android", "success send data")
        }
    }
}