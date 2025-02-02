package com.example.vfaceoff.imageAnalysis

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.AndroidViewModel
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.max

const val DELEGATE_CPU = 0
const val DELEGATE_GPU = 1
const val DEFAULT_FACE_DETECTION_CONFIDENCE = 0.5F
const val DEFAULT_FACE_TRACKING_CONFIDENCE = 0.5F
const val DEFAULT_FACE_PRESENCE_CONFIDENCE = 0.5F
const val DEFAULT_NUM_FACES = 1
const val OTHER_ERROR = 0
const val GPU_ERROR = 1

data class LandmarkerUiState(
    val mostRecentGesture: String? = null,
)

class TrackingViewModel(
    val context: Context,
    var minFaceDetectionConfidence: Float = DEFAULT_FACE_DETECTION_CONFIDENCE,
    var minFaceTrackingConfidence: Float = DEFAULT_FACE_TRACKING_CONFIDENCE,
    var minFacePresenceConfidence: Float = DEFAULT_FACE_PRESENCE_CONFIDENCE,
    var maxNumFaces: Int = DEFAULT_NUM_FACES,
    var currentDelegate: Int = DELEGATE_CPU,
    var runningMode: RunningMode = RunningMode.IMAGE,
    val faceLandmarkerHelperListener: LandmarkerListener? = null
    ) {



    //init {
    //    TODO()
    //}

    private val faceLandmarker by lazy {
        val baseOptionsBuilder = BaseOptions.builder().setModelAssetPath("face_landmarker.task") // change to model location in assets
//
        val baseOptions = baseOptionsBuilder.build()
//
        val optionsBuilder =
            FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setMinFaceDetectionConfidence(minFaceDetectionConfidence)
                .setMinTrackingConfidence(minFaceTrackingConfidence)
                .setMinFacePresenceConfidence(minFacePresenceConfidence)
                .setNumFaces(maxNumFaces)
                //.(this::returnLivestreamResult)
                //.setErrorListener(this::returnLivestreamError)
                .setRunningMode(runningMode)

        if (runningMode == RunningMode.LIVE_STREAM) {
            optionsBuilder
                .setResultListener(this::returnLivestreamResult)
                .setErrorListener(this::returnLivestreamError)
        }
//
        val options = optionsBuilder.build()
        FaceLandmarker.createFromOptions(context, options)
    }

    private fun returnLivestreamResult(
        result: FaceLandmarkerResult,
        input: MPImage
    ) {
        if( result.faceLandmarks().size > 0 ) {
            val finishTimeMs = SystemClock.uptimeMillis()
            val inferenceTime = finishTimeMs - result.timestampMs()

            faceLandmarkerHelperListener?.onResults(
                ResultBundle(
                    result,
                    inferenceTime,
                    input.height,
                    input.width
                )
            )
        }
        else {
            faceLandmarkerHelperListener?.onEmpty()
        }
    }

    private fun returnLivestreamError(error: RuntimeException) {
        faceLandmarkerHelperListener?.onError(
            error.message ?: "An unknown error has occurred"
        )
    }

    val imageAnalyzer = ImageAnalysis.Analyzer { image ->
        val imageBitmap = image.toBitmap()
        val scale = 500f / max(image.width, image.height)

        val scaleAndRotate = Matrix().apply {
            postScale(scale, scale)
            postRotate(image.imageInfo.rotationDegrees.toFloat())
        }
        val scaleAndRotatedBmp = Bitmap.createBitmap(imageBitmap, 0, 0, image.width, image.height, scaleAndRotate, true)

        Log.v("cameraxdemo", "Received frame for analysis: ${image.width} x ${image.height}")
        image.close()

        faceLandmarker?.detectAsync(BitmapImageBuilder(scaleAndRotatedBmp).build(), System.currentTimeMillis())
    }



    data class ResultBundle(
        val result: FaceLandmarkerResult,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )

    interface LandmarkerListener {
        fun onError(error: String, errorCode: Int = OTHER_ERROR)
        fun onResults(resultBundle: ResultBundle)

        fun onEmpty() {}
    }


}