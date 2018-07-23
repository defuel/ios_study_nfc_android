package net.defuel.nfc_android

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import java.util.*

class MainActivity_old : Activity() {

    lateinit var mNfcAdapter : NfcAdapter
    lateinit var mPendingIntent: PendingIntent

    lateinit var bleManager: BluetoothManager
    lateinit var bleAdapter:BluetoothAdapter
    lateinit var bleLeAdvertiser:BluetoothLeAdvertiser
    lateinit var btGattService : BluetoothGattService
    lateinit var bleGattCharacteristic: BluetoothGattCharacteristic
    lateinit var bleGattServer: BluetoothGattServer
    lateinit var text : TextView
    var bleDevice : BluetoothDevice? = null

    val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
            Log.d("nfc_android", "status:" + status)
        }

        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if(newState == BluetoothProfile.STATE_CONNECTED){
                bleDevice = device
                runOnUiThread { text.text = "connect" }
            } else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                bleDevice = null
                runOnUiThread { text.text = "disconnect" }
            }
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            if(offset != 0){
                bleGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset, null)
                return
            }
            bleGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic?.value)
        }

        override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice?, requestId: Int, descriptor: BluetoothGattDescriptor?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value)
        }

        override fun onDescriptorReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor?) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor)
            if(offset != 0){
                bleGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset, null)
                return
            }
            bleGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor?.value)
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            super.onNotificationSent(device, status)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var layout : LinearLayout = LinearLayout(this)
        text = TextView(this)
        text.text = "start advertising"
        layout.addView(text)
        setContentView(layout!!)

        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)

        var intent:Intent = Intent(this, javaClass)
        intent.flags =  Intent.FLAG_ACTIVITY_SINGLE_TOP

        mPendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        bleManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bleAdapter = bleManager.adapter
        bleLeAdvertiser = bleAdapter.bluetoothLeAdvertiser
        Log.d("nfc_android", "adv:" + bleAdapter.isMultipleAdvertisementSupported())
        Log.d("nfc_android", "fil:" + bleAdapter.isOffloadedFilteringSupported())
        Log.d("nfc_android", "scan:" + bleAdapter.isOffloadedScanBatchingSupported())
        if(bleLeAdvertiser != null){
            btGattService = BluetoothGattService(UUID.fromString(getString(R.string.service_uuid)), BluetoothGattService.SERVICE_TYPE_PRIMARY)
            bleGattCharacteristic = BluetoothGattCharacteristic(UUID.fromString(getString(R.string.characteristic_uuid)),
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ or  BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE)
            btGattService.addCharacteristic(bleGattCharacteristic)
            val descriptor = BluetoothGattDescriptor(UUID.fromString(getString(R.string.characteristic_config_uuid)),
                    BluetoothGattDescriptor.PERMISSION_READ or  BluetoothGattDescriptor.PERMISSION_WRITE)
            bleGattCharacteristic.addDescriptor(descriptor)
            bleGattServer = bleManager.openGattServer(this, gattServerCallback)
            bleGattServer.addService(btGattService)
            val dataBuilder = AdvertiseData.Builder()
            val settingsBuilder = AdvertiseSettings.Builder()
            dataBuilder.setIncludeTxPowerLevel(false)
            dataBuilder.addServiceUuid(ParcelUuid.fromString(getString(R.string.service_uuid)))
            settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            bleLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), object : AdvertiseCallback(){
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {

                }

                override fun onStartFailure(errorCode: Int) {

                }
            })
        }
    }

    override fun onResume() {
        super.onResume()

        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()

        mNfcAdapter.disableForegroundDispatch(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        bleGattServer.close()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        var uid = intent!!.getByteArrayExtra(NfcAdapter.EXTRA_ID)
        Log.d("nfc_android", getNfcUidString(uid))
        if(bleDevice != null){
            bleGattCharacteristic.setValue(getNfcUidString(uid))
            bleGattServer.notifyCharacteristicChanged(bleDevice, bleGattCharacteristic, true)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?) {
        if(requestCode == 0){
            Log.d("nfc_android", "success_permission")
        }
    }

    fun getNfcUidString(array: ByteArray) : String{
        var sb = StringBuilder()
        for(b in array){
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }
}