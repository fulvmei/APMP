package com.chengfu.fuexoplayer.audio;

import com.chengfu.fuexoplayer.IMediaPlayer;

public interface IAudioPlay extends IMediaPlayer {

    void setAudioPath(String path);

    String getAudioPath();

    void stopPlayback();
}
