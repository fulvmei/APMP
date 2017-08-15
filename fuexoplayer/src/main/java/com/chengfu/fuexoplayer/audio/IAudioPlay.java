package com.chengfu.fuexoplayer.audio;

import com.chengfu.fuexoplayer.IMediaPlayer;

public interface IAudioPlay extends IMediaPlayer {
	
	public void setAudioPath(String path);

	public String getAudioPath(); 
}
