package com.jiyi.power.app.bean

data class LoginBean(
    val id: Long,
    val name: String,
    val avatarUrl: String
) {
    companion object {
        const val LOGIN_INFO_KEY = "login_info"
    }
}
