package com.lakelobster.trainerspin

import android.util.Log
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.ViewModel
import com.lakelobster.trainerspin.util.APP_LOG_TAG

class TacxViewModel : ViewModel() {

    val scannedDevices: ObservableArrayList<BtScanHolder> = ObservableArrayList<BtScanHolder>()

    //CONTROL PAGE Data Items

    val power = ObservableInt(30)
    val cadence = ObservableInt(15)

    val status = ObservableField<String>("Disconnected")

    val slope10x = ObservableInt(0)


    var onStateChanged: ((TacxControlState) -> Unit)? = null
    var vmState: TacxControlState = TacxControlState.SPLASH
        private set(value) {
            field = value
            onStateChanged?.invoke(value)
        }

    val handler = android.os.Handler()

    // Bluetooth Control Callbacks
    var onStartScanning: (() -> (Unit))? = null
    var onStopScanning: (() -> (Unit))? = null
    var onDisconnect: (() -> (Unit))? = null
    var onConnectDevice: ((BtScanHolder) -> Unit)? = null

    enum class TacxControlState {
        SPLASH,
        LOCATION_PERMISSION,
        LOCATION_ENABLED,
        SEARCHING,
        NO_DEVICES,
        CONNECTING,
        DISCOVERING_SERVICES,
        MISSING_SERVICES,
        CONTROLLING,
        FINISHED,
    }

    fun next(arg: Any? = null) {
        val last = vmState

        when (vmState) {
            TacxControlState.SPLASH -> {
                vmState = TacxControlState.LOCATION_PERMISSION
            }
            TacxControlState.LOCATION_PERMISSION -> {
                vmState = TacxControlState.LOCATION_ENABLED
            }
            TacxControlState.LOCATION_ENABLED -> {
                vmState = TacxControlState.SEARCHING
                //Start Searching
                startScan()
            }
            TacxControlState.SEARCHING -> {
                if (deviceToUse == null) {
                    vmState = TacxControlState.NO_DEVICES
                } else {
                    handler.removeCallbacks(scanTimeout)
                    vmState = TacxControlState.CONNECTING
                    onConnectDevice!!.invoke(deviceToUse!!)
                }
                stopScan()
            }
            TacxControlState.NO_DEVICES,
            TacxControlState.MISSING_SERVICES -> {
                // try again
                vmState = TacxControlState.SEARCHING
                startScan()
            }
            TacxControlState.CONNECTING -> {
                vmState = TacxControlState.DISCOVERING_SERVICES
            }
            TacxControlState.DISCOVERING_SERVICES -> {
                if (arg == false) {
                    vmState = TacxControlState.MISSING_SERVICES
                } else {

                    vmState = TacxControlState.CONTROLLING
                }
            }
        }
    }

    fun back() {
        when (vmState) {
            TacxControlState.SPLASH,
            TacxControlState.LOCATION_PERMISSION,
            TacxControlState.LOCATION_ENABLED,
            TacxControlState.MISSING_SERVICES,
            TacxControlState.NO_DEVICES -> {
                vmState = TacxControlState.FINISHED
            }
            TacxControlState.SEARCHING -> {
                stopScan()
                vmState = TacxControlState.SPLASH
            }
            TacxControlState.CONTROLLING -> {
                onDisconnect?.invoke()
                deviceToUse = null
                vmState = TacxControlState.SPLASH
            }
            TacxControlState.DISCOVERING_SERVICES,
            TacxControlState.CONNECTING -> {
                //Do nothing!
                onDisconnect?.invoke()
                deviceToUse = null
                vmState = TacxControlState.SPLASH
            }
        }
    }

    fun startScan() {
        onStartScanning?.invoke()
        handler.postDelayed({
            scanFinished()
        }, 40000)
    }

    val scanTimeout: (() -> Unit) = {
        Log.d(APP_LOG_TAG, "Bluetooth Timeout Reached!")
        scanFinished()
    }

    fun stopScan() {
        onStopScanning?.invoke()
    }

    fun scanFinished() {
        if (vmState == TacxControlState.SEARCHING) {
            next()
        } else {
            Log.e(APP_LOG_TAG, "Scan finished called at the wrong time!")
        }
    }

    var deviceToUse: BtScanHolder? = null
        private set

    fun selectDevice(device: BtScanHolder) {
        deviceToUse = device
    }

    fun reportScannedDevice(foundDevice: BtScanHolder) {
        if (!scannedDevices.any { device -> device.Address == foundDevice.Address }) {
            scannedDevices.add(foundDevice)
        }
    }
}