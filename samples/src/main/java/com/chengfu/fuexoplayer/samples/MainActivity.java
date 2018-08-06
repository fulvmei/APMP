package com.chengfu.fuexoplayer.samples;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.VideoView;

import com.chengfu.fuexoplayer.code.source.builder.DashMediaSourceBuilder;
import com.chengfu.fuexoplayer.code.source.builder.HlsMediaSourceBuilder;
import com.chengfu.fuexoplayer.code.source.builder.SsMediaSourceBuilder;
import com.chengfu.fuexoplayer.widget.ExoVideoView;


public class MainActivity extends AppCompatActivity {

//    String url = "http://vod.hi.gzstv.com/vod_7b887f9e2d604d89b77fd4e7e4bb6ff5.m3u8";
    String url = "http://pili-live-hdl.gzstv.net/icvkuzqj/test713.flv";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        VideoView video = findViewById(R.id.video);

        video.setVideoPath(url);
        video.start();
//

//        ExoVideoView exoVideoView = findViewById(R.id.exoVideoView);
//
////        exoVideoView.setVideoPath(url);
//
//        exoVideoView.setVideoPath(url, new DashMediaSourceBuilder());
//        exoVideoView.start();

        findViewById(R.id.music_samples).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MusicPlayerActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TestService.class);
                startService(intent);
            }
        });

        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TestService.class);
                stopService(intent);
            }
        });

    }
}
