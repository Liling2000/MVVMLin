package com.jiyi.power.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jiyi.power.app.bean.Certification
import com.jiyi.power.databinding.ItemCertificationBinding

class CertificationAdapter(private val onClick: (Certification) -> Unit) :
    RecyclerView.Adapter<CertificationAdapter.ViewHolder>() {

    override fun getItemCount() = Certification.entries.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemCertificationBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(Certification.entries[position])
    }

    inner class ViewHolder(private val binding: ItemCertificationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(certification: Certification) = with(binding) {
            textTitle.setText(certification.titleRes)
            textDescription.setText(certification.descriptionRes)
            root.setOnClickListener { onClick(certification) }
        }
    }
}
