package de.jnns.bmsmonitor.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.util.Log
import de.jnns.bmsmonitor.bms.BmsCellInfoResponse
import de.jnns.bmsmonitor.bms.BmsGeneralInfoResponse
import java.util.*

@ExperimentalUnsignedTypes
class VescGattClientCallback(
    //val onLcdToMcuDataAvailable: (data: LcdToMcuResponse) -> Unit,
    //val onMcuToLcdDataAvailable: (data: McuToLcdResponse) -> Unit,
    val onConnectionSucceeded: () -> Unit,
    val onConnectionFailed: () -> Unit
) :
    BluetoothGattCallback() {

    var isConnected = false

    lateinit var readCharacteristic: BluetoothGattCharacteristic
    lateinit var writeCharacteristic: BluetoothGattCharacteristic

    private val uartUuid = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    private val rxUuid = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    private val txUuid = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")

    private val bufferSize: Int = 80
    private var uartBuffer = ByteArray(bufferSize)
    private var uartBufferPos: Int = 0
    private var uartBytesLeft: Int = 0

    private var isInFrame = false

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.d("BluetoothGatt", "connection failed")
            onConnectionFailed()
            isConnected = false
            return
        }

        if (newState == BluetoothProfile.STATE_CONNECTED) {
            gatt.discoverServices()
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            isConnected = false
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)

        if (status != BluetoothGatt.GATT_SUCCESS) {
            return
        }

        val uartService = gatt.getService(uartUuid)

        if (uartService != null) {
            readCharacteristic = uartService.getCharacteristic(rxUuid)
            writeCharacteristic = uartService.getCharacteristic(txUuid)

            gatt.setCharacteristicNotification(writeCharacteristic, true)
            gatt.setCharacteristicNotification(readCharacteristic, true)

            onConnectionSucceeded()
            isConnected = true
        }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        super.onCharacteristicChanged(gatt, characteristic)

        Log.d("BluetoothGatt", "BLE Data (" + characteristic.value.size + "): " + characteristic.value.toHexString())

        for (byte: Byte in characteristic.value) {
            if (uartBufferPos >= bufferSize) {
                isInFrame = false
                uartBufferPos = 0
                uartBytesLeft = 0
            }

            uartBuffer[uartBufferPos] = byte

            if (isInFrame) {
                if (uartBufferPos == 3) {
                    uartBytesLeft = byte.toInt()
                }

                if (byte.toUByte() == 0x77.toUByte() && uartBytesLeft < 1) {
                    isInFrame = false
                    onFrameComplete(uartBufferPos)
                    uartBufferPos = 0
                    uartBytesLeft = 0
                } else {

                    uartBufferPos++
                    uartBytesLeft--
                }
            } else if (byte.toUByte() == 0xDD.toUByte()) {
                isInFrame = true
                uartBufferPos++
            }
        }
    }

    private fun onFrameComplete(size: Int) {
        if (size <= 0) {
            return
        }

        val frameBytes = uartBuffer.slice(IntRange(0, size)).toByteArray()

        Log.i("VESCResponse", "FrameData (" + frameBytes.size + "): " + frameBytes.toHexString())

        //if (frameBytes[1] == 0x3.toByte()) {
        //    val generalInfo = BmsGeneralInfoResponse(frameBytes)
        //    onGeneralInfoCallback(generalInfo)
        //} else if (frameBytes[1] == 0x4.toByte()) {
        //    val cellInfo = BmsCellInfoResponse(frameBytes)
        //    //onCellInfoCallback(cellInfo)
        //}
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
}