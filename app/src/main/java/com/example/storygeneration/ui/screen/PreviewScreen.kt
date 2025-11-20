package com.example.storygeneration.ui.screen

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL

@OptIn(UnstableApi::class)
@Composable
fun PreviewScreen(navController: NavController) {
    val context = LocalContext.current
    val (isPlaying, setIsPlaying) = remember { mutableStateOf(false) }
    val (currentTime, setCurrentTime) = remember { mutableStateOf(0L) }
    val (duration, setDuration) = remember { mutableStateOf(0L) }
    var volume by remember { mutableFloatStateOf(1.0f) }
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Function to export video
    fun exportVideo() {
        coroutineScope.launch(Dispatchers.IO) {
            isExporting = true
            exportStatus = "Exporting video..."

            try {
                val videoUri =
                    Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                val videoTitle = "story_generation_video_${System.currentTimeMillis()}.mp4"
                val videoDescription = "Generated story video"

                // Create content values for MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, videoTitle)
                    put(MediaStore.Video.Media.DESCRIPTION, videoDescription)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/StoryGeneration")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }

                // Get content resolver and insert video
                val contentResolver = context.contentResolver
                val uri = contentResolver.insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues
                ) ?: throw IOException("Failed to create media store entry")

                // Download and save the video
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val inputStream = URL(videoUri.toString()).openStream()
                    inputStream.copyTo(outputStream)
                } ?: throw IOException("Failed to open output stream")

                // Update media store to make it available
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)

                // Show success message
                withContext(Dispatchers.Main) {
                    exportStatus = null
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Video exported successfully",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    exportStatus = null
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Export failed: ${e.message}",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            } finally {
                isExporting = false
            }
        }
    }

    // Permission handling
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(), onResult = { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                exportVideo()
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Permissions required to export video",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        })

    // Initialize ExoPlayer
    DisposableEffect(Unit) {
        val exoPlayer = ExoPlayer.Builder(context).build()
        player = exoPlayer

        // Example video URL - replace with your actual video path
        val mediaItem =
            MediaItem.fromUri("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        // Set up player listeners for tracking state (optional, for external use)
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                setIsPlaying(playWhenReady)
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int
            ) {
                setCurrentTime(exoPlayer.currentPosition)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                setDuration(exoPlayer.duration)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    setDuration(exoPlayer.duration)
                }
            }
        }

        exoPlayer.addListener(listener)

        // Lifecycle observer to pause/play player when screen is resumed/paused
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.playWhenReady = false
                }

                Lifecycle.Event.ON_RESUME -> {
                    exoPlayer.playWhenReady = isPlaying
                }

                Lifecycle.Event.ON_DESTROY -> {
                    exoPlayer.release()
                    player = null
                }

                else -> { /* Do nothing */
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            exoPlayer.removeListener(listener)
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            exoPlayer.release()
            player = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Button(onClick = { navController.popBackStack() }) {
                    Text("Back")
                }
            }

            // Preview title
            Text(
                "Preview",
                fontSize = 24.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            // Video Player using AndroidView with PlayerView
            // ExoPlayer原生控件已包含：播放/暂停按钮、进度条、时间显示、音量控制等功能
            val localPlayer = player
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = localPlayer
                        useController = true  // 启用原生控制器
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                        // 可选：自定义控制器行为
                        controllerShowTimeoutMs = 3000  // 3秒后自动隐藏控制器
                        controllerAutoShow = true       // 自动显示控制器
                        // showBuffering设置需要通过方法而不是直接赋值
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
                    .padding(vertical = 16.dp)
            )

            // Export Video button
            Button(
                onClick = {
                    // Request permissions and export video
                    storagePermissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                },
                enabled = !isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text("Exporting...", modifier = Modifier.padding(start = 8.dp))
                } else {
                    Text("Export Video")
                }
            }
        }
    }
}

// Helper function to format time
fun formatTime(timeInMillis: Long): String {
    val seconds = (timeInMillis / 1000) % 60
    val minutes = (timeInMillis / (1000 * 60)) % 60
    val hours = timeInMillis / (1000 * 60 * 60)
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewScreenPreview() {
    val navController = rememberNavController()
    PreviewScreen(navController = navController)
}