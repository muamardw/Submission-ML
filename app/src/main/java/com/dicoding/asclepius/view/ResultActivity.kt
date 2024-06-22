package com.dicoding.asclepius.view

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.dicoding.asclepius.databinding.ActivityResultBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import com.dicoding.asclepius.history.HistoryDatabase
import com.dicoding.asclepius.history.HistoryPrediction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.io.File
import java.io.FileOutputStream

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    companion object {
        const val EXTRA_IMAGE_URI = "img_uri"
        const val TAG = "imagePicker"
        const val RESULT_TEXT = "result_text"
        const val REQUEST_HISTORY_UPDATE = 1

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI) ?: ""
        if (imageUriString.isNotEmpty()) {
            val imageUri = Uri.parse(imageUriString)
            displayImage(imageUri)

            val imageClassifierHelper = ImageClassifierHelper(
                context = this,
                classifierListener = object : ImageClassifierHelper.ClassifierListener {
                    override fun onError(errorMessage: String) {
                        Log.d(TAG, "Error: $errorMessage")
                    }

                    override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
                        results?.let { showResults(it) }
                    }
                }
            )
            imageClassifierHelper.classifyImage(imageUri)
        } else {
            Log.e(TAG, "No image URI provided")
            finish()
        }

        binding.buttonSave.setOnClickListener {
            val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI) ?: ""
            val result = binding.resultText.text.toString()
            if (imageUriString.isNotEmpty()) {
                val imageUri = Uri.parse(imageUriString)
                showToast("Data saved")
                savePredictionToDatabase(imageUri, result)
            } else {
                showToast("No image URI provided")
                finish()
            }
        }
    }

    private fun displayImage(uri: Uri) {
        Log.d(TAG, "Displaying image: $uri")
        binding.resultImage.setImageURI(uri)
    }

    private fun showResults(results: List<Classifications>) {
        val firstResult = results.first()
        val label = firstResult.categories.first().label
        val score = firstResult.categories.first().score

        fun Float.formatToString(): String {
            return String.format("%.2f%%", this * 100)
        }
        binding.resultText.text = "$label ${score.formatToString()}"

    }

    private fun savePredictionToDatabase(imageUri: Uri, result: String) {
        if (result.isNotEmpty()) {
            val fileName = "cropped_image_${System.currentTimeMillis()}.jpg"
            val destinationFile = File(cacheDir, fileName)
            val destinationUri = Uri.fromFile(destinationFile)
            try {
                contentResolver.openInputStream(imageUri)?.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val prediction = HistoryPrediction(imagePath = destinationUri.toString(), result = result)
                lifecycleScope.launch(Dispatchers.IO) {
                    val database = HistoryDatabase.getInstance(applicationContext)
                    try {
                        database.predictionHistoryDao().insertHistories(prediction)
                        Log.d(TAG, "Prediction saved successfully: $prediction")
                        val predictions = database.predictionHistoryDao().getAllHistories()
                        Log.d(TAG, "All predictions after save: $predictions")
                        gotoHistory(destinationUri, result)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to save prediction: $prediction", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save prediction due to exception: $e")
            }
        } else {
            Log.e(TAG, "Result is empty, cannot save prediction to database.")
        }

    }

    private fun gotoHistory(imageUri: Uri, result: String) {
        val intent = Intent(this, HistoryActivity::class.java).apply {
            putExtra(RESULT_TEXT, result)
            putExtra(EXTRA_IMAGE_URI, imageUri.toString())
        }
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}