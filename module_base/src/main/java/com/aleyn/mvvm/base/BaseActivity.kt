package com.aleyn.mvvm.base

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.insets.ColorProtection
import androidx.core.view.insets.ProtectionLayout
import androidx.viewbinding.ViewBinding
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.aleyn.mvvm.R
import com.aleyn.mvvm.event.Message
import com.aleyn.mvvm.extend.flowLaunch
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType

/**
 *   @author : Aleyn
 *   time   : 2019/11/01
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    protected lateinit var mBinding: VB

    private var dialog: MaterialDialog? = null
    private lateinit var edgeToEdgeContainer: ProtectionLayout
    private var contentRoot: View? = null
    private var initialContentPadding: Insets = Insets.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 16 always enforces edge-to-edge for apps targeting API 36.
        // Enable the same behavior on older releases so every version follows one layout path.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContentView(createEdgeToEdgeContent(initBinding()))
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
    @Suppress("DEPRECATION")
    protected fun setSystemBars(
        @ColorRes statusBarColorRes: Int = R.color.color_f6f7f9,
        statusBarLightMode: Boolean = true,
        @ColorRes navBarColorRes: Int? = statusBarColorRes
    ) {
        val statusBarColor = ContextCompat.getColor(this, statusBarColorRes)
        val protections = buildList {
            add(ColorProtection(WindowInsetsCompat.Side.TOP, statusBarColor))
            navBarColorRes?.let {
                add(
                    ColorProtection(
                        WindowInsetsCompat.Side.BOTTOM,
                        ContextCompat.getColor(this@BaseActivity, it)
                    )
                )
            }
        }
        edgeToEdgeContainer.setProtections(protections)

        // These colors are used only by pre edge-to-edge platform versions.
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = statusBarLightMode
            isAppearanceLightNavigationBars = true
        }
        applySystemBarInsets()
    }

    /**
     * Splash and other deliberately immersive pages can draw behind the bars. Interactive pages
     * keep controls clear of status bars, navigation bars, display cutouts, and landscape side bars.
     */
    protected open fun shouldApplySystemBarInsets(): Boolean = true

    /**
     * Re-inflates the ViewBinding hierarchy inside the existing window container. This is useful
     * for handled configuration changes (for example, an in-place locale switch) because replacing
     * the Activity window would briefly expose its background and cause a visible flash.
     */
    protected fun rebindContent() {
        val previousContent = contentRoot
        val content = initBinding()
        previousContent?.let(edgeToEdgeContainer::removeView)
        captureContentRoot(content)
        // ProtectionLayout owns additional child views for the system-bar protections. Keep those
        // children intact and insert app content below them; removeAllViews() corrupts its internal
        // ProtectionGroup state and crashes the next setProtections() call.
        edgeToEdgeContainer.addView(content, 0, matchParentLayoutParams())
    }

    private fun createEdgeToEdgeContent(content: View): View {
        captureContentRoot(content)
        return ProtectionLayout(this).also { container ->
            edgeToEdgeContainer = container
            container.addView(content, matchParentLayoutParams())
        }
    }

    private fun captureContentRoot(content: View) {
        contentRoot = content
        initialContentPadding = Insets.of(
            content.paddingLeft,
            content.paddingTop,
            content.paddingRight,
            content.paddingBottom
        )
    }

    private fun matchParentLayoutParams() = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )

    private fun applySystemBarInsets() {
        val root = contentRoot ?: return
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val insets = if (shouldApplySystemBarInsets()) {
                windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
                )
            } else {
                Insets.NONE
            }
            view.setPadding(
                initialContentPadding.left + insets.left,
                initialContentPadding.top + insets.top,
                initialContentPadding.right + insets.right,
                initialContentPadding.bottom + insets.bottom
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(root)
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
