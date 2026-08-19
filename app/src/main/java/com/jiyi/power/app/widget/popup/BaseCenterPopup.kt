package com.jiyi.power.app.widget.popup

import android.content.Context
import com.lxj.xpopup.core.CenterPopupView

abstract class BaseCenterPopup(context: Context) : CenterPopupView(context) {
    protected fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
