package com.riis.parking_lot_drone

import android.Manifest
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem
import dji.common.flightcontroller.virtualstick.RollPitchControlMode
import dji.common.flightcontroller.virtualstick.VerticalControlMode
import dji.common.flightcontroller.virtualstick.YawControlMode
import dji.common.product.Model
import dji.sdk.base.BaseProduct
import dji.sdk.camera.Camera
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager
import dji.sdk.products.Aircraft
import dji.sdk.products.HandHeld
import dji.sdk.sdkmanager.DJISDKManager

class MainActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {

    private val TAG = MainActivity::class.java.name

    private lateinit var takeOffButton: Button
    private lateinit var landButton: Button
    private lateinit var videoSurface: TextureView

    private var receivedVideoDataListener: VideoFeeder.VideoDataListener? = null
    private var codecManager: DJICodecManager? = null

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

        videoSurface.surfaceTextureListener = this

        receivedVideoDataListener = VideoFeeder.VideoDataListener { videoBuffer, size ->
           codecManager?.sendDataToDecoder(videoBuffer, size)
        }

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
}