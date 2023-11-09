package com.vecto_example.vecto.ui_bottom

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.databinding.FragmentMypageLikepostBinding
import com.vecto_example.vecto.retrofit.VectoService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MypageLikepostFragment : Fragment() {
    lateinit var binding: FragmentMypageLikepostBinding

    private lateinit var mysearchpostAdapter: MysearchpostAdapter

    private var pageNo = 0
    private var cnt = 0
    private var pageList = mutableListOf<Int>()
    private var responseData = mutableListOf<VectoService.PostResponse>()
    private var responsePageData = mutableListOf<Int>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMypageLikepostBinding.inflate(inflater, container, false)

        mysearchpostAdapter = MysearchpostAdapter(requireContext())
        val searchRecyclerView = binding.LikePostRecyclerView
        searchRecyclerView.adapter = mysearchpostAdapter
        searchRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        Glide.with(this)
            .load(Auth._profileImage.value)
            .circleCrop()
            .placeholder(R.drawable.profile_basic) // 로딩 중 표시될 이미지
            .error(R.drawable.profile_basic) // 에러 발생 시 표시될 이미지
            .into(binding.ProfileImage)


        binding.UserNameText.text = Auth._nickName.value


        getPostList()

        return binding.root
    }

    private fun getPostList() {
        val vectoService = VectoService.create()

        val call = vectoService.getUserLikePost(Auth._userId.value.toString(), pageNo)
        call.enqueue(object : Callback<VectoService.VectoResponse<List<Int>>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<List<Int>>>, response: Response<VectoService.VectoResponse<List<Int>>>) {
                if(response.isSuccessful){
                    Log.d("LIKEPOSTID", "성공: ${response.body()}")

                    cnt = 0
                    responseData.clear()
                    responsePageData.clear()

                    if(response.body()?.result == null)
                    {
                        //TODO 페이지의 끝
                    }
                    else
                    {
                        pageList = response.body()?.result!!.toMutableList()

                        for(item in response.body()!!.result!!){
                            getPostInfo(item)
                        }

                    }
                }
                else{
                    Log.d("LIKEPOSTID", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<List<Int>>>, t: Throwable) {
                Log.d("LIKEPOSTID", "실패")
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
                    Log.d("LIKEPOSTINFO", "성공: ${response.body()}")

                    val result = response.body()!!.result


                    responseData.add(result!!)
                    responsePageData.add(feedid)
                    cnt++

                    Log.d("POSTINFO", "저장된 Post 크기: ${mysearchpostAdapter.feedInfo.size}")

                    if(cnt == pageList.size)//마지막 항목일 경우
                    {

                        var idxcnt = 0
                        Log.d("pageList", pageList.toString())
                        Log.d("responsePageData", responsePageData.toString())
                        Log.d("responseData", responseData.toString())


                        while(cnt != 0) {
                            for (i in 0 until pageList.size) {
                                Log.d("i", i.toString())
                                if (pageList[idxcnt] == responsePageData[i]) {
                                    Log.d("FOR ROOP", "같지 않습니다.")
                                    Log.d("FOR ROOP", "${pageList[idxcnt]} == ${responsePageData[i]}")
                                    mysearchpostAdapter.feedInfo.add(responseData[i])
                                    mysearchpostAdapter.feedID.add(responsePageData[i])
                                    cnt--
                                    break
                                }
                            }

                            idxcnt++
                        }

                        mysearchpostAdapter.notifyDataSetChanged()
                    }
                }
                else{
                    Log.d("LIKEPOSTINFO", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<VectoService.PostResponse>>, t: Throwable) {
                Log.d("LIKEPOSTINFO", "실패")
            }
        })
    }

}