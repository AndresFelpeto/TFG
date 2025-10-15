package com.example.footanalyzer
import android.text.Html

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView

class ExplainingAdapter(private val pages: List<ExplainingContent>) :
    RecyclerView.Adapter<ExplainingAdapter.PageViewHolder>() {

    inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView = itemView.findViewById<TextView>(R.id.text_explaining)
        val imageView = itemView.findViewById<ImageView>(R.id.image_explaining)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.items_explaining, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val currentPage = pages[position]
        holder.textView.text = HtmlCompat.fromHtml(currentPage.text, HtmlCompat.FROM_HTML_MODE_LEGACY)
        if (currentPage.image != null) {
            holder.imageView.setImageResource(currentPage.image)
            holder.imageView.visibility = View.VISIBLE
        } else {
            holder.imageView.visibility = View.GONE
        }
    }


    override fun getItemCount(): Int = pages.size
}
