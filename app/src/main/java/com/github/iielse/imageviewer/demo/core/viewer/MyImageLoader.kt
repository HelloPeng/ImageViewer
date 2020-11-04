package com.github.iielse.imageviewer.demo.core.viewer

import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.iielse.imageviewer.core.ImageLoader
import com.github.iielse.imageviewer.core.Photo
import com.github.iielse.imageviewer.demo.R
import com.github.iielse.imageviewer.demo.business.ViewerHelper
import com.github.iielse.imageviewer.demo.business.find
import com.github.iielse.imageviewer.demo.data.MyData
import com.github.iielse.imageviewer.demo.utils.appContext
import com.github.iielse.imageviewer.demo.utils.bindLifecycle
import com.github.iielse.imageviewer.demo.utils.log
import com.github.iielse.imageviewer.demo.utils.toast
import com.github.iielse.imageviewer.utils.Config
import com.github.iielse.imageviewer.widgets.PDFView2
import com.github.iielse.imageviewer.widgets.video.ExoVideoView
import com.github.iielse.imageviewer.widgets.video.ExoVideoView2
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.source.MediaSourceEventListener
import com.google.android.exoplayer2.ui.PlayerControlView
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener3
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

class MyImageLoader : ImageLoader {

    override fun load(view: PDFView2, data: Photo, viewHolder: RecyclerView.ViewHolder) {
        val url = (data as? MyData?)?.url ?: return
        log("-------加载pdf------$url")
        val dataFile = view.context.cacheDir
        val fileNames = url.split("FileName".toRegex()).toTypedArray()
        var fileName: String? = null
        if (fileNames.size > 1) {
            fileName = fileNames[1].substring(1)
            try {
                fileName = URLDecoder.decode(fileName, "utf-8")
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
        }
        DownloadTask.Builder(url, dataFile)
                .setMinIntervalMillisCallbackProcess(300)
                .setFilename(fileName)
                .build().enqueue(object : DownloadListener3() {
                    override fun started(task: DownloadTask) {
                        Log.d("Task", "started")
                    }

                    override fun completed(task: DownloadTask) {
                        Log.d("Task", "completed")
                       // lc.onResourceReady(task.file)
                        view.fromFile(task.file)
                                .scrollHandle(DefaultScrollHandle(view.context))
                                .load()
                    }

                    override fun canceled(task: DownloadTask) {
                        Log.d("Task", "canceled")
                    }

                    override fun error(task: DownloadTask, e: Exception) {
                        Log.d("Task", "error" + e.message)
                       // lc.onLoadFailed(null)
                    }

                    override fun warn(task: DownloadTask) {
                        Log.d("Task", "warn")
                    }

                    override fun retry(task: DownloadTask, cause: ResumeFailedCause) {
                        Log.d("Task", "retry")
                    }

                    override fun connected(task: DownloadTask, blockCount: Int, currentOffset: Long, totalLength: Long) {
                        Log.d("Task", "connected")
                    }

                    override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
                        Log.d("Task", String.format("====progress====currentOffset = %s,totalLength = %s", currentOffset, totalLength))
                    }
                })
    }
    /**
     * 根据自身photo数据加载图片.可以使用其它图片加载框架.
     */
    override fun load(view: ImageView, data: Photo, viewHolder: RecyclerView.ViewHolder) {
        val it = (data as? MyData?)?.url ?: return
        Glide.with(view).load(it)
                .override(view.width, view.height)
                .placeholder(view.drawable)
                .into(view)
    }

    override fun load(exoVideoView: ExoVideoView2, data: Photo, viewHolder: RecyclerView.ViewHolder) {
        val it = (data as? MyData?)?.url ?: return
        val cover = viewHolder.itemView.findViewById<ImageView>(R.id.imageView)
        cover.visibility = View.VISIBLE
        val loadingTask = Runnable {
            findLoadingView(viewHolder)?.visibility = View.VISIBLE
        }
        cover.postDelayed(loadingTask, Config.DURATION_TRANSITION + 1500)
        Glide.with(exoVideoView).load(it)
                .placeholder(cover.drawable)
                .into(cover)

        exoVideoView.addAnalyticsListener(object : AnalyticsListener {
            override fun onLoadError(eventTime: AnalyticsListener.EventTime, loadEventInfo: MediaSourceEventListener.LoadEventInfo, mediaLoadData: MediaSourceEventListener.MediaLoadData, error: IOException, wasCanceled: Boolean) {
                findLoadingView(viewHolder)?.visibility = View.GONE
                viewHolder.find<TextView>(R.id.errorPlaceHolder)?.text = error.message
            }
        })
        exoVideoView.setVideoRenderedCallback(object : ExoVideoView.VideoRenderedListener {
            override fun onRendered(view: ExoVideoView) {
                cover.visibility = View.GONE
                cover.removeCallbacks(loadingTask)
                findLoadingView(viewHolder)?.visibility = View.GONE
            }
        })

        val playerControlView = viewHolder.find<PlayerControlView>(R.id.playerControlView)
        exoVideoView.addListener(object : ExoVideoView2.Listener {
            override fun onDrag(view: ExoVideoView2, fraction: Float) {
                if (!ViewerHelper.simplePlayVideo) {
                    playerControlView?.visibility = View.GONE
                }
            }

            override fun onRestore(view: ExoVideoView2, fraction: Float) {
                if (!ViewerHelper.simplePlayVideo) {
                    playerControlView?.visibility = View.VISIBLE
                }
            }

            override fun onRelease(view: ExoVideoView2) {
            }
        })

        exoVideoView.prepare(it)
    }

    /**
     * 根据自身photo数据加载超大图.subsamplingView数据源需要先将内容完整下载到本地.需要注意生命周期
     */
    override fun load(subsamplingView: SubsamplingScaleImageView, data: Photo, viewHolder: RecyclerView.ViewHolder) {
        val it = (data as? MyData?)?.url ?: return
        subsamplingDownloadRequest(it)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { findLoadingView(viewHolder)?.visibility = View.VISIBLE }
                .doFinally { findLoadingView(viewHolder)?.visibility = View.GONE }
                .doOnNext { subsamplingView.setImage(ImageSource.uri(Uri.fromFile(it))) }
                .doOnError { toast(it.message) }
                .subscribe().bindLifecycle(subsamplingView)
    }

    private fun subsamplingDownloadRequest(url: String): Observable<File> {
        return Observable.create {
            try {
                it.onNext(Glide.with(appContext()).downloadOnly().load(url).submit().get())
                it.onComplete()
            } catch (e: java.lang.Exception) {
                if (!it.isDisposed) it.onError(e)
            }
        }
    }

    private fun findLoadingView(viewHolder: RecyclerView.ViewHolder): View? {
        return viewHolder.itemView.findViewById<ProgressBar>(R.id.loadingView)
    }
}
