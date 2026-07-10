package com.jiyi.power.utils

import coil.load
import com.jiyi.power.network.entity.BannerBean
import com.youth.banner.adapter.BannerImageAdapter
import com.youth.banner.holder.BannerImageHolder

/**
 *   @auther : Aleyn
 *   time   : 2019/09/05
 */
class GlideImageLoader : BannerImageAdapter<BannerBean>(null) {

    override fun onBindView(
        holder: BannerImageHolder?,
        data: BannerBean?,
        position: Int,
        size: Int
    ) {
        holder?.imageView?.load(data?.imagePath)
    }

}