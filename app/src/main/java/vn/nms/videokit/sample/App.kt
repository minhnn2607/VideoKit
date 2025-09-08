package vn.nms.videokit.sample

import android.app.Application
import vn.nms.videokit.videokit.VideoKit

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        VideoKit.init(this)
    }
}