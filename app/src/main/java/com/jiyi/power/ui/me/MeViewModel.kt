package com.jiyi.power.ui.me

import com.aleyn.mvvm.base.BaseViewModel
import com.aleyn.mvvm.extend.getOrThrow
import com.jiyi.power.network.entity.UsedWeb
import com.jiyi.power.utils.InjectorUtil
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 *   @auther : Aleyn
 *   time   : 2019/11/14
 */
class MeViewModel : BaseViewModel() {

    private val homeRepository by lazy { InjectorUtil.getHomeRepository() }

    private val _popularWeb = MutableSharedFlow<MutableList<UsedWeb>>()
    val popularWeb: SharedFlow<MutableList<UsedWeb>> = _popularWeb


    fun getPopularWeb() {
        launch {
            homeRepository.getPopularWeb().getOrThrow().let {
                _popularWeb.emit(it)
            }
        }
    }
}