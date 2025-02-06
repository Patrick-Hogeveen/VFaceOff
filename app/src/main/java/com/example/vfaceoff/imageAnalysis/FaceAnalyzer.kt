package com.example.vfaceoff.imageAnalysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.vfaceoff.imageAnalysis.TrackingViewModel.LandmarkerListener
import com.example.vfaceoff.imageAnalysis.TrackingViewModel.ResultBundle
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.max

typealias ObjectDetectorCallback = (image: List<TrackingViewModel.ResultBundle>) -> Unit




class FaceAnalyzer (
    private val listener: ObjectDetectorCallback,
    var minFaceDetectionConfidence: Float = DEFAULT_FACE_DETECTION_CONFIDENCE,
    var minFaceTrackingConfidence: Float = DEFAULT_FACE_TRACKING_CONFIDENCE,
    var minFacePresenceConfidence: Float = DEFAULT_FACE_PRESENCE_CONFIDENCE,
    var maxNumFaces: Int = DEFAULT_NUM_FACES,
    var currentDelegate: Int = DELEGATE_CPU,
    var runningMode: RunningMode = RunningMode.LIVE_STREAM,
    val faceLandmarkerHelperListener: LandmarkerListener? = null,
    val context: Context
) : ImageAnalysis.Analyzer {


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

            //landmarkerState.update {
            //    it.copy(
            //        result = result,
            //        inferenceTime = inferenceTime,
            //        inputImageHeight = input.height,
            //        inputImageWidth = input.width,
            //    )
            //}

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

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        if (image.image == null) return
        val imageBitmap = image.toBitmap()
        val scale = 500f / max(image.width, image.height)

        val scaleAndRotate = Matrix().apply {
            postScale(scale, scale)
            postRotate(image.imageInfo.rotationDegrees.toFloat())
        }
        val scaleAndRotatedBmp = Bitmap.createBitmap(imageBitmap, 0, 0, image.width, image.height, scaleAndRotate, true)

        Log.v("cameraxdemo", "Received frame for analysis: ${image.width} x ${image.height}")
        image.close()



        val detectedFaces = faceLandmarker?.detect(BitmapImageBuilder(scaleAndRotatedBmp).build(),/* System.currentTimeMillis()*/)

        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - detectedFaces!!.timestampMs()
        val detectedResults = ResultBundle(
            detectedFaces,
            inferenceTime,
            image.height,
            image.width
        )
        listener(listOf(detectedResults))

    }

}