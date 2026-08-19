package com.jiyi.power.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.NumberPicker
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aleyn.mvvm.base.BaseActivity
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ToastUtils
import com.jiyi.power.R
import com.jiyi.power.app.adapter.TimerOptionAdapter
import com.jiyi.power.app.bean.TimerOption
import com.jiyi.power.app.bean.TimerSettingType
import com.jiyi.power.app.bean.TimerSettingUiData
import com.jiyi.power.app.viewmodel.TimerSettingViewModel
import com.jiyi.power.databinding.ActivityTimerSettingBinding
import com.jiyi.power.databinding.DialogCustomTimerBinding
import kotlinx.coroutines.launch

class TimerSettingActivity : BaseActivity<ActivityTimerSettingBinding>() {
    private val viewModel by viewModels<TimerSettingViewModel>()
    private val adapter = TimerOptionAdapter(::onOptionClick)

    override fun initView(savedInstanceState: Bundle?) {
        val background = ContextCompat.getColor(this, R.color.color_f6f7f9)
        BarUtils.setStatusBarColor(this, background)
        BarUtils.setStatusBarLightMode(this, true)
        BarUtils.setNavBarColor(this, background)
        mBinding.buttonBack.setOnClickListener { finish() }
        mBinding.timerOptions.layoutManager = GridLayoutManager(this, 2)
        mBinding.timerOptions.adapter = adapter
        mBinding.timerOptions.isNestedScrollingEnabled = false
        mBinding.timerOptions.addItemDecoration(GridSpacingDecoration(resources.getDimensionPixelSize(R.dimen.timer_grid_spacing)))
        mBinding.buttonConfirm.setOnClickListener {
            val sent = viewModel.confirm()
            ToastUtils.showShort(if (sent) R.string.timer_setting_success else R.string.timer_setting_pending)
            if (sent) finish()
        }
        observeState()
    }

    override fun initData() {
        val typeName = intent.getStringExtra(EXTRA_TYPE)
        val type = runCatching { TimerSettingType.valueOf(typeName.orEmpty()) }
            .getOrDefault(TimerSettingType.SHUTDOWN)
        viewModel.initialize(type)
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.uiState.collect(::render) }
        }
    }

    private fun render(state: TimerSettingUiData) = with(mBinding) {
        toolbarTitle.setText(if (state.type == TimerSettingType.SHUTDOWN) R.string.timer_shutdown_title else R.string.timer_reminder_title)
        timerDescription.setText(if (state.type == TimerSettingType.SHUTDOWN) R.string.timer_shutdown_description else R.string.timer_reminder_description)
        adapter.submitList(state.options)
    }

    private fun onOptionClick(option: TimerOption) {
        if (option.isCustom) showCustomTimeDialog(option.time) else viewModel.select(option)
    }

    private fun showCustomTimeDialog(currentMinutes: Int) {
        val dialogBinding = DialogCustomTimerBinding.inflate(LayoutInflater.from(this))
        dialogBinding.hourPicker.configure(0, MAX_HOURS, currentMinutes / 60)
        dialogBinding.minutePicker.configure(0, 59, currentMinutes % 60)
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.timer_custom_dialog_title)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.timer_cancel, null)
            .setPositiveButton(R.string.timer_confirm, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val minutes = dialogBinding.hourPicker.value * 60 + dialogBinding.minutePicker.value
                if (minutes == 0) ToastUtils.showShort(R.string.timer_custom_invalid) else {
                    viewModel.setCustomTime(minutes)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun NumberPicker.configure(min: Int, max: Int, selected: Int) {
        minValue = min
        maxValue = max
        value = selected.coerceIn(min, max)
        wrapSelectorWheel = false
    }

    private class GridSpacingDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: android.graphics.Rect, view: android.view.View, parent: RecyclerView, state: RecyclerView.State) {
            val position = parent.getChildAdapterPosition(view)
            val column = position % 2
            outRect.left = if (column == 0) 0 else spacing / 2
            outRect.right = if (column == 0) spacing / 2 else 0
            outRect.bottom = spacing
        }
    }

    companion object {
        const val EXTRA_TYPE = "timer_setting_type"
        private const val MAX_HOURS = 99

        fun start(context: Context, type: TimerSettingType) {
            context.startActivity(Intent(context, TimerSettingActivity::class.java).putExtra(EXTRA_TYPE, type.name))
        }
    }
}
