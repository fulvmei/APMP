package com.chengfu.fuexoplayer.video;

import com.chengfu.fuexoplayer.IMediaPlayer;

import android.support.annotation.Nullable;

public interface IVideoPlay extends IMediaPlayer{

	void setVideoPath(@Nullable String path);

	String getVideoPath();
	
	boolean restart();

	void suspend();
	
	void stopPlayback(boolean clearSurface);
	
}
