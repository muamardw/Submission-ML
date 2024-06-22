package com.dicoding.asclepius.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ItemHistoryBinding

class HistoryAdapter(private val predictionList: List<HistoryPrediction>):
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>()  {
    private var onDeleteClickListener: OnDeleteClickListener? = null

    interface OnDeleteClickListener {
        fun onDeleteClick(position: Int)
    }

    fun setOnDeleteClickListener(listener: OnDeleteClickListener) {
        onDeleteClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryAdapter.ViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryAdapter.ViewHolder, position: Int) {
        val currentItem = predictionList[position]
        if (currentItem.result.isNotEmpty()) {
            holder.bind(currentItem)
            holder.itemView.visibility = View.VISIBLE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) // Atur kembali parameter tata letak
        } else {
            holder.itemView.visibility = View.GONE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
        }
    }

    override fun getItemCount() = predictionList.size

    inner class ViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(prediction: HistoryPrediction) {
            Glide.with(itemView.context)
                .load(prediction.imagePath)
                .placeholder(R.drawable.ic_place_holder)
                .error(R.drawable.ic_launcher_background)
                .into(binding.historyImg)
            binding.historyLabel.text = prediction.result
            binding.btnDelete.setOnClickListener {
                onDeleteClickListener?.onDeleteClick(adapterPosition)
            }
        }
    }

}