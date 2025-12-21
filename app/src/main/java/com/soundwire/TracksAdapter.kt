package com.soundwire

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TracksAdapter(
    private val tracks: List<Track>,
    private val onClick: (index: Int) -> Unit
) : RecyclerView.Adapter<TracksAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = v.findViewById(R.id.tvSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_track, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = tracks[position]
        holder.tvTitle.text = t.title
        holder.tvSubtitle.text = when (t.source) {
            TrackSource.LOCAL -> "На телефоне"
            TrackSource.SERVER -> t.artist ?: "С компьютера"
        }
        holder.itemView.setOnClickListener { onClick(position) }
    }

    override fun getItemCount(): Int = tracks.size
}
