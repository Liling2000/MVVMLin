package com.jiyi.power.ui.project.adapter

import coil.load
import com.aleyn.mvvm.binding.binding
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.jiyi.power.R
import com.jiyi.power.databinding.ItemProjectListBinding
import com.jiyi.power.network.entity.ArticlesBean

/**
 * @author : Aleyn
 * @date : 2025/4/23 20:09
 */
class ProjectAdapter : BaseQuickAdapter<ArticlesBean, BaseViewHolder>(R.layout.item_project_list){

    override fun convert(
        holder: BaseViewHolder,
        item: ArticlesBean
    ) {
        val binding = holder.binding(ItemProjectListBinding::bind)
        binding.tvProjectListAtticleType.text = item.chapterName
        binding.ivProjectListAtticleIc.load(item.envelopePic)
        binding.tvProjectListAtticleTitle.text = item.title
        binding.tvProjectListAtticleTime.text = item.niceDate
        binding.tvProjectListAtticleAuther.text = item.author
    }
}