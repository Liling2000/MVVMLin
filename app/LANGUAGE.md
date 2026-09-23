# Android 应用内语言切换方案

本文档记录当前项目的完整实现，也可以作为新项目接入应用内语言切换的操作指南。

## 一、最终方案

语言切换由 `AppCompatDelegate.setApplicationLocales()` 统一完成：

- 普通 Activity、Fragment、XML、RecyclerView、Dialog 等内容由 Android/AppCompat 按标准配置变更流程重建，自动读取新语言资源。
- 不使用 `Resources.updateConfiguration()` 修改全局 Resources。
- 不使用全局语言监听器、事件总线或 `LanguageRefreshDispatcher`。
- 不要求 Home、Me 等业务页面实现 `onLanguageChanged()` 并逐个修改 TextView。
- 不通过 Fragment detach/attach 刷新页面。
- 只让当前显示的语言选择页接管 `locale|layoutDirection`，在原 Window 内重新绑定一次自己的布局，从而消除 Activity 重建时的黑帧。

调用关系如下：

```text
用户选择语言
    ↓
保存语言 tag
    ↓
AppCompatDelegate.setApplicationLocales()
    ↓
LanguageActivity 保留 Window，只重新绑定自身布局
    ↓
后台 MainActivity 按系统流程重建
    ↓
HomeFragment、MeFragment、底部导航等自动读取新语言资源
```

## 二、新项目需要的基础配置

### 1. 添加 AppCompat

项目需要使用较新的 AndroidX AppCompat。本项目使用：

```kotlin
implementation("androidx.appcompat:appcompat:1.7.0")
```

Activity 应继承 `AppCompatActivity`，或者继承一个最终基于 `AppCompatActivity` 的 BaseActivity。

### 2. 配置语言资源目录

建议明确维护默认语言和每一种受支持语言：

```text
res/values/strings.xml       默认资源，当前项目为英文
res/values-en/strings.xml    英文
res/values-zh/strings.xml    中文
```

普通布局中的文案必须引用资源：

```xml
android:text="@string/me_switch_language"
```

动态文案使用当前 Activity 或 Fragment 的 Context：

```kotlin
textView.text = getString(R.string.me_switch_language)
```

不要长期保存旧 Activity 的 Context，也不要使用切换前创建的 Resources 对象读取文案。

本项目在 `build.gradle.kts` 中启用了：

```kotlin
androidResources {
    generateLocaleConfig = true
}
```

Android Gradle Plugin 会根据资源目录生成应用支持的语言配置。当前项目还会在构建时把默认英文资源同步为明确的 `values-en`。新项目如果直接维护完整的 `values-en/strings.xml`，不需要复制这个 Sync 任务。

### 3. 声明 AppCompat 语言存储服务

在 `<application>` 中加入：

```xml
<service
    android:name="androidx.appcompat.app.AppLocalesMetadataHolderService"
    android:enabled="false"
    android:exported="false">
    <meta-data
        android:name="autoStoreLocales"
        android:value="true" />
</service>
```

该配置让 AppCompat 在旧版本 Android 上兼容应用语言存储。当前项目另外用 SharedPreferences 保存业务层的语言 tag，用于实现指定的首次启动规则和语言列表选中状态。

## 三、定义支持的语言

把语言列表集中配置，切换页的 RecyclerView 直接读取该列表：

```kotlin
data class LanguageOption(
    val tag: String,
    @StringRes val label: Int,
)

object AppLanguages {
    val items = listOf(
        LanguageOption("zh", R.string.language_chinese),
        LanguageOption("en", R.string.language_english),
    )
}
```

这里的 tag 应使用标准 BCP 47 语言标签，例如 `zh`、`en`、`ja`、`zh-Hant`。

以后增加语言时，只需要：

1. 新建对应资源目录，例如 `values-ja/strings.xml`。
2. 在默认资源和其他语言资源中增加语言名称。
3. 在 `AppLanguages.items` 中增加 `LanguageOption("ja", ...)`。

