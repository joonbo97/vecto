package com.vecto_example.vecto.ui_bottom

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.NotificationActivity
import com.vecto_example.vecto.R
import com.vecto_example.vecto.model.data.Auth
import com.vecto_example.vecto.databinding.FragmentSearchBinding
import com.vecto_example.vecto.retrofit.VectoService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchFragment : Fragment(){
    private lateinit var binding: FragmentSearchBinding
    private lateinit var mysearchpostAdapter: MysearchpostAdapter

    var query = ""
    private var pageNo = 0
    private var cnt = 0
    private var pageList = mutableListOf<Int>()
    private var responseData = mutableListOf<VectoService.PostResponse>()
    private var responsePageData = mutableListOf<Int>()

    private var loadingFlag = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        pageNo = 0


        mysearchpostAdapter = MysearchpostAdapter(requireContext())
        val searchRecyclerView = binding.SearchRecyclerView
        searchRecyclerView.adapter = mysearchpostAdapter
        searchRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        searchRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (!recyclerView.canScrollVertically(1)) {
                    if(pageNo != -1 && !loadingFlag)
                    {
                        startLoading(1)
                        pageNo++
                        mysearchpostAdapter.pageNo = pageNo
                        getPostList()
                    }
                }
            }
        })

        startLoading(0)
        getPostList()



        Auth.showFlag.observe(viewLifecycleOwner) { showFlag ->
            if(Auth.showFlag.value == true && Auth.loginFlag.value == true)//확인 안한 알림이 있을 경우
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


        binding.SearchIconImage.setOnClickListener {
            if(binding.editTextID.text.isEmpty())
            {
                Toast.makeText(requireContext(), "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startLoading(0)

            pageNo = 0
            cnt = 0
            mysearchpostAdapter = MysearchpostAdapter(requireContext())
            binding.SearchRecyclerView.adapter = mysearchpostAdapter

            binding.NoneImage.visibility = View.GONE
            binding.NoneText.visibility = View.GONE

            mysearchpostAdapter.feedID.clear()
            mysearchpostAdapter.feedInfo.clear()
            query = binding.editTextID.text.toString()
            mysearchpostAdapter.query = query

            searchRecyclerView.clearOnScrollListeners()
            searchRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    if (!recyclerView.canScrollVertically(1)) {
                        if(pageNo != -1)
                        {
                            pageNo++
                            mysearchpostAdapter.pageNo = pageNo
                            getSearchPostList(query)
                        }
                    }
                }
            })
            getSearchPostList(query)
            Toast.makeText(requireContext(), "${binding.editTextID.text}에 대한 결과입니다.", Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            if(!loadingFlag) {
                startLoading(0)

                pageNo = 0
                cnt = 0
                mysearchpostAdapter = MysearchpostAdapter(requireContext())
                binding.SearchRecyclerView.adapter = mysearchpostAdapter

                binding.NoneImage.visibility = View.GONE
                binding.NoneText.visibility = View.GONE

                mysearchpostAdapter.feedID.clear()
                mysearchpostAdapter.feedInfo.clear()

                if (query.isEmpty()) {
                    getPostList()
                } else {
                    getSearchPostList(query)
                }
                loadingFlag = false
            }
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun getPostList() {
        val vectoService = VectoService.create()

        val call = vectoService.getFeedList(pageNo)
        call.enqueue(object : Callback<VectoService.VectoResponse<List<Int>>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<List<Int>>>, response: Response<VectoService.VectoResponse<List<Int>>>) {
                if(response.isSuccessful){
                    Log.d("POSTID", "성공: ${response.body()}")

                    cnt = 0
                    responseData.clear()
                    responsePageData.clear()

                    if(response.body()?.result?.isEmpty() == true)
                    {
                        pageNo = -1
                        mysearchpostAdapter.pageNo = pageNo
                        endLoading()
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
                    Log.d("POSTID", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "잠시후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    endLoading()
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<List<Int>>>, t: Throwable) {
                Log.d("POSTID", "실패")
                Toast.makeText(requireContext(), getText(R.string.APIFailToastMessage), Toast.LENGTH_SHORT).show()
                endLoading()
            }

        })
    }

    private fun getSearchPostList(q: String) {
        val vectoService = VectoService.create()

        val call = vectoService.getSearchFeedList(pageNo, q)
        call.enqueue(object : Callback<VectoService.VectoResponse<List<Int>>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<List<Int>>>, response: Response<VectoService.VectoResponse<List<Int>>>) {
                if(response.isSuccessful){
                    Log.d("SEARCHPOSTID", "성공: ${response.body()}")

                    cnt = 0
                    responseData.clear()
                    responsePageData.clear()

                    if(response.body()?.result?.isEmpty() == true)
                    {
                        if(pageNo == 0)//검색결과가 없을 경우
                        {
                            binding.SearchRecyclerView.adapter = null
                            binding.NoneImage.visibility = View.VISIBLE
                            binding.NoneText.visibility = View.VISIBLE
                        }

                        pageNo = -1
                        mysearchpostAdapter.pageNo = pageNo
                        endLoading()
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
                    Log.d("SEARCHPOSTID", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "잠시후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    endLoading()
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<List<Int>>>, t: Throwable) {
                Log.d("SEARCHPOSTID", "실패")
                Toast.makeText(requireContext(), getText(R.string.APIFailToastMessage), Toast.LENGTH_SHORT).show()
                endLoading()
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


                    responseData.add(result!!)
                    responsePageData.add(feedid)
                    cnt++

                    Log.d("POSTINFO", "저장된 Post 크기: ${mysearchpostAdapter.feedInfo.size}")

                    if(cnt == pageList.size)//마지막 항목일 경우
                    {
                        var idxcnt = 0

                        while(cnt != 0) {
                            for (i in 0 until pageList.size) {
                                if (pageList[idxcnt] == responsePageData[i]) {
                                    mysearchpostAdapter.feedInfo.add(responseData[i])
                                    mysearchpostAdapter.feedID.add(responsePageData[i])
                                    cnt--
                                    break
                                }
                            }

                            idxcnt++
                        }

                        mysearchpostAdapter.notifyDataSetChanged()
                        endLoading()

                    }
                }
                else{
                    Log.d("POSTINFO", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "잠시후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    endLoading()
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<VectoService.PostResponse>>, t: Throwable) {
                Log.d("POSTINFO", "실패")
                Toast.makeText(requireContext(), getText(R.string.APIFailToastMessage), Toast.LENGTH_SHORT).show()
                endLoading()
            }

        })
    }

    private fun startLoading(type: Int){
        when(type){
            0 -> binding.progressBarCenter.visibility = View.VISIBLE
            1 -> binding.progressBar.visibility = View.VISIBLE
        }
        loadingFlag = true
    }
    private fun endLoading(){
        binding.progressBarCenter.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        loadingFlag = false
    }
}