package com.example.backend_trial

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.yalantis.ucrop.UCrop
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class GalleryFragment : Fragment() {
    private val PICK_IMAGE = 1
    private var imageUri: Uri? = null

    private lateinit var imageView: ImageView
    private lateinit var tvResult: TextView
    private lateinit var btnSelect: Button
    private lateinit var btnCrop: Button
    private lateinit var btnProcess: Button
    private lateinit var btnRetry: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_gallery, container, false)
        imageView = view.findViewById(R.id.imageView)
        tvResult = view.findViewById(R.id.tvResult)
        btnSelect = view.findViewById(R.id.btnSelect)
        btnCrop = view.findViewById(R.id.btnCrop)
        btnProcess = view.findViewById(R.id.btnProcess)
        btnRetry = view.findViewById(R.id.btnRetry)

        btnSelect.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE)
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
                btnProcess.visibility = View.GONE
                btnCrop.visibility = View.GONE
                uploadImage(it)
            }
        }

        btnRetry.setOnClickListener {
            resetToInitialState()
        }

        resetToInitialState()
        return view
    }

    private fun startCrop(uri: Uri) {
        val destinationFileName = "cropped_${System.currentTimeMillis()}.jpg"
        UCrop.of(uri, Uri.fromFile(File(requireContext().cacheDir, destinationFileName)))
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(800, 800)
            .start(requireContext(), this)
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
                    btnRetry.visibility = View.GONE
                    btnSelect.visibility = View.GONE
                    tvResult.visibility = View.GONE
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
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .build()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url(ApiConfig.BASE_URL + ApiConfig.ANALYZE_IRIS_ENDPOINT)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    tvResult.text = "Error: ${e.message}"
                    tvResult.visibility = View.VISIBLE
                    btnRetry.visibility = View.VISIBLE
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string()
                requireActivity().runOnUiThread {
                    try {
                        val json = JSONObject(result)
                        if (json.has("error")) {
                            tvResult.text = "Error: ${json.getString("error")}" 
                        } else {
                            tvResult.text = "IPR: %.3f".format(json.getDouble("ipr"))
                            val base64Image = json.getString("visualization")
                            val imageBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            imageView.setImageBitmap(bitmap)
                        }
                        tvResult.visibility = View.VISIBLE
                        imageView.visibility = View.VISIBLE
                        btnRetry.visibility = View.VISIBLE
                    } catch (e: JSONException) {
                        tvResult.text = "Server error: $result"
                        tvResult.visibility = View.VISIBLE
                        btnRetry.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        tvResult.text = "Error parsing result: ${e.message}"
                        tvResult.visibility = View.VISIBLE
                        btnRetry.visibility = View.VISIBLE
                    }
                }
            }
        })
    }

    private fun createTempFileFromUri(uri: Uri): File {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
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
        val file = File(requireContext().cacheDir, getFileName(uri))
        val outputStream = FileOutputStream(file)
        resizedBitmap?.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.close()
        return file
    }

    private fun getFileName(uri: Uri): String {
        var name = "temp_image"
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
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

    private fun resetToInitialState() {
        imageView.setImageDrawable(null)
        imageView.visibility = View.GONE
        tvResult.text = "Result will appear here"
        tvResult.visibility = View.GONE
        btnProcess.visibility = View.GONE
        btnCrop.visibility = View.GONE
        btnRetry.visibility = View.GONE
        btnSelect.visibility = View.VISIBLE
        imageUri = null
    }
} 