package cc.ibooker.zruncamera2.camera

import android.content.Context
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Rect
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewOutlineProvider

/**
 * @program: ZRunCamera2
 * @description: 照相机预览View
 * @author: zoufengli01
 * @create: 2021-11-01 10:20
 */
class Camera2PreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {
    private val executor: Camera2Executor
    var radius: Float? = 80f

    init {
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                val rect = Rect()
                view?.getGlobalVisibleRect(rect)
                val leftMargin = 0
                val topMargin = 0
                val selfRect = Rect(
                    leftMargin, topMargin,
                    rect.right - rect.left - leftMargin,
                    rect.bottom - rect.top - topMargin
                )
                radius?.let { outline?.setRoundRect(selfRect, it) }
            }
        }
        clipToOutline = true
        holder.setFormat(PixelFormat.TRANSPARENT)
        holder.setKeepScreenOn(true)
        holder.addCallback(this)
        // 创建Camera2Executor
        executor =
            Camera2Executor(
                context
            )
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        executor.openCamera(holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        executor.releaseCamera()
    }

    fun setCamera2Listener(camera2Listener: ICamera2Listener) {
        executor.setCamera2Listener(camera2Listener)
    }

    fun takePicture() {
        executor.takePicture()
    }

    fun releaseCamera() {
        executor.releaseCamera()
    }

    /**
     * 设置照片路径
     */
    fun setFilePath(filePath: String?) {
        executor.setFilePath(filePath)
    }
}