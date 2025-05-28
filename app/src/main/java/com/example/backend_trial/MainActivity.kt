package com.example.backend_trial

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

class MainActivity : AppCompatActivity() {
    private val PICK_IMAGE = 1
    private var imageUri: Uri? = null
    private lateinit var imageView: ImageView
    private lateinit var tvResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        tvResult = findViewById(R.id.tvResult)

        findViewById<Button>(R.id.btnSelect).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE)
        }

        findViewById<Button>(R.id.btnUpload).setOnClickListener {
            imageUri?.let { uploadImage(it) }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            Glide.with(this).load(imageUri).into(imageView)
        }
    }

    private fun uploadImage(uri: Uri) {
        val file = createTempFileFromUri(uri)
        val client = OkHttpClient()
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
                runOnUiThread { tvResult.text = "Error: ${e.message}" }
            }
            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string()
                runOnUiThread {
                    try {
                        val json = JSONObject(result)
                        if (json.has("error")) {
                            tvResult.text = "Error: ${json.getString("error")}" 
                        } else {
                            tvResult.text = "IPR: ${json.getDouble("ipr")}" 
                            val base64Image = json.getString("visualization")
                            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            imageView.setImageBitmap(bitmap)
                        }
                    } catch (e: Exception) {
                        tvResult.text = "Error parsing result: ${e.message}"
                    }
                }
            }
        })
    }

    private fun createTempFileFromUri(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, getFileName(uri))
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file
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