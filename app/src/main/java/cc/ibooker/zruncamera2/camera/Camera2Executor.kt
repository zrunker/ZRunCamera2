package cc.ibooker.zruncamera2.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.text.TextUtils
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import androidx.core.app.ActivityCompat
import cc.ibooker.zruncamera2.camera.util.ScreenUtil.getScreenH
import cc.ibooker.zruncamera2.camera.util.ScreenUtil.getScreenW
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.math.abs

/**
 * @program: ZRunCamera2
 * @description: 照相机管理类
 * @author: zoufengli01
 * @create: 2021-11-01 10:30
 */
class Camera2Executor(  /*上下文对象*/
    private val mContext: Context
) {
    private var cThread: HandlerThread? = null
    private var uiHandler: Handler? = null
    private var cHandler: Handler? = null

    /*相机管理类*/
    private var mCameraManager: CameraManager? = null

    /*相机设备*/
    private var mCameraDevice: CameraDevice? = null

    /*相机捕捉会话*/
    private var mCaptureSession: CameraCaptureSession? = null

    /*相机ID【默认后置摄像头】*/
    private var mCameraId = CameraCharacteristics.LENS_FACING_FRONT.toString()

    /*Image读取类*/
    private var mImageReader: ImageReader? = null

    /*相机监听*/
    private var mCamera2Listener: ICamera2Listener? = null

    /*图片格式*/
    private val mImageFormat = ImageFormat.JPEG

    /*照片路径*/
    private var filePath: String? = null

    /**
     * 开始子线程【预览】
     */
    private fun startHandlerT() {
        if (cThread == null) {
            cThread = HandlerThread("Camera2Executor")
            cThread!!.start()
        }
        if (cHandler == null) {
            cHandler = Handler(cThread!!.looper)
        }
    }

    /**
     * 停止子线程【预览】
     */
    private fun stopHandlerT() {
        if (cThread != null) {
            cThread!!.quitSafely()
            cThread = null
        }
        if (cHandler != null) {
            cHandler!!.removeCallbacksAndMessages(null)
            cHandler = null
        }
    }

    private fun init() {
        mCameraManager = mContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        uiHandler = Handler(Looper.getMainLooper())
    }

    /**
     * 初始化ImageReader
     */
    private fun initImageReader() {
        if (mImageReader == null) {
            try {
                val cameraCharacteristics = mCameraManager!!.getCameraCharacteristics(mCameraId)
                val size = getImgSize(cameraCharacteristics)
                val width = size!!.width
                val height = size.height
                // 设置图片大小
                mImageReader = ImageReader.newInstance(width, height, mImageFormat, 2)
                val listener = OnImageAvailableListener { reader -> // 获取照片数据
                    val image = reader.acquireLatestImage()
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer[bytes]

                    // 字节码转文件
                    if (TextUtils.isEmpty(filePath)) {
                        filePath = (mContext.externalCacheDir!!.absolutePath
                                + File.separator + System.currentTimeMillis() + ".jpg")
                    } else {
                        val temp = filePath!!.lowercase(Locale.ROOT)
                        if (!(temp.endsWith(".jpeg")
                                    || temp.endsWith(".jpg")
                                    || temp.endsWith(".png")
                                    || temp.endsWith(".webp"))
                        ) {
                            filePath = (mContext.externalCacheDir!!.absolutePath
                                    + File.separator + System.currentTimeMillis() + ".jpg")
                        }
                    }
                    val file = File(filePath)
                    var fos: FileOutputStream? = null
                    try {
                        fos = FileOutputStream(file)
                        fos.write(bytes)
                        fos.flush()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    }

//                        // 字节码转Bitmap
//                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (mCamera2Listener != null && file.exists()) {
                        mCamera2Listener!!.onCameraSuccess(file)
                    }
                    image.close()
                }
                mImageReader!!.setOnImageAvailableListener(listener, uiHandler)
            } catch (e: CameraAccessException) {
                if (mCamera2Listener != null) {
                    mCamera2Listener!!.onCameraError(
                        ErrorCode.CAMERA_ACCESS_EXCEPTION.setMsg(e.message!!)
                    )
                }
            }
        }
    }

    /**
     * 设置摄像头方向
     *
     * @param direction 0-后置摄像头，1-前置摄像头
     * CameraCharacteristics.LENS_FACING_FRONT
     * CameraCharacteristics.LENS_FACING_BACK
     */
    fun setCameraDirection(direction: Int): Camera2Executor {
        mCameraId = direction.toString()
        return this
    }

    /**
     * 设置相机回调
     */
    fun setCamera2Listener(camera2Listener: ICamera2Listener?): Camera2Executor {
        mCamera2Listener = camera2Listener
        return this
    }

    /**
     * 设置照片路径
     */
    fun setFilePath(filePath: String?): Camera2Executor {
        this.filePath = filePath
        return this
    }

    /**
     * 打开摄像头
     */
    @SuppressLint("MissingPermission")
    fun openCamera(sHolder: SurfaceHolder?) {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // 需要动态申请权限
            if (mCamera2Listener != null) {
                mCamera2Listener!!.onCameraError(ErrorCode.NO_CAMERA_PERMISSION)
            }
        } else {
            try {
                // 回收旧的相机
                releaseCamera()
                // 相机回调
                val callback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        mCameraDevice = camera
                        // 开启预览
                        startPreView(sHolder)
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        stopPreView()
                        releaseCamera()
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        stopPreView()
                        releaseCamera()
                        if (mCamera2Listener != null) {
                            mCamera2Listener!!.onCameraError(ErrorCode.CAMERA_STATE_ERROR)
                        }
                    }
                }
                // 开启相机
                mCameraManager!!.openCamera(mCameraId, callback, uiHandler)
            } catch (e: CameraAccessException) {
                if (mCamera2Listener != null) {
                    mCamera2Listener!!.onCameraError(
                        ErrorCode.CAMERA_ACCESS_EXCEPTION.setMsg(e.message!!)
                    )
                }
            }
        }
    }

    /**
     * 开启预览
     */
    private fun startPreView(sHolder: SurfaceHolder?) {
        if (mCameraDevice != null && sHolder != null) {
            try {
                // 初始化ImageReader
                initImageReader()
                // 开启HandlerThread
                startHandlerT()
                // 获取预览CaptureRequest.Builder
                val captureRequestBuilder =
                    mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                // 自动对焦
                captureRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                // 自动曝光
                captureRequestBuilder.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                )
                // 设置目标
                val surface = sHolder.surface
                captureRequestBuilder.addTarget(surface)
                // 输出Surface
                val outputs = listOf(surface, mImageReader!!.surface)
                // 会话回调
                val callback: CameraCaptureSession.StateCallback =
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            try {
                                mCaptureSession = session
                                mCaptureSession!!.setRepeatingRequest(
                                    captureRequestBuilder.build(),
                                    null,
                                    cHandler
                                )
                            } catch (e: CameraAccessException) {
                                if (mCamera2Listener != null) {
                                    mCamera2Listener!!.onCameraError(
                                        ErrorCode.CAMERA_ACCESS_EXCEPTION.setMsg(e.message!!)
                                    )
                                }
                            }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            if (mCamera2Listener != null) {
                                mCamera2Listener!!.onCameraError(ErrorCode.CONFIGURE_FAILED)
                            }
                        }
                    }
                // 创建会话Session
                mCameraDevice!!.createCaptureSession(outputs, callback, cHandler)
            } catch (e: CameraAccessException) {
                if (mCamera2Listener != null) {
                    mCamera2Listener!!.onCameraError(
                        ErrorCode.CAMERA_ACCESS_EXCEPTION.setMsg(e.message!!)
                    )
                }
            }
        }
    }

    /**
     * 关闭预览
     */
    private fun stopPreView() {
        if (mCaptureSession != null) {
            try {
                mCaptureSession!!.stopRepeating()
            } catch (e: CameraAccessException) {
                if (mCamera2Listener != null) {
                    mCamera2Listener!!.onCameraError(
                        ErrorCode.CAMERA_ACCESS_EXCEPTION.setMsg(e.message!!)
                    )
                }
            }
        }
    }

    /**
     * 拍照
     */
    fun takePicture() {
        if (mCameraDevice != null) {
            try {
                // 初始化ImageReader
                initImageReader()
                // 开启HandlerThread
                startHandlerT()
                // 获取拍照CaptureRequest.Builder
                val captureRequestBuilder = mCameraDevice!!.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE
                )
                // 设置目标
                captureRequestBuilder.addTarget(mImageReader!!.surface)
                // 自动对焦
                captureRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                // 自动曝光
                captureRequestBuilder.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                )
                // 根据设备方向计算设置照片的方向
                val cameraCharacteristics = mCameraManager!!.getCameraCharacteristics(mCameraId)
                captureRequestBuilder.set(
                    CaptureRequest.JPEG_ORIENTATION,
                    getRotation(cameraCharacteristics)
                )
                // 拍照
                mCaptureSession!!.capture(captureRequestBuilder.build(), null, cHandler)
            } catch (e: CameraAccessException) {
                if (mCamera2Listener != null) {
                    mCamera2Listener!!.onCameraError(
                        ErrorCode.CAMERA_ACCESS_EXCEPTION.setMsg(e.message!!)
                    )
                }
            }
        }
    }

    /**
     * 回收相机
     */
    fun releaseCamera() {
        if (mCameraDevice != null) {
            mCameraDevice!!.close()
            mCameraDevice = null
        }
        if (mCaptureSession != null) {
            mCaptureSession!!.close()
            mCaptureSession = null
        }
        if (mImageReader != null) {
            mImageReader!!.close()
            mImageReader = null
        }
        stopHandlerT()
    }

    /**
     * 获取照片方向角度
     */
    private fun getRotation(cameraCharacteristics: CameraCharacteristics): Int {
        var displayRotation = (mContext as Activity).windowManager.defaultDisplay.rotation
        when (displayRotation) {
            Surface.ROTATION_0 -> displayRotation = 90
            Surface.ROTATION_90 -> displayRotation = 0
            Surface.ROTATION_180 -> displayRotation = 270
            Surface.ROTATION_270 -> displayRotation = 180
        }
        val sensorOrientation =
            cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        return (displayRotation + sensorOrientation + 270) % 360
    }

    /**
     * 获取图片大小，取最接近当前屏幕的宽高比的尺寸
     */
    private fun getImgSize(cameraCharacteristics: CameraCharacteristics): Size? {
        // 计算当前屏幕的宽高比
        val screenWidth = getScreenW(mContext)
        val screenHeight = getScreenH(mContext)
        val screenRatio = screenWidth * 100f / screenHeight
        // 获取相机支持的所有尺寸
        val map = cameraCharacteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        )
        val sizeList = map!!.getOutputSizes(mImageFormat)
        // 对比尺寸
        val tempMap: MutableMap<Float, Size> = HashMap()
        val tempList: MutableList<Float> = ArrayList()
        for (item in sizeList) {
            val width = item.width
            val height = item.height
            val ratio = width * 100f / height
            val ratioDiff = abs(ratio - screenRatio)
            tempMap[ratioDiff] = item
            tempList.add(ratioDiff)
        }
        tempList.sort()
        // 取出最合适尺寸
        return tempMap[tempList[0]]
    }

    init {
        init()
    }
}