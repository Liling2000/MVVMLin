package com.jiyi.power.ui.mobilepower.activity

import android.os.Bundle
import com.aleyn.mvvm.widget.ComToolBar
import com.alibaba.android.arouter.facade.annotation.Route
import com.jiyi.power.R
import com.jiyi.power.ui.mobilepower.compat.MobilePowerBaseActivity

@Route(path = "/mobilepower/version", name = "移动电源设备版本页")
class MobilePowerVersionActivity : MobilePowerBaseActivity() {
    override fun getLayoutId(): Int = R.layout.activity_mobile_power_version
    override fun onInitView(bundle: Bundle?) { findViewById<ComToolBar?>(R.id.toolbar)?.setTitStr(getString(R.string.str_version)) }

}