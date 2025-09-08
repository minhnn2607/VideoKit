package vn.nms.videokit.videokit

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import vn.nms.videokit.videokit.extensions.formatMilliSecondToTime
import vn.nms.videokit.videokit.extensions.getErrorLog
import kotlin.text.isNullOrEmpty

object VideoKit {
    private var isInitialized = false
    private lateinit var appContext: Context

    private val sessionIds = mutableListOf<Long>()
    fun init(context: Context) {
        if (!isInitialized) {
            appContext = context.applicationContext
            isInitialized = true
        }
    }

    fun trimVideo(
        input: String?,
        output: String?,
        startTime: Long,
        endTime: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (input.isNullOrEmpty() || output.isNullOrEmpty()) {
            onError("Invalid file path")
            return
        }
        val startTimeFormatted = startTime.formatMilliSecondToTime()
        val endTimeFormatted = (endTime - startTime).formatMilliSecondToTime()
        val command = "-ss %s -t %s -i %s -c:a copy -c:v copy %s".format(
            startTimeFormatted,
            endTimeFormatted,
            input,
            output
        )
        val session = FFmpegKit.executeAsync(command) { session ->
            if (ReturnCode.isSuccess(session.returnCode)) {
                onSuccess()
            } else {
                onError(session.getErrorLog())
            }
            sessionIds.remove(session.sessionId)
        }
        sessionIds.add(session.sessionId)
    }

    fun joinVideo(
        input: List<String>, output: String?, onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        if (input.size <= 1) onError("Invalid size")
        if (output.isNullOrEmpty() || input.any { it.isEmpty() }) {
            onError("Invalid file path")
            return
        }
        val codecName = getCodecName(input.first())
        val videoTag = getVideoTag(codecName)
        val command =
            if (videoTag.isNotEmpty()) {
                "-max_reload 1 -y -i concat:%s -c:v copy -c:a copy -tag:v %s %s".format(
                    input.joinToString(separator = "|"),
                    videoTag,
                    output
                )
            } else {
                "-max_reload 1 -y -i concat:%s -c:v copy -c:a copy %s".format(
                    input.joinToString(separator = "|"),
                    output
                )
            }
        val session = FFmpegKit.executeAsync(command) { session ->
            if (ReturnCode.isSuccess(session.returnCode)) {
                onSuccess()
            } else {
                onError(session.getErrorLog())
            }
            sessionIds.remove(session.sessionId)
        }
        sessionIds.add(session.sessionId)
    }

    private fun getCodecName(input: String?): String {
        try {
            val mediaInfo = FFprobeKit.getMediaInformation(input)
            val videoStream = mediaInfo.mediaInformation.streams.firstOrNull { it.type == "video" }
            val codecName =
                videoStream?.allProperties?.getString("codec_name").orEmpty().lowercase()
            return codecName
        } catch (_: Exception) {
            return ""
        }
    }

    private fun getVideoTag(codecName: String): String {
        return with(codecName) {
            when {
                contains("h264") or contains("avc") -> "avc1"
                contains("h265") or contains("hevc") -> "hvc1"
                contains("mpeg4") -> "mp4v"
                contains("xvid") -> "xvid"
                contains("divx") -> "divx"
                contains("vp8") -> "vp8"
                contains("vp9") -> "vp8"
                contains("mpg1") or contains("mpeg1") -> "mpg1"
                contains("mpg2") or contains("mpeg2") -> "mpg2"
                contains("dv") or contains("dvsd") or contains("dvhd") -> "dvsd"
                contains("mjpg") or contains("mjpeg") -> "mjpg"
                else -> ""
            }
        }
    }

    fun release() {
        sessionIds.forEach {
            FFmpegKit.cancel(it)
        }
    }
}