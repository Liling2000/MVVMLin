package com.jiyi.power.ui.mobilepower.activity

import android.os.Bundle
import androidx.activity.viewModels
import com.aleyn.mvvm.widget.ComToolBar
import com.alibaba.android.arouter.facade.annotation.Route
import com.jiyi.power.R
import com.jiyi.power.ui.mobilepower.compat.MobilePowerBaseActivity
import com.jiyi.power.ui.mobilepower.viewmodel.MainFragmentViewModel

@Route(path = "/mobilepower/shut_down_time_set")
class MobilePowerShutDownTimeSetActivity : MobilePowerBaseActivity() {
    private val viewModel: MainFragmentViewModel by viewModels()
    override fun getLayoutId(): Int = R.layout.activity_mobile_power_shut_down_time_set
    override fun onInitView(bundle: Bundle?) { findViewById<ComToolBar?>(R.id.toolbar)?.setTitStr(getString(R.string.str_bs_choose_time)) }

}