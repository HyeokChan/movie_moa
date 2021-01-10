package kr.ac.kumoh.s20150868.moviemoa_test



import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import kotlinx.android.synthetic.main.fragment_review.view.*
import kr.ac.kumoh.s20150868.moviemoa_test.ScheduleFragment.Companion.titleArray
import org.json.JSONObject
import java.net.CookieHandler
import java.net.CookieManager

/**
 * A simple [Fragment] subclass.
 */
class ReviewFragment : androidx.fragment.app.Fragment() {

    var rAdapter = reviewAdapter()
    companion object { const val KEY_MOVIE = "movie_name" }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        var view = inflater.inflate(R.layout.fragment_review, container, false)

        view.rcReviewMovie.apply{
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = rAdapter
            itemAnimator = DefaultItemAnimator()
        }
        view.rcReviewMovie.addItemDecoration(DividerItemDecoration(context, 1));

        // Inflate the layout for this fragment
        return view
    }

    inner class reviewAdapter () : RecyclerView.Adapter<reviewAdapter.ViewHolder>() {
        inner class ViewHolder: RecyclerView.ViewHolder, View.OnClickListener{
            val txTitle : TextView
            constructor(root: View) : super(root){
                root.setOnClickListener(this)
                txTitle = root.findViewById(R.id.txTitle)
            }
            override fun onClick(p0: View?) {
                val intent = Intent(context, ReviewActivity::class.java)
                intent.putExtra(KEY_MOVIE, MainActivity.reviewTitleArray[adapterPosition])
                startActivity(intent)

            }

        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): reviewAdapter.ViewHolder {
            val root = LayoutInflater.from(context)
                .inflate(R.layout.review_movie_list,parent,false)
            return ViewHolder(root)
        }

        override fun getItemCount(): Int {
            return MainActivity.reviewTitleArray.size
        }

        override fun onBindViewHolder(holder: reviewAdapter.ViewHolder, position: Int) {
            holder.txTitle.text = MainActivity.reviewTitleArray[position]
        }
    }
}