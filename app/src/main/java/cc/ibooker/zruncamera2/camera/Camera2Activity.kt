package cc.ibooker.zruncamera2.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import cc.ibooker.zruncamera2.R
import cc.ibooker.zruncamera2.camera.util.UriUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.io.File

/**
 * @program: ZRunCamera2
 * @description: 相机界面
 * @author: zoufengli01
 * @create: 2021-11-01 18:06
 */
class Camera2Activity : AppCompatActivity(), View.OnClickListener, ICamera2Listener {
    private var cPreView: Camera2PreView? = null
    private var ivPreview: ImageView? = null
    private var ivTakePicture: ImageView? = null
    private var llOper: LinearLayout? = null
    private var tvRetry: TextView? = null
    private var tvOk: TextView? = null
    private var tvTip: TextView? = null
    private var tvCancel: TextView? = null
    private val needPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    private val requestPermissionCode = 1010

    /*照片路径*/
    private var filePath: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera2)
        val intent = intent
        if (intent != null) {
            val uri = intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)
            if (uri != null) {
                filePath = UriUtil.getPath(this, uri)
            }
        }

        // 初始化View
        initView()

        // 申请权限
        for (permission in needPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, needPermissions, requestPermissionCode)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cPreView!!.releaseCamera()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestPermissionCode) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "获取权限失败！", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun initView() {
        cPreView = findViewById(R.id.c_pre_view)
        cPreView?.setCamera2Listener(this)
        cPreView?.setFilePath(filePath)
        ivPreview = findViewById(R.id.iv_preview)
        ivTakePicture = findViewById(R.id.iv_take_picture)
        ivTakePicture?.setOnClickListener(this)
        tvTip = findViewById(R.id.tv_tip)
        llOper = findViewById(R.id.ll_oper)
        tvRetry = findViewById(R.id.tv_retry)
        tvRetry?.setOnClickListener(this)
        tvOk = findViewById(R.id.tv_ok)
        tvOk?.setOnClickListener(this)
        tvCancel = findViewById(R.id.tv_cancel)
        tvCancel?.setOnClickListener(this)

        cameraBefore()
    }

    @SuppressLint("NonConstantResourceId")
    override fun onClick(v: View) {
        when (v.id) {
            R.id.iv_take_picture -> {
                cPreView!!.takePicture()
                ivTakePicture!!.isEnabled = false
            }
            R.id.tv_retry -> cameraBefore()
            R.id.tv_ok -> {
                val data = Intent()
                data.putExtra("filePath", filePath)
                setResult(RESULT_OK, data)
                finish()
            }
            R.id.tv_cancel -> finish()
        }
    }

    override fun onCameraSuccess(file: File?) {
        if (file != null) {
            filePath = file.absolutePath
            Glide.with(this)
                .load(file)
                .centerCrop()
                .transform(GlideRoundRectTransform(80f))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(ivPreview!!)
            cameraAfter()
        }
    }

    override fun onCameraError(errorCode: ErrorCode?) {
        if (errorCode != null) {
            Toast.makeText(this, errorCode.msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun cameraBefore() {
        cPreView!!.visibility = View.VISIBLE
        ivTakePicture!!.visibility = View.VISIBLE
        ivPreview!!.visibility = View.GONE
        llOper!!.visibility = View.GONE
        tvTip!!.text = "将拍摄的内容置于取景框内"
        ivTakePicture!!.isEnabled = true
        tvRetry!!.isEnabled = false
        tvOk!!.isEnabled = false
        tvCancel!!.visibility = View.VISIBLE
    }

    private fun cameraAfter() {
        ivPreview!!.visibility = View.VISIBLE
        llOper!!.visibility = View.VISIBLE
        cPreView!!.visibility = View.GONE
        ivTakePicture!!.visibility = View.GONE
        tvTip!!.text = "拍好了"
        ivTakePicture!!.isEnabled = false
        tvRetry!!.isEnabled = true
        tvOk!!.isEnabled = true
        tvCancel!!.visibility = View.GONE
    }
}