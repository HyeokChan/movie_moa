package kr.ac.kumoh.s20150868.moviemoa_test

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.FloatProperty
import android.util.Log
import android.util.LruCache
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.PermissionChecker.checkCallingOrSelfPermission
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.NetworkImageView
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.custom_calendar.*
import kotlinx.android.synthetic.main.fragment_schedule.*
import kotlinx.android.synthetic.main.fragment_schedule.view.*
import org.json.JSONObject
import java.lang.Exception
import java.net.CookieHandler
import java.net.CookieManager
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

class ScheduleFragment : Fragment() {
    companion object {
        var titleArray = ArrayList<String>()
        const val QUEUE_TAG = "VolleyRequest"
        const val SERVER_URL = "http://rnjsgur12.cafe24.com/OPENSOURCE"
    }
    data class MovieSchedule(var title : String, var position : String, var address : String, var date : String, var mvTime : String , var seat : String,var tube : String,var movie_cor : String)
    var scheduleArray = ArrayList<MovieSchedule>()
    lateinit var mQueue: RequestQueue
    var mResult: JSONObject? = null //nullable이므로 ?를 넣어주자
    var mAdapter = ScheduleAdapter()
    var hAdapter = TitleAdpater()
    lateinit var mImageLoader: ImageLoader

    //거리계산
    var my_location : Location = Location("my_location")
    var movie_location : Location = Location("movie_location")

    //영화 선택
    var selected_movie:String=""
    var selected_boolean:Boolean=false

    //시간 설정
    lateinit var calendarDl: Dialog

    //날짜 선택
    var selected_date:Boolean = false
    var selected_date_finish:String=""

