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
import java.util.*

class MainActivity_old2 : Activity() {

    lateinit var mNfcAdapter : NfcAdapter
    lateinit var mPendingIntent: PendingIntent

    lateinit var text : TextView

    private var connectJob : Job? = null
    private var sendJob : Job? = null

    private var mBluetoothAdapter : BluetoothAdapter? = null
    private var mBluetoothServerSocket: BluetoothServerSocket? = null
    private var mBluetoothSocket: BluetoothSocket? = null

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

        if(mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        }
        connectBluetooth()
        text.text = "start thread"
    }

    override fun onPause() {
        super.onPause()

        mNfcAdapter.disableForegroundDispatch(this)
        connectJob?.cancel()
        sendJob?.cancel()

        cancel()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val uid = intent!!.getByteArrayExtra(NfcAdapter.EXTRA_ID)
        Log.d("nfc_android", getNfcUidString(uid))
        if(mBluetoothSocket != null){
            sendNfcUid(getNfcUidString(uid))
        }
    }

    private fun getNfcUidString(array: ByteArray) : String{
        val sb = StringBuilder()
        for(b in array){
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }

    private fun connectBluetooth(){
        connectJob = launch {
            mBluetoothServerSocket = mBluetoothAdapter!!.listenUsingInsecureRfcommWithServiceRecord(
                    getString(R.string.device_name),
                    UUID.fromString(getString(R.string.service_uuid)))
            Log.d("nfc_android", "bluetooth_connect_wait:" + getString(R.string.service_uuid))
            runOnUiThread { text.text = "bluetooth_connect_wait" }
            mBluetoothSocket = mBluetoothServerSocket!!.accept()
            Log.d("nfc_android","bluetooth_connect_success")
            runOnUiThread { text.text = "bluetooth_connect_success" }
            mBluetoothServerSocket!!.close()
            mBluetoothServerSocket = null
        }
    }

    private fun sendNfcUid(uid : String){
        sendJob = launch {
            mBluetoothSocket!!.outputStream.write(uid.toByteArray())
            Log.d("nfc_android","bluetooth_send:" + uid)
            runOnUiThread { text.text = uid }
        }
    }

    private fun cancel(){
        if(mBluetoothServerSocket != null){
            mBluetoothServerSocket!!.close()
            mBluetoothServerSocket = null
        }
        if(mBluetoothSocket != null){
            mBluetoothSocket!!.close()
            mBluetoothSocket = null
        }
    }
}