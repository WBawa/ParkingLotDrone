package com.riis.parking_lot_drone

import android.Manifest
import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import dji.common.flightcontroller.virtualstick.*
import dji.common.product.Model
import dji.sdk.base.BaseProduct
import dji.sdk.camera.Camera
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager
import dji.sdk.products.Aircraft
import dji.sdk.products.HandHeld
import dji.sdk.sdkmanager.DJISDKManager
import java.util.*

class MainActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {

    private val TAG = MainActivity::class.java.name

    private lateinit var takeOffButton: Button
    private lateinit var landButton: Button
    private lateinit var videoSurface: TextureView
    private lateinit var autoButton: ToggleButton

    private var receivedVideoDataListener: VideoFeeder.VideoDataListener? = null
    private var codecManager: DJICodecManager? = null

    private var sendDataTimer: Timer? = null
    private var sendDataTask: SendDataTask? = null

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
        videoSurface = findViewById(R.id.textureView)
        autoButton = findViewById(R.id.auto_button)

        videoSurface.surfaceTextureListener = this

        receivedVideoDataListener = VideoFeeder.VideoDataListener { videoBuffer, size ->
           codecManager?.sendDataToDecoder(videoBuffer, size)
        }

        viewModel.startSdkRegistration(this)
        initFlightController()
        viewModel.getFlightController()?.setVirtualStickModeEnabled(true, null)

        takeOffButton.setOnClickListener{
            viewModel.getFlightController()!!.startTakeoff(null)
            showToast("Takeoff Success")
        }

        landButton.setOnClickListener {
            viewModel.getFlightController()!!.startLanding(null)
            showToast("Landing Success")
        }

        autoButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
//                val verticalJoyControlMaxSpeed = 2f
                val verticalJoyControlMaxSpeed = 12f
                val yawJoyControlMaxSpeed = 30f
                showToast("trying to move")
                if (null == sendDataTimer) {
                    sendDataTask =
                        SendDataTask(10f, 10f, yawJoyControlMaxSpeed, verticalJoyControlMaxSpeed)
                    sendDataTimer = Timer()
                    sendDataTimer?.schedule(sendDataTask, 0, 200)
                }
            } else {
                sendDataTimer?.cancel()
                sendDataTimer = null
                showToast("setting to null")
            }
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

    private fun getProductInstance(): BaseProduct? {
        return DJISDKManager.getInstance().product
    }

    private fun getCameraInstance(): Camera? {
        if (getProductInstance() == null) return null

        return if (getProductInstance() is Aircraft) {
            (getProductInstance() as Aircraft).camera
        } else if (getProductInstance() is HandHeld) {
            (getProductInstance() as HandHeld).camera
        } else
            null
    }

    private fun initPreviewer() {
        val product: BaseProduct = getProductInstance() ?: return
        if (!product.isConnected) {
            showToast(getString(R.string.disconnected))
        } else {
            videoSurface.surfaceTextureListener = this
            if (product.model != Model.UNKNOWN_AIRCRAFT) {
                VideoFeeder.getInstance().primaryVideoFeed.addVideoDataListener(
                    receivedVideoDataListener
                )
            }
        }
    }

    private fun uninitPreviewer() {
        val camera: Camera = getCameraInstance() ?: return
        VideoFeeder.getInstance().primaryVideoFeed.addVideoDataListener(null)

    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (codecManager == null) {
            codecManager = DJICodecManager(this, surface, width, height)
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        codecManager?.cleanSurface()
        codecManager = null

        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    }

    override fun onResume() {
        super.onResume()
        initPreviewer()
    }

    override fun onPause() {
        uninitPreviewer()
        super.onPause()
    }

    override fun onDestroy() {
        uninitPreviewer()
        super.onDestroy()
    }

    inner class SendDataTask(pitch: Float, roll: Float, yaw: Float, throttle: Float): TimerTask() {
        private val mPitch = pitch
        private val mRoll = roll
        private val mYaw = yaw
        private val mThrottle = throttle


        override fun run() {
            viewModel.getFlightController()?.setVirtualStickModeEnabled(true, null)
            viewModel.getFlightController()?.sendVirtualStickFlightControlData(FlightControlData(mPitch, mRoll, mYaw, mThrottle), null)
        }
    }
}