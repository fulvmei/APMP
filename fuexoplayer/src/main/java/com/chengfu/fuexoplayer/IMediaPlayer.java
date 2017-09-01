package com.chengfu.fuexoplayer;

import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;

/**
 * 播放器接口
 *
 * @author ChengFu
 */
public interface IMediaPlayer {

    void start();

    void pause();

    long getDuration();

    long getCurrentPosition();

    void seekTo(@IntRange(from = 0) long milliseconds);

    boolean isPlaying();

    int getBufferPercentage();

    void setVolume(float volume);

    float getVolume();

    int getAudioSessionId();

    void reset();

    void release();

}
