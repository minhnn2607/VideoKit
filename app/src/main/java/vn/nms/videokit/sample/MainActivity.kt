package vn.nms.videokit.sample

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.slider.RangeSlider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import vn.nms.videokit.videokit.VideoKit
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var selectedVideo: String? = null

    private var selectedVideos = mutableListOf<String>()

    private var isSelectingVideo1: Boolean = false
    private var isSelectingVideo2: Boolean = false

    private var tvInput1: TextView? = null
    private var tvInput2: TextView? = null
    private var tvOutput1: TextView? = null
    private var tvOutput2: TextView? = null
    private var btnJoin: Button? = null
    private var btnTrim: Button? = null

    private var btnPlay1: Button? = null

    private var btnPlay2: Button? = null
    private var videoSlider: RangeSlider? = null
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var pickFileLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }
        tvInput1 = findViewById(R.id.tvInput1)
        tvInput2 = findViewById(R.id.tvInput2)
        tvOutput1 = findViewById(R.id.tvOutput1)
        tvOutput2 = findViewById(R.id.tvOutput2)
        videoSlider = findViewById(R.id.videoSlider)
        btnJoin = findViewById(R.id.btnJoin)
        btnTrim = findViewById(R.id.btnTrim)
        btnPlay1 = findViewById(R.id.btnPlay1)
        btnPlay2 = findViewById(R.id.btnPlay2)

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    Toast.makeText(this, "Permission Granted ✅", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permission Denied ❌", Toast.LENGTH_SHORT).show()
                }
            }
        pickFileLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val fileName = System.currentTimeMillis().toString() + ".mp4"
                copyUriToInternalStorage(this, uri, fileName)
                val filePath = filesDir.absolutePath + "/" + fileName
                if (isSelectingVideo1) {
                    selectedVideo = filePath
                    tvInput1?.text = selectedVideo
                    videoSlider?.visibility = View.VISIBLE
                    val videoDuration = getVideoDuration(this, uri)
                    videoSlider?.valueFrom = 0.0f
                    videoSlider?.valueTo = videoDuration.toFloat()
                    videoSlider?.values =
                        listOf(videoDuration.toFloat() / 10f, 8f * videoDuration.toFloat() / 10f)
                }
                if (isSelectingVideo2) {
                    selectedVideos.add(filePath)
                    tvInput2?.text = selectedVideos.joinToString(separator = "\n")
                }
//                Toast.makeText(this, "File selected: $uri", Toast.LENGTH_LONG).show()
            } else {
//                Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show()
            }
            isSelectingVideo1 = false
            isSelectingVideo2 = false
        }


        findViewById<Button>(R.id.btnSelectFile).setOnClickListener {
            isSelectingVideo1 = true
            pickFileLauncher.launch(arrayOf("video/*"))
        }
        findViewById<Button>(R.id.btnSelectMultiFile).setOnClickListener {
            isSelectingVideo2 = true
            pickFileLauncher.launch(arrayOf("video/*"))
        }
        findViewById<Button>(R.id.btnReset).setOnClickListener {
            selectedVideo = null
            tvInput1?.text = ""
            selectedVideos.clear()
            tvInput2?.text = ""
            tvOutput1?.text = ""
            tvOutput2?.text = ""
            filesDir.deleteRecursively()
            videoSlider?.visibility = View.GONE
            btnPlay1?.visibility = View.GONE
            btnPlay2?.visibility = View.GONE
        }
        findViewById<Button>(R.id.btnTrim).setOnClickListener {
            btnJoin?.text = "Processing"
            val output = filesDir.absolutePath + "/" + System.currentTimeMillis() + ".mp4"
            VideoKit.trimVideo(
                selectedVideo,
                output,
                videoSlider?.values?.firstOrNull()?.toLong()?:0L,
                videoSlider?.values?.lastOrNull()?.toLong()?:0L,
                onSuccess = {
                    lifecycleScope.launch(Dispatchers.Main){
                        tvOutput1?.text = output
                        btnJoin?.text = "Trim"
                        btnPlay1?.visibility = View.VISIBLE
                    }
                },
                onError = {
                    lifecycleScope.launch(Dispatchers.Main){
                        btnJoin?.text = "Trim"
                        Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                    }
                })
        }
        findViewById<Button>(R.id.btnJoin).setOnClickListener {

        }
        findViewById<Button>(R.id.btnPlay1).setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("videoPath", tvOutput1?.text.toString())
            startActivity(intent)
        }
        findViewById<Button>(R.id.btnPlay2).setOnClickListener {

        }
        checkAndRequestStoragePermission()
    }

    private fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val outputFile = File(context.filesDir, fileName)

            inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            outputFile // return the copied file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun checkAndRequestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO // or READ_MEDIA_IMAGES, depending on use
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                Toast.makeText(this, "Permission Already Granted ✅", Toast.LENGTH_SHORT).show()
            }

            shouldShowRequestPermissionRationale(permission) -> {
                // Explain to the user why you need this permission
                Toast.makeText(
                    this,
                    "Storage permission is required to select videos",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(permission)
            }

            else -> {
                // Directly request the permission
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun getVideoDuration(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationStr =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLong() ?: 0L // duration in milliseconds
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        VideoKit.release()
    }
}