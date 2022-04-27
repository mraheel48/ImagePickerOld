package com.example.imagepickerold

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.imagepickerold.databinding.ActivityMainBinding
import java.io.FileDescriptor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    val PICK_IMAGE = 1
    private val workerHandler = Handler(Looper.getMainLooper())
    private val workerThread: ExecutorService = Executors.newCachedThreadPool()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imagePicker.setOnClickListener {
            threeWayToPickerUpImage()
        }
    }

    //But in some devices, while setting the intent type, the above solution will clear the intent data (MediaStore.Images.Media.EXTERNAL_CONTENT_URI) which could hinder the gallery opening process.
    @Suppress("DEPRECATION")
    private fun oneWayToPickerUpImage() {
        val pickPhoto = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(pickPhoto, PICK_IMAGE)
    }

    //But in some devices, the above solution will not fetch the image with EXIF information such as orientation. So in those devices, EXIF processing such as changing image orientation could not be performed as expected.
    @Suppress("DEPRECATION")
    private fun twoWayToPickerUpImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE)
    }

    //Finally, I suggest the below code which allows user to select an image from any gallery application which does not cause any problem and does not show up any warning
    @Suppress("DEPRECATION")
    private fun threeWayToPickerUpImage() {
        val intent = Intent()
        intent.action = Intent.ACTION_PICK
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        startActivityForResult(intent, PICK_IMAGE)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null && resultCode == RESULT_OK && requestCode == PICK_IMAGE) {

            Toast.makeText(this, "data is not null", Toast.LENGTH_SHORT).show()

            val uri = data.data

            if (uri != null) {
                setImageAsyncTask(uri)
            }

        }
    }

    private fun setImageAsyncTask(uri: Uri) {

        workerThread.execute {
            val bitmap = getBitmap(uri)
            bitmap?.let {
                workerHandler.post {
                    binding.imageView.setImageBitmap(it)
                }
            }
        }

        /*workerHandler.post {
            getBitmap(uri)?.let {
                binding.imageView.setImageBitmap(it)
            }
        }*/
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