package com.aleyn.mvvm.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.aleyn.mvvm.R
import com.aleyn.mvvm.event.Message
import com.aleyn.mvvm.extend.flowLaunch
import com.blankj.utilcode.util.BarUtils
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType

/**
 *   @author : Aleyn
 *   time   : 2019/11/01
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    protected lateinit var mBinding: VB

    private var dialog: MaterialDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(initBinding())
        initSystemBars()
        initView(savedInstanceState)
        initObserve()
        initData()
    }

    /** 在绑定视图后、initView 前配置系统栏；特殊页面可重写，无需调用 super。 */
    protected open fun initSystemBars() {
        setSystemBars()
    }

    /** 颜色参数为资源 ID；导航栏默认跟随状态栏，传 null 则保留原导航栏颜色。 */
    protected fun setSystemBars(
        @ColorRes statusBarColorRes: Int = R.color.color_f6f7f9,
        statusBarLightMode: Boolean = true,
        @ColorRes navBarColorRes: Int? = statusBarColorRes
    ) {
        BarUtils.setStatusBarColor(this, ContextCompat.getColor(this, statusBarColorRes))
        BarUtils.setStatusBarLightMode(this, statusBarLightMode)
        navBarColorRes?.let {
            BarUtils.setNavBarColor(this, ContextCompat.getColor(this, it))
        }
    }

    abstract fun initView(savedInstanceState: Bundle?)

    open fun initObserve() {}

    abstract fun initData()


    /**
     * 注册 UI 事件
     */
    fun registerDefUIChange(viewModel: BaseViewModel) {
        flowLaunch {
            viewModel.waitDialog.collect {
                if (it.isShow) showLoading(it.tipsText) else dismissLoading()
            }
        }
        flowLaunch {
            viewModel.msgEvent.collect(::handleEvent)
        }
    }

    /**
     * ViewBinding
     */
    @Suppress("UNCHECKED_CAST")
    open fun initBinding(): View {
        var type = javaClass.genericSuperclass
        var superclass = javaClass.superclass
        while (superclass != null) {
            if (type is ParameterizedType) {
                try {
                    type.actualTypeArguments.find {
                        ViewBinding::class.java.isAssignableFrom(it as Class<*>)
                    }?.let {
                        val cls = it as Class<VB>
                        val method = cls.getDeclaredMethod("inflate", LayoutInflater::class.java)
                        mBinding = method.invoke(null, layoutInflater) as VB
                        return mBinding.root
                    }
                } catch (_: NoSuchMethodException) {
                } catch (_: ClassCastException) {
                } catch (e: InvocationTargetException) {
                    throw e.targetException
                }
            }
            type = superclass.genericSuperclass
            superclass = superclass.superclass
        }
        throw IllegalArgumentException("ViewBinding class not found")
    }

    open fun handleEvent(msg: Message) {}

    /**
     * 打开等待框
     */
    protected fun showLoading(tips: String = "") {
        (dialog ?: MaterialDialog(this)
            .cancelable(false)
            .cornerRadius(8f)
            .customView(R.layout.custom_progress_dialog_view, noVerticalPadding = true)
            .lifecycleOwner(this)
            .maxWidth(R.dimen.dialog_width).also {
                dialog = it
            }).apply {
            getCustomView().findViewById<TextView>(R.id.tvTip).text =
                tips.ifBlank { getString(R.string.now_loading) }
            if (!isShowing) show()
        }
    }

    /**
     * 关闭等待框
     */
    protected fun dismissLoading() {
        dialog?.run { if (isShowing) dismiss() }
    }

    override fun onDestroy() {
        dismissLoading()
        dialog = null
        super.onDestroy()
    }

}
