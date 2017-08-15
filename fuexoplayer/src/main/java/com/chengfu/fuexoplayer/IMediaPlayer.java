package com.chengfu.fuexoplayer;

import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;

/**
 * 播放器接口
 * 
 * @author ChengFu
 *
 */
public interface IMediaPlayer {

//	void setMediaPath(@Nullable String path);
//
//	String getMediaPath();
	
	void start();

	void pause();
	
//	boolean restart();
//
//	void suspend();
	
//	void stopPlayback(boolean clearSurface);

	long getDuration();

	long getCurrentPosition();

	void seekTo(@IntRange(from = 0) long milliseconds);

	boolean isPlaying();

	int getBufferPercentage();

//	boolean canPause();
//
//	boolean canSeekBackward();
//
//	boolean canSeekForward();

	void setVolume(@FloatRange(from = 0.0, to = 1.0) int volume);

	int getAudioSessionId();

	void reset();
	
	void release();

}
