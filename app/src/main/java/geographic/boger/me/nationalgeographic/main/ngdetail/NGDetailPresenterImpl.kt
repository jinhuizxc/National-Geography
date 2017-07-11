package geographic.boger.me.nationalgeographic.main.ngdetail

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import com.facebook.common.executors.CallerThreadExecutor
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.request.ImageRequest
import geographic.boger.me.nationalgeographic.R
import geographic.boger.me.nationalgeographic.core.NGRumtime
import geographic.boger.me.nationalgeographic.core.NGUtil
import geographic.boger.me.nationalgeographic.main.ContentType
import geographic.boger.me.nationalgeographic.main.selectdate.SelectDateAlbumData
import geographic.boger.me.nationalgeographic.util.Timber
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.*

/**
 * Created by BogerChan on 2017/6/30.
 */

class NGDetailPresenterImpl(
        val data: SelectDateAlbumData? = null,
        val offlineData: NGDetailData? = null) : INGDetailPresenter {

    private var mUI: INGDetailUI? = null

    private val mModel: INGDetailModel by lazy {
        NGDetailModelImpl()
    }

    override fun init(ui: INGDetailUI) {
        mUI = ui
        if (offlineData != null) {
            ui.refreshData(offlineData.picture)
            ui.contentType = ContentType.CONTENT
        } else {
        mModel.requestNGDetailData(data!!.id,
                onStart = {
                    ui.contentType = ContentType.LOADING
                },
                onError = {
                    ui.contentType = ContentType.ERROR
                },
                onComplete = {
                    ui.contentType = ContentType.CONTENT
                },
                onNext = {
                    NGRumtime.favoriteNGDetailDataSupplier.syncFavoriteState(it)
                    ui.refreshData(it.picture)
                })
        }
    }

    override fun shareNGDetailImage(url: String) {
        mUI?.showTipMessage(R.string.tip_share_img_start)
        fetchImage(
                url,
                File(NGRumtime.cacheImageDir, NGUtil.toMD5(url)),
                {
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "image/jpg"
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(it))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    mUI?.startActivity(Intent.createChooser(intent, mUI?.getResouceString(R.string.title_share)))
                },
                {
                    mUI?.showTipMessage(R.string.tip_share_img_error)
                })
    }

    override fun saveNGDetailImage(url: String) {
        mUI?.showTipMessage(R.string.tip_save_img_start)
        fetchImage(
                url,
                File(NGRumtime.externalAlbumDir, "${NGUtil.toMD5(url)}.jpg"),
                {
                    if (mUI == null) {
                        return@fetchImage
                    }
                    mUI!!.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(it)))
                    mUI!!.showTipMessage(
                            String.format(Locale.US, mUI!!.getResouceString(R.string.template_tip_save_img_complete), it.absolutePath))
                },
                {
                    mUI?.showTipMessage(R.string.tip_share_img_error)
                })
    }

    override fun setNGDetailItemFavoriteState(data: NGDetailPictureData) {
        val supplier = NGRumtime.favoriteNGDetailDataSupplier
        if (data.favorite) {
            data.favorite = false
            if (!supplier.removeNGDetailDataToFavorite(data)) {
                data.favorite = true
            }
        } else {
            data.favorite = true
            if (!supplier.addNGDetailDataToFavorite(data)) {
                data.favorite = false
            }
        }
        mUI?.setFavoriteButtonState(data.favorite)
    }

    private fun fetchImage(
            url: String,
            dstFile: File,
            succ: (File) -> Unit,
            err: () -> Unit) {
        val imagePipline = Fresco.getImagePipeline()
        val dataSource = imagePipline.fetchDecodedImage(ImageRequest.fromUri(url),
                ImageRequest.RequestLevel.FULL_FETCH)

        dataSource.subscribe(object : BaseBitmapDataSubscriber() {
            override fun onFailureImpl(dataSource: DataSource<CloseableReference<CloseableImage>>?) {
                err()
            }

            override fun onNewResultImpl(bitmap: Bitmap?) {
                if (bitmap == null) {
                    err()
                    return
                }
                if (!saveBitmap(bitmap, dstFile)) {
                    err()
                    return
                }
                succ(dstFile)
            }

        }, CallerThreadExecutor.getInstance())
    }

    private fun saveBitmap(bmp: Bitmap, file: File): Boolean {
        var stream: OutputStream? = null
        try {
            stream = file.outputStream()
            file.createNewFile()
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            return true
        } catch (e: IOException) {
            Timber.e(e)
            return false
        } finally {
            stream?.close()
        }
    }

    override fun destroy() {
        mModel.cancelPendingCall()
    }
}