package com.aleyn.mvvm.bean

import android.text.TextUtils

/**
 * 首页的商城图标
 * @param iconType 0 : 图片  1 : gif
 * @param tamperProof: 用于判断iconUrl显示的内容是否不同
 */
data class NavigateIconBean(val selectedIcon : String, val unSelectedIcon : String, val iconType : Long, val isShow : Boolean){

    fun isSelectedIconUrlNotEmpty() : Boolean{
        return !TextUtils.isEmpty(selectedIcon)
    }

    fun isUnSelectedIconUrlNotEmpty() : Boolean{
        return !TextUtils.isEmpty(unSelectedIcon)
    }

    /**
     * icon是不是图片
     */
    fun iconIsPic() : Boolean{
        return iconType == 0L
    }

    /**
     * icon是不是Gif
     */
    fun iconIsGif() : Boolean{
        return iconType == 1L
    }
}