package com.example.imagepickerold

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.imagepickerold.databinding.ActivityMainBinding
import java.io.FileDescriptor

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imagePicker.setOnClickListener {

            val pickPhoto = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            @Suppress("DEPRECATION")
            startActivityForResult(pickPhoto, 1)
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null && resultCode == RESULT_OK) {

            Toast.makeText(this, "data is not null", Toast.LENGTH_SHORT).show()

            val uri = data.data

            if (uri != null) {

                getBitmap(uri)?.let {
                    binding.imageView.setImageBitmap(it)
                }
            }

        }
    }

    private fun getBitmapFromContentResolver(shareUri: Uri): Bitmap? {
        var bitmap: Bitmap? = null
        try {
            val parcelFileDescriptor: ParcelFileDescriptor =
                contentResolver.openFileDescriptor(shareUri, "r")!!
            val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
            bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
        return bitmap
    }

    @Suppress("DEPRECATION")
    fun getBitmap(fileUri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(this.contentResolver, fileUri))
            } else {
                MediaStore.Images.Media.getBitmap(this.contentResolver, fileUri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}