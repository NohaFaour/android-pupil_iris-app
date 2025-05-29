package com.example.backend_trial

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import android.graphics.BitmapFactory
import android.util.Base64
import org.json.JSONObject
import android.view.View
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONException
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.TimeUnit
import android.graphics.Bitmap
import com.yalantis.ucrop.UCrop
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val PICK_IMAGE = 1
    private val CAPTURE_IMAGE = 2
    private val CAMERA_PERMISSION_CODE = 100
    private val CROP_IMAGE = 3
    private var imageUri: Uri? = null
    private lateinit var imageView: ImageView
    private lateinit var tvResult: TextView
    private lateinit var btnCapture: Button
    private lateinit var btnSelect: Button
    private lateinit var btnProcess: Button
    private lateinit var btnCrop: Button
    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        tvResult = findViewById(R.id.tvResult)
        btnCapture = findViewById(R.id.btnCapture)
        btnSelect = findViewById(R.id.btnSelect)
        btnProcess = findViewById(R.id.btnProcess)
        btnCrop = findViewById(R.id.btnCrop)

        // Initial state: only show capture/select buttons
        imageView.visibility = View.GONE
        tvResult.visibility = View.GONE
        btnProcess.visibility = View.GONE
        btnCrop.visibility = View.GONE
        btnCapture.visibility = View.VISIBLE
        btnSelect.visibility = View.VISIBLE

        btnSelect.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE)
        }

        btnCapture.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
            } else {
                launchCamera()
            }
        }

        btnCrop.setOnClickListener {
            imageUri?.let { uri ->
                startCrop(uri)
            }
        }

        btnProcess.setOnClickListener {
            imageUri?.let {
                tvResult.text = "Result will appear here"
                tvResult.visibility = View.VISIBLE
                uploadImage(it)
            }
        }
    }

    private fun launchCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = createImageFile()
        val photoURI = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            photoFile
        )
        imageUri = photoURI
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        startActivityForResult(intent, CAPTURE_IMAGE)
    }

    private fun startCrop(uri: Uri) {
        val destinationFileName = "cropped_${System.currentTimeMillis()}.jpg"
        val uCropIntent = UCrop.of(uri, Uri.fromFile(File(cacheDir, destinationFileName)))
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(800, 800)
            .start(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                tvResult.text = "Camera permission denied"
                tvResult.visibility = View.VISIBLE
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PICK_IMAGE -> {
                if (resultCode == Activity.RESULT_OK) {
                    imageUri = data?.data
                    Glide.with(this).load(imageUri).into(imageView)
                    imageView.visibility = View.VISIBLE
                    btnCrop.visibility = View.VISIBLE
                    btnProcess.visibility = View.VISIBLE
                    btnCapture.visibility = View.GONE
                    btnSelect.visibility = View.GONE
                    tvResult.visibility = View.GONE
                }
            }
            CAPTURE_IMAGE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val file = File(currentPhotoPath ?: "")
                    if (file.exists() && file.length() > 0) {
                        imageUri = Uri.fromFile(file)
                        Glide.with(this).load(imageUri).into(imageView)
                        imageView.visibility = View.VISIBLE
                        btnCrop.visibility = View.VISIBLE
                        btnProcess.visibility = View.VISIBLE
                        btnCapture.visibility = View.GONE
                        btnSelect.visibility = View.GONE
                        tvResult.visibility = View.GONE
                    } else {
                        tvResult.text = "Failed to capture image"
                        tvResult.visibility = View.VISIBLE
                    }
                }
            }
            UCrop.REQUEST_CROP -> {
                if (resultCode == Activity.RESULT_OK) {
                    val resultUri = UCrop.getOutput(data!!)
                    resultUri?.let {
                        imageUri = it
                        Glide.with(this).load(it).into(imageView)
                    }
                } else if (resultCode == UCrop.RESULT_ERROR) {
                    val cropError = UCrop.getError(data!!)
                    tvResult.text = "Crop error: ${cropError?.message}"
                    tvResult.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun uploadImage(uri: Uri) {
        val file = createTempFileFromUri(uri)
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url("http://192.168.0.111:8000/analyze_iris/") // <-- Replace with your server's IP!
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                runOnUiThread {
                    tvResult.text = "Error: ${e.message}"
                    tvResult.visibility = View.VISIBLE
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string()
                runOnUiThread {
                    try {
                        val json = JSONObject(result)
                        if (json.has("error")) {
                            tvResult.text = "Error: ${json.getString("error")}" 
                        } else {
                            tvResult.text = "IPR: %.3f".format(json.getDouble("ipr"))
                            val base64Image = json.getString("visualization")
                            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            imageView.setImageBitmap(bitmap)
                        }
                        tvResult.visibility = View.VISIBLE
                        imageView.visibility = View.VISIBLE
                        btnProcess.visibility = View.GONE
                    } catch (e: JSONException) {
                        tvResult.text = "Server error: $result"
                        tvResult.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        tvResult.text = "Error parsing result: ${e.message}"
                        tvResult.visibility = View.VISIBLE
                    }
                }
            }
        })
    }

    private fun createTempFileFromUri(uri: Uri): File {
        if (currentPhotoPath != null && uri == Uri.fromFile(File(currentPhotoPath!!)) && File(currentPhotoPath!!).exists()) {
            return File(currentPhotoPath!!)
        }
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        // Resize the bitmap if it's too large
        val maxDim = 800
        val resizedBitmap = if (bitmap != null && (bitmap.width > maxDim || bitmap.height > maxDim)) {
            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val (newWidth, newHeight) = if (aspectRatio > 1) {
                maxDim to (maxDim / aspectRatio).toInt()
            } else {
                (maxDim * aspectRatio).toInt() to maxDim
            }
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
        val file = File(cacheDir, getFileName(uri))
        val outputStream = FileOutputStream(file)
        resizedBitmap?.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.close()
        return file
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File = cacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "temp_image"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }
}