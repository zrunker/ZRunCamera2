package cc.ibooker.zruncamera2.camera

enum class ErrorCode(var code: Int, var msg: String) {
    NO_CAMERA_PERMISSION(1, "没有CAMERA权限"),
    CAMERA_STATE_ERROR(2, "相机状态错误"),
    CONFIGURE_FAILED(3, "配置信息设置失败"),
    CAMERA_ACCESS_EXCEPTION(4, "相机异常");

    fun setMsg(msg: String): ErrorCode {
        this.msg = msg
        return this
    }

    override fun toString(): String {
        return "ErrorCode{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                '}'
    }
}