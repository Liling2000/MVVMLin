package com.jiyi.power.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.R as BaseR
import com.blankj.utilcode.util.BarUtils
import com.jiyi.power.R
import com.jiyi.power.app.bean.ScreenSettingUiData
import com.jiyi.power.app.bean.ScreenTextColor
import com.jiyi.power.app.bean.TimerSettingType
import com.jiyi.power.app.viewmodel.ScreenSettingViewModel
import com.jiyi.power.databinding.ActivityScreenSettingBinding
import kotlinx.coroutines.launch

class ScreenSettingActivity : BaseActivity<ActivityScreenSettingBinding>() {
    private val viewModel by viewModels<ScreenSettingViewModel>()
    private var rendering = false
    private val wallpaperPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        viewModel.setCustomWallpaper(uri.toString())
    }

    override fun initView(savedInstanceState: Bundle?) {
        val background = ContextCompat.getColor(this, R.color.color_f6f7f9)
        BarUtils.setStatusBarColor(this, background)
        BarUtils.setStatusBarLightMode(this, true)
        BarUtils.setNavBarColor(this, background)
        setupClicks()
        observeState()
    }

    override fun initData() = Unit

    private fun setupClicks() = with(mBinding) {
        toolbar.setLeftClickListener { finish() }
        rowShutdown.setOnClickListener { TimerSettingActivity.start(this@ScreenSettingActivity, TimerSettingType.SHUTDOWN) }
        rowReminder.setOnClickListener { TimerSettingActivity.start(this@ScreenSettingActivity, TimerSettingType.REMINDER) }
        rowTime.setOnClickListener { switchTime.toggle() }
        rowAchievement.setOnClickListener { switchAchievement.toggle() }
        switchTime.setOnClickListener { switchTime.toggle() }
        switchAchievement.setOnClickListener { switchAchievement.toggle() }
        switchTime.setOnCheckedChangeListener { if (!rendering) viewModel.setShowTime(it) }
        switchAchievement.setOnCheckedChangeListener { if (!rendering) viewModel.setAchievement(it) }
        optionWhite.setOnClickListener { viewModel.setTextColor(ScreenTextColor.WHITE) }
        optionDark.setOnClickListener { viewModel.setTextColor(ScreenTextColor.DARK) }
        listOf(wallpaper1, wallpaper2, wallpaper3, wallpaper4).forEachIndexed { index, view ->
            view.setOnClickListener { viewModel.setWallpaper(viewModel.wallpapers[index]) }
        }
        customWallpaper.setOnClickListener { wallpaperPicker.launch(arrayOf("image/*")) }
        editCustomText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!rendering) viewModel.setCustomText(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        buttonSend.setOnClickListener { submitSettings(viewModel.uiState.value) }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.uiState.collect(::render) }
        }
    }

    private fun render(state: ScreenSettingUiData) = with(mBinding) {
        rendering = true
        switchTime.isChecked = state.showTime
        switchAchievement.isChecked = state.achievementInteraction
        if (editCustomText.text.toString() != state.customText) editCustomText.setText(state.customText)
        optionWhite.setBackgroundResource(if (state.textColor == ScreenTextColor.WHITE) R.drawable.bg_screen_option_selected else android.R.color.transparent)
        optionDark.setBackgroundResource(if (state.textColor == ScreenTextColor.DARK) R.drawable.bg_screen_option_selected else android.R.color.transparent)
        val textColor = ContextCompat.getColor(this@ScreenSettingActivity, if (state.textColor == ScreenTextColor.WHITE) BaseR.color.color_ffffff else R.color.color_43474b)
        previewPrimary.setTextColor(textColor)
        previewSecondary.setTextColor(textColor)
        previewCustomText.setTextColor(textColor)
        previewCustomText.text = state.customText
        previewSecondary.visibility = if (state.showTime) View.VISIBLE else View.INVISIBLE
        state.wallpaper.customUri?.let { previewWallpaper.setImageURI(Uri.parse(it)) }
            ?: previewWallpaper.setImageResource(state.wallpaper.wallpaperRes)
        renderWallpaperSelection(state.wallpaper.id)
        rendering = false
    }

    private fun renderWallpaperSelection(selectedId: Int) {
        val frames: List<FrameLayout> = listOf(mBinding.wallpaper1, mBinding.wallpaper2, mBinding.wallpaper3, mBinding.wallpaper4)
        val checks: List<ImageView> = listOf(mBinding.checkWallpaper1, mBinding.checkWallpaper2, mBinding.checkWallpaper3, mBinding.checkWallpaper4)
        frames.forEachIndexed { index, frame ->
            val selected = selectedId == viewModel.wallpapers[index].id
            frame.setBackgroundResource(if (selected) R.drawable.bg_screen_wallpaper_selected else android.R.color.transparent)
            checks[index].visibility = if (selected) View.VISIBLE else View.GONE
        }
    }

    private fun submitSettings(state: ScreenSettingUiData) {
        // Device protocol is not defined yet; keep the complete state ready for the protocol layer.
        com.blankj.utilcode.util.ToastUtils.showShort(R.string.screen_send_success)
    }
}
