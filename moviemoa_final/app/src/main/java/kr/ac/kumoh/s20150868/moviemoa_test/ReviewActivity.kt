package kr.ac.kumoh.s20150868.moviemoa_test

import android.graphics.Bitmap
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.collection.LruCache
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.android.synthetic.main.activity_review.*
import org.json.JSONObject
import java.net.CookieHandler
import java.net.CookieManager

class ReviewActivity : AppCompatActivity() {

    val label : Array<String> = arrayOf("감독연출","연기력","스토리","영상미","OST")
    data class ReviewData(var name : String, var pro : Int, var act : Int, var sto : Int, var bea : Int , var ost : Int )
    private lateinit var mLoader : ImageLoader



    var reviewArray = ArrayList<ReviewData>()
    //영화정보
    var genre : String =""
    var dir : String = ""
    var actor : String = ""
    var grade : String = ""
    var url : String = ""

    //리뷰
    var name : String = ""
    var pro : Int = 0
    var act : Int = 0
    var sto : Int = 0
    var bea : Int = 0
    var ost : Int = 0
    companion object {
        const val QUEUE_TAG = "VolleyRequest"
        const val SERVER_URL = "http://rnjsgur12.cafe24.com/OPENSOURCE"
    }

    lateinit var mQueue: RequestQueue

    var mResult: JSONObject? = null
    var mResult2: JSONObject? = null
    var title : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)
        title = intent.getStringExtra(ReviewFragment.KEY_MOVIE)
        txTitle.text = title

        mQueue = Volley.newRequestQueue(this)

        mLoader = ImageLoader(mQueue,
            object : ImageLoader.ImageCache {
                private val cache = LruCache<String, Bitmap>(20)
                override fun getBitmap(url:String): Bitmap?{
                    return cache.get(url)
                }

                override fun putBitmap(url: String, bitmap: Bitmap) {
                    cache.put(url, bitmap)
                }
            })
        CookieHandler.setDefault(CookieManager())
        //mQueue = Volley.newRequestQueue(this)

        requestmvInfo()
        requestReview()

        //poster.setImageUrl("https://movie-phinf.pstatic.net/20191121_221/1574298335357mqgLk_JPEG/movie_image.jpg?type=m203_290_2", mLoader)


    }

    private fun requestmvInfo(){
        val url = "${SERVER_URL}/select_Info.php"

        val request = JsonObjectRequest(
            Request.Method.GET,
            url, null,
            Response.Listener { response -> //객체 생성
                mResult2 = response
                drawList2()
            },
            Response.ErrorListener { error -> Toast.makeText(this,error.toString(), Toast.LENGTH_LONG).show() }
        )
        request.tag = QUEUE_TAG
        mQueue.add(request)
    }
    private fun requestReview() {
        val url = "${SERVER_URL}/select_review.php"

        val request = JsonObjectRequest(
            Request.Method.GET,
            url, null,
            Response.Listener { response -> //객체 생성
                mResult = response
                drawList()
            },
            Response.ErrorListener { error -> Toast.makeText(this,error.toString(), Toast.LENGTH_LONG).show() }
        )
        request.tag = QUEUE_TAG
        mQueue.add(request) //실제 인터넷 연결

    }
    private fun drawList2(){
        val items = mResult2?.getJSONArray("list") ?: return

        for (i in 0 until items.length())
        {
            var item = items[i] as JSONObject

            name = item.getString("movie_name")
            //Log.i("제이슨 파싱", name)
            //Log.i("인텐트 타이틀", title)
            if(name == title)
            {
                genre = item.getString("genre")
                dir = item.getString("director")
                actor = item.getString("actor")
                grade = item.getString("grade")
                url = item.getString("poster")
                txGenre.text = "장르 : " +  genre
                txDir.text = "감독 : " + dir
                txAct.text = "출연 : " + actor
                txGrade.text = "등급 : " + grade
                poster.setImageUrl(url, mLoader)
            }
        }
    }

    private fun drawList() {
        val items = mResult?.getJSONArray("list") ?: return //json 파일의 list 배열을 가지고 와서 items에 넣어라
        reviewArray.clear()

        for (i in 0 until items.length()){
            var item = items[i] as JSONObject

            name = item.getString("name")

            if(name == title) {
                pro = item.getInt("pro")
                if(pro < 0 )
                    pro = 0
                act = item.getInt("act")
                if(act < 0 )
                    act = 0
                sto = item.getInt("sto")
                if(sto < 0 )
                    sto = 0
                bea = item.getInt("bea")
                if(bea < 0 )
                    bea = 0
                ost = item.getInt("ost")
                if(ost < 0 )
                    ost = 0
                break
            }
            else
            {
                name = "리뷰 정보가 없습니다"
                pro = 0
                act = 0
                sto = 0
                bea = 0
                ost = 0
            }

        }
        reviewArray.add(ReviewData(name,pro,act,sto,bea,ost)) // 객체 생성 mArray에 추가시켜준다


//        mAdapter.notifyDataSetChanged() //데이터 셋이 바뀌었으니 다시 그려라
//        hAdapter.notifyDataSetChanged()
        Log.i("review", reviewArray.toString())

        Log.i("감독연출", reviewArray[0].pro.toString())
        Log.i("연기력", reviewArray[0].act.toString())
        Log.i("스토리", reviewArray[0].sto.toString())
        Log.i("영상미", reviewArray[0].bea.toString())
        Log.i("OST", reviewArray[0].ost.toString())

        var dataSet1: RadarDataSet =  RadarDataSet(dataValue1(), "영화 리뷰 분석")
        dataSet1.setColor(Color.RED)

        var data : RadarData = RadarData()

        data.addDataSet(dataSet1)

        var xAxis : XAxis = radar_chart.xAxis
        xAxis.setValueFormatter(IndexAxisValueFormatter(label))

        radar_chart.data = data
        radar_chart.invalidate()
    }

    override fun onStop() {
        super.onStop()
        mQueue.cancelAll(ScheduleFragment.QUEUE_TAG) // 이걸로 되있는거는 onStop에서 모두 캔슬해라
    }


    private fun dataValue1() : ArrayList<RadarEntry> {
        var dataVals : ArrayList<RadarEntry> = ArrayList<RadarEntry>()

        dataVals.add(RadarEntry(reviewArray[0].pro.toFloat()))
        dataVals.add(RadarEntry(reviewArray[0].act.toFloat()))
        dataVals.add(RadarEntry(reviewArray[0].sto.toFloat()))
        dataVals.add(RadarEntry(reviewArray[0].bea.toFloat()))
        dataVals.add(RadarEntry(reviewArray[0].ost.toFloat()))
        return dataVals
    }
}
