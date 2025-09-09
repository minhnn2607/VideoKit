package vn.nms.videokit.android.extensions

import com.arthenica.ffmpegkit.Session
import java.util.Locale

fun Long.formatMilliSecondToTime(): String {
    try {
        val hours = (this / 1000L) / 3600L
        val minutes = ((this / 1000L) % 3600L) / 60L
        val seconds = (this / 1000L) % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } catch (_: Exception) {
        return ""
    }
}

fun Session.getErrorLog(): String {
    val logs = this.allLogs.orEmpty().filterNot {
        it.message.trim().length < 15 ||
                it.message == "\n" ||
                it.message.isNullOrEmpty() ||
                it.message.trim().startsWith("lib") ||
                it.message.trim().startsWith("ffmpeg version") ||
                it.message.trim().startsWith("built with Android") ||
                it.message.trim().startsWith("Copyright") ||
                it.message.trim().startsWith("frame=") ||
                it.message.trim().startsWith("configuration") ||
                it.message.trim().startsWith("[hls") ||
                it.message.trim().startsWith("Duration:") ||
                it.message.trim().startsWith("Input #0") ||
                it.message.trim().startsWith("Output #0") ||
                it.message.trim().startsWith("Stream #0") ||
                it.message.trim().startsWith("Metadata") ||
                it.message.trim().startsWith("Stream mapping") ||
                it.message.trim().startsWith("Press [q]") ||
                it.message.trim().startsWith("variant_bitrate") ||
                it.message.trim().startsWith("Media Presentation") ||
                it.message.trim().startsWith("encoder") ||
                it.message.trim().startsWith("title") ||
                it.message.trim().startsWith("service_name") ||
                it.message.trim().startsWith("service_provider")
    }
        .takeLast(10).map { it.message.trim().take(300) }
    val finalLog = logs.joinToString(separator = "|")
    return finalLog
}