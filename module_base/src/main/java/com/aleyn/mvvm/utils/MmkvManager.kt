package com.aleyn.mvvm.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV

/**
 * MMKV 统一管理类，应用启动时先调用 init。
 */
object MmkvManager {

    @PublishedApi
    internal val gson by lazy { Gson() }

    @PublishedApi
    internal val kv: MMKV
        get() = MMKV.defaultMMKV()

    /**
     * 初始化 MMKV。
     */
    fun init(context: Context) {
        MMKV.initialize(context.applicationContext)
    }

    fun putString(key: String, value: String?) {
        kv.encode(key, value)
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return kv.decodeString(key, defaultValue) ?: defaultValue
    }

    fun putBoolean(key: String, value: Boolean) {
        kv.encode(key, value)
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return kv.decodeBool(key, defaultValue)
    }

    fun putInt(key: String, value: Int) {
        kv.encode(key, value)
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return kv.decodeInt(key, defaultValue)
    }

    fun <T> putObject(key: String, value: T?) {
        if (value == null) {
            remove(key)
        } else {
            putString(key, gson.toJson(value))
        }
    }

    inline fun <reified T> getObject(key: String): T? {
        val json = getString(key)
        if (json.isBlank()) return null
        return runCatching { gson.fromJson(json, T::class.java) }.getOrNull()
    }

    inline fun <reified T> putList(key: String, value: List<T>) {
        putString(key, gson.toJson(value))
    }

    inline fun <reified T> getList(key: String): MutableList<T> {
        val json = getString(key)
        if (json.isBlank()) return mutableListOf()
        val type = object : TypeToken<MutableList<T>>() {}.type
        return runCatching {
            gson.fromJson<MutableList<T>>(json, type)
        }.getOrNull() ?: mutableListOf()
    }

    fun remove(key: String) {
        kv.removeValueForKey(key)
    }
}
