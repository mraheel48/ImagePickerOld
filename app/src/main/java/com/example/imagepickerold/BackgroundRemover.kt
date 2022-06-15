package com.example.imagepickerold

import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import com.example.imagepickerold.asyncWorking.MLCropAsyncTask
import com.example.imagepickerold.databinding.ActivityBackgroundRemoverBinding
import com.example.imagepickerold.utils.ImageUtils
import com.example.imagepickerold.utils.StoreManager
import com.example.imagepickerold.utils.Util
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BackgroundRemover : AppCompatActivity() {

    val workerThread: ExecutorService = Executors.newCachedThreadPool()
    val workerHandler = Handler(Looper.getMainLooper())

    var faceBitmap: Bitmap? = null

    var mCount = 0
    private var cutBit: Bitmap? = null

    lateinit var binding: ActivityBackgroundRemoverBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackgroundRemoverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cropProgressBar.visibility = View.VISIBLE

        /*Constants.mainBitmap = ContextCompat.getDrawable(this, R.drawable.n)?.let { it1 ->
            Util.drawableToBitmap(
                it1
            )
        }*/



        workerHandler.postDelayed({

            if (Constants.mainBitmap != null) {

               /* val imageUri: Uri? = getImageUri(Constants.mainBitmap!!)
                StoreManager.setCurrentCropedBitmap(this, null)
                StoreManager.setCurrentCroppedMaskBitmap(this, null)
                val bitmapFromUri =
                    com.example.imagepickerold.utils.Constants.getBitmapFromUri(
                        this,
                        imageUri,
                        720f,
                        720f
                    )

                Constants.mainBitmap = bitmapFromUri
                StoreManager.setCurrentOriginalBitmap(this, bitmapFromUri)

                settingBitmap(Constants.mainBitmap!!)*/

                StoreManager.setCurrentCropedBitmap(this, null)
                StoreManager.setCurrentCroppedMaskBitmap(this, null)

                settingBitmap(Constants.mainBitmap!!)

            } else {
                Util.showToast(this, "Bitmap is null")
            }
        }, 2000)


    }

    fun getImageUri(bitmap: Bitmap): Uri? {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, ByteArrayOutputStream())
        val contentResolver = contentResolver
        return Uri.parse(
            MediaStore.Images.Media.insertImage(
                contentResolver,
                bitmap,
                "IMG_" + System.currentTimeMillis(),
                null as String?
            )
        )
    }

    private fun settingBitmap(bitmap: Bitmap) {

        faceBitmap = ImageUtils.getBitmapResize(
            bitmap,
            binding.objectImage.width,
            binding.objectImage.height
        )

        workerHandler.post {
            binding.rootLayout.layoutParams = FrameLayout.LayoutParams(
                faceBitmap!!.width,
                faceBitmap!!.height
            )

            cutmaskNew()
        }

    }

    private fun cutmaskNew() {

        binding.cropProgressBar.visibility = View.GONE

        object : CountDownTimer(5000, 1000) {
            override fun onFinish() {}
            override fun onTick(j: Long) {
                mCount++
                if (binding.cropProgressBar.progress <= 90) {
                    binding.cropProgressBar.progress = mCount * 5
                }
            }
        }.start()

        MLCropAsyncTask({ bitmap, bitmap2, i, i2 ->

            val width: Int = faceBitmap!!.getWidth()
            val height: Int = faceBitmap!!.getHeight()
            val i3 = width * height
            faceBitmap!!.getPixels(IntArray(i3), 0, width, 0, 0, width, height)
            val iArr = IntArray(i3)
            val createBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            createBitmap.setPixels(iArr, 0, width, 0, 0, width, height)
            cutBit = ImageUtils.getMask(faceBitmap, createBitmap, width, height)
            cutBit = Bitmap.createScaledBitmap(
                bitmap!!,
                cutBit!!.getWidth(),
                cutBit!!.getHeight(),
                false
            )
            runOnUiThread(Runnable {
                if (Palette.from(cutBit!!).generate().dominantSwatch == null) {
                    Toast.makeText(
                        this,
                        " this@MainActivity.getString(R.string.txt_not_detect_human)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                binding.objectImage.setImageBitmap(cutBit)
            })
        }, this, binding.cropProgressBar).execute()

    }
}