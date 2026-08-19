package com.jiyi.power.app.utils

import com.jiyi.power.app.network.HomeRepository
import com.jiyi.power.app.network.HomeNetWork

object InjectorUtil {

    fun getHomeRepository() = HomeRepository.getInstance(HomeNetWork.getInstance())

}