Adapter 和切换逻辑不需要再增加语言分支。

## 四、首次启动和持久化规则

当前产品规则为：

1. 第一次安装，没有本地记录时，读取系统首选语言。
2. 系统语言为中文时选择中文。
3. 系统语言不是中文时统一选择英文。
4. 立即保存解析结果。
5. 用户手动切换后覆盖保存结果。
6. 后续启动始终使用保存的语言，不再跟随系统变化。

规则应写成独立的纯函数，便于单元测试：

```kotlin
fun resolveLanguage(
    saved: String?,
    systemLanguage: String,
    supported: List<String>,
): String {
    if (saved in supported) return requireNotNull(saved)
    return if (systemLanguage.equals("zh", ignoreCase = true)) "zh" else "en"
}
```

如果另一个项目希望在“未手动选择语言”时始终跟随系统，可以额外保存一个 `followSystem` 状态；不要把空字符串当作某种语言 tag。

## 五、LanguageManager 的实现

核心原则是先保存状态，再调用 AppCompat 官方接口：

```kotlin
object LanguageManager {
    private const val PREFS = "app_language"
    private const val KEY_TAG = "selected_tag"

    var selectedTag: String? = null
        private set

    @MainThread
    fun initialize(context: Context) {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val savedTag = preferences.getString(KEY_TAG, null)
        val systemLanguage = context.resources.configuration.locales[0]?.language.orEmpty()
        val supported = AppLanguages.items.map { it.tag }
        val tag = resolveLanguage(savedTag, systemLanguage, supported)

        selectedTag = tag
        preferences.edit().putString(KEY_TAG, tag).apply()
        applyLocales(tag)
    }

    @MainThread
    fun select(context: Context, tag: String) {
        require(AppLanguages.items.any { it.tag == tag })
        if (selectedTag == tag) return

        selectedTag = tag
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TAG, tag)
            .apply()
        applyLocales(tag)
    }

    private fun applyLocales(tag: String) {
        val locales = LocaleListCompat.forLanguageTags(tag)
        if (AppCompatDelegate.getApplicationLocales() != locales) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }
}
```

不要在这里执行以下操作：

```kotlin
resources.updateConfiguration(...)
activity.recreate()
fragment.detach()
fragment.attach()
```

`setApplicationLocales()` 自己负责通知和重建 Activity。再次手动调用 `recreate()` 容易造成重复重建、返回栈异常或更明显的闪烁。

## 六、Application 初始化

在自定义 Application 的 `onCreate()` 中尽早初始化，并确保它发生在第一个 Activity 创建之前：

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LanguageManager.initialize(this)

        // 其他 SDK 初始化……
    }
}
```

同时在清单中声明 Application：

```xml
<application
    android:name=".MyApplication"
    ... />
```

不需要覆写 Application 或 BaseActivity 的 `attachBaseContext()`，也不要给 LayoutInflater 替换一个 `createConfigurationContext()` 生成的 Context。View 应继续持有真实 Activity Context，否则自定义控件、FragmentActivity 转换、输入法、Dialog 和 Window 功能都可能异常。

## 七、普通业务页面如何更新

普通 Activity 不声明 locale 相关的 `configChanges`。语言改变后让 AppCompat 正常重建它们：

```xml
<activity
    android:name=".MainActivity"
    android:exported="false" />
