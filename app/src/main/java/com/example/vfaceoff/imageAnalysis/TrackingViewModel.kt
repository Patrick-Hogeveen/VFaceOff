package com.example.vfaceoff.imageAnalysis

import android.app.Application
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.AndroidViewModel

class TrackingViewModel(application: Application): AndroidViewModel(application) {
    val imageAnalyzer = ImageAnalysis.Analyzer { image ->
        Log.v("cameraxdemo", "Received frame for analysis: ${image.width} x ${image.height}")
        image.close()
    }




}