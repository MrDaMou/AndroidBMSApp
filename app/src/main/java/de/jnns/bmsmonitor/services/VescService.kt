package de.jnns.bmsmonitor.services

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import de.jnns.bmsmonitor.bluetooth.VescGattClientCallback
import de.jnns.bmsmonitor.bluetooth.BleManager
import de.jnns.bmsmonitor.bms.BmsCellInfoResponse
import de.jnns.bmsmonitor.bms.BmsGeneralInfoResponse
import de.jnns.bmsmonitor.data.BatteryData
import de.jnns.bmsmonitor.static.EVescProfile
import io.realm.Realm


@ExperimentalUnsignedTypes
class VescService : Service() {
    // Binder stuff
    private val binder = LocalBinder()

    private val cmdGetMcConf: ByteArray = ubyteArrayOf(0x02U, 0x01U, 0x0EU, 0xE1U, 0xCEU, 0x03U).toByteArray()


    // bluetooth stuff
    private lateinit var bluetoothGatt: BluetoothGatt
    private lateinit var gattClientCallback: VescGattClientCallback
    private lateinit var currentBleDevice: BluetoothDevice

    // Handler that is going to poll data from the bms
    // it is going to toggle "dataModeSwitch" and
    // request General or Cell data
    private val dataHandler: Handler = Handler()
    private var dataModeSwitch = false
    private var dataPollDelay: Long = 0

    // we need both datasets to update the view
    private var cellInfoReceived = false
    private var generalInfoReceived = false

    // bluetooth device mac to use
    private lateinit var bleMac: String
    private lateinit var blePin: String
    private lateinit var bleName: String

    // no need to refresh data in the background
    private var isInForeground = false

    // is connected
    private var isConnected = false
    private var isConnecting = false

    private lateinit var listener: SharedPreferences.OnSharedPreferenceChangeListener

    override fun onCreate() {
        super.onCreate()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        bleMac = prefs.getString("macAddressVesc", "")!!
        blePin = prefs.getString("blePinVesc", "")!!

        BleManager.i.onUpdateFunctions.add {
            searchForDeviceAndConnect()
        }

        listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "macAddressVesc") {
                bleMac = PreferenceManager.getDefaultSharedPreferences(this).getString("macAddressVesc", "")!!

                disconnectFromDevice()
                searchForDeviceAndConnect()
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(listener)

        isInForeground = true
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun onConnectionFailed() {
        isConnected = false
        isConnecting = false

        connectToDevice()
    }

    private fun onConnectionSucceeded() {
        isConnected = true
        isConnecting = false

        /*
        dataHandler.postDelayed(object : Runnable {
            override fun run() {
                if (gattClientCallback.isConnected) {
                    if (isInForeground) {
                        if (dataModeSwitch) {
                            writeBytes(cmdGeneralInfo)
                        } else {
                            writeBytes(cmdCellInfo)
                        }

                        dataModeSwitch = !dataModeSwitch

                        dataHandler.postDelayed(this, dataPollDelay)
                    }
                } else {
                    connectToDevice()
                }
            }
        }, dataPollDelay)
        */
    }

    private fun searchForDeviceAndConnect() {
        val bleDevice = BleManager.i.bleDevices.firstOrNull { x -> x.address.equals(bleMac, ignoreCase = true) }

        if (bleDevice != null) {
            currentBleDevice = bleDevice
            connectToDevice()
        }
    }

    private fun connectToDevice() {
        if (!isConnected && !isConnecting) {
            isConnecting = true

            // bluetooth uart callbacks
            gattClientCallback = VescGattClientCallback(
                ::onConnectionSucceeded,    // on connection succeeded
                ::onConnectionFailed        // on connection fails
            )

            if(blePin.isNotEmpty())
                currentBleDevice.setPin(blePin.toByteArray())
            currentBleDevice.createBond()

            bluetoothGatt = currentBleDevice.connectGatt(this, false, gattClientCallback)
        }
    }

    private fun disconnectFromDevice() {
        if (isConnected) {
            bluetoothGatt.close()

            isConnected = false
        }
    }

    private fun writeBytes(bytes: ByteArray) {
        gattClientCallback.writeCharacteristic.value = bytes
        bluetoothGatt.writeCharacteristic(gattClientCallback.writeCharacteristic)
    }

    public fun setProfile(profile: EVescProfile){
        writeBytes(profile.bytes)
    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): VescService = this@VescService
    }

    fun requestMcConfTemp(){
        Log.i("Request sent", "Get McConf request has been sent")
        writeBytes(cmdGetMcConf)
    }
}