package com.rudolas.doomfire2

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.view.View
import android.view.Window
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_fullscreen.*
import java.util.*

/**
 * An example full-screen activity that shows cool Doom Fire animation
 * for portrait or landscape mode
 */
class FullscreenActivity : AppCompatActivity() {
    private val mHidePart2Runnable = Runnable {
        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreen_content.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    private var changeTime: Long = 0
    private var fireWidth = 100
    private var fireHeight = 100
    private var numberOfPixels = 100
    private var isFired = true
    private var isPortrait = true

    private lateinit var firePixels: IntArray
    private lateinit var firePixelColors: IntArray
    private lateinit var bitmap: Bitmap

    private var mVisible: Boolean = false

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        logMsg("onCreate")
        setContentView(R.layout.activity_fullscreen)

        mVisible = true
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        logMsg("onPostCreate")
        fullscreen_content.postDelayed(
            {
                isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                logMsg("SCREEN ${if (isPortrait) "PORTRAIT" else "LANDSCAPE"}")
                val width = fullscreen_content.measuredWidth / (if (isPortrait) 10 else 16)  // 10
                fireWidth = width
                val height = fullscreen_content.measuredHeight / (if (isPortrait) 6 else 8)  // 6
                fireHeight = height
                numberOfPixels = width * height
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                firePixels = IntArray(numberOfPixels) { 0 }
                firePixelColors = IntArray(numberOfPixels) { fireColorsPalette[0] }

                fullscreen_content.setImageBitmap(bitmap)
                fullscreen_content.setOnClickListener { toggle() }

                AsyncTask.execute {
                    changeTime = SystemClock.uptimeMillis()
                    Timer().scheduleAtFixedRate(object : TimerTask() {
                        override fun run() {
                            createFireSource()
                            calculateFirePropagation()
                            runOnUiThread {
                                bitmap.setPixels(firePixelColors, 0, fireWidth, 0, 0, fireWidth, fireHeight)
                                fullscreen_content.invalidate()
                            }
                        }
                    }, 1, 1)
                }
            }, 800
        )
    }

    override fun onStart() {
        super.onStart()
        logMsg("onStart")
    }

    override fun onResume() {
        super.onResume()
        logMsg("onResume")
        mHidePart2Runnable.run()
    }

    private fun calculateFirePropagation() {

        val pixelsCount = numberOfPixels
        val step = if (isPortrait) 1.33 else 1.5
        val initValue = if (isPortrait) fireHeight / 2 else 0
        for (row in initValue until fireHeight) {
            val pixelOffset = fireWidth * row
            for (column in 0 until fireWidth) {
                val currentPixelIndex = column + pixelOffset
                val bellowPixelIndex = currentPixelIndex + fireWidth

                if (bellowPixelIndex >= pixelsCount) {
                    break
                }

                val decay = Math.floor(Random().nextDouble() * step).toInt()  // 1.33

                val bellowPixelFireIntensity = firePixels[bellowPixelIndex]
                val intensity = bellowPixelFireIntensity - decay
                val newFireIntensity = if (intensity > 0) intensity else 0

                val posChange = (currentPixelIndex - .005 * decay).toInt()
                val pos = if (posChange >= 0) posChange else currentPixelIndex

                firePixels[pos] = newFireIntensity
                firePixelColors[pos] = fireColorsPalette[newFireIntensity]
            }
        }
    }

    private fun toggle() {
        changeTime = SystemClock.uptimeMillis()
        if (isFired) {
            clearFireSource()
        } else {
            createFireSource()
        }
        isFired = !isFired
    }

    private fun createFireSource() {
        val actColor = firePixels.last()
        if (isFired && actColor < 36) {
            val diffTime = SystemClock.uptimeMillis() - changeTime
//            logMsg("Act color $actColor $diffTime")
            val newColor = (diffTime / 80).toInt()
            val overFlowFireIndex = numberOfPixels
            for (column in 0 until fireWidth) {
                firePixels[(overFlowFireIndex - fireWidth) + column] = newColor
            }
        }
    }

    private fun clearFireSource() {
        val overFlowFireIndex = fireWidth * fireHeight
        for (column in 0 until fireWidth) {
            firePixels[(overFlowFireIndex - fireWidth) + column] = 0
        }
    }

    companion object {

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300

        private val fireColorsPalette = arrayOf(
            Color.rgb(7, 7, 7),
            Color.rgb(31, 7, 7),
            Color.rgb(47, 15, 7),
            Color.rgb(71, 15, 7),
            Color.rgb(87, 23, 7),
            Color.rgb(103, 31, 7),
            Color.rgb(119, 31, 7),
            Color.rgb(143, 39, 7),
            Color.rgb(159, 47, 7),
            Color.rgb(175, 63, 7),
            Color.rgb(191, 71, 7),
            Color.rgb(199, 71, 7),
            Color.rgb(223, 79, 7),
            Color.rgb(223, 87, 7),
            Color.rgb(223, 87, 7),
            Color.rgb(215, 95, 7),
            Color.rgb(215, 95, 7),
            Color.rgb(215, 95, 7),
            Color.rgb(215, 103, 15),
            Color.rgb(207, 111, 15),
            Color.rgb(207, 119, 15),
            Color.rgb(207, 127, 15),
            Color.rgb(207, 135, 23),
            Color.rgb(199, 135, 23),
            Color.rgb(199, 143, 23),
            Color.rgb(199, 151, 31),
            Color.rgb(191, 159, 31),
            Color.rgb(191, 159, 31),
            Color.rgb(191, 167, 39),
            Color.rgb(191, 167, 39),
            Color.rgb(191, 175, 47),
            Color.rgb(183, 175, 47),
            Color.rgb(183, 183, 47),
            Color.rgb(183, 183, 55),
            Color.rgb(207, 207, 111),
            Color.rgb(223, 223, 159),
            Color.rgb(239, 239, 199),
            Color.rgb(255, 255, 255)
        )
    }

    private fun logMsg(msg: String) = android.util.Log.d("FullscreenActivity", msg)
}
