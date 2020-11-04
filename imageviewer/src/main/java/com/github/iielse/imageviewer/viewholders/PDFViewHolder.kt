package com.github.iielse.imageviewer.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.github.iielse.imageviewer.ImageViewerAdapterListener
import com.github.iielse.imageviewer.R
import com.github.iielse.imageviewer.adapter.ItemType
import com.github.iielse.imageviewer.core.Components
import com.github.iielse.imageviewer.core.Photo
import com.github.iielse.imageviewer.widgets.PDFView2
import com.github.iielse.imageviewer.widgets.video.ExoVideoView2
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_imageviewer_pdf.*
import kotlinx.android.synthetic.main.item_imageviewer_video.*

/**
 * 作者：吕振鹏
 * E-mail:lvzhenpeng@pansoft.com
 * 创建时间：2020年11月03日
 * 时间：9:21 AM
 * 版本：v1.0.0
 * 类描述：
 * 修改时间：
 */
class PDFViewHolder(override val containerView: View, callback: ImageViewerAdapterListener) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    init {
        pdfView.addListener(object : PDFView2.Listener {
            override fun onDrag(view: PDFView2, fraction: Float) = callback.onDrag(this@PDFViewHolder, view, fraction)
            override fun onRestore(view: PDFView2, fraction: Float) = callback.onRestore(this@PDFViewHolder, view, fraction)
            override fun onRelease(view: PDFView2) = callback.onRelease(this@PDFViewHolder, view)
        })
        Components.requireVHCustomizer().initialize(ItemType.PDF, this)
    }

    fun bind(item: Photo) {
        pdfView.setTag(R.id.viewer_adapter_item_key, item.id())
        pdfView.setTag(R.id.viewer_adapter_item_data, item)
        pdfView.setTag(R.id.viewer_adapter_item_holder, this)
        Components.requireVHCustomizer().bind(ItemType.PDF, item, this)
        Components.requireImageLoader().load(pdfView, item, this)
    }
}