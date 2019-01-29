package com.chengfu.fuexoplayer.samples;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.VideoView;

import com.chengfu.fuexoplayer.IVideoController;
import com.chengfu.fuexoplayer.code.source.builder.DashMediaSourceBuilder;
import com.chengfu.fuexoplayer.code.source.builder.HlsMediaSourceBuilder;
import com.chengfu.fuexoplayer.code.source.builder.MediaSourceBuilder;
import com.chengfu.fuexoplayer.code.source.builder.SsMediaSourceBuilder;
import com.chengfu.fuexoplayer.widget.ExoVideoView;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.TransferListener;


public class MainActivity extends AppCompatActivity {

//    String url = "https://mov.bn.netease.com/open-movie/nos/mp4/2015/08/27/SB13F5AGJ_sd.mp4";
    String url = "http://sel.gzstv.com/recordings/z1.icvkuzqj.secw/1547173500_1547180880.m3u8";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ExoVideoView exoVideoView = findViewById(R.id.exoVideoView);

        exoVideoView.setVideoPath(url);
        exoVideoView.start();

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

//    public void test(){
//        ExoVideoView exoVideoView=new ExoVideoView(this);
//
//exoVideoView.setVideoListener(new ExoVideoView.VideoListener() {
//    @Override
//    public void onReady() {
//
//    }
//
//    @Override
//    public void onPlayPauseChanged(boolean play) {
//
//    }
//
//    @Override
//    public void onLoadingChanged(boolean isLoading) {
//
//    }
//
//    @Override
//    public void onError(ExoPlaybackException error) {
//
//    }
//
//    @Override
//    public void onCompletion() {
//
//    }
//});
//
//
//        IVideoController videoController;
//        exoVideoView.setVideoPath(url);
//
//exoVideoView.start();//开始播放
//exoVideoView.pause();//暂停播放
//exoVideoView.getDuration();//获取播放总时长
//exoVideoView.getCurrentPosition();//获取当前播放时长
//exoVideoView.getBufferPercentage();//获取缓存百分比
//exoVideoView.isPlaying();//是否正在播放
//exoVideoView.seekTo(0);//跳到当前位置
//exoVideoView.stopPlayback(false);//停止播放
//exoVideoView.restart();//重新播放
//
//exoVideoView.release();//释放播放器
//    }

    public class RtmpMediaSourceBuilder extends MediaSourceBuilder {
        @NonNull
        @Override
        public MediaSource build(@NonNull Context context, @NonNull Uri uri, @NonNull String userAgent, @NonNull Handler handler, @Nullable TransferListener transferListener) {
            DataSource.Factory dataSourceFactory = buildDataSourceFactory(context, userAgent, null);
            DataSource.Factory meteredDataSourceFactory = buildDataSourceFactory(context, userAgent, transferListener);
            return new DashMediaSource(uri, dataSourceFactory, new DefaultDashChunkSource.Factory(meteredDataSourceFactory), handler, null);
        }
    }
}