    //로딩중
    lateinit var progressDialog: ProgressDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        var view : View = inflater.inflate(R.layout.fragment_schedule, container, false)
        CookieHandler.setDefault(CookieManager())
        mQueue = Volley.newRequestQueue(context)
        my_location.latitude = MainActivity.my_latitude
        my_location.longitude = MainActivity.my_longitude
        view.rcTitle.apply{
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = hAdapter
            itemAnimator = DefaultItemAnimator()
        }
        view.rcSchedule.apply{
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
            itemAnimator = DefaultItemAnimator()
        }
        view.rcSchedule.addItemDecoration(DividerItemDecoration(context, 1));
        mImageLoader = ImageLoader(mQueue, object : ImageLoader.ImageCache{
            private  val cache = LruCache<String, Bitmap>(20)
            override fun getBitmap(url: String): Bitmap? {
                return cache.get(url)
            }
            override fun putBitmap(url: String?, bitmap: Bitmap?) {
                cache.put(url,bitmap)
            }
        })
        view.selectDateBt.setOnClickListener {
            calendarDl = Dialog(it.getContext());
            calendarDl.requestWindowFeature(Window.FEATURE_NO_TITLE);
            calendarDl.setContentView(R.layout.custom_calendar);

            calendarDl.calVw.setOnDateChangeListener { calendarView, year, month, dayOfMonth ->
                selected_date = true
                if(dayOfMonth < 10){
                    selected_date_finish =year.toString()+"-"+(month+1)+"-0"+dayOfMonth
                }
                else{
                    selected_date_finish =year.toString()+"-"+(month+1)+"-"+dayOfMonth
                }
                Log.i("selected_date_finish",selected_date_finish)
                loading()
                requestMovie()
            }
            calendarDl.show()

        }
        loading()
        requestMovie()
        return view
    }
    override fun onStop() {
        super.onStop()
        mQueue.cancelAll(QUEUE_TAG) // 이걸로 되있는거는 onStop에서 모두 캔슬해라
    }

    fun loading() {
        //로딩
        android.os.Handler().postDelayed(
            {
                progressDialog = ProgressDialog(context)
                progressDialog.isIndeterminate = true
                progressDialog.setMessage("잠시만 기다려 주세요")
                progressDialog.show()
            }, 0
        )
    }
    fun loadingEnd() {
        android.os.Handler().postDelayed(
            { progressDialog.dismiss() }, 0
        )
    }

    private fun requestMovie() {
        val url = "$SERVER_URL/select_movies.php"
        val request = JsonObjectRequest(
            Request.Method.GET,
            url, null,
            Response.Listener { response -> //객체 생성
                mResult = response
                drawList()
            },
            Response.ErrorListener { error -> Toast.makeText(context,error.toString(), Toast.LENGTH_LONG).show() }
        )
        request.tag = QUEUE_TAG
        mQueue.add(request) //실제 인터넷 연결
    }
    private fun drawList() {

        val items = mResult?.getJSONArray("list") ?: return //json 파일의 list 배열을 가지고 와서 items에 넣어라
        scheduleArray.clear() //초기화
        titleArray.clear()
        for (i in 0 until items.length()){
            var item = items[i] as JSONObject
            var movie_name = item.getString("movie_name")
            if(!titleArray.contains(movie_name)){
                titleArray.add(movie_name)
            }
            if(movie_name==selected_movie || selected_boolean==false)
            {
                var latitude = item.getString("latitude").toDouble()
                var longitude = item.getString("longitude").toDouble()
                movie_location.latitude = latitude
                movie_location.longitude = longitude

                var distance:Float = my_location.distanceTo(movie_location)
                var date = item.getString("date")
                if((date==MainActivity.format_time && selected_date==false) || (selected_date==true&&date==selected_date_finish)){
                    if(distance<20000)
                    {
                        var address = item.getString("address")
                        var position_name = item.getString("position_name")
                        var time = item.getString("time")
                        var seat = item.getString("seat")
                        var tube = item.getString("tube")
                        var movie_cor = item.getString("movie_cor")
                        scheduleArray.add(MovieSchedule(movie_name,position_name,address,date,time,seat,tube,movie_cor)) // 객체 생성 mArray에 추가시켜준다
                    }
                }
            }
        }
        Log.i("sche??",scheduleArray.size.toString())
        if(scheduleArray.size==0){
            scheduleArray.add(MovieSchedule("상영하는 영화관이 없습니다","","","","","","","not.png")) // 객체 생성 mArray에 추가시켜준다
        }
        loadingEnd()

        mAdapter.notifyDataSetChanged() //데이터 셋이 바뀌었으니 다시 그려라
        hAdapter.notifyDataSetChanged()
    }
    inner class ScheduleAdapter() : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>(){
        inner class ViewHolder : RecyclerView.ViewHolder{
            val txTitle : TextView
            val txCinema : TextView
            val txTube : TextView
            val txDate : TextView
            val txTime : TextView
            val txseat : TextView
            val image: NetworkImageView
            constructor(root: View) : super(root){
                txTitle = root.findViewById(R.id.txTitle)
                txCinema = root.findViewById(R.id.txCinema)
                txTube = root.findViewById(R.id.txTube)
                txDate = root.findViewById(R.id.txDate)
                txTime = root.findViewById(R.id.txTime)
                txseat = root.findViewById(R.id.seat)
                image = root.findViewById(R.id.image)
            }

        }
        override fun getItemCount(): Int {
            return scheduleArray.size
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleAdapter.ViewHolder {
            val root = LayoutInflater.from(context)
                .inflate(R.layout.movie_schedule,parent,false)
            return ViewHolder(root)
        }
        override fun onBindViewHolder(holder: ScheduleAdapter.ViewHolder, position: Int) {
            holder.txTitle.text = scheduleArray[position]?.title
            holder.txCinema.text = scheduleArray[position]?.position
            holder.txTube.text = scheduleArray[position]?.tube
            holder.txDate.text = scheduleArray[position]?.date
            holder.txTime.text = scheduleArray[position]?.mvTime
            holder.txseat.text = scheduleArray[position]?.seat
            holder.image.setImageUrl(SERVER_URL+"/"+scheduleArray[position].movie_cor, mImageLoader)
            if(scheduleArray[position].movie_cor=="cgv_image.jpg"){
                holder.itemView.setOnClickListener {
                    var uri:Uri = Uri.parse("http://www.cgv.co.kr")
                    var intent= Intent(Intent.ACTION_VIEW,uri)
                    startActivity(intent)
                }
            }
            else if(scheduleArray[position].movie_cor=="lotte_image.jpg"){
                holder.itemView.setOnClickListener {
                    var uri:Uri = Uri.parse("http://www.lottecinema.co.kr")
                    var intent= Intent(Intent.ACTION_VIEW,uri)
                    startActivity(intent)
                }
            }
            else if(scheduleArray[position].movie_cor=="mega_image.png"){
                holder.itemView.setOnClickListener {
                    var uri:Uri = Uri.parse("http://www.megabox.co.kr")
                    var intent= Intent(Intent.ACTION_VIEW,uri)
                    startActivity(intent)
                }
            }
        }
    }
    inner class TitleAdpater () : RecyclerView.Adapter<TitleAdpater.ViewHolder>() {
        inner class ViewHolder(root: View): RecyclerView.ViewHolder(root){
            val txTitle : TextView = root.findViewById(R.id.txTitle2)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TitleAdpater.ViewHolder {
            val root = LayoutInflater.from(context)
                .inflate(R.layout.horizon_movie_title,parent,false)
            return ViewHolder(root)
        }
        override fun getItemCount(): Int {
            return titleArray.size
        }
        override fun onBindViewHolder(holder: TitleAdpater.ViewHolder, position: Int) {
            holder.txTitle.text = titleArray[position]
            holder.txTitle.setOnClickListener {
                selected_movie = holder.txTitle.text.toString()
                selected_boolean=true
                Log.i("tlqkftlqkf",selected_movie)
                loading()
                requestMovie()
            }
        }
    }
}
