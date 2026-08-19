package com.aleyn.mvvm.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.aleyn.mvvm.R
import com.aleyn.mvvm.bean.NavigateIconBean

/**
 * 重构后的导航栏控件
 * 优化点：
 * 1. 简化 TabParam 为数据类，利用默认参数支持多种构造需求，保持向后兼容。
 * 2. 自动管理 Fragment 切换（add/show/hide），减少 Activity 冗余逻辑。
 * 3. 增强代码可读性，使用现代化 Fragment 实例化方式。
 * 4. 优化红点与选中状态处理逻辑。
 */
class NavigateTabBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), View.OnClickListener {

    private val mViewHolderList = mutableListOf<ViewHolder>()
    private var mTabSelectListener: OnTabSelectedListener? = null
    private var mCurrentTag: String? = null
    private var mRestoreTag: String? = null

    private var mMainContentLayoutId: Int
    private var mSelectedTextColor: ColorStateList? = null
    private var mNormalTextColor: ColorStateList? = null
    private var mTabTextSize: Float = 0f
    private var mDefaultSelectedTab = 0
    private var mCurrentSelectedTab = 0
    private var mIsRoundBg = false

    companion object {
        private const val MAX_COUNT = 99
        private const val P_BUNDLE_DATA_STR = "p_bundle_data_str"
        private const val KEY_CURRENT_TAG = "com.startsmake.template.currentTag"
    }

    init {
        orientation = HORIZONTAL
        val typedArray =
            context.theme.obtainStyledAttributes(attrs, R.styleable.NavigateTabBar, 0, 0)
        mNormalTextColor =
            typedArray.getColorStateList(R.styleable.NavigateTabBar_navigateTabTextColor)
                ?: ContextCompat.getColorStateList(context, R.color.color_a0a0ab)
        val selectedTabTextColor =
            typedArray.getColorStateList(R.styleable.NavigateTabBar_navigateTabSelectedTextColor)
        mTabTextSize =
            typedArray.getDimensionPixelSize(R.styleable.NavigateTabBar_navigateTabTextSize, 0)
                .toFloat()
        mMainContentLayoutId = typedArray.getResourceId(R.styleable.NavigateTabBar_containerId, 0)

        mSelectedTextColor = selectedTabTextColor ?: run {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
            ContextCompat.getColorStateList(context, typedValue.resourceId)
        }
        typedArray.recycle()
    }

