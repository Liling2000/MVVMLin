package com.jiyi.power.ui.project

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aleyn.mvvm.base.BaseVMFragment
import com.aleyn.mvvm.event.Message
import com.aleyn.mvvm.extend.flowLaunch
import com.google.android.material.tabs.TabLayout
import com.jiyi.power.databinding.ProjectFragmentBinding
import com.jiyi.power.network.entity.ArticlesBean
import com.jiyi.power.ui.detail.DetailActivity
import com.jiyi.power.ui.project.adapter.ProjectAdapter

class ProjectFragment : BaseVMFragment<ProjectViewModel, ProjectFragmentBinding>() {


    companion object {
        fun newInstance() = ProjectFragment()
    }

    private val mAdapter by lazy(LazyThreadSafetyMode.NONE) { ProjectAdapter() }

    override fun initView(savedInstanceState: Bundle?) {
        mBinding.rvProject.apply {
            layoutManager = LinearLayoutManager(requireActivity())
            adapter = mAdapter
        }
        mBinding.tbProject.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.getProjectList(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })
    }

    override fun lazyLoadData() {
        viewModel.getFirstData()
    }


    override fun initObserve() {
        flowLaunch {
            viewModel.navData.collect { list ->
                list.map {
                    mBinding.tbProject.newTab().apply { text = it.name }
                }.forEach {
                    mBinding.tbProject.addTab(it)
                }
            }
        }
        flowLaunch {
            viewModel.items.collect {
                mAdapter.setList(it)
            }
        }
    }

    override fun handleEvent(msg: Message) {
        when (msg.code) {
            0 -> {
                val bean = msg.obj as ArticlesBean
                val intent = Intent().apply {
                    setClass(requireActivity(), DetailActivity::class.java)
                    putExtra("url", bean.link)
                }
                startActivity(intent)
            }
        }
    }
}
