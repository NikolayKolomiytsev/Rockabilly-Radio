package nk.rockabillyradio.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.os.Vibrator
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wang.avi.AVLoadingIndicatorView
import io.github.nikhilbhutani.analyzer.DataAnalyzer
import nk.rockabillyradio.Const
import nk.rockabillyradio.Const.ACTION
import nk.rockabillyradio.R
import nk.rockabillyradio.audio.Player
import nk.rockabillyradio.connections.GetTrackInfo
import nk.rockabillyradio.notifications.NotificationService
import nk.rockabillyradio.views.CircularSeekBar
import nk.rockabillyradio.views.CircularSeekBar.OnCircularSeekBarChangeListener

class MainActivity : AppCompatActivity() {

    private val PREFERENCE_FILE_KEY = "Preference"
    var sharedPref: SharedPreferences? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        activity = this
        sharedPref = getSharedPreferences(
                PREFERENCE_FILE_KEY, Context.MODE_PRIVATE)
        try {
            app = this.packageManager.getApplicationInfo("nk.rockabillyradio", 0)
        } catch (e: PackageManager.NameNotFoundException) {
            val toast = Toast.makeText(this, "error in getting icon", Toast.LENGTH_SHORT)
            toast.show()
            e.printStackTrace()
        }
        initialise()
        setCustomFont()
        startListenVolume()
        GetTrackInfo().execute()
        // Sorry for this, i don't have this radio API :c
        startRefreshing()
    }

    // Initialise all views, animations and set click listeners
    fun initialise() {
        background = findViewById(R.id.bckg)
        title_tv = findViewById(R.id.title_tv)
        track_tv = findViewById(R.id.track_tv)
        artist_tv = findViewById(R.id.artist_tv)
        volumeChanger = findViewById(R.id.circularSeekBar1)
        playing_animation = findViewById(R.id.playing_anim)
        playing_animation!!.visibility = View.GONE
        loading_animation = findViewById(R.id.load_animation)
        control_button = findViewById(R.id.control_button)
        control_button!!.setOnClickListener(controlButtonListener)
    }

    // Function for set custom font to title text view in toolbar
    fun setCustomFont() {
        val tf = Typeface.createFromAsset(this.assets, "radio.ttf")
        title_tv!!.typeface = tf
    }

    // Function for listen and change volume from seek bar to player
    fun startListenVolume() {
        Player.setVolume((100 - volumeChanger!!.progress) / 100f)
        volumeChanger!!.setOnSeekBarChangeListener(object : OnCircularSeekBarChangeListener {
            override fun onProgressChanged(circularSeekBar: CircularSeekBar, progress: Int, fromUser: Boolean) {
                Player.setVolume((100 - circularSeekBar.progress) / 100f)
            }

            override fun onStopTrackingTouch(seekBar: CircularSeekBar?) {}
            override fun onStartTrackingTouch(seekBar: CircularSeekBar?) {}
        })
    }

    // Service for background audio playing
    fun startPlayerService() {
        val serviceIntent = Intent(this@MainActivity, NotificationService::class.java)
        serviceIntent.action = ACTION.STARTFOREGROUND_ACTION
        startService(serviceIntent)
    }

    // Vibrate when click on control button
    fun vibrate() {
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(Const.VIBRATE_TIME.toLong())
    }

    fun startRefreshing() {
        dataAnalyzer = DataAnalyzer(this)
        val t: Thread = object : Thread() {
            override fun run() {
                try {
                    while (!isInterrupted) {
                        sleep(Const.PHOTO_LOAD_REFRESH_TIME.toLong())
                        runOnUiThread { GetTrackInfo().execute() }
                    }
                } catch (e: InterruptedException) {
                }
            }
        }
        t.start()
    }

    var controlButtonListener = View.OnClickListener {
        if (controlIsActivated == false) {
            startPlayerService()
            control_button!!.setImageResource(R.drawable.pause)
            playing_animation!!.visibility = View.GONE
            loading_animation!!.visibility = View.VISIBLE
            control_button!!.visibility = View.GONE
            controlIsActivated = true
            vibrate()
        } else {
            Player.stop()
            control_button!!.setImageResource(R.drawable.play)
            playing_animation!!.visibility = View.GONE
            loading_animation!!.visibility = View.VISIBLE
            control_button!!.visibility = View.VISIBLE
            controlIsActivated = false
            vibrate()
        }
    }

    override fun onBackPressed() {
        Player.stop()
        super.onBackPressed()
    }

    companion object {
        var activity: Activity? = null
        // Screen background photo
        var background: ImageView? = null
        //Strings for showing song data and detecting old album photo
        var artist: String? = null
        var track: String? = null
        var album: String? = null
        var albumOld: String? = null
        var title_tv: TextView? = null
        var artist_tv: TextView? = null
        var track_tv: TextView? = null
        var data_tv: TextView? = null
        var volumeChanger: CircularSeekBar? = null
        // Animation on bottom of the screen when stream is loaded
        var playing_animation: AVLoadingIndicatorView? = null
        // Stram loading animation on the center of screen
        var loading_animation: AVLoadingIndicatorView? = null
        // Button for start/stop playing audio
        var control_button: ImageButton? = null
        // Boolean for check if play/pause button is activated
        var controlIsActivated = false
        var dataAnalyzer: DataAnalyzer? = null
        var app: ApplicationInfo? = null
        // Function to set track and artist names to text views and change background photo
        fun setSongData() {
            artist_tv!!.text = artist
            track_tv!!.text = track
        }
    }
}