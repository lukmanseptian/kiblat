package com.example.kiblat

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.kiblat.ui.theme.KiblatTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ln
import kotlin.math.tan

class MainActivity : ComponentActivity(),SensorEventListener {
    private lateinit var sensorManager: SensorManager


    private var floatGravity = FloatArray(3)
    private var floatGeoMagnetic = FloatArray(3)
    private var floatOrientation = FloatArray(3)
    private var floatRotationMatrix = FloatArray(9)


    private val rotationDegree = mutableFloatStateOf(0f)


    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var locationRequired: Boolean = false


    private val context = this@MainActivity


    private val permissions = arrayOf(
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
    )


    override fun onResume() {
        super.onResume()
        if (locationRequired) {
            startLocationUpdate()
        }
    }


    override fun onPause() {
        super.onPause()
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        val defaultLat = -6.97465995592464
        val defaultLng = 108.50091662123228
        val kabahLat = 21.42264920624544
        val kabahLng = 39.82639113882412


        setContent {
            var userCurrentLocation by remember {
                mutableStateOf(LatLng(defaultLat, defaultLng))
            }


            locationCallback = object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult) {
                    super.onLocationResult(p0)
                    for (location in p0.locations) {
                        userCurrentLocation = LatLng(location.latitude, location.longitude)
                    }
                }
            }


            val angle = getBearing(
                userCurrentLocation.latitude,
                userCurrentLocation.longitude,
                kabahLat,
                kabahLng
            )


            KiblatTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Kiblat(rotationDegree = this.rotationDegree.value, angle = -angle.toFloat()

                    )
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun startLocationUpdate() {
        locationCallback?.let {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 100
            )
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(3000)
                .setMaxUpdateDelayMillis(100)
                .build()


            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                it,
                Looper.getMainLooper()
            )
        }
    }
    private fun setupSensorStuff() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }


        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }


    private fun getBearing(startLat: Double, startLng: Double, endLat: Double, endLng: Double): Double {
        val latitude1 = Math.toRadians(startLat)
        val longitude1 = Math.toRadians(-startLng)
        val latitude2 = Math.toRadians(endLat)
        val longitude2 = Math.toRadians(-endLng)


        var dLong = longitude2 - longitude1


        val dPhi = ln(tan(latitude2 / 2.0 + Math.PI / 4.0) / tan(latitude1 / 2.0 + Math.PI / 4.0))
        if (abs(dLong) > Math.PI) dLong = if (dLong > 0.0) -(2.0 * Math.PI - dLong)
        else 2.0 * Math.PI + dLong


        return (Math.toDegrees(atan2(dLong, dPhi)) + 360.0) % 360.0
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            floatGravity = event.values
            SensorManager.getRotationMatrix(floatRotationMatrix, null, floatGravity, floatGeoMagnetic)
            SensorManager.getOrientation(floatRotationMatrix, floatOrientation)


            val degree = floatOrientation[0]*180/3.14159
            this.rotationDegree.value = -degree.toFloat()
        } else if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            floatGeoMagnetic = event.values
            SensorManager.getRotationMatrix(floatRotationMatrix, null, floatGravity, floatGeoMagnetic)
            SensorManager.getOrientation(floatRotationMatrix, floatOrientation)


            val degree = floatOrientation[0]*180/3.14159
            this.rotationDegree.value = -degree.toFloat()
        }
    }


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {


    }



    @Composable
    fun Kiblat(rotationDegree:Float,angle:Float=0f) {
        val launchMultiplePermission =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionMaps ->
                val areGranted = permissionMaps.values.reduce { acc, next ->
                    acc && next
                }
                if (areGranted) {
                    locationRequired = true
                    startLocationUpdate()
                    Toast.makeText(
                        context,
                        "Permission Granted",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT)
                        .show()
                }
            }


        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Column {
                Column (modifier = Modifier.rotate(rotationDegree), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.width(intrinsicSize = IntrinsicSize.Max)) {
                        Box {
                            Column (verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,modifier = Modifier.size(330.dp)) {
                                Image(
                                    painter = painterResource(id = R.drawable.cardinal_points),
                                    contentDescription = "cardinal points"
                                )
                            }
                        }
                        Box(modifier = Modifier.rotate(angle)){
                            Column (modifier=Modifier.size(330.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top){
                                Image(
                                    painter = painterResource(id = R.drawable.kaaba),
                                    contentDescription = "kaabah",
                                    modifier=Modifier.size(36.dp)
                                )
                                Image(
                                    painter = painterResource(id = R.drawable.chevron_up),
                                    contentDescription = "chevron"
                                )
                            }
                        }
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(330.dp)
                ) {
                    Button(onClick = {
                        if (permissions.all {
                                ContextCompat.checkSelfPermission(
                                    context,
                                    it
                                ) == PackageManager.PERMISSION_GRANTED
                            }) {
                            startLocationUpdate()
                        } else {
                            launchMultiplePermission.launch(permissions)
                        }
                    }

                    ) {
                        Text(text = "get your location")
                    }
                }
            }

        }
    }

    @Preview(showBackground = true)
    @Composable
    fun KiblatPreview() {
        KiblatTheme {
            Kiblat(0f,500f)
        }
    }


}

