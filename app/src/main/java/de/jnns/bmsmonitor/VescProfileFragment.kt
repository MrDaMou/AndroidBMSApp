package de.jnns.bmsmonitor

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.github.anastr.speedviewlib.components.Section
import com.google.gson.Gson
import de.jnns.bmsmonitor.data.BikeData
import de.jnns.bmsmonitor.databinding.FragmentVescBinding
import de.jnns.bmsmonitor.services.VescService
import de.jnns.bmsmonitor.services.BmsService


@ExperimentalUnsignedTypes
class VescProfileFragment : Fragment() {
    private var _binding: FragmentVescBinding? = null
    private val binding get() = _binding!!

    var vescService: VescService? = null
    var vescServiceBound = false

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val msg: String = intent.getStringExtra("bikeData")!!

                binding.labelStatus.text = String.format(resources.getString(R.string.connectedToBike), intent.getStringExtra("deviceName"))

                if (msg.isNotEmpty()) {
                    updateUi(Gson().fromJson(msg, BikeData::class.java))
                }
            } catch (ex: Exception) {
                // ignored
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mMessageReceiver, IntentFilter("bikeDataIntent"))
        _binding = FragmentVescBinding.inflate(inflater, container, false)

        Intent(activity, BmsService::class.java).also { intent ->
            activity?.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.speedViewSpeed.clearSections()
        binding.speedViewSpeed.addSections(
            Section(0.0f, 0.2f, ContextCompat.getColor(requireContext(), R.color.white), 72.0f),
            Section(0.2f, 0.4f, ContextCompat.getColor(requireContext(), R.color.batteryDischargeLow), 72.0f),
            Section(0.4f, 0.6f, ContextCompat.getColor(requireContext(), R.color.batteryDischargeMedium), 72.0f),
            Section(0.6f, 0.8f, ContextCompat.getColor(requireContext(), R.color.batteryDischargeHigh), 72.0f),
            Section(0.8f, 1.0f, ContextCompat.getColor(requireContext(), R.color.batteryDischargeHighest), 72.0f)
        )

        binding.speedViewSpeed.maxSpeed = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("maxSpeed", "35")!!.toFloat()


    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        activity?.unbindService(mConnection)
    }

    private fun updateUi(bikeData: BikeData) {
        requireActivity().runOnUiThread {
            binding.speedViewSpeed.speedTo(bikeData.speed.toFloat())

            binding.labelSpeed.text = bikeData.speed.toString()
            binding.labelAssistLevel.text = bikeData.assistLevel.toString()
        }
    }

    private fun doStuff(){
        Intent(activity, BmsService::class.java).also { intent ->
            activity?.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        }

        vescService?.setBallernProfile()
    }

    /** Defines callbacks for service binding, passed to bindService()  */
    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder: VescService.LocalBinder = service as VescService.LocalBinder
            vescService = binder.service
            vescServiceBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            vescServiceBound = false
        }
    }
}