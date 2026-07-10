package com.jiyi.power.data

import com.aleyn.cache.CacheMode
import com.aleyn.mvvm.base.BaseModel
import com.jiyi.power.app.base.BaseResult
import com.jiyi.power.data.http.HomeNetWork
import com.jiyi.power.network.entity.BannerBean
import com.jiyi.power.network.entity.HomeListBean
import kotlinx.coroutines.flow.Flow

/**
 *   @author : Aleyn
 *   time   : 2019/10/29
 */
class HomeRepository private constructor(
    private val netWork: HomeNetWork
) : BaseModel() {

    fun getBannerData(refresh: Boolean): Flow<BaseResult<List<BannerBean>>> {
        val cacheModel =
            if (refresh) CacheMode.NETWORK_PUT_CACHE else CacheMode.READ_CACHE_NETWORK_PUT
        return netWork.getBannerData(cacheModel)
    }

    fun getHomeList(page: Int, refresh: Boolean = false): Flow<BaseResult<HomeListBean>> {
        val cacheModel =
            if (refresh) CacheMode.NETWORK_PUT_CACHE else CacheMode.READ_CACHE_NETWORK_PUT
        return netWork.getHomeList(page, cacheModel)
    }

    suspend fun getNaviJson() = netWork.getNaviJson()

    suspend fun getProjectList(page: Int, cid: Int) = netWork.getProjectList(page, cid)

    suspend fun getPopularWeb() = netWork.getPopularWeb()

    companion object {

        @Volatile
        private var INSTANCE: HomeRepository? = null

        fun getInstance(netWork: HomeNetWork) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: HomeRepository(netWork).also { INSTANCE = it }
            }
    }
}