```

只要页面遵守下面的规则，就不需要写语言刷新代码：

- XML 文案使用 `@string/...`。
- 动态文案在创建或绑定 View 时调用当前 Context 的 `getString()`。
- RecyclerView 在重建页面后重新创建 Adapter/ViewHolder。
- Dialog、Popup 和自定义 View 使用当前 Activity Context。
- 需要保留的 Tab、滚动位置、筛选条件等通过 `savedInstanceState` 或 ViewModel 保存。

本项目的 MainActivity 会保存当前底部 Tab。重建后 NavigateTabBar 恢复原 Tab，因此从语言页返回时仍然停留在 Me 页面，但页面及 Fragment 都已经使用新语言重新创建。

## 八、无黑闪的关键处理

### 1. 黑闪产生的原因

直接调用 `setApplicationLocales()` 时，位于最上层的语言选择 Activity 默认也会重建。在部分 Android 版本和定制系统上，旧 Window 被移除后，新 Window 的首帧还没有提交，中间几十毫秒会由系统合成成黑色。

仅设置下面这些属性通常不能彻底解决这个 relaunch 间隙：

```xml
android:windowBackground="..."
android:windowAnimationStyle="..."
```

它们仍然建议保留，用于统一启动背景和消除 Activity 转场动画，但真正消除黑帧的关键是让最上层语言页在切换时保留同一个 Window。

### 2. 只让语言页处理 locale 变化

在清单中仅为 LanguageActivity 声明：

```xml
<activity
    android:name=".LanguageActivity"
    android:configChanges="locale|layoutDirection"
    android:exported="false" />
```

不要把这段配置放到所有 Activity 上。否则所有页面都不会自动重建，项目又会退化成每个页面手动刷新文案。

### 3. 在原 Window 中重新绑定语言页

LanguageActivity 收到配置变化后，只同步重新创建自己的 View 层：

```kotlin
override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)

    binding = ActivityLanguageBinding.inflate(layoutInflater)
    setContentView(binding.root)
    bindViews()
}
```

`bindViews()` 应负责重新设置 Toolbar、RecyclerView、Adapter 和点击监听。不要再次手动调用 `onCreate()`，也不要重新创建 Activity。

当前项目的 BaseActivity 通过 `initBinding()` 完成同样的重新绑定：

```kotlin
override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    setContentView(initBinding())
    initSystemBars()
    initView(null)
}
```

这个局部重新绑定只存在于语言选择页，用于保持顶层 Window。MainActivity、HomeFragment、MeFragment 等业务页面仍然走系统标准重建流程。

### 4. 禁用语言页转场

主题中提供零时长动画：

```xml
<style name="AppWindowAnimation" parent="@android:style/Animation.Activity">
    <item name="android:activityOpenEnterAnimation">@anim/anim_noop</item>
    <item name="android:activityOpenExitAnimation">@anim/anim_noop</item>
    <item name="android:activityCloseEnterAnimation">@anim/anim_noop</item>
    <item name="android:activityCloseExitAnimation">@anim/anim_noop</item>
</style>
```

语言页还可以在切换前禁用 Window 转场：

```kotlin
private fun disableWindowTransitions() {
    window.setWindowAnimations(0)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
    }
}
```

Android 13 及以下可以继续配合 `overridePendingTransition(0, 0)`。它虽然已在新 API 中废弃，但用于旧系统兼容仍然有效。

### 5. 启动主题与普通页面主题分离

Android 12+ 的 Splash 属性只放在启动 Activity 使用的主题中：

```xml
<style name="AppTheme" parent="Theme.AppCompat.Light.NoActionBar">
    <item name="android:windowBackground">@color/color_f7f9fb</item>
    <item name="android:windowAnimationStyle">@style/AppWindowAnimation</item>
</style>

<style name="AppStartTheme" parent="AppTheme">
    <item name="android:windowSplashScreenBackground">@color/color_f7f9fb</item>
    <item name="android:windowSplashScreenAnimatedIcon">@drawable/splash_window_icon</item>
</style>
```

```xml
<activity
    android:name=".StartActivity"
    android:theme="@style/AppStartTheme"
    ... />
