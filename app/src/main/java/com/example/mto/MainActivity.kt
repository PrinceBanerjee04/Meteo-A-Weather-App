package com.example.mto

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.audiofx.Equalizer.Settings
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.mto.databinding.ActivityMainBinding
import com.example.mto.pojo.ModelClass
import com.example.mto.utils.ApiUtilities
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.atomic.LongAccumulator
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var activityMainBinding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding=DataBindingUtil.setContentView(this,R.layout.activity_main)
        supportActionBar?.hide()

        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(this)
        activityMainBinding.rlMainLayout.visibility= View.GONE

        getCurrentLocation()

        activityMainBinding.etGetCityName.setOnEditorActionListener({ v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                getCityWeather(activityMainBinding.etGetCityName.text.toString())
                val view = this.currentFocus
                if (view != null) {
                    val imm: InputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    activityMainBinding.etGetCityName.clearFocus()
                }
                true
            } else false
        })
    }

    private fun getCityWeather(cityName:String){
        activityMainBinding.pbLoading.visibility=View.VISIBLE
        ApiUtilities.getApiInterface()?.getCityWeatherData(cityName, API_KEY)?.enqueue(object :Callback<ModelClass>{
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                setDataOnViews(response.body())
            }

            override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                Toast.makeText(applicationContext,"Not a Valid City Name",Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun getCurrentLocation(){
        if(checkPermissions())
        {
            if(isLocationEnabled())
            {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermission()
                    return
                }
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this){ task->

                    val location:Location?=task.result
                    if(location==null)
                    {
                        Toast.makeText(applicationContext,"Null",Toast.LENGTH_SHORT).show()
                    }
                    else
                    {
                        fetchCurrentLocationWeather(location.latitude.toString(),location.longitude.toString())
                    }
                }
            }
            else
            {
                Toast.makeText(this,"Turn on Location",Toast.LENGTH_SHORT).show()
                val intent=Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
        else
        {
            requestPermission()
        }
    }

    private fun fetchCurrentLocationWeather(latitude:String,longitude:String){
        activityMainBinding.pbLoading.visibility=View.VISIBLE
        ApiUtilities.getApiInterface()?.getCurrentWeatherData(latitude,longitude,API_KEY)?.enqueue(object :Callback<ModelClass>{
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                if(response.isSuccessful)
                {
                    setDataOnViews(response.body())
                }
            }

            override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                Toast.makeText(applicationContext,"Error",Toast.LENGTH_SHORT).show()
            }

        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setDataOnViews(body:ModelClass?){
        val sdf=SimpleDateFormat("dd/MM/yyyy hh:mm")
        val currentDate=sdf.format(Date())
        activityMainBinding.tvDateAndTime.text=currentDate

        activityMainBinding.tvDayMaxTemp.text="Day "+kelvinToCelsius(body!!.main.temp_max)+"°"
        activityMainBinding.tvDayMinTemp.text="Night "+kelvinToCelsius(body!!.main.temp_min)+"°"
        activityMainBinding.tvTemp.text=""+kelvinToCelsius(body!!.main.temp)+"°"
        activityMainBinding.tvFeelsLike.text="Feels like "+kelvinToCelsius(body!!.main.feels_like)+"°"

        activityMainBinding.tvWeatherType.text=body.weather[0].main
        activityMainBinding.tvSunrise.text=timeStampToLocalDate(body.sys.sunrise.toLong())
        activityMainBinding.tvSunset.text=timeStampToLocalDate(body.sys.sunset.toLong())

        activityMainBinding.tvPressure.text=body.main.pressure.toString()
        activityMainBinding.tvHumidity.text=body.main.humidity.toString()+" %"
        activityMainBinding.tvWindSpeed.text=body.wind.speed.toString()+" m/s"

        activityMainBinding.tvTempFahrenheit.text=""+((kelvinToCelsius(body.main.temp).times(1.8).plus(32)).roundToInt())
        activityMainBinding.etGetCityName.setText(body.name)

        updateUI(body.weather[0].id)



    }

    private fun updateUI(id :Int ){
        if(id in 200..232){
            //Thunderstorm
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor=resources.getColor(R.color.thunderstorm)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.thunderstorm))
            activityMainBinding.rlSubLayout.background=ContextCompat.getDrawable(this@MainActivity,R.color.thunderstorm) //Add bg

            activityMainBinding.llMainBgBelow.background=ContextCompat.getDrawable(this@MainActivity,R.color.thunderstorm) //Add bg
            activityMainBinding.llMainBgAbove.background=ContextCompat.getDrawable(this@MainActivity,R.color.thunderstorm) //Add bg
            activityMainBinding.ivWeatherBg.setImageResource(R.color.thunderstorm) //Add bg
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.thunderstorm1) //Change Icon
        }else if(id in 300..321){
            //Drizzle
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor=resources.getColor(R.color.drizzle)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.drizzle))
            activityMainBinding.rlSubLayout.background=ContextCompat.getDrawable(this@MainActivity,R.color.drizzle) //Add bg

            activityMainBinding.llMainBgBelow.background=ContextCompat.getDrawable(this@MainActivity,R.color.drizzle) //Add bg
            activityMainBinding.llMainBgAbove.background=ContextCompat.getDrawable(this@MainActivity,R.color.drizzle) //Add bg
            activityMainBinding.ivWeatherBg.setImageResource(R.color.drizzle) //Add bg
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.lightrain) //Change Icon
        }else if(id in 500..531){
            //Rain
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor=resources.getColor(R.color.rain)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.rain))
            activityMainBinding.rlSubLayout.background=ContextCompat.getDrawable(this@MainActivity,R.color.rain) //Add bg

            activityMainBinding.llMainBgBelow.background=ContextCompat.getDrawable(this@MainActivity,R.color.rain) //Add bg
            activityMainBinding.llMainBgAbove.background=ContextCompat.getDrawable(this@MainActivity,R.color.rain) //Add bg
            activityMainBinding.ivWeatherBg.setImageResource(R.color.rain) //Add bg
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.shower) //Change Icon
        }else if(id in 600..620){
            //Snow
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor=resources.getColor(R.color.snow)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.snow))
            activityMainBinding.rlSubLayout.background=ContextCompat.getDrawable(this@MainActivity,R.color.snow) //Add bg

            activityMainBinding.llMainBgBelow.background=ContextCompat.getDrawable(this@MainActivity,R.color.snow) //Add bg
            activityMainBinding.llMainBgAbove.background=ContextCompat.getDrawable(this@MainActivity,R.color.snow) //Add bg
            activityMainBinding.ivWeatherBg.setImageResource(R.color.snow) //Add bg
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.snow1) //Change Icon
        }else if(id in 701..781){
            //Mist
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor=resources.getColor(R.color.atmosphere)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.atmosphere))
            activityMainBinding.rlSubLayout.background=ContextCompat.getDrawable(this@MainActivity,R.color.atmosphere) //Add bg

            activityMainBinding.llMainBgBelow.background=ContextCompat.getDrawable(this@MainActivity,R.color.atmosphere) //Add bg
            activityMainBinding.llMainBgAbove.background=ContextCompat.getDrawable(this@MainActivity,R.color.atmosphere) //Add bg
            activityMainBinding.ivWeatherBg.setImageResource(R.color.atmosphere) //Add bg
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.fog) //Change Icon
        }else if(id==800){
            //Clear or Sunny
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor=resources.getColor(R.color.clear)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.clear))
            activityMainBinding.rlSubLayout.background=ContextCompat.getDrawable(this@MainActivity,R.color.clear) //Add bg

            activityMainBinding.llMainBgBelow.background=ContextCompat.getDrawable(this@MainActivity,R.color.clear) //Add bg
            activityMainBinding.llMainBgAbove.background=ContextCompat.getDrawable(this@MainActivity,R.color.clear) //Add bg
            activityMainBinding.ivWeatherBg.setImageResource(R.color.clear) //Add bg
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.sunny) //Change Icon
        }else{
            //Clouds
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor=resources.getColor(R.color.clouds)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.clouds))
            activityMainBinding.rlSubLayout.background=ContextCompat.getDrawable(this@MainActivity,R.color.clouds) //Add bg

            activityMainBinding.llMainBgBelow.background=ContextCompat.getDrawable(this@MainActivity,R.color.clouds) //Add bg
            activityMainBinding.llMainBgAbove.background=ContextCompat.getDrawable(this@MainActivity,R.color.clouds) //Add bg
            activityMainBinding.ivWeatherBg.setImageResource(R.color.clouds) //Add bg
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.cloudy) //Change Icon
        }

        activityMainBinding.pbLoading.visibility=View.GONE
        activityMainBinding.rlMainLayout.visibility=View.VISIBLE
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun timeStampToLocalDate(timeStamp:Long):String{
        val localTime=timeStamp.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
        }
        return localTime.toString()
    }

    private fun kelvinToCelsius(temp:Double):Double{
        var intTemp=temp
        intTemp=intTemp.minus(273)
        return intTemp.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
    }

    companion object{
        private const val PERMISSION_REQUEST_ACCESS_LOCATION=100
        const val API_KEY=""
    }

    private fun isLocationEnabled():Boolean{
        val locationManager:LocationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)||locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun checkPermissions():Boolean
    {
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)
        {
            return true
        }
        return false
    }

    private fun requestPermission(){
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_ACCESS_LOCATION)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode== PERMISSION_REQUEST_ACCESS_LOCATION)
        {
            if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(applicationContext,"Granted",Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            }
            else
            {
                Toast.makeText(applicationContext,"Denied",Toast.LENGTH_SHORT).show()
            }
        }
    }
}