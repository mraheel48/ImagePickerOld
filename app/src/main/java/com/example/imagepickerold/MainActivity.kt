package com.example.imagepickerold

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.imagepickerold.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.io.FileDescriptor
import java.io.IOException
import java.lang.RuntimeException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val PICK_IMAGE = 1
    private val workerHandler = Handler(Looper.getMainLooper())
    private val workerThread: ExecutorService = Executors.newCachedThreadPool()
    private val parentJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob)

    private val job = SupervisorJob()
    private val ioScope by lazy { CoroutineScope(job + Dispatchers.IO) }


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
                //setImageAsyncTask(uri)
                //exampleMethod(uri)
               workingFine(uri)

            }

        }
    }

    private  fun workingFine(uri: Uri){

        ioScope.launch {
            // New coroutine that can call suspend functions
            val bitmap = scaleDown(fetchData(uri),720f)
            //To Switch the context of Dispatchers
            withContext(Dispatchers.Main) {
                bitmap?.let {
                    binding.imageView.setImageBitmap(it)
                }
            }
        }

    }

    //TODO takes URI of the image and returns bitmap
    private fun uriToBitmap(selectedFileUri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun setImageAsyncTask(uri: Uri) {

        try {

            //val bitmap = getBitmap(uri)
            var bitmap: Bitmap? = null

            coroutineScope.launch {
                bitmap = getOriginalBitmapAsync(uri).await()
            }

            bitmap?.let {
                workerHandler.post {
                    // binding.imageView.setImageURI(uri)
                    binding.imageView.setImageBitmap(it)
                }
            }
        } catch (ex: RuntimeException) {
            ex.printStackTrace()
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

    // 1
    private fun getOriginalBitmapAsync(fileUri: Uri): Deferred<Bitmap> =
        // 2
        coroutineScope.async(Dispatchers.IO) {
            // 3
            /*URL(fileUri).openStream().use {
                return@async BitmapFactory.decodeStream(it)
            }*/

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return@async ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(
                        this@MainActivity.contentResolver,
                        fileUri
                    )
                )
            } else {
                return@async MediaStore.Images.Media.getBitmap(
                    this@MainActivity.contentResolver,
                    fileUri
                )
            }
        }

    private fun sampleOne(fileUri: Uri): Bitmap? {

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

    private fun exampleMethod(fileUri: Uri) {
        // Starts a new coroutine within the scope
        ioScope.launch {
            // New coroutine that can call suspend functions
            val bitmap = fetchData(fileUri)
            //To Switch the context of Dispatchers
            withContext(Dispatchers.Main) {
                bitmap?.let {
                    binding.imageView.setImageBitmap(it)
                }
            }
        }
    }

    private suspend fun exampleMethod2(fileUri: Uri) {

        ioScope.launch {

            val result = ioScope.async(Dispatchers.IO) {
                fetchData(fileUri)
            }
            result.await()

            withContext(Dispatchers.Main) {

            }
        }

    }


    fun fetchData(fileUri: Uri): Bitmap? {

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

    private fun scaleDown(
        realImage: Bitmap?, maxImageSize: Float
    ): Bitmap? {

        if (realImage != null) {

            Log.d("myBitmapVal", "${realImage.width} -- ${realImage.height}")

            return if (realImage.width > maxImageSize || realImage.height > maxImageSize) {

                val ratio: Float = realImage.height.toFloat() / realImage.width.toFloat()
                val width = Constants.screenWidthInPixel
                val height = Constants.screenWidthInPixel * ratio

                Log.d("myNewBitmap", "${ratio}")

                return Bitmap.createScaledBitmap(
                    realImage, width.toInt(),
                    height.toInt(), true
                )

            } else {
                realImage
            }
        } else {
            return null
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        parentJob.cancel()
        job.cancel()
    }


}