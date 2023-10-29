package com.example.vecto.ui_bottom

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
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

    private var pageNo = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        mysearchpostAdapter = MysearchpostAdapter(requireContext())
        val searchRecyclerView = binding.SearchRecyclerView
        searchRecyclerView.adapter = mysearchpostAdapter
        searchRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        getPostList()

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

        val call = vectoService.getFeedInfo(feedid)
        call.enqueue(object : Callback<VectoService.VectoResponse<VectoService.PostResponse>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<VectoService.PostResponse>>, response: Response<VectoService.VectoResponse<VectoService.PostResponse>>) {
                if(response.isSuccessful){
                    Log.d("POSTINFO", "성공: ${response.body()}")

                    val result = response.body()!!.result
                    mysearchpostAdapter.feedInfo.add(result!!)
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