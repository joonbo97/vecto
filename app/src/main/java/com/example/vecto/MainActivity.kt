package com.example.vecto

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.vecto.databinding.ActivityMainBinding
import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.vecto.Data.Auth.pathPoints
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.PolylineOverlay
import com.naver.maps.map.util.FusedLocationSource

class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationServiceCallback {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mapView: MapFragment
    private lateinit var locationSource: FusedLocationSource // 위치를 반환하는 구현체
    private lateinit var naverMap: NaverMap



    private val pathPoints = mutableListOf<LatLng>()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationService.LocationServiceBinder
            val locationService = binder.getService()
            binder.setCallback(this@MainActivity)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }
    }

    private fun bindToLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }











    companion object {
        private const val LOCATION_PERMISSTION_REQUEST_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val permission: Array<String> = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS)
        //필요한 permission array

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_DENIED
            ||ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_DENIED
            ||ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED){ // 둘 중 하나라도 권한이 없는 경우
            ActivityCompat.requestPermissions(this, permission, LOCATION_PERMISSTION_REQUEST_CODE) // permission request
        }
        else
        {
            initMap()
        }

        //requestNotificationPermission()

        binding.StartServiceButton.setOnClickListener {
            val serviceIntent = Intent(this, LocationService::class.java)
            serviceIntent.action = Actions.START_FOREGROUND
            startService(serviceIntent)
            Toast.makeText(this, "백그라운드 서비스를 시작합니다", Toast.LENGTH_SHORT).show()
        }

        binding.StopServiceButton.setOnClickListener{
            val serviceIntent = Intent(this, LocationService::class.java)
            serviceIntent.action = Actions.STOP_FOREGROUND
            startService(serviceIntent)
            Toast.makeText(this, "백그라운드 서비스를 종료합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initMap(){
        mapView = supportFragmentManager.findFragmentById(R.id.naver_map) as MapFragment?
            ?: MapFragment.newInstance().also {
                supportFragmentManager.beginTransaction().add(R.id.naver_map, it).commit()
            }
        mapView.getMapAsync(this)

        locationSource = FusedLocationSource(this, LOCATION_PERMISSTION_REQUEST_CODE)
    }

    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        naverMap.locationSource = locationSource
        naverMap.locationTrackingMode = LocationTrackingMode.Follow
        naverMap.moveCamera(CameraUpdate.zoomTo(18.0))

        val polyline = PolylineOverlay()

        naverMap.addOnLocationChangeListener { location ->


            val colorHex = "#EC008C"
            val color = Color.parseColor(colorHex)
            pathPoints.add(LatLng(location.latitude, location.longitude))
            if (pathPoints.size > 1) {
                polyline.coords = pathPoints
                if (polyline.map == null) {
                    polyline.width = 10
                    polyline.capType = PolylineOverlay.LineCap.Round
                    polyline.joinType = PolylineOverlay.LineJoin.Round
                    polyline.color = color

                    polyline.map = naverMap
                }
            }
        }
    }

    override fun onLocationUpdate(latitude: Double, longitude: Double) {
        val colorHex = "#EC008C"
        val color = Color.parseColor(colorHex)
        pathPoints.add(LatLng(latitude, longitude))

        val polyline = PolylineOverlay()
        polyline.coords = pathPoints
        polyline.width = 10
        polyline.capType = PolylineOverlay.LineCap.Round
        polyline.joinType = PolylineOverlay.LineJoin.Round
        polyline.color = color
        polyline.map = naverMap
    }
}