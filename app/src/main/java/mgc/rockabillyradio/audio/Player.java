package mgc.rockabillyradio.audio;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import mgc.rockabillyradio.MainActivity;
import mgc.rockabillyradio.R;

/**
 * Created by kolyas on 01.10.2016.
 */

public class Player {

    static SimpleExoPlayer exoPlayer;

    public static void start(String URL, Context context)
    {
        if(exoPlayer!=null)
        {
            exoPlayer.stop();
        }
        Uri URI = Uri.parse(URL);
        Log.e("URL", URL);
        DataSource.Factory dataSourceFactory =
                new DefaultHttpDataSourceFactory(Util.getUserAgent(context, "mgc.rockabillyradio"));
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(URI);
        exoPlayer = ExoPlayerFactory.newSimpleInstance(context);
// Prepare the player with the SmoothStreaming media source.
        exoPlayer.prepare(mediaSource);
        exoPlayer.setPlayWhenReady(true);
        exoPlayer.addListener(new com.google.android.exoplayer2.Player.EventListener() {
            @Override
            public int hashCode() {
                return super.hashCode();
            }
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                Log.e("playbackState",exoPlayer.getPlaybackState()+"");
                // This state if player is ready to work and loaded all data
                if(playbackState == 3)
                {
                    MainActivity.playing_animation.setVisibility(View.VISIBLE);
                    MainActivity.loading_animation.setVisibility(View.GONE);
                    MainActivity.control_button.setVisibility(View.VISIBLE);
                    MainActivity.control_button.setImageResource(R.drawable.pause);
                }
            }
        });
    }

    public static void stop()
    {
        if(exoPlayer!=null) {
            exoPlayer.stop();
        }
    }

    public static void setVolume(float volume)
    {
        if(exoPlayer!= null) {
            exoPlayer.setVolume(volume);
        }
    }
}
