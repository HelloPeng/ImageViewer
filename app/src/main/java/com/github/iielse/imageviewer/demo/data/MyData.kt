package com.github.iielse.imageviewer.demo.data

import com.github.iielse.imageviewer.adapter.ItemType
import com.github.iielse.imageviewer.core.Photo
import com.github.iielse.imageviewer.demo.utils.VideoUtils

data class MyData(val id: Long,
                  val url: String,
                  val subsampling: Boolean = false,
                  val isPdf: Boolean = false,
                  val desc: String = "[$id] Caption or other information for this picture [$id]") : Photo {
    override fun id(): Long = id
    override fun itemType(): @ItemType.Type Int {
        return when {
            VideoUtils.isVideoSource(url) -> ItemType.VIDEO
            subsampling -> ItemType.SUBSAMPLING
            isPdf -> ItemType.PDF
            else -> ItemType.PHOTO
        }
    }
}
