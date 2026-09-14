
package com.base.baseus.widget.wheelview

/**
 * WheelView 滚动停止后的选中监听器
 *
 * @author zyyoona7
 */
interface OnItemSelectedListener {

    fun onItemSelected(wheelView: PickWheelView, adapter: ArrayWheelAdapter<*>, position: Int)
}


/**
 * WheelView 滚动时 position 变化监听器
 *
 * @author zyyoona7
 */
interface OnItemPositionChangedListener {

    fun onItemChanged(wheelView: PickWheelView, oldPosition: Int, newPosition: Int)
}


/**
 * WheelView 滚动变化监听器
 *
 * @author zyyoona7
 */
interface OnScrollChangedListener {

    fun onScrollChanged(wheelView: PickWheelView, scrollOffsetY: Int)

    fun onScrollStateChanged(wheelView: PickWheelView, state: Int)
}