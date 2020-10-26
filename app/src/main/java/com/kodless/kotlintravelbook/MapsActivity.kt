package com.kodless.kotlintravelbook

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.kodless.kotlintravelbook.Activity.MainActivity
import com.kodless.kotlintravelbook.Model.Place
import java.lang.Exception
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onBackPressed() {
        super.onBackPressed()

        val intentToMain = Intent(applicationContext, MainActivity::class.java)
        startActivity(intentToMain)
        finish()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener(myListener)
       locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener = object : LocationListener{
            override fun onLocationChanged(location: Location) {

                if(location != null){
                    val newUserLocation = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newUserLocation, 15f))
                }

            }

        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }else{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2, 2f, locationListener)
            val intent = intent
            val info = intent.getStringExtra("info")

            if(info.equals("new")){
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if(lastLocation != null){
                    val lastLocationLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocationLatLng, 15f))
                }
            }else{
                mMap.clear()

                var selectedPalce = intent.getSerializableExtra("secilenYer") as Place
                val selectedLocation = LatLng(selectedPalce.latitude!!, selectedPalce.longitude!!)
                mMap.addMarker(MarkerOptions().title(selectedPalce.adress).position(selectedLocation))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15f))
            }


        }
    }

    val myListener = object  : GoogleMap.OnMapLongClickListener{
        override fun onMapLongClick(p0: LatLng?) {

            val geocoder = Geocoder(this@MapsActivity, Locale.getDefault())
            var adress = ""

            if(p0!= null){
                val adressList = geocoder.getFromLocation(p0.latitude, p0.longitude, 1)
                if(adressList != null && adressList.size>0){
                    if(adressList[0].thoroughfare != null){
                        adress += adressList[0].thoroughfare + " "
                        if(adressList[0].subThoroughfare!= null){
                            adress += adressList[0].subThoroughfare + " "
                        }
                    }
                }else{
                    adress = "Geçerli bir adres bulunamadı"
                }

                mMap.addMarker(MarkerOptions().position(p0).title(adress))

                val newPlace = Place(adress, p0.latitude, p0.longitude)

                val dialog = AlertDialog.Builder(this@MapsActivity)
                dialog.setCancelable(false)
                dialog.setTitle("Kaydetmek istiyor musun?")
                dialog.setMessage(newPlace.adress)
                dialog.setPositiveButton("Yes"){ dialog, which ->
                    //SQLite kayıt işlemi
                    try {

                        val database = openOrCreateDatabase("Yerler", Context.MODE_PRIVATE, null)
                        database.execSQL("CREATE TABLE IF NOT EXISTS yerler (address VARCHAR, latitude DOUBLE, longitude DOUBLE)")
                        val toCompile = "INSERT INTO yerler (address, latitude, longitude) VALUES (?,?,?)"
                        val sqlLiteStatement = database.compileStatement(toCompile)
                        sqlLiteStatement.bindString(1, newPlace.adress)
                        sqlLiteStatement.bindDouble(2, newPlace.latitude!!)
                        sqlLiteStatement.bindDouble(3, newPlace.longitude!!)
                        sqlLiteStatement.execute()



                    }catch (e: Exception){
                        e.printStackTrace()
                    }

                    Toast.makeText(applicationContext,"SQLite kayıt ekleme tamamlandı", Toast.LENGTH_LONG).show()

                }.setNegativeButton("No"){ dialog, which ->
                    Toast.makeText(applicationContext, "Konum kaydedilmedi", Toast.LENGTH_LONG).show()
                }

                dialog.show()
            }

        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if(requestCode == 1){
            if (grantResults.size>0){
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2, 2f, locationListener)

                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}