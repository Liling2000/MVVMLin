package com.jiyi.power.app

import com.jiyi.power.app.utils.CmdConstant
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aleyn.mvvm.base.BaseActivity
import com.blankj.utilcode.util.ToastUtils
import com.jiyi.power.R
import com.jiyi.power.app.bean.CustomTimeUiState
import com.jiyi.power.app.bean.TimerSettingType
import com.jiyi.power.app.viewmodel.CustomTimeViewModel
import com.jiyi.power.app.viewmodel.DeviceCommandViewModel
import com.jiyi.power.databinding.ActivityCustomTimeBinding
import com.base.baseus.widget.wheelview.ArrayWheelAdapter
import com.base.baseus.widget.wheelview.OnItemSelectedListener
import com.base.baseus.widget.wheelview.PickWheelView
import kotlinx.coroutines.launch

class CustomTimeActivity : BaseActivity<ActivityCustomTimeBinding>() {
    private val viewModel: CustomTimeViewModel by viewModels()

    override fun initSystemBars() {
        setSystemBars(statusBarColorRes = R.color.color_f6f7f9)
    }

    override fun initView(savedInstanceState: Bundle?) {
        with(mBinding) {
        toolbar.setLeftClickListener { finish() }
            hourPicker.configureWheel(
                CustomTimeViewModel.MIN_HOUR, CustomTimeViewModel.MAX_HOUR
            ) { viewModel.updateHour(it) }
            minutePicker.configureWheel(
                CustomTimeViewModel.MIN_MINUTE, CustomTimeViewModel.MAX_MINUTE
            ) { viewModel.updateMinute(it) }
            buttonAdd15.setOnClickListener { viewModel.addMinutes(15) }
            buttonAdd30.setOnClickListener { viewModel.addMinutes(30) }
            buttonAddHour.setOnClickListener { viewModel.addMinutes(60) }
            buttonReset.setOnClickListener { viewModel.reset() }
            buttonConfirm.setOnClickListener {
                if (!viewModel.confirm()) ToastUtils.showShort(R.string.power_command_failed)
            }
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch { viewModel.uiState.collect(::render) }
                    launch {
                        viewModel.commandEvents.collect { event ->
                            when (event) {
                                is DeviceCommandViewModel.CommandEvent.WriteSucceeded -> if (event.functionCode in setOf(CmdConstant.FunctionCode.CODE_C0, CmdConstant.FunctionCode.CODE_C1)) {
                                    ToastUtils.showShort(R.string.timer_setting_success)
                                    finish()
                                }
                                is DeviceCommandViewModel.CommandEvent.WriteFailed,
                                DeviceCommandViewModel.CommandEvent.Disconnected -> ToastUtils.showShort(R.string.power_command_failed)
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }

    override fun initData() {
        val type = runCatching {
            TimerSettingType.valueOf(
                intent.getStringExtra(EXTRA_TYPE).orEmpty()
            )
        }.getOrDefault(TimerSettingType.SHUTDOWN)
        viewModel.initialize(type, intent.getIntExtra(EXTRA_MINUTES, 0))
    }

    private fun render(state: CustomTimeUiState) = with(mBinding) {
        textHour.text = formatTime(state.hour)
        textMinute.text = formatTime(state.minute)
        hourPicker.setSelectedPosition(state.hour)
        minutePicker.setSelectedPosition(state.minute)
        buttonReset.isEnabled = state.totalMinutes > 0
        buttonReset.alpha = if (buttonReset.isEnabled) 1f else RESET_DISABLED_ALPHA
    }

    private fun PickWheelView.configureWheel(min: Int, max: Int, listener: (Int) -> Unit) {
        setData((min..max).map(::formatTime))
        setOnItemSelectedListener(object : OnItemSelectedListener {
            override fun onItemSelected(
                wheelView: PickWheelView, adapter: ArrayWheelAdapter<*>, position: Int
            ) {
                listener(position + min)
            }
        })
    }

    private fun formatTime(value: Int): String = value.toString().padStart(2, '0')

    companion object {
        private const val EXTRA_TYPE = "custom_time_type"
        private const val EXTRA_MINUTES = "custom_time_minutes"
        private const val RESET_DISABLED_ALPHA = 0.45f

        fun start(context: Context, type: TimerSettingType, minutes: Int) {
            context.startActivity(Intent(context, CustomTimeActivity::class.java).apply {
                putExtra(EXTRA_TYPE, type.name)
                putExtra(EXTRA_MINUTES, minutes)
            })
        }
    }
}
