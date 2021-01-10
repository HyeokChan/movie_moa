package kr.ac.kumoh.s20150868.moviemoa_test

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.*
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.PermissionChecker
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.NetworkImageView
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.lang.Exception
import java.net.CookieHandler
import java.net.CookieManager
import java.text.SimpleDateFormat
import java.util.*
private const val PERMISSION_REQUEST = 10
class MainActivity : AppCompatActivity() {

    lateinit var  scheduleFragment : ScheduleFragment
    lateinit var reviewFragment: ReviewFragment
    lateinit var boxOfficeFragment: BoxOfficeFragment

    //거리
    lateinit var locationManager: LocationManager
    private var hasGps = false
    private var locationGps: Location? = null
    private var permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    lateinit var mQueue: RequestQueue
    var mResult: JSONObject? = null


    companion object{
        var my_latitude:Double = 0.0
        var my_longitude:Double = 0.0
        var format_time=""
        var last_timeStr =""
        var reviewTitleArray = ArrayList<String>()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //거리
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){
            if(checkPermission(permissions)){
                getLocation()
            }
            else{
                requestPermissions(permissions, PERMISSION_REQUEST)
            }
        }
        else{
            getLocation()
        }

        scheduleFragment = ScheduleFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.frame_layout, scheduleFragment)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit()

        val bottomNavigation : BottomNavigationView = findViewById(R.id.btm_nav)

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.schedule -> {
                    scheduleFragment = ScheduleFragment()
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.frame_layout, scheduleFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit()
                }
                R.id.review -> {
                    reviewFragment = ReviewFragment()
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.frame_layout, reviewFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit()
                }
                R.id.boxoffice -> {
                    boxOfficeFragment = BoxOfficeFragment()
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.frame_layout, boxOfficeFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit()
                }
            }
            true
        }
        //오늘날짜
        var format1: SimpleDateFormat = SimpleDateFormat ( "yyyy-MM-dd");
        var time:Calendar = Calendar.getInstance();
        format_time = format1.format(time.time)

        var format2: SimpleDateFormat = SimpleDateFormat ( "yyyyMMdd");
        var last_timeInt : Int = format2.format(time.time).toInt() - 1
        last_timeStr  = last_timeInt.toString()


        mQueue = Volley.newRequestQueue(this)
        CookieHandler.setDefault(CookieManager())

        requestTitle()

        Log.i("날짜",last_timeStr)
    }


    private fun requestTitle(){
        val url = "${ReviewActivity.SERVER_URL}/select_Info.php"

        val request = JsonObjectRequest(
            Request.Method.GET,
            url, null,
            Response.Listener { response -> //객체 생성
                mResult = response
                drawList()
            },
            Response.ErrorListener { error -> Toast.makeText(this,error.toString(), Toast.LENGTH_LONG).show() }
        )
        request.tag = ReviewActivity.QUEUE_TAG
        mQueue.add(request)
    }

    private fun drawList() {
        val items = mResult?.getJSONArray("list") ?: return //json 파일의 list 배열을 가지고 와서 items에 넣어라
        reviewTitleArray.clear()
        for (i in 0 until items.length()) {
            var item = items[i] as JSONObject
            var name = item.getString("movie_name")
            reviewTitleArray.add(name)

        }

    }
        @SuppressLint("MissingPermission")
    private fun getLocation(){
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if(hasGps){
            if(hasGps)
            {
                Log.d("CodeAndroidLocation","hasGps")
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,0F,object :LocationListener{
                    override fun onLocationChanged(location: Location?) {
                        if(location!=null){
                            locationGps = location
                            //
                            Log.d("CodeAndroidLocation1","Gps Latitude:"+locationGps!!.latitude)
                            Log.d("CodeAndroidLocation1","Gps longitude:" + locationGps!!.longitude)
                        }
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String?) {}
                    override fun onProviderDisabled(provider: String?) {}
                })
                val localGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if(localGpsLocation!=null)
                {
                    locationGps = localGpsLocation
                }
            }
            if(locationGps!=null ){
                Log.d("CodeAndroidLocation","Gps Latitude:"+locationGps!!.latitude)
                Log.d("CodeAndroidLocation","Gps longitude:" + locationGps!!.longitude)
                my_latitude = locationGps!!.latitude
                my_longitude = locationGps!!.longitude
            }
        }
        else{
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }
    private fun checkPermission(permissionArray: Array<String>):Boolean{
        var allSuccess = true
        for(i in permissionArray.indices){
            if(checkCallingOrSelfPermission(permissionArray[i])== PackageManager.PERMISSION_DENIED){
                allSuccess = false
            }
        }
        return allSuccess
    }

}
