package com.chengfu.fuexoplayer.samples;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.chengfu.fuexoplayer.ExoAudioPlayer;
import com.chengfu.fuexoplayer.ExoPlayException;
import com.chengfu.fuexoplayer.widget.ExoVideoView;

/**
 * Created by ChengFu on 2018/3/15.
 */

public class MusicPlayerActivity extends AppCompatActivity {

    private String url = "http://hls1.gzstv.com/livegztv/xinwen/index.m3u8";
    private ExoAudioPlayer audioPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        ExoVideoView exoVideo=findViewById(R.id.exoVideo);
        exoVideo.setVideoPath("http://storage.gzstv.net/uploads/media/xux/314-4.mp4LL");

        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioPlayer.setAudioPath(url);
                audioPlayer.start();
            }
        });

        findViewById(R.id.pause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioPlayer.pause();
            }
        });

        initAudioPlayer();

    }

    private void initAudioPlayer() {
        audioPlayer = new ExoAudioPlayer(this);
//        audioPlayer.setAudioListener(this);
//        audioPlayer.setVideoController(mAudioPlayerController);
    }

//    @Override
//    public void onLoadingChanged(boolean isLoading) {
//
//    }
//
//    @Override
//    public void onPlayPauseChanged(boolean play) {
//
//    }
//
//    @Override
//    public void onError(ExoPlayException error) {
//
//    }
//
//    @Override
//    public void onCompletion() {
//
//    }
}
