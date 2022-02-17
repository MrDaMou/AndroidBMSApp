package de.jnns.bmsmonitor

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import de.jnns.bmsmonitor.bluetooth.BleService
import de.jnns.bmsmonitor.databinding.ActivityMainBinding
import de.jnns.bmsmonitor.services.VescService
import de.jnns.bmsmonitor.services.BmsService
import io.realm.Realm

@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity() {
    val binding get() = _binding!!

    public var vescService: VescService? = null
    public var vescServiceBound = false

    private var _binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Realm.init(this)
        Realm.compactRealm(Realm.getDefaultConfiguration()!!)

        // delete data older than 24h
        // val realm = Realm.getDefaultInstance()
        // realm.where<BatteryData>().lessThan("timestamp", System.currentTimeMillis() - 86400000)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val batteryEnabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("batteryEnabled", false)
        val vescEnabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("vescEnabled", false)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 4711)
        } else {
            if (batteryEnabled) {
                Intent(this, BmsService::class.java).also { intent -> startService(intent) }
            }

            if (vescEnabled) {
                Intent(this, VescService::class.java).also { intent ->
                    startService(intent) }.also { intent ->
                    bindService(intent, vescServiceConnection, Context.BIND_AUTO_CREATE)
                }
            }

            Intent(this, BleService::class.java).also { intent -> startService(intent) }
        }

        val batteryFragment = BatteryFragment()
        val bikeFragment = VescProfileFragment()
        val statsFragment = StatsFragment()
        val settingsFragment = SettingsFragment()

        binding.bottomNavigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.page_battery -> supportFragmentManager.beginTransaction().apply { replace(R.id.nav_host_fragment, batteryFragment).commit() }
                R.id.page_bike -> supportFragmentManager.beginTransaction().apply { replace(R.id.nav_host_fragment, bikeFragment).commit() }
                R.id.page_stats -> supportFragmentManager.beginTransaction().apply { replace(R.id.nav_host_fragment, statsFragment).commit() }
                R.id.page_settings -> supportFragmentManager.beginTransaction().apply { replace(R.id.nav_host_fragment, settingsFragment).commit() }
            }
            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            4711 -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Intent(this, BmsService::class.java).also { intent ->
                        startService(intent)
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean = findNavController(R.id.nav_host_fragment).navigateUp() || super.onSupportNavigateUp()

    private val vescServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            val binder = service as VescService.LocalBinder
            vescService = binder.getService()
            vescServiceBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            vescServiceBound = false
        }
    }
}