    /**
     * 添加 Tab
     * @param fragmentClass Fragment 类
     * @param tabParam Tab 参数
     * @param layout 自定义布局（可选）
     */
    @JvmOverloads
    fun addTab(
        fragmentClass: Class<out Fragment>,
        tabParam: TabParam,
        @LayoutRes layout: Int = R.layout.view_comui_tab
    ) {
        val title = tabParam.title
            ?: if (tabParam.titleStringRes != 0) context.getString(tabParam.titleStringRes) else ""

        val view = LayoutInflater.from(context).inflate(layout, this, false)
        view.isFocusable = true

        val holder = ViewHolder().apply {
            tabIndex = mViewHolderList.size
            this.fragmentClass = fragmentClass
            tag = fragmentClass.name
            pageParam = tabParam
            tabIcon = view.findViewById(R.id.tab_icon)
            tabTitle = view.findViewById(R.id.tab_title)
            tabCount = view.findViewById(R.id.tab_count)
            bundleData = tabParam.bundleData
        }

        holder.tabTitle?.apply {
            text = title
            visibility = if (TextUtils.isEmpty(title) || !tabParam.isShowTit) GONE else VISIBLE
            if (mTabTextSize != 0f) setTextSize(TypedValue.COMPLEX_UNIT_PX, mTabTextSize)
            setTextColor(mNormalTextColor)
        }

        if (tabParam.backgroundColor > 0) {
            view.setBackgroundResource(tabParam.backgroundColor)
        }

        updateTabUi(holder, false)

        view.tag = holder
        view.setOnClickListener(this)
        mViewHolderList.add(holder)

        addView(view, LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f))
    }

    private fun updateTabUi(holder: ViewHolder, isSelected: Boolean) {
        val param = holder.pageParam ?: return
        holder.tabTitle?.setTextColor(if (isSelected) mSelectedTextColor else mNormalTextColor)

        if (param.isBackground) {
            setPressIcon(holder)
        } else {
            val resId = if (isSelected) param.iconSelectedResId else param.iconResId
            if (resId > 0) {
                holder.tabIcon?.setImageResource(resId)
                holder.tabIcon?.visibility = VISIBLE
            } else {
                holder.tabIcon?.visibility = INVISIBLE
            }
        }
        holder.tabIcon?.rootView?.isSelected = isSelected
    }

    override fun onClick(v: View) {
        val holder = v.tag as? ViewHolder ?: return
        // 允许外部监听，并决定是否继续默认的切换逻辑
        mTabSelectListener?.onTabSelected(holder)
//        showFragment(holder)
    }

    /**
     * 显示对应的 Fragment 并更新 UI 状态
     */
    fun showFragment(holder: ViewHolder?) {
        val activity = context as? FragmentActivity ?: return
        holder ?: return

        if (TextUtils.equals(holder.tag, mCurrentTag)) return

        val transaction = activity.supportFragmentManager.beginTransaction()

        // 隐藏当前正在显示的 Fragment
        mCurrentTag?.let { tag ->
            activity.supportFragmentManager.findFragmentByTag(tag)?.let { transaction.hide(it) }
            mViewHolderList.find { it.tag == tag }?.let { updateTabUi(it, false) }
        }

        // 获取或创建目标 Fragment
        var fragment = activity.supportFragmentManager.findFragmentByTag(holder.tag)
        if (fragment == null) {
            fragment = holder.fragmentClass?.getDeclaredConstructor()?.newInstance() as? Fragment
            fragment?.let {
                if (!TextUtils.isEmpty(holder.bundleData)) {
                    it.arguments =
                        Bundle().apply { putString(P_BUNDLE_DATA_STR, holder.bundleData) }
                }
                transaction.add(mMainContentLayoutId, it, holder.tag)
            }
        } else {
            transaction.show(fragment)
        }

        transaction.commitAllowingStateLoss()
        updateTabUi(holder, true)
        mCurrentTag = holder.tag
        mCurrentSelectedTab = holder.tabIndex
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (mMainContentLayoutId == 0 || mViewHolderList.isEmpty()) return

        val defaultHolder = if (!TextUtils.isEmpty(mRestoreTag)) {
            val found = mViewHolderList.find { TextUtils.equals(mRestoreTag, it.tag) }
            mRestoreTag = null
            found ?: mViewHolderList[mDefaultSelectedTab]
        } else {
            mViewHolderList[mDefaultSelectedTab]
        }
        showFragment(defaultHolder)
    }

    /**
     * 设置 Tab 的红点/数字
     */
    fun setCountDot(index: Int, count: Int) {
        mViewHolderList.getOrNull(index)?.tabCount?.apply {
            visibility = if (count > 0) VISIBLE else GONE
            text = if (count > MAX_COUNT) "$MAX_COUNT+" else count.toString()
        }
    }

    fun setTabSelectListener(tabSelectListener: OnTabSelectedListener?) {
        mTabSelectListener = tabSelectListener
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_CURRENT_TAG, mCurrentTag)
    }

    fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        mRestoreTag = savedInstanceState?.getString(KEY_CURRENT_TAG)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setPressIcon(holder: ViewHolder) {
        val param = holder.pageParam ?: return
        val sld = StateListDrawable().apply {
            addState(
                intArrayOf(android.R.attr.state_selected),
                ContextCompat.getDrawable(context, param.iconSelectedResId)
            )
            addState(
                intArrayOf(android.R.attr.state_pressed),
                ContextCompat.getDrawable(context, param.iconSelectedResId)
            )
            addState(intArrayOf(), ContextCompat.getDrawable(context, param.iconResId))
        }
        holder.tabIcon?.setImageDrawable(sld)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
    }

    fun setDefaultSelectedTab(index: Int) {
        if (index in mViewHolderList.indices) {
            mDefaultSelectedTab = index
        }
    }

    var currentSelectedTab: Int
        get() = mCurrentSelectedTab
        set(index) {
            mViewHolderList.getOrNull(index)?.let { showFragment(it) }
        }

    fun cleanAllViewHolder() {
        mViewHolderList.clear()
        removeAllViews()
    }

    class ViewHolder {
        var tag: String? = null
        var pageParam: TabParam? = null
        var tabIcon: ImageView? = null
        var tabTitle: TextView? = null
        var tabCount: TextView? = null
        var fragmentClass: Class<*>? = null
        var tabIndex: Int = 0
        var bundleData: String? = null
    }

    interface OnTabSelectedListener {
        fun onTabSelected(holder: ViewHolder?)
    }

    /**
     * Tab 参数配置类
     */
    data class TabParam(
        @DrawableRes val iconResId: Int = 0,
        @DrawableRes val iconSelectedResId: Int = 0,
        val title: String? = null,
        val isShowTit: Boolean = true,
        val bundleData: String? = null,
        val isBackground: Boolean = false,
        @StringRes val titleStringRes: Int = 0,
        val backgroundColor: Int = 0,
        val iconBean: NavigateIconBean? = null
    ) {
        // 向后兼容的构造函数
        constructor(iconResId: Int, iconSelectedResId: Int, title: String?) : this(
            iconResId, iconSelectedResId, title, true
        )

        constructor(
            iconResId: Int, iconSelectedResId: Int, title: String?, isShowTit: Boolean
        ) : this(iconResId, iconSelectedResId, title, isShowTit, "")

        constructor(
            iconResId: Int,
            iconSelectedResId: Int,
            title: String?,
            isShowTit: Boolean,
            bundleData: String?
        ) : this(iconResId, iconSelectedResId, title, isShowTit, bundleData, false)

        constructor(iconResId: Int, iconSelectedResId: Int, titleStringRes: Int) : this(
            iconResId, iconSelectedResId, null, true, null, false, titleStringRes
        )

        constructor(
            backgroundColor: Int, iconResId: Int, iconSelectedResId: Int, titleStringRes: Int
        ) : this(
            iconResId, iconSelectedResId, null, true, null, false, titleStringRes, backgroundColor
        )

        constructor(
            backgroundColor: Int, iconResId: Int, iconSelectedResId: Int, title: String?
        ) : this(iconResId, iconSelectedResId, title, true, null, false, 0, backgroundColor)

        constructor(
            iconResId: Int,
            iconSelectedResId: Int,
            title: String?,
            isBackground: Boolean,
            iconBean: NavigateIconBean?
        ) : this(iconResId, iconSelectedResId, title, true, null, isBackground, 0, 0, iconBean)
    }
}
