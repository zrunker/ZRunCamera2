package cc.ibooker.zruncamera2.camera

import java.io.File

/**
 * @program: ZRunCamera2
 * @description: 相机回调监听
 * @author: zoufengli01
 * @create: 2021-11-01 11:12
 */
interface ICamera2Listener {
    fun onCameraSuccess(file: File?)
    fun onCameraError(errorCode: ErrorCode?)
}