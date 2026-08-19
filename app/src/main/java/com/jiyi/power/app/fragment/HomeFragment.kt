package com.jiyi.power.app.fragment

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aleyn.mvvm.base.BaseFragment
import com.aleyn.mvvm.R as BaseR
import com.alibaba.android.arouter.launcher.ARouter
import com.jiyi.power.R
import com.jiyi.power.app.adapter.HomeBannerAdapter
import com.jiyi.power.app.adapter.HomeDeviceAdapter
import com.jiyi.power.app.adapter.HomeDeviceItem
import com.jiyi.power.app.bean.BleDeviceStore
import com.jiyi.power.app.common.RouterPath
import com.jiyi.power.databinding.HomeFragmentBinding
import com.jiyi.power.app.MobilePowerMainActivity
import com.youth.banner.indicator.CircleIndicator

class HomeFragment : BaseFragment<HomeFragmentBinding>() {
    private val deviceAdapter = HomeDeviceAdapter(this::openDevice)

    override fun initView(savedInstanceState: Bundle?) {
        setupBanner()
        setupDeviceList()
        mBinding.buttonAddDevice.setOnClickListener { openDeviceScanner() }
    }

    private fun setupBanner() = with(mBinding.bannerHome) {
        adapter = HomeBannerAdapter(
            listOf(
                R.mipmap.device_home_ximalaya_banner,
                R.mipmap.device_home_ximalaya_banner,
                R.mipmap.device_home_ximalaya_banner
            )
        )
        indicator = CircleIndicator(requireContext())
        setIndicatorNormalColorRes(BaseR.color.color_ffffff)
        setIndicatorSelectedColorRes(R.color.color_004098)
        setIndicatorSpace(10)
        setIndicatorRadius(4)
        setLoopTime(4_000)
        addBannerLifecycleObserver(viewLifecycleOwner)
    }

    private fun setupDeviceList() {
        mBinding.recyclerDevices.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deviceAdapter
            setHasFixedSize(true)
        }
        deviceAdapter.submitList(
            listOf(
                HomeDeviceItem.Device(
                    getString(R.string.home_device_name),
                    getString(R.string.home_device_description),
                    getString(R.string.home_device_power),
                    getString(R.string.home_bluetooth_connected),
                    R.mipmap.ic_bs_devic_logo
                ), HomeDeviceItem.AddDevice
            )
        )
    }

    private fun openDeviceScanner() {
        ARouter.getInstance().build(RouterPath.BLE_SCAN).navigation()
    }

    private fun openDevice(item: HomeDeviceItem.Device?) {
        val savedDevice = BleDeviceStore.getDevices().lastOrNull()
        if (item == null) {
            openDeviceScanner()
            return
        }
        ARouter.getInstance().build(RouterPath.MOBILE_POWER_MAIN)
            .withString(MobilePowerMainActivity.EXTRA_DEVICE_SN, "")
            .navigation()
    }
}
