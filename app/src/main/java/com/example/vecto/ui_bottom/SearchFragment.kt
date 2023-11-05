package com.example.vecto.ui_bottom

import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.vecto.LoginActivity
import com.example.vecto.NotificationActivity
import com.example.vecto.R
import com.example.vecto.data.Auth
import com.example.vecto.databinding.FragmentSearchBinding
import com.example.vecto.retrofit.VectoService
import okhttp3.internal.notify
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchFragment : Fragment(){
    private lateinit var binding: FragmentSearchBinding
    private lateinit var mysearchpostAdapter: MysearchpostAdapter

    //private lateinit var userNicknameText: TextView

    private var pageNo = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        //userNicknameText = binding.UserNameText

        mysearchpostAdapter = MysearchpostAdapter(requireContext())
        val searchRecyclerView = binding.SearchRecyclerView
        searchRecyclerView.adapter = mysearchpostAdapter
        searchRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        getPostList()

        /*Auth._nickName.observe(viewLifecycleOwner) { nickname ->
            if (Auth.loginFlag.value!!) {
                userNicknameText.text = Auth._nickName.value
            }
        }*/

        Auth.showFlag.observe(viewLifecycleOwner) { showFlag ->
            if(Auth.showFlag.value == true)//확인 안한 알림이 있을 경우
            {
                binding.AlarmIconImage.setImageResource(R.drawable.alarmon_icon)
            }
            else//확인 안한 알림이 없을 경우
            {
                binding.AlarmIconImage.setImageResource(R.drawable.alarmoff_icon)
            }
        }

        binding.AlarmIconImage.setOnClickListener {
            val intent = Intent(context, NotificationActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }

    private fun getPostList() {
        val vectoService = VectoService.create()

        val call = vectoService.getFeedList(pageNo)
        call.enqueue(object : Callback<VectoService.VectoResponse<List<Int>>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<List<Int>>>, response: Response<VectoService.VectoResponse<List<Int>>>) {
                if(response.isSuccessful){
                    Log.d("POSTID", "성공: ${response.body()}")

                    if(response.body()?.result == null)
                    {
                        //TODO 페이지의 끝
                    }
                    else
                    {
                        for(item in response.body()!!.result!!){
                            getPostInfo(item)
                        }
                    }
                }
                else{
                    Log.d("POSTID", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<List<Int>>>, t: Throwable) {
                Log.d("POSTID", "실패")
            }

        })
    }





    private fun getPostInfo(feedid: Int) {
        val vectoService = VectoService.create()

        val call: Call<VectoService.VectoResponse<VectoService.PostResponse>>

        if(Auth.loginFlag.value == true)
        {
            call = vectoService.getFeedInfo("Bearer ${Auth.token}", feedid)
        }
        else
        {
            call = vectoService.getFeedInfo(feedid)
        }

        call.enqueue(object : Callback<VectoService.VectoResponse<VectoService.PostResponse>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<VectoService.PostResponse>>, response: Response<VectoService.VectoResponse<VectoService.PostResponse>>) {
                if(response.isSuccessful){
                    Log.d("POSTINFO", "성공: ${response.body()}")

                    val result = response.body()!!.result

                    mysearchpostAdapter.feedInfo.add(result!!)
                    mysearchpostAdapter.feedID.add(feedid)
                    Log.d("POSTINFO", "저장된 Post 크기: ${mysearchpostAdapter.feedInfo.size}")

                    mysearchpostAdapter.notifyDataSetChanged()
                }
                else{
                    Log.d("POSTINFO", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<VectoService.PostResponse>>, t: Throwable) {
                Log.d("POSTINFO", "실패")
            }

        })
    }

}