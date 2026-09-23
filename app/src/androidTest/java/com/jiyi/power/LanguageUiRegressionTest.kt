package com.jiyi.power

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.aleyn.mvvm.widget.NavigateTabBar
import com.jiyi.power.app.LanguageActivity
import com.jiyi.power.app.LoginActivity
import com.jiyi.power.app.MainActivity
import com.jiyi.power.app.language.LanguageManager
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream

@RunWith(AndroidJUnit4::class)
class LanguageUiRegressionTest {
    @Test
    fun loginInputRetainsActivityContextAndAcceptsTyping() {
        launch(LoginActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val input = activity.findViewById<EditText>(R.id.edit_name)
                assertSame(activity, input.context)
                input.requestFocus()
                input.setText("LocaleTest")
                assertEquals("LocaleTest", input.text.toString())
            }
        }
    }

    @Test
    fun localeSwitchKeepsLanguageWindowAndRecreatesMainContent() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val originalTag = requireNotNull(LanguageManager.selectedTag)
        try {
            launch(MainActivity::class.java).use { scenario ->
                lateinit var initialMain: MainActivity
                scenario.onActivity { activity ->
                    initialMain = activity
                    assertSame(
                        activity,
                        activity.findViewById<NavigateTabBar>(R.id.bottom_navigation).context,
                    )
                    activity.findViewById<NavigateTabBar>(R.id.bottom_navigation)
                        .currentSelectedTab = 1
                }

                for (tag in listOf("zh", "en", "zh", "en")) {
                    val beforeSwitch = resumedActivity(MainActivity::class.java)
                    instrumentation.runOnMainSync {
                        beforeSwitch.findViewById<android.view.View>(R.id.item_language).performClick()
                    }
                    val languageActivity = resumedActivity(LanguageActivity::class.java)
                    val languageLabel = when {
                        tag == "en" -> "English"
                        LanguageManager.selectedTag == "en" -> "Simplified Chinese"
                        else -> "简体中文"
                    }
                    onView(withText(languageLabel)).perform(click())

                    assertSame(languageActivity, resumedActivity(LanguageActivity::class.java))
                    pressBack()

                    val currentMain = resumedActivity(MainActivity::class.java)
                    assertNotSame(initialMain, currentMain)
                    instrumentation.runOnMainSync {
                        assertEquals(
                            if (tag == "en") "User agreement" else "用户协议",
                            currentMain.findViewById<android.widget.TextView>(R.id.text_user_agreement).text.toString(),
                        )
                        currentMain.findViewById<NavigateTabBar>(R.id.bottom_navigation)
                            .currentSelectedTab = 0
                    }
                    instrumentation.waitForIdleSync()
                    instrumentation.runOnMainSync {
                        assertEquals(
                            if (tag == "en") "Primary devices" else "主要设备",
                            currentMain.findViewById<android.widget.TextView>(R.id.text_primary_devices).text.toString(),
                        )
                        assertEquals(
                            if (tag == "en") "Add new device" else "添加新设备",
                            currentMain.findViewById<android.widget.TextView>(R.id.text_add_device).text.toString(),
                        )
                        currentMain.findViewById<NavigateTabBar>(R.id.bottom_navigation)
                            .currentSelectedTab = 1
                    }
                }
            }

            launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    assertEquals(
                        "Primary devices",
                        activity.findViewById<android.widget.TextView>(R.id.text_primary_devices).text.toString(),
                    )
                }
            }
        } finally {
            instrumentation.runOnMainSync {
                LanguageManager.select(instrumentation.targetContext, originalTag)
            }
        }
    }

    private fun <A : Activity> resumedActivity(type: Class<A>): A {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.uptimeMillis() + 5_000
        var result: A? = null
        while (result == null && SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                result = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .firstOrNull { type.isInstance(it) }
                    ?.let(type::cast)
            }
            if (result == null) SystemClock.sleep(50)
        }
        return requireNotNull(result) { "${type.simpleName} did not reach RESUMED" }
    }

    private fun <A : Activity> launch(type: Class<A>): ActivityHandle<A> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand(
            "am start -n com.jiyi.power/.app.StartActivity",
        ).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        }
        val deadline = SystemClock.uptimeMillis() + 10_000
        var foreground = false
        while (!foreground && SystemClock.uptimeMillis() < deadline) {
            instrumentation.runOnMainSync {
                foreground = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .any { it is MainActivity || it is LoginActivity }
            }
            if (!foreground) SystemClock.sleep(100)
        }
        assertTrue("App launcher did not reach a foreground screen", foreground)
        val intent = Intent(instrumentation.targetContext, type)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = type.cast(instrumentation.startActivitySync(intent))!!
        instrumentation.waitForIdleSync()
        return ActivityHandle(activity)
    }

    private class ActivityHandle<A : Activity>(private val activity: A) : AutoCloseable {
        fun onActivity(action: (A) -> Unit) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync { action(activity) }
            instrumentation.waitForIdleSync()
        }

        override fun close() = onActivity { it.finish() }
    }
}
