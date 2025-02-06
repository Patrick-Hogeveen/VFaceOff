import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import com.example.vfaceoff.imageAnalysis.FaceDetectorHelper
import com.google.mediapipe.examples.facedetection.OverlayView
//import com.google.mediapipe.examples.facelandmarker.OverlayView
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine





class FaceDetectorComposeBuilder(
    private val context: Context,
    private val scope: CoroutineScope,
    //private val cameraPermissionRequest: PermissionResultRequest
) : FaceDetectorHelper.DetectorListener {
    private val TAG = "FaceDetectorBuilder"
    private lateinit var faceDetectorHelper: FaceDetectorHelper
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var containerModifier: Modifier
    private var containerShape = RectangleShape



    @Composable
    fun Build(
        modifier: Modifier = Modifier,
        containerShape: Shape = RectangleShape,
    ) {
        this.containerModifier = modifier
        this.containerShape = containerShape
        Log.v("Test", "Here I am")
        //CheckCameraPermission()
        OrganismFaceDetector()
    }

    @Composable
    private fun CheckCameraPermission() {
        var isGranted by remember {
            mutableStateOf(false)
        }

        Box(
            modifier = containerModifier,
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = containerShape
            ) {
                // If isGranted == true, show the camera
                AnimatedVisibility(
                    visible = isGranted,
                    enter = scaleIn(tween(550)),
                    exit = scaleOut(tween(550))
                ) {
                    Log.v("Test", "Here I am again :o")
                    OrganismFaceDetector()
                }
            }
        }

        //when (PackageManager.PERMISSION_GRANTED) {
        //    ContextCompat.checkSelfPermission(
        //        context,
        //        "android.permission.CAMERA"
        //    ) -> {
        //        isGranted = true
        //    }
//
        //    else -> {
        //        cameraPermissionRequest {
        //            isGranted = it
        //        }.launch("android.permission.CAMERA")
        //    }
        //}
    }

    @Composable
    private fun OrganismFaceDetector(
    ) {
        val lensFacing = CameraSelector.LENS_FACING_FRONT
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        val preview = Preview.Builder().build()
        val previewView = remember {
            PreviewView(context)
        }
        val overlayView = remember {
            OverlayView(context, null)
        }
        val cameraxSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        LifecycleEventObserver { owner, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> owner.lifecycleScope.launch { doOnResume() }
                Lifecycle.Event.ON_PAUSE -> owner.lifecycleScope.launch { doOnPause() }
                else -> {}
            }
        }

        LaunchedEffect(lensFacing) {
            val cameraProvider = context.getCameraProvider()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraxSelector, preview)
            preview.setSurfaceProvider(previewView.surfaceProvider)
            integrateFaceDetector(lifecycleOwner)
            withContext(Dispatchers.Default) {
                if (faceDetectorHelper.isClosed()) {
                    faceDetectorHelper.setupFaceDetector()
                }
            }
        }

        this.previewView = previewView.apply {
            scaleType = PreviewView.ScaleType.FIT_START
        }
        this.overlayView = overlayView


        Box(modifier = Modifier.aspectRatio(0.75f), contentAlignment = Alignment.Center) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            AndroidView(factory = { overlayView }, modifier = Modifier.fillMaxSize())
        }

    }

    private suspend fun doOnResume() {
        if (!::faceDetectorHelper.isInitialized) return
        withContext(Dispatchers.Default) {
            if (faceDetectorHelper.isClosed()) {
                faceDetectorHelper.setupFaceDetector()
            }
        }
    }

    private suspend fun doOnPause() {
        if (this::faceDetectorHelper.isInitialized) {
            // Close the face detector and release resources
            withContext(Dispatchers.Default) {
                faceDetectorHelper.clearFaceDetector()
            }
        }
    }

    private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
        suspendCoroutine { continuation ->
            ProcessCameraProvider.getInstance(this).also { cameraProvider ->
                cameraProvider.addListener({
                    continuation.resume(cameraProvider.get())
                }, ContextCompat.getMainExecutor(this))
            }
        }

    private suspend fun integrateFaceDetector(lifecycleOwner: LifecycleOwner) {
        withContext(Dispatchers.Default) {
            faceDetectorHelper = FaceDetectorHelper(
                context = context,
                threshold = 0.5f, // you can edit this, ranged from 0 to 1
                currentDelegate = FaceDetectorHelper.DELEGATE_CPU,
                faceDetectorListener = this@FaceDetectorComposeBuilder,
                runningMode = RunningMode.LIVE_STREAM
            )

            // Wait for the views to be properly laid out
            previewView.post {
                // Set up the camera and its use cases
                setUpCamera(lifecycleOwner)
            }
        }
    }

    private fun setUpCamera(lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases(lifecycleOwner)
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner) {

        // CameraProvider
        val cameraProvider =
            cameraProvider
                ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector - makes assumption that we're only using the front camera
        val cameraSelector =
            CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview =
            Preview.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder().setAspectRatioStrategy(
                        AspectRatioStrategy(
                            AspectRatio.RATIO_4_3,
                            AspectRatioStrategy.FALLBACK_RULE_AUTO
                        )
                    ).build()
                )
                .setTargetRotation(previewView.display.rotation)
                .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder().setAspectRatioStrategy(
                        AspectRatioStrategy(
                            AspectRatio.RATIO_4_3,
                            AspectRatioStrategy.FALLBACK_RULE_AUTO
                        )
                    ).build()
                )
                .setTargetRotation(previewView.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(
                        Executors.newSingleThreadExecutor(),
                        faceDetectorHelper::detectLivestreamFrame
                    )
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(previewView.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    override fun onError(error: String, errorCode: Int) {
        scope.launch(Dispatchers.Main) {
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResults(resultBundle: FaceDetectorHelper.ResultBundle) {
        scope.launch(Dispatchers.Main) {
            if (::overlayView.isInitialized) {
                // Pass necessary information to OverlayView for drawing on the canvas
                val detectionResult = resultBundle.results[0]
                overlayView.setResults(
                    detectionResult,
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth
                )

                // Force a redraw
                overlayView.invalidate()
            }
        }
    }
}