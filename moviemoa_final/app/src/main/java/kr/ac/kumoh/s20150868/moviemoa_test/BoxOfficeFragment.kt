package kr.ac.kumoh.s20150868.moviemoa_test


import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.fragment_box_office.view.*
import org.json.JSONObject
import java.net.CookieHandler
import java.net.CookieManager
import java.time.format.DateTimeFormatter

/**
 * A simple [Fragment] subclass.
 */
class BoxOfficeFragment : Fragment() {
    var bAdapter = boxAdapter()
    data class BoxInfo(var rank : String, var Title : String, var audience : String)
    var bArray = ArrayList<BoxInfo>()

    lateinit var mQueue: RequestQueue

    var mResult: JSONObject? = null //nullable이므로 ?를 넣어주자
    companion object{
        const val QUEUE_TAG = "VolleyRequest"
        const val SERVER_URL = "http://192.168.217.226/"
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        var view = inflater.inflate(R.layout.fragment_box_office, container, false)

        view.rcBoxoffice.apply{
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = bAdapter
            itemAnimator = DefaultItemAnimator()
        }
        view.rcBoxoffice.addItemDecoration(DividerItemDecoration(context, 1))

        CookieHandler.setDefault(CookieManager())
        mQueue = Volley.newRequestQueue(context)

        requestbox()

        // Inflate the layout for this fragment
        return view

    }

    override fun onStop() {
        super.onStop()
        mQueue.cancelAll(ScheduleFragment.QUEUE_TAG) // 이걸로 되있는거는 onStop에서 모두 캔슬해라
    }
    private fun requestbox() {


        val url = "http://www.kobis.or.kr/kobisopenapi/webservice/rest/boxoffice/searchDailyBoxOfficeList.json?key=b9d5714d219a4106d69418a06f993918&targetDt=" + MainActivity.last_timeStr

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
        val boxOfficeResult = mResult?.getJSONObject("boxOfficeResult")
        val dailyBoxOfficeList = boxOfficeResult?.getJSONArray("dailyBoxOfficeList")
        //Log.i("제이슨 파싱", dailyBoxOfficeList.toString())
        bArray.clear()
        for (i in 0 until dailyBoxOfficeList!!.length()) {
            val item = dailyBoxOfficeList[i] as JSONObject
            val rank = item.getString("rank")
            val movieNm = item.getString("movieNm")
            val audiAcc = item.getString("audiAcc")
            bArray.add(BoxInfo(rank,movieNm,audiAcc))// 객체 생성 mArray에 추가시켜준다
        }
        Log.i("제이슨 파싱", bArray.toString())
        bAdapter.notifyDataSetChanged() //데이터 셋이 바뀌었으니 다시 그려라
    }
    inner class boxAdapter() : RecyclerView.Adapter<boxAdapter.ViewHolder>(){
        inner class ViewHolder(root:View):RecyclerView.ViewHolder(root){
            val txRank : TextView = root.findViewById(R.id.rank)
            val txTitle : TextView = root.findViewById(R.id.mvTitle)
            val txAudience : TextView = root.findViewById(R.id.audience)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): boxAdapter.ViewHolder {
            val root = LayoutInflater.from(context)
                .inflate(R.layout.boxoffice_ltem, parent, false)
            return ViewHolder(root)
        }

        override fun getItemCount(): Int {
            return bArray.size
        }

        override fun onBindViewHolder(holder: boxAdapter.ViewHolder, position: Int) {
            holder.txRank.text = bArray[position]?.rank
            holder.txTitle.text = bArray[position]?.Title
            holder.txAudience.text= "누적 관객수 : " +bArray[position]?.audience + "명"
        }

    }

}
