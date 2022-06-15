package com.example.imagepickerold

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.imagepickerold.databinding.ActivityMainBinding
import com.example.imagepickerold.utils.StoreManager
import kotlinx.coroutines.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private lateinit var binding: ActivityMainBinding
    private val PICK_IMAGE = 123123
    private val CAMERA_IMAGE = 123
    private val workerHandler = Handler(Looper.getMainLooper())
    private val workerThread: ExecutorService = Executors.newCachedThreadPool()
    private val parentJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob)

    private val job = SupervisorJob()
    private val ioScope by lazy { CoroutineScope(job + Dispatchers.IO) }

    private var cameraStatus: Boolean = false

    private val readPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(
            Manifest.permission.ACCESS_MEDIA_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    } else {
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    private val cameraPermission = arrayOf(Manifest.permission.CAMERA)

    private var screenWidth: Float = 720f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val viewTreeObserver = binding.rootLayout.viewTreeObserver

        if (viewTreeObserver!!.isAlive) {

            viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    binding.rootLayout.viewTreeObserver?.removeOnGlobalLayoutListener(this)

                    Log.d("myWidth", "${binding.rootLayout.width} -- ${binding.rootLayout.height}")

                    screenWidth = binding.rootLayout.width.toFloat()

                }
            })
        }


        binding.imagePicker.setOnClickListener {
            //threeWayToPickerUpImage()
            cameraStatus = false
            StoreManager.setCurrentCropedBitmap(this, null)
            StoreManager.setCurrentCroppedMaskBitmap(this, null)
            openGallery()
        }

        binding.btnCamera.setOnClickListener {
            cameraStatus = true
            StoreManager.setCurrentCropedBitmap(this, null)
            StoreManager.setCurrentCroppedMaskBitmap(this, null)
            openCamera()
        }


    }

    private fun openGallery() {
        if (EasyPermissions.hasPermissions(this, *readPermission)) {
            openGalleryNewWay()
        } else {
            EasyPermissions.requestPermissions(
                this, "We need permissions because this and that",
                PICK_IMAGE, *readPermission
            )
        }
    }

    private fun openCamera() {

        if (EasyPermissions.hasPermissions(this, *cameraPermission)) {
            openCameraNewWay()
            //registerTakePictureLauncher(initTempUri())
        } else {
            EasyPermissions.requestPermissions(
                this, "We need permissions because this and that",
                CAMERA_IMAGE, *cameraPermission
            )
        }
    }

    private fun initTempUri(): Uri? {
        //gets the temp_images dir
        val tempImagesDir = File(
            applicationContext.filesDir, //this function gets the external cache dir
            getString(R.string.temp_images_dir)
        ) //gets the directory for the temporary images dir

        tempImagesDir.mkdir() //Create the temp_images dir

        //Creates the temp_image.jpg file
        val tempImage = File(
            tempImagesDir, //prefix the new abstract path with the temporary images dir path
            getString(R.string.temp_image)
        ) //gets the abstract temp_image file name

        return if (Build.VERSION.SDK_INT >= 24) {
            FileProvider.getUriForFile(
                applicationContext,
                getString(R.string.authorities),
                tempImage
            )
        } else {
            Uri.fromFile(tempImage)
        }


        //Returns the Uri object to be used with ActivityResultLauncher
        /*return FileProvider.getUriForFile(
            applicationContext,
            getString(R.string.authorities),
            tempImage
        )*/
    }

    private fun registerTakePictureLauncher(path: Uri) {

        //Creates the ActivityResultLauncher
        val resultLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) {
            binding.imageView.setImageURI(null) //rough handling of image changes. Real code need to handle different API levels.
            binding.imageView.setImageURI(path)
        }

        resultLauncher.launch(path) //launches the activity here

    }

    var cam_uri: Uri? = null

    private fun openCameraNewWay() {
        /* val values = ContentValues()
         values.put(MediaStore.Images.Media.TITLE, "New Picture")
         values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
         cam_uri = contentResolver.insert(
             MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
             values
         )
         val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
         cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cam_uri)*/
        cam_uri = initTempUri()
        cam_uri?.let {
            takePicture.launch(it)
        }
    }


    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->

            // The image was saved into the given Uri -> do something with it
            if (success) {

                if (cam_uri != null) {
                    // val bitmap = scaleDown(getBitmap(cam_uri!!), screenWidth)
                    val bitmap = scaleDown(getBitmap(cam_uri!!), screenWidth)
                    // val bitmap = getBitmap(cam_uri!!)
                    if (bitmap != null) {
                        Constants.mainBitmap = bitmap
                        StoreManager.setCurrentOriginalBitmap(this, bitmap)
                        startActivity(Intent(this, BackgroundRemover::class.java))
                    }
                } else {
                    Log.d("myCameraImage", "image is not save")
                }

            }
        }

    private val cameraIntentNew = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {

            if (cam_uri != null) {
                Log.d("myCameraImage", "image is save")
                val bitmap = getBitmap(cam_uri!!)
                if (bitmap != null) {
                    Constants.mainBitmap = bitmap
                    StoreManager.setCurrentOriginalBitmap(this, bitmap)
                    startActivity(Intent(this, SecondScreen::class.java))
                }
                //binding.imageView.setImageBitmap(bitmap!!)
            } else {
                Log.d("myCameraImage", "image is not save")
            }
        }
    }

    // Receiver
    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                val value = it.data?.data

                if (value != null) {
                    //val bitmap = scaleDown(getBitmap(value), screenWidth)
                    val bitmap = com.example.imagepickerold.utils.Constants.getBitmapFromUri(
                        this,
                        value, screenWidth, screenWidth
                    )

                    if (bitmap != null) {
                        Constants.mainBitmap = bitmap
                        StoreManager.setCurrentOriginalBitmap(this, bitmap)
                        startActivity(Intent(this, SecondScreen::class.java))
                    }
                }

                Log.d("myData", "${value}")

            }
        }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun openGalleryNewWay() {
        val intent = Intent()
        intent.action = Intent.ACTION_PICK
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        getResult.launch(intent)
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

    private fun workingFine(uri: Uri) {

        ioScope.launch {
            // New coroutine that can call suspend functions
            val bitmap = scaleDown(fetchData(uri), screenWidth)
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

            Log.d(
                "myBitmapVal",
                "${realImage.width} -- ${realImage.height} -- max width ${maxImageSize}"
            )

            return if (realImage.width > maxImageSize || realImage.height > maxImageSize) {

                val ratio: Float = realImage.height.toFloat() / realImage.width.toFloat()
                val width = maxImageSize
                val height = maxImageSize * ratio

                Log.d("myNewBitmap", "$ratio")

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

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {

        Log.d("myPermission", "allow")

        if (cameraStatus) {
            Log.d("onPermissionsGranted", "open Camera")
            openCameraNewWay()
        } else {
            Log.d("onPermissionsGranted", "open Gallery")
            openGalleryNewWay()
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Log.d("myPermission", "not allow")
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        }
    }


}