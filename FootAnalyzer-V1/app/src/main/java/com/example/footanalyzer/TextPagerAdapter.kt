package com.example.footanalyzer
import android.text.Html
import android.os.Build

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TextPagerAdapter(private val pages: List<String>) :
    RecyclerView.Adapter<TextPagerAdapter.PageViewHolder>() {

    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.page_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.page_text_item, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val html = pages[position]
        val formatted =
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        holder.textView.text = formatted
    }


    override fun getItemCount(): Int = pages.size
}
