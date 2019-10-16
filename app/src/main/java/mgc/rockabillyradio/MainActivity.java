package mgc.rockabillyradio;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.wang.avi.AVLoadingIndicatorView;

import java.text.DecimalFormat;

import io.github.nikhilbhutani.analyzer.DataAnalyzer;
import mgc.rockabillyradio.audio.Player;
import mgc.rockabillyradio.connections.GetTrackInfo;
import mgc.rockabillyradio.notifications.NotificationService;

public class MainActivity extends AppCompatActivity {

    public static Activity activity;

    // Screen background photo
    public static ImageView background;

    //Strings for showing song data and detecting old album photo
    public static String artist, track, album, albumOld;

    public static TextView title_tv, artist_tv, track_tv, data_tv;
    public static CircularSeekBar volumeChanger;

    // Animation on bottom of the screen when stream is loaded
    public static AVLoadingIndicatorView playing_animation;

    // Stram loading animation on the center of screen
    public static AVLoadingIndicatorView loading_animation;

    // Button for start/stop playing audio
    public static ImageButton control_button;

    // Boolean for check if play/pause button is activated
    public static boolean controlIsActivated = false;
    public static DataAnalyzer dataAnalyzer;
    public static ApplicationInfo app;
    private final String PREFERENCE_FILE_KEY = "Preference";
    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;
        sharedPref = getSharedPreferences(
                PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
        try {
            app = this.getPackageManager().getApplicationInfo("mgc.rockabillyradio", 0);
        } catch (PackageManager.NameNotFoundException e) {
            Toast toast = Toast.makeText(this, "error in getting icon", Toast.LENGTH_SHORT);
            toast.show();
            e.printStackTrace();
        }
        initialise();
        setCustomFont();
        startListenVolume();
        new GetTrackInfo().execute();
        startRefreshing();
        startCounting();

        if (sharedPref.getBoolean("Show", true)) {
            showHelloWorldDialog();
        }
    }

    // Initialise all views, animations and set click listeners
    void initialise() {
        background = findViewById(R.id.bckg);
        title_tv = findViewById(R.id.title_tv);
        track_tv = findViewById(R.id.track_tv);
        artist_tv = findViewById(R.id.artist_tv);
        volumeChanger = findViewById(R.id.circularSeekBar1);
        playing_animation = findViewById(R.id.playing_anim);
        playing_animation.setVisibility(View.GONE);
        loading_animation = findViewById(R.id.load_animation);
        control_button = findViewById(R.id.control_button);
        control_button.setOnClickListener(controlButtonListener);
        data_tv = findViewById(R.id.data_tv);
        data_tv.setVisibility(View.VISIBLE);
    }

    // Function for set custom font to title text view in toolbar
    void setCustomFont() {
        Typeface tf = Typeface.createFromAsset(this.getAssets(), "radio.ttf");
        title_tv.setTypeface(tf);
    }

    // Function for listen and change volume from seek bar to player
    void startListenVolume() {
        Player.setVolume((100 - volumeChanger.getProgress()) / 100f);
        volumeChanger.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, int progress, boolean fromUser) {
                Player.setVolume((100 - circularSeekBar.getProgress()) / 100f);
            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {

            }

        });
    }

    // Function to set track and artist names to text views and change background photo
    public static void setSongData() {
        artist_tv.setText(artist);
        track_tv.setText(track);
    }

    // Service for background audio playing
    public void startPlayerService() {
        Intent serviceIntent = new Intent(MainActivity.this, NotificationService.class);
        serviceIntent.setAction(Const.ACTION.STARTFOREGROUND_ACTION);
        startService(serviceIntent);
    }

    // Vibrate when click on control button
    public void vibrate() {
        ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(Const.VIBRATE_TIME);
    }

    public void startRefreshing()
    {
        dataAnalyzer = new DataAnalyzer(this);
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(Const.PHOTO_LOAD_REFRESH_TIME);
                        runOnUiThread(() -> new GetTrackInfo().execute());
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        t.start();
    }


    public void startCounting()
    {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(() -> {
                            if(dataAnalyzer!=null) {
                                MainActivity.data_tv.setText(convertToStringRepresentation(Long.valueOf(MainActivity.dataAnalyzer.getReceivedData(MainActivity.app))));
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        t.start();
    }

    View.OnClickListener controlButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (controlIsActivated == false) {
                startPlayerService();
                control_button.setImageResource(R.drawable.pause);
                playing_animation.setVisibility(View.GONE);
                loading_animation.setVisibility(View.VISIBLE);
                control_button.setVisibility(View.GONE);
                controlIsActivated = true;
                vibrate();
            } else {
                Player.stop();
                control_button.setImageResource(R.drawable.play);
                playing_animation.setVisibility(View.GONE);
                loading_animation.setVisibility(View.VISIBLE);
                control_button.setVisibility(View.VISIBLE);
                controlIsActivated = false;
                vibrate();
            }
        }
    };

    public static String convertToStringRepresentation(final long value){
        final long[] dividers = new long[] { Const.T, Const.G, Const.M, Const.K, 1 };
        final String[] units = new String[] { "TB", "GB", "MB", "KB", "B" };
        if(value < 1)
            throw new IllegalArgumentException("Invalid file size: " + value);
        String result = null;
        for(int i = 0; i < dividers.length; i++){
            final long divider = dividers[i];
            if(value >= divider){
                result = format(value, divider, units[i]);
                break;
            }
        }
        return result;
    }

    private static String format(final long value,
                                 final long divider,
                                 final String unit){
        final double result =
                divider > 1 ? (double) value / (double) divider : (double) value;
        return new DecimalFormat("#,##0.#").format(result) + " " + unit;
    }


    @Override
    public void onBackPressed() {
        Player.stop();
        super.onBackPressed();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void enter(View view) {

    }

    void showHelloWorldDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.hello_dialog, null);

        TextView text = layout.findViewById(R.id.text);
        text.setMovementMethod(LinkMovementMethod.getInstance());
        text.setText(Html.fromHtml("Hello, my name is Nikolay Kolomiytsev i'm Android Developer from 2013, subscribe in my Instagram for pay respect, Peace! ☮"));
        builder.setPositiveButton("SUBSCRIBE", (dialog, which) -> {
            Intent browserIntent = new
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/kolyas.mgs"));
            startActivity(browserIntent);
        });
        builder.setNegativeButton("Don't Show", (dialog, which) -> {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("Show", false);
            editor.commit();
        });
        text.setTextColor(Color.BLACK);
        builder.setView(layout);
        AlertDialog alert = builder.show();
    }
}
