package com.jiyi.power.ui.mobilepower.fragment

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aleyn.mvvm.base.BaseVMFragment
import com.aleyn.mvvm.extend.flowLaunch
import com.jiyi.power.databinding.MeFragmentBinding
import com.jiyi.power.ui.detail.DetailActivity
import com.jiyi.power.ui.me.MeViewModel
import com.jiyi.power.ui.me.MeWebAdapter

class MeFragment : BaseVMFragment<MeViewModel, MeFragmentBinding>() {

    private val mAdapter by lazy { MeWebAdapter() }

    companion object {
        fun newInstance() = MeFragment()
    }

    override fun initView(savedInstanceState: Bundle?) {
        with(mBinding.rvMeUesdWeb) {
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
        }
        mAdapter.setOnItemClickListener { _, _, position ->
            val intent = Intent().apply {
                setClass(requireContext(), DetailActivity::class.java)
                putExtra("url", (mAdapter.data[position]).link)
            }
            startActivity(intent)
        }


    }

    override fun initObserve() {
        flowLaunch {
            viewModel.popularWeb.collect {
                mAdapter.setList(it)
            }
        }
    }


    override fun lazyLoadData() {
        viewModel.getPopularWeb()
    }
}