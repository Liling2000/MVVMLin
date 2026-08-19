package com.jiyi.power.app.adapter

import com.aleyn.mvvm.binding.binding
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.jiyi.power.R
import com.jiyi.power.databinding.ItemUsedwebBinding
import com.jiyi.power.app.network.entity.UsedWeb

/**
 *   @auther : Aleyn
 *   time   : 2019/11/14
 */

class MeWebAdapter :
    BaseQuickAdapter<UsedWeb, BaseViewHolder>(R.layout.item_usedweb) {

    override fun convert(holder: BaseViewHolder, item: UsedWeb) {
        val binding = holder.binding(ItemUsedwebBinding::bind)
        binding.tvName.text = item.name
        binding.tvLink.text = item.link
    }

}