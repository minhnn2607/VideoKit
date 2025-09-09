package vn.nms.videokit.sample

import android.app.Application
import vn.nms.videokit.android.VideoKit

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        VideoKit.init(this)
    }
}