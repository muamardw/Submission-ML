package com.dicoding.asclepius.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import com.dicoding.asclepius.R
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.lang.IllegalStateException

@Suppress("DEPRECATION")
class ImageClassifierHelper(
    var threshold: Float = 0.1f,
    var maxResults: Int = 3,
    val modelName: String = "cancer_classification.tflite",
    val context: Context,
    val classifierListener: ClassifierListener?
){

    private var imageClassifier: ImageClassifier? = null
    interface ClassifierListener {
        fun onError(errorMsg: String)
        fun onResults(
            results: List<org.tensorflow.lite.task.vision.classifier.Classifications>?,
            inferenceTime: Long
        )
    }

    init {
        setupImageClassifier()
    }

    private fun setupImageClassifier() {
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResults)
        val baseOptionBuilder = BaseOptions.builder()
            .setNumThreads(4)

        optionsBuilder.setBaseOptions(baseOptionBuilder.build())
        imageClassifier = try {
            ImageClassifier.createFromFileAndOptions(context, modelName, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            classifierListener?.onError(context.getString(R.string.image_classifier_failed))
            Log.e(TAG, e.message.toString())
            null
        }
    }

    fun classifyImage(imageUri: Uri) {
        imageClassifier?.let {
            val bitmap = getImageBitmap(imageUri)
            val tensorImage = preprocessImage(bitmap)
            val results = performInference(tensorImage)
            notifyResults(results)
        } ?: setupImageClassifier()
    }

    private fun getImageBitmap(imageUri: Uri): Bitmap {
        val bitmap = when {
            checkVersion() -> {
                val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source)
            }
            else -> MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        }.copy(Bitmap.Config.ARGB_8888, true)
        return bitmap
    }

    private fun checkVersion(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    }

    private fun performInference(tensorImage: TensorImage): List<org.tensorflow.lite.task.vision.classifier.Classifications>? {
        var inferenceTime = SystemClock.uptimeMillis()
        val results = imageClassifier?.classify(tensorImage)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        classifierListener?.onResults(results, inferenceTime)
        return results
    }

    private fun preprocessImage(bitmap: Bitmap): TensorImage {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(CastOp(DataType.UINT8))
            .build()
        return imageProcessor.process(TensorImage.fromBitmap(bitmap))
    }

    private fun notifyResults(results: List<org.tensorflow.lite.task.vision.classifier.Classifications>?) {
        classifierListener?.onResults(results, 0)
    }

    companion object {
        private const val TAG = "ImageClassifierHelper"
    }
}