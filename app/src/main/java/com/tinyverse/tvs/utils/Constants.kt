package com.tinyverse.tvs.utils

object Constants {
    const val MTV_SERVICE_ROOT_FOLDER_NAME = ".mtv_repo"
    const val MTV_SERVICE_PORT = "9888"
    const val MTV_SERVICE_TYPE = "sdk"
    const val MTV_SERVICE_APP_NAME = "mtv"
    const val MTV_SERVICE_TEST_API = "http://127.0.0.1:$MTV_SERVICE_PORT/sdk/health"
    const val TVS_WEB_URL = "https://dev.tinyverse.space/" //web view main
    const val TVS_WEB_VERSION_URL = "https://dev.tinyverse.space/version.txt"

    //val url = "https://service.tinyverse.space/test.html"
    //var url = "https://dev.tinyverse.space/"
    //var url = "https://test.tinyverse.space/"
    //var url = "http://192.168.1.104:5173"
    //var url = "https://webcam-test.com/"
    //val url = "https://dragonir.github.io/h5-scan-qrcode/#/"

    enum class ServerMsg{
        LAUNCH_SUCCESS,
        LAUNCH_FAILED,
        PORT_LISTENING_OK,
        PORT_LISTENING_FAIL,
        API_ACCESS_OK,
        API_ACCESS_FAIL
    }
}
