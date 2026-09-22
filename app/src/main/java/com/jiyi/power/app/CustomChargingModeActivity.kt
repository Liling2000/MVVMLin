package com.jiyi.power.app

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aleyn.mvvm.base.BaseActivity
import com.blankj.utilcode.util.ToastUtils
import com.jiyi.power.R
import com.jiyi.power.app.adapter.PowerChannelAdapter
import com.jiyi.power.app.viewmodel.CustomChargingModeViewModel
import com.jiyi.power.databinding.ActivityCustomChargingModeBinding
import kotlinx.coroutines.launch

class CustomChargingModeActivity : BaseActivity<ActivityCustomChargingModeBinding>() {
    companion object {
        const val EXTRA_MODE_ID = "extra_custom_mode_id"
    }

    private val viewModel: CustomChargingModeViewModel by viewModels()
    private val channelAdapter =
        PowerChannelAdapter { index, delta -> viewModel.adjustPower(index, delta) }
    private var modeId: Long? = null

    override fun initSystemBars() {
        setSystemBars(statusBarColorRes = R.color.color_f6f7f9)
    }

    override fun initView(savedInstanceState: Bundle?) {
        with(mBinding) {
            modeId = intent.getLongExtra(EXTRA_MODE_ID, -1L).takeIf { it > 0 }
            toolbar.setLeftClickListener { finish() }
            toolbar.setRightIconClickListener { saveMode() }
            recyclerChannels.layoutManager = LinearLayoutManager(this@CustomChargingModeActivity)
            recyclerChannels.adapter = channelAdapter
            viewModel.load(modeId)
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.channels.collect { channels ->
                        channelAdapter.submitList(channels)
                        textTotalPower.text = getString(
                            R.string.custom_mode_total_power_value, channels.sumOf { it.power })
                    }
                }
            }
            modeId?.let {
                CustomChargingModeRepository.findById(it)
                    ?.let { mode -> editModeName.setText(mode.name) }
            }
        }
    }

    override fun initData() = Unit

    private fun saveMode() {
        val name = mBinding.editModeName.text?.toString()?.trim().orEmpty()
        if (name.isBlank()) {
            mBinding.editModeName.error = getString(R.string.custom_mode_name_required)
            return
        }
        CustomChargingModeRepository.save(viewModel.toMode(modeId, name))
        ToastUtils.showShort(R.string.custom_mode_saved)
        finish()
    }
}
