package com.jiyi.power.app.adapter

import android.view.ViewGroup
import android.widget.ImageView
import com.youth.banner.adapter.BannerAdapter
import com.youth.banner.holder.BannerImageHolder

class HomeBannerAdapter(images: List<Int>) : BannerAdapter<Int, BannerImageHolder>(images) {
    override fun onCreateHolder(parent: ViewGroup, viewType: Int): BannerImageHolder {
        val imageView = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return BannerImageHolder(imageView)
    }

    override fun onBindView(holder: BannerImageHolder, data: Int, position: Int, size: Int) {
        holder.imageView.setImageResource(data)
    }
}
