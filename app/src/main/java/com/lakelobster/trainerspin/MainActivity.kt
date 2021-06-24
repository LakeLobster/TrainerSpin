package com.lakelobster.trainerspin

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.content.PermissionChecker
import androidx.databinding.Observable
import androidx.lifecycle.ViewModelProvider
import com.lakelobster.trainerspin.util.APP_LOG_TAG
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.ByteBuffer
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.xor

class MainActivity : AppCompatActivity() {

    lateinit var viewModel : TacxViewModel

    private val splashFragment = SplashFragment()
    private val listFragment = DeviceListFragment()
    private val noDeviceFragment = NoDeviceFragment()
    private val wrongDeviceFragment = WrongDeviceFragment()
    private val controlFragment = ControlFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this).get(TacxViewModel::class.java)
        viewModel.onStateChanged = advanceState

        viewModel.power.set(0)
        viewModel.cadence.set(0)

        val fm = supportFragmentManager
        val transaction = fm.beginTransaction()
        transaction.replace(R.id.fragmentHolder, splashFragment)
        transaction.commit()

        setupViewModelCallbacks()

        toolbar.setNavigationOnClickListener {
            viewModel.back()
        }


    }

    override fun onBackPressed() {
        viewModel.back()
        if(viewModel.vmState == TacxViewModel.TacxControlState.FINISHED)
        {
            finish()
        }
    }

    val advanceState = { newState: TacxViewModel.TacxControlState ->

        val fm = supportFragmentManager
        val transaction = fm.beginTransaction()
        transaction.setCustomAnimations(
            android.R.anim.slide_in_left,
            android.R.anim.slide_out_right
        )

        when(newState) {
            TacxViewModel.TacxControlState.SPLASH ->
            {
                transaction.replace(R.id.fragmentHolder, splashFragment)
            }
            TacxViewModel.TacxControlState.LOCATION_PERMISSION -> {
                checkPermission()
            }
            TacxViewModel.TacxControlState.LOCATION_ENABLED ->
            {
                testForLocationEnabled()
            }
            TacxViewModel.TacxControlState.SEARCHING ->
            {
                transaction.replace(R.id.fragmentHolder, listFragment)
            }
            TacxViewModel.TacxControlState.NO_DEVICES ->
            {
                transaction.replace(R.id.fragmentHolder,noDeviceFragment)
            }
            TacxViewModel.TacxControlState.CONNECTING ->
            {
                //Show a blank screen for this one
                transaction.remove(listFragment)
            }
            TacxViewModel.TacxControlState.DISCOVERING_SERVICES ->
            {
                //Do nothing
            }
            TacxViewModel.TacxControlState.MISSING_SERVICES ->
            {
                transaction.replace(R.id.fragmentHolder,wrongDeviceFragment)
            }
            TacxViewModel.TacxControlState.CONTROLLING ->
            {
                transaction.replace(R.id.fragmentHolder,controlFragment)
            }
            TacxViewModel.TacxControlState.FINISHED ->
            {
                finish()
            }
        }

        transaction.commit()
        Unit
    }

    override fun onPause() {
        super.onPause()
        tacxbt?.close() //Just to be sure
    }

    fun checkPermission() {

        if (PermissionChecker.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PermissionChecker.PERMISSION_GRANTED
        ) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                var adb = AlertDialog.Builder(this).setTitle("You need location")
                .setCancelable(true)
                    .setMessage("This is necessary for your phone to detect devices.")
                    .setNegativeButton("Cancel", { d, w -> }).setPositiveButton("OK", { d, w ->
                        requestPermissions(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            SplashFragment.FINE_LOCATION_REQUEST
                        );
                    }).create()
                adb.show()
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    SplashFragment.FINE_LOCATION_REQUEST
                );
            }
        } else {
            testForLocationEnabled()
        }
    }

    fun testForLocationEnabled()
    {
        if(!androidx.core.location.LocationManagerCompat.isLocationEnabled(getSystemService(Context.LOCATION_SERVICE) as LocationManager)
        )
        {
            val builder =
                AlertDialog.Builder(this)
            builder.setMessage("Location services must be enabled in order to scan for devices. Enable it now?")
                .setCancelable(false)
                .setPositiveButton(
                    "Yes"
                ) { dialog, id -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                .setNegativeButton(
                    "No"
                ) { dialog, id -> dialog.cancel() }
            val alert = builder.create()
            alert.show()
            return
        }

        //We are good to go
        viewModel.next()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == SplashFragment.FINE_LOCATION_REQUEST)
        {
            assert(grantResults.size == 1)
            when(grantResults[0])
            {
                PackageManager.PERMISSION_DENIED ->
                {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
                PackageManager.PERMISSION_GRANTED ->
                {
                    viewModel.next() //Goes to checking for location turned on
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    val btScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            Log.d(APP_LOG_TAG, "Found device: ${result?.device?.address} - ${result?.device?.name}")
            val foundDevice = BtScanHolder(
                result!!.device.address,
                result.device.name ?: "(Unknown)"
            )
            viewModel.reportScannedDevice(foundDevice)
        }
    }

    fun setupViewModelCallbacks() {
        viewModel.onStartScanning = {
            BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner.startScan(btScanCallback)
        }

        viewModel.onStopScanning = {
            BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner.stopScan(btScanCallback)
        }

        viewModel.onConnectDevice = { dev ->

            val tacx = BluetoothAdapter.getDefaultAdapter().getRemoteDevice( dev.Address)
            tacxbt = tacx.connectGatt(this, true, connectCallback)
        }

        viewModel.onDisconnect = {
            tacxbt?.disconnect()
        }
    }

    ///////////////////////////////////////////////
    // Bluetooth Stuff
    ///////////////////////////////////////////////

    var tacxbt : BluetoothGatt? = null


    val TACX_SERVICE: UUID = UUID.fromString("6e40fec1-b5a3-f393-e0a9-e50e24dcca9e")
    val TACX_TX: UUID = UUID.fromString("6e40fec2-b5a3-f393-e0a9-e50e24dcca9e")
    val TACX_RX: UUID = UUID.fromString("6e40fec3-b5a3-f393-e0a9-e50e24dcca9e")
    val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    var RXCharacteristic: BluetoothGattCharacteristic? = null

    val connectCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            Log.d(APP_LOG_TAG,"Connected!")
            if(newState == BluetoothProfile.STATE_CONNECTED)
            {
                viewModel.next()
            }
            viewModel.status.set(
            when(newState)

            {
                BluetoothProfile.STATE_CONNECTED -> "Connected"
                BluetoothProfile.STATE_CONNECTING -> "Connecting"
                BluetoothProfile.STATE_DISCONNECTED -> "Disconnected"
                BluetoothProfile.STATE_DISCONNECTING -> "Disconnecting"
                else -> "Unknown"
            }
            )
            gatt?.discoverServices()

        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            if (gatt == null) {
                Log.wtf(APP_LOG_TAG,"This shouldn't happen!")
                viewModel.next(false)
                return
            }

            //dumpServiceDetails(gatt)
            var rxs = gatt.getService(TACX_SERVICE)
            if (rxs == null) {
                Log.e(APP_LOG_TAG, "Couldn't find Taxc service!")
                viewModel.next(false)
                return
            }

            RXCharacteristic = rxs.getCharacteristic(TACX_RX)
            var tx = rxs.characteristics.firstOrNull { it.uuid.equals(TACX_TX) }
            if (tx == null) {
                Log.e(APP_LOG_TAG, "Couldn't find RX characteristic!")
                viewModel.next(false)
                return
            }


            var txd = tx.getDescriptor(CCCD)
            if (txd == null) {
                Log.e(APP_LOG_TAG, "Couldn't find TX characteristic!")
                viewModel.next(false)
                return
            }

            gatt.setCharacteristicNotification(tx, true)
            txd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(txd)

            val rx = RXCharacteristic
            if (rx == null) {
                viewModel.next(false)
                return
            }

            Log.d(APP_LOG_TAG, "Ready to go!")
            viewModel.next()



            viewModel.slope10x.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback()
            {
                override fun onPropertyChanged(
                    sender: Observable?,
                    propertyId: Int
                ) {
                    setSlope(viewModel.slope10x.get().toFloat()/ 10.0f)
                }
            })
        }


        fun setSlope(slope: Float) {
            val intslope = ((slope + 200.0) / 0.01f).toInt().toShort()
            val bytes = ByteBuffer.allocate(2).putShort(intslope).array()

            val packet =
                byteArrayOf(-92, 9, 78, 5, 51, -1, -1, -1, -1, bytes[1], bytes[0], -1, 0)
            var x = 0.toByte()
            for (i in 0..packet.size - 1) {
                x = x.xor(packet[i])
            }
            packet[packet.size - 1] = x
            RXCharacteristic?.value = packet

            tacxbt?.writeCharacteristic(RXCharacteristic)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
        }

        override fun onCharacteristicChanged(
                             gatt: BluetoothGatt?,
                             characteristic: BluetoothGattCharacteristic?
                         ) {

             //Spit the contents out for debugging
             //var bytestring = characteristic!!.value.joinToString("-") {
             //    "%02x".format(it)
             //}

             //Log.d(APP_LOG_TAG, String.format("Characteristic changed: thread ID: %d, data: %s", android.os.Process.myTid(),bytestring));

            if (characteristic!!.uuid.equals(TACX_TX)) {

                    if(characteristic.value[4] == 0x19.toByte())
                    {
                        //Power info
                        viewModel.cadence.set(characteristic.value[6].toInt())

                        val powerLSB = characteristic.value[9].toInt()
                        val powerMSB = characteristic.value[10].and(0x0F).toInt()
                        viewModel.power.set(powerMSB.shl(8).or(powerLSB))
                    }
            }
            super.onCharacteristicChanged(gatt, characteristic)
        }

    }
}