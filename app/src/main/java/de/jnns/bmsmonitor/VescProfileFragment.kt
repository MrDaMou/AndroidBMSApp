package de.jnns.bmsmonitor

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import de.jnns.bmsmonitor.static.EVescProfile


@ExperimentalUnsignedTypes
class VescProfileFragment : Fragment() {
    private var _binding: FragmentVescBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVescBinding.inflate(inflater, container, false)



        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        binding.buttonBallern.setOnClickListener{
            profileButtonClick(EVescProfile.BALLERN)
        }
        binding.buttonCruise.setOnClickListener{
            profileButtonClick(EVescProfile.CRUISE)
        }
        binding.buttonLegal.setOnClickListener{
        }

        val ayy = 3.1415926f
        Toast.makeText(activity, java.lang.Float.toHexString(ayy), Toast.LENGTH_LONG)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun updateUi(bikeData: BikeData) {
        //requireActivity().runOnUiThread {
        // TODO: update UI
        //}
        return
    }

    private fun profileButtonClick(profile: EVescProfile){
            if ((activity as MainActivity).vescServiceBound) {
                (activity as MainActivity).vescService?.setProfile(profile)
                Toast.makeText(activity, "Profile Set: " + profile.name, Toast.LENGTH_SHORT).show()
            }
            else
                Toast.makeText(activity, "VESC Service not connected", Toast.LENGTH_SHORT).show()
    }
}