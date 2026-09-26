package com.jiyi.power.app

import com.jiyi.power.app.utils.CmdConstant
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aleyn.mvvm.base.BaseActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.aleyn.mvvm.R as BaseR
import com.jiyi.power.R
import com.jiyi.power.app.bean.ScreenSettingUiData
import com.jiyi.power.app.bean.ScreenTextColor
import com.jiyi.power.app.bean.TimerSettingType
import com.jiyi.power.app.adapter.ScreenWallpaperAdapter
import com.jiyi.power.app.common.RouterPath
import com.jiyi.power.app.viewmodel.ScreenSettingViewModel
import com.jiyi.power.app.viewmodel.DeviceCommandViewModel
import com.jiyi.power.databinding.ActivityScreenSettingBinding
import kotlinx.coroutines.launch

@Route(path = RouterPath.PAGE_ROUTE_THEME)
class ScreenSettingActivity : BaseActivity<ActivityScreenSettingBinding>() {
    private val viewModel by viewModels<ScreenSettingViewModel>()
    private val deviceSn by lazy {
        intent.getStringExtra(MobilePowerMainActivity.EXTRA_DEVICE_SN)
    }
    // 不在 Activity 构造阶段解引用 by viewModels()；点击发生时页面已完成挂载。
    private val wallpaperAdapter = ScreenWallpaperAdapter { viewModel.setWallpaper(it) }
    private var rendering = false
    private var submitPending = false
    private val wallpaperPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@registerForActivityResult
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.setCustomWallpaper(uri.toString())
        }

    override fun initView(savedInstanceState: Bundle?) {
        mBinding.recyclerWallpapers.apply {
            layoutManager = GridLayoutManager(this@ScreenSettingActivity, 2)
            adapter = wallpaperAdapter
            isNestedScrollingEnabled = false
            addItemDecoration(WallpaperGridDecoration(resources.getDimensionPixelSize(R.dimen.dp16)))
        }
        setupClicks()
        observeState()
    }

    override fun initData() {
        viewModel.bindDevice(deviceSn)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshReminder()
        viewModel.refreshCustomText()
    }

    private fun setupClicks() = with(mBinding) {
        toolbar.setLeftClickListener { finish() }

        rowReminder.setOnClickListener {
            TimerSettingActivity.start(
                this@ScreenSettingActivity, TimerSettingType.REMINDER, deviceSn
            )
        }
        rowTime.setOnClickListener { switchTime.toggle() }
        rowAchievement.setOnClickListener { switchAchievement.toggle() }
        switchTime.setOnClickListener { switchTime.toggle() }
        switchAchievement.setOnClickListener { switchAchievement.toggle() }
        switchTime.setOnCheckedChangeListener { if (!rendering) viewModel.setShowTime(it) }
        switchAchievement.setOnCheckedChangeListener { if (!rendering) viewModel.setAchievement(it) }
        optionWhite.setOnClickListener { viewModel.setTextColor(ScreenTextColor.WHITE) }
        optionDark.setOnClickListener { viewModel.setTextColor(ScreenTextColor.DARK) }
        customWallpaper.setOnClickListener { wallpaperPicker.launch(arrayOf("image/*")) }
        editCustomText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!rendering) viewModel.setCustomText(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
        buttonSend.setOnClickListener { submitSettings() }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch {
                    viewModel.commandEvents.collect { event ->
                        when (event) {
                            is DeviceCommandViewModel.CommandEvent.WriteSucceeded -> if (
                                submitPending && event.functionCode == CmdConstant.FunctionCode.CODE_C2
                            ) {
                                submitPending = false
                                dismissLoading()
                                com.blankj.utilcode.util.ToastUtils.showShort(R.string.screen_send_success)
                            }
                            is DeviceCommandViewModel.CommandEvent.WriteFailed -> if (submitPending) {
                                submitPending = false
                                dismissLoading()
                                com.blankj.utilcode.util.ToastUtils.showShort(R.string.power_command_failed)
                            }
                            DeviceCommandViewModel.CommandEvent.Disconnected -> if (submitPending) {
                                submitPending = false
                                dismissLoading()
                                com.blankj.utilcode.util.ToastUtils.showShort(R.string.power_command_failed)
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun render(state: ScreenSettingUiData) = with(mBinding) {
        rendering = true
        textReminderTime.text = formatReminderTime(state.reminderMinutes)
        switchTime.isChecked = state.showTime
        switchAchievement.isChecked = state.achievementInteraction
        if (editCustomText.text.toString() != state.customText) editCustomText.setText(state.customText)
        optionWhite.setBackgroundResource(if (state.textColor == ScreenTextColor.WHITE) R.drawable.bg_screen_option_selected else android.R.color.transparent)
        optionDark.setBackgroundResource(if (state.textColor == ScreenTextColor.DARK) R.drawable.bg_screen_option_selected else android.R.color.transparent)
        val textColor = ContextCompat.getColor(
            this@ScreenSettingActivity,
            if (state.textColor == ScreenTextColor.WHITE) BaseR.color.color_ffffff else R.color.color_43474b
        )

        state.wallpaper.customUri?.let { previewWallpaper.setImageURI(Uri.parse(it)) }
            ?: previewWallpaper.setImageResource(state.wallpaper.wallpaperRes)
        wallpaperAdapter.submit(viewModel.wallpapers, state.wallpaper.id)
        rendering = false
    }

    private fun formatReminderTime(minutes: Int?): String =
        minutes?.takeIf { it > 0 }?.let { "%02d:%02d".format(it / 60, it % 60) }.orEmpty()

    private fun submitSettings() {
        ScreenWallpaperRepository.select(viewModel.uiState.value.wallpaper)
        val sent = viewModel.submit()
        submitPending = sent
        if (sent) {
            showLoading(getString(R.string.setting_in_progress))
        } else {
            com.blankj.utilcode.util.ToastUtils.showShort(R.string.power_command_failed)
        }
    }

    /** 两列壁纸的间距交由 RecyclerView 处理，item 本身只负责图片与选中图标。 */
    private class WallpaperGridDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: android.graphics.Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State,
        ) {
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION) return
            outRect.left = if (position % 2 == 0) 0 else spacing / 2
            outRect.right = if (position % 2 == 0) spacing / 2 else 0
            if (position >= 2) outRect.top = spacing
        }
    }
}
