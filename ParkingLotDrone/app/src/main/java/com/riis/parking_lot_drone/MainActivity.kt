package com.riis.parking_lot_drone

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.VIBRATE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.SYSTEM_ALERT_WINDOW,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.RECORD_AUDIO
            ), 1
        )

        takeOffButton = findViewById(R.id.take_off_button)
        landButton = findViewById(R.id.land_button)

        viewModel.startSdkRegistration(this)
        initFlightController()

        takeOffButton.setOnClickListener{
            viewModel.getFlightController()!!.startTakeoff(null)
            showToast("Takeoff Success")
        }

        landButton.setOnClickListener {
            viewModel.getFlightController()!!.startLanding(null)
            showToast("Landing Success")
        }

    }


    private fun initFlightController() {

        viewModel.getFlightController()?.let {
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