```

普通 Activity 使用 AppTheme，避免语言切换或普通页面重建时错误套用 Splash 配置。

## 九、切换按钮的标准写法

RecyclerView 点击语言后只调用 LanguageManager：

```kotlin
adapter = LanguageAdapter(LanguageManager.selectedTag.orEmpty()) { item ->
    disableWindowTransitions()
    LanguageManager.select(this, item.tag)
}
```

不需要在回调中：

- 重启 Application 或进程。
- 清空 Activity 栈。
- 跳回首页。
- 发送全局刷新事件。
- 遍历所有 Fragment 修改 TextView。

语言页收到 `onConfigurationChanged()` 后会重新创建 Adapter，并根据 `selectedTag` 自动显示新的选中项。

## 十、常见问题

### 切换后当前页面变了，但返回 Home/Me 还是旧语言

通常是业务 Activity 声明了 `locale|layoutDirection`，导致它没有重建；或者项目使用了全局 Resources 修改方案。只让 LanguageActivity 处理这两个配置变化，其他 Activity 交给 AppCompat 重建。

### 返回后 Tab 回到第一个页面

语言切换会重建 MainActivity。把当前 Tab 保存进 `savedInstanceState`，重建后再恢复。

### XML 已切换，动态列表文案还是旧语言

检查列表数据里是否长期保存了已经格式化完成的旧字符串。页面重建时重新生成显示数据，或者在 ViewHolder 绑定时通过当前 Context 调用 `getString()`。

### 冷启动又变回系统语言

确认：

- 用户选择的 tag 已写入 SharedPreferences。
- Application 中调用了 `LanguageManager.initialize()`。
- 初始化发生在第一个 Activity 创建前。
- 保存的 tag 仍然存在于 `AppLanguages.items`。

### 出现黑闪或短暂看到上一页

确认：

- LanguageActivity 声明了 `locale|layoutDirection`。
- 语言页在 `onConfigurationChanged()` 中只重绑 View，没有调用 `recreate()`。
- 没有先 finish 语言页再切换语言，否则会短暂暴露下面的 Me 页面。
- 没有把 LanguageActivity 设置成透明窗口。
- 启动 Splash 主题没有应用到所有 Activity。

## 十一、新项目接入检查表

- [ ] Activity 基于 AppCompatActivity。
- [ ] 配置 `values`、`values-en`、`values-zh` 等资源目录。
- [ ] XML 文案全部使用 string 资源。
- [ ] 定义统一的 `AppLanguages` 列表。
- [ ] 定义首次启动语言选择规则。
- [ ] LanguageManager 保存 tag 并调用 `setApplicationLocales()`。
- [ ] Application 启动时初始化 LanguageManager。
- [ ] Manifest 配置 AppCompat locale 存储服务。
- [ ] 只有 LanguageActivity 声明 `locale|layoutDirection`。
- [ ] LanguageActivity 在 `onConfigurationChanged()` 中重新绑定自身 View。
- [ ] 普通业务页面不实现手动语言刷新。
- [ ] 需要保留的页面状态支持 Activity 重建恢复。
- [ ] 启动主题与普通 AppTheme 分开。
- [ ] 真机验证中英互切、返回栈、冷启动、旋转和输入框。
- [ ] 用录屏逐帧检查是否出现全黑帧。

## 十二、本项目对应文件

- `app/src/main/java/com/jiyi/power/app/language/LanguageManager.kt`：语言持久化和 AppCompat 调用。
- `app/src/main/java/com/jiyi/power/app/language/LanguagePolicy.kt`：首次启动规则。
- `app/src/main/java/com/jiyi/power/app/language/AppLanguages.kt`：语言配置列表。
- `app/src/main/java/com/jiyi/power/app/LanguageActivity.kt`：无黑闪语言选择页。
- `app/src/main/AndroidManifest.xml`：locale 服务、LanguageActivity configChanges、启动主题。
- `app/src/main/res/values/styles.xml`：普通主题和零转场动画。
- `app/src/main/res/values-v31/styles.xml`：Android 12+ 独立启动主题。
- `app/src/androidTest/java/com/jiyi/power/LanguageUiRegressionTest.kt`：设备回归测试。

当前真机回归结果：语言切换前的旧实现录屏检测到全黑帧；采用“LanguageActivity 保留 Window，业务页面标准重建”后，录屏未检测到全黑帧，多次中英文切换、Home/Me 文案、返回栈和冷启动语言恢复均正常。
