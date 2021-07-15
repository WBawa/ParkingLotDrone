package com.riis.parking_lot_drone

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem
import dji.common.flightcontroller.virtualstick.RollPitchControlMode
import dji.common.flightcontroller.virtualstick.VerticalControlMode
import dji.common.flightcontroller.virtualstick.YawControlMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.name

    private lateinit var takeOffButton: Button
    private lateinit var landButton: Button
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        takeOffButton = findViewById(R.id.take_off_button)
        landButton = findViewById(R.id.land_button)

        initFlightController()

        takeOffButton.setOnClickListener{
//            viewModel.getFlightController()?.startTakeoff(null)
//            showToast("Takeoff Success")
            if (viewModel.getFlightController() == null) {
                showToast("LOL YOU THOUGHT")
            }
//            viewModel.getFlightController()!!.startTakeoff { djiError ->
//                if (djiError != null) {
//                    Log.i(TAG, djiError.description)
//                    showToast("Takeoff Error: ${djiError.description}")
//                } else {
//                    Log.i(TAG,"Takeoff Success")
//                    showToast("Takeoff Success")
//                }
//            }
        }

        landButton.setOnClickListener {
            showToast("in here lol")
            viewModel.getFlightController()?.startLanding { djiError ->
                if (djiError != null) {
                    Log.i(TAG, djiError.description)
                    showToast("Landing Error: ${djiError.description}")
                } else {
                    Log.i(TAG,"Start Landing Success")
                    showToast("Start Landing Success")
                }
            }
        }

    }


    private fun initFlightController() {

        viewModel.getFlightController()?.let {
            it.setVirtualStickModeEnabled(true) { djiError ->
                if (djiError != null) {
                    Log.i(TAG, djiError.description)
                    showToast("Virtual Stick: Could not disable virtual stick")
                } else {
                    Log.i(TAG,"Disable Virtual Stick Success")
                    showToast("Virtual Sticks Disabled")
                }
            }
            it.rollPitchControlMode = RollPitchControlMode.VELOCITY
            it.yawControlMode = YawControlMode.ANGULAR_VELOCITY
            it.verticalControlMode = VerticalControlMode.VELOCITY
            it.rollPitchCoordinateSystem = FlightCoordinateSystem.BODY
        }

    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}