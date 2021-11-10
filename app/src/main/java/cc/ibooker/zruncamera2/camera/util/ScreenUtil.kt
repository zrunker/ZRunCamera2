package cc.ibooker.zruncamera2.camera.util

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * @program: ZRunCamera2
 * @description: 屏幕管理类
 * @author: zoufengli01
 * @create: 2021-10-12 17:34
 */
object ScreenUtil {
    @JvmStatic
    fun getScreenW(context: Context): Int {
        val wm =
            context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        wm.defaultDisplay.getMetrics(outMetrics)
        return outMetrics.widthPixels
    }

    @JvmStatic
    fun getScreenH(context: Context): Int {
        val wm =
            context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        wm.defaultDisplay.getMetrics(outMetrics)
        return outMetrics.heightPixels
    }
}