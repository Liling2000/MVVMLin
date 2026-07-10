package com.jiyi.power.utils

import com.jiyi.power.data.HomeRepository
import com.jiyi.power.data.http.HomeNetWork

object InjectorUtil {

    fun getHomeRepository() = HomeRepository.getInstance(HomeNetWork.getInstance())

}