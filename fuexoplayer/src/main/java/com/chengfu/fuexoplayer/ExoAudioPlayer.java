package com.chengfu.fuexoplayer;

import android.content.Context;
import android.util.Log;


import com.chengfu.fuexoplayer.audio.IAudioPlay;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

import java.io.IOException;

/**
 * 基于ExoPlayer的音频播放控件
 *
 * @author ChengFu
 */
public class ExoAudioPlayer implements IAudioPlay {

    // STATIC
    public static final String TAG = "ExoAudioPlayer";

    private final Context mContext;
    private final ExoPlayerListener mExoPlayerListener;

    private ExoMediaPlayer mExoMediaPlayer;
    private IAudioController mAudioController;

    private String mAudioPath;
    private AudioListener mAudioListener;

    public static interface AudioListener {
        public void onLoadingChanged(boolean isLoading);

        public void onPlayPauseChanged(boolean play);

        public void onError(ExoPlaybackException error);

        public void onCompletion();
    }

    public ExoAudioPlayer(Context context) {
        mContext = context;

        mExoPlayerListener = new ExoPlayerListener();
        initPlayer();
    }


    private void initPlayer() {
        mExoMediaPlayer = new ExoMediaPlayer(mContext);

        mExoMediaPlayer.addListener(mExoPlayerListener);

        mExoMediaPlayer.setPlayWhenReady(false);
    }

    public void setAudioListener(AudioListener audioListener) {
        mAudioListener = audioListener;
    }

    public AudioListener getAudioListener() {
        return mAudioListener;
    }

    public void setVideoController(IAudioController controller) {
        mAudioController = controller;
        attachVideoController();
    }

    private void attachVideoController() {
        if (mAudioController != null) {
            mAudioController.setMediaPlayer(this);
            mAudioController.setEnabled(isInPlaybackState());
        }
    }

    public boolean isInPlaybackState() {
        return mExoMediaPlayer.getPlaybackState() == Player.STATE_READY;
    }


    @Override
    public void setAudioPath(String path) {
        if (path == null) {
            stopPlayback();
            if (mAudioListener != null) {
                mAudioListener.onError(ExoPlaybackException.createForSource(new IOException("VideoPath is null")));
            }
            return;
        }
        mAudioPath = path;
        mExoMediaPlayer.setPlayWhenReady(false);
        mExoMediaPlayer.prepare(path);
    }

    @Override
    public String getAudioPath() {
        return mAudioPath;
    }

    @Override
    public void start() {
        mExoMediaPlayer.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        mExoMediaPlayer.setPlayWhenReady(false);

    }


    @Override
    public void stopPlayback() {
        mExoMediaPlayer.stop();
    }

    @Override
    public long getDuration() {
        return mExoMediaPlayer.getDuration();
    }

    @Override
    public long getCurrentPosition() {
        return mExoMediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(long milliseconds) {
        mExoMediaPlayer.seekTo(milliseconds);
    }

    @Override
    public boolean isPlaying() {
        return mExoMediaPlayer.getPlaybackState() == Player.STATE_READY && mExoMediaPlayer.getPlayWhenReady();
    }

    @Override
    public int getBufferPercentage() {
        return mExoMediaPlayer.getBufferedPercentage();
    }

    @Override
    public void setVolume(float volume) {
        if (mExoMediaPlayer != null) {
            mExoMediaPlayer.setVolume(volume);
        }
    }

    @Override
    public float getVolume() {
        if (mExoMediaPlayer != null) {
            return mExoMediaPlayer.getVolume();
        }
        return 0f;
    }

    @Override
    public int getAudioSessionId() {
        return mExoMediaPlayer.getAudioSessionId();
    }

    @Override
    public void reset() {
        stopPlayback();
        mAudioPath = null;
    }

    @Override
    public void release() {
        mAudioController = null;
        mAudioPath = null;
        mExoMediaPlayer.release();
    }

    private final class ExoPlayerListener
            implements Player.EventListener {

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {
            Log.i(TAG, "onTimelineChanged---timeline=" + timeline + ",manifest=" + manifest);
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            Log.i(TAG, "onTracksChanged---trackGroups=" + trackGroups + ",trackSelections=" + trackSelections);
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            Log.i(TAG, "onLoadingChanged---isLoading=" + isLoading);
            if (mAudioListener != null) {
                mAudioListener.onLoadingChanged(isLoading);
            }
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            Log.i(TAG, "onPlayerStateChanged---playWhenReady=" + playWhenReady + ",playbackState=" + playbackState);

            if (playbackState == Player.STATE_READY && mAudioListener != null) {
                mAudioListener.onLoadingChanged(false);
            }
            if (playbackState == Player.STATE_READY) {
                if (mAudioController != null) {
                    mAudioController.setEnabled(true);
                }
            } else if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                if (mAudioController != null) {
                    mAudioController.setEnabled(false);
                }
            }
            if (playbackState == Player.STATE_ENDED) {
                if (mAudioListener != null) {
                    mAudioListener.onCompletion();
                }
            }
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            Log.i(TAG, "onRepeatModeChanged---repeatMode=" + repeatMode);
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Log.i(TAG, "onPlayerError---error=" + error);
            if (mAudioListener != null) {
                mAudioListener.onError(error);
            }
        }

        @Override
        public void onPositionDiscontinuity() {
            Log.i(TAG, "onPositionDiscontinuity");
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            Log.i(TAG, "onPlaybackParametersChanged---playbackParameters=" + playbackParameters);
        }
    }
}
