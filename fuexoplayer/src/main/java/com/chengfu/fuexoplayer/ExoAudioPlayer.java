package com.chengfu.fuexoplayer;

import android.content.Context;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.util.Log;


import com.chengfu.fuexoplayer.audio.IAudioPlay;
import com.chengfu.fuexoplayer.code.source.builder.MediaSourceBuilder;
import com.chengfu.fuexoplayer.widget.ExoVideoView;
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

    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    public static final float VOLUME_DUCK = 0.2f;

    // we don't have audio focus, and can't duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // we don't have focus, but can duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // we have full audio focus
    private static final int AUDIO_FOCUSED = 2;

    private final Context mContext;
    private final ExoPlayerListener mExoPlayerListener;

    private ExoMediaPlayer mExoMediaPlayer;
    private IAudioController mAudioController;
    private boolean mPlayOnFocusGain;

    private String mAudioPath;
    private AudioListener mAudioListener;

    private int mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
    private final AudioManager mAudioManager;


    public interface AudioListener {
        void onLoadingChanged(boolean isLoading);

        void onPlayPauseChanged(boolean play);

        void onError(ExoPlaybackException error);

        void onCompletion();
    }

    public ExoAudioPlayer(Context context) {
        mContext = context;

        mAudioManager = (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

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
        setAudioPath(path, null);
    }

    public void setAudioPath(String path, MediaSourceBuilder builder) {
        if (path == null) {
            stopPlayback();
            if (mAudioListener != null) {
                mAudioListener.onError(ExoPlaybackException.createForSource(new IOException("VideoPath is null")));
            }
            return;
        }
        mAudioPath = path;
        mExoMediaPlayer.setPlayWhenReady(false);
        mExoMediaPlayer.prepare(path, builder);
    }

    @Override
    public String getAudioPath() {
        return mAudioPath;
    }

    @Override
    public void start() {
        tryToGetAudioFocus();
        mExoMediaPlayer.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        mExoMediaPlayer.setPlayWhenReady(false);
        if (mAudioListener != null) {
            mAudioListener.onPlayPauseChanged(isPlaying());
        }
    }


    @Override
    public void stopPlayback() {
        giveUpAudioFocus();
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

    private void tryToGetAudioFocus() {
        int result =
                mAudioManager.requestAudioFocus(
                        mOnAudioFocusChangeListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mCurrentAudioFocusState = AUDIO_FOCUSED;
        } else {
            mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    private void giveUpAudioFocus() {
        if (mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener)
                == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    private void configurePlayerState() {
        if (mCurrentAudioFocusState == AUDIO_NO_FOCUS_NO_DUCK) {
            // We don't have audio focus and can't duck, so we have to pause
            pause();
        } else {

            int maxSystemMusicVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
            int currentSystemMusicVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

            if (mCurrentAudioFocusState == AUDIO_NO_FOCUS_CAN_DUCK) {
                // We're permitted to play, but only if we 'duck', ie: play softly
                setVolume(VOLUME_DUCK);
            } else {
                setVolume((float) currentSystemMusicVolume / (float) maxSystemMusicVolume);
            }

            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                mExoMediaPlayer.setPlayWhenReady(true);
                mPlayOnFocusGain = false;
            }
        }
    }

    private final AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_GAIN:
                            mCurrentAudioFocusState = AUDIO_FOCUSED;
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            // Audio focus was lost, but it's possible to duck (i.e.: play quietly)
                            mCurrentAudioFocusState = AUDIO_NO_FOCUS_CAN_DUCK;
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            // Lost audio focus, but will gain it back (shortly), so note whether
                            // playback should resume
                            mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
                            mPlayOnFocusGain = isPlaying();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS:
                            // Lost audio focus, probably "permanently"
                            mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;
                            break;
                    }
                    if (mExoMediaPlayer != null) {
                        configurePlayerState();
                    }
                }
            };

    private final class ExoPlayerListener
            implements Player.EventListener {


        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
//            Log.i(TAG, "onTimelineChanged---timeline=" + timeline + ",manifest=" + manifest);
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
                if (mAudioListener != null) {
                    mAudioListener.onPlayPauseChanged(isPlaying());
                }
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
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
            Log.i(TAG, "onShuffleModeEnabledChanged---shuffleModeEnabled=" + shuffleModeEnabled);
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Log.i(TAG, "onPlayerError---error=" + error);
            if (mAudioListener != null) {
                mAudioListener.onError(error);
            }
        }

        @Override
        public void onPositionDiscontinuity(int reason) {
            Log.i(TAG, "onPositionDiscontinuity---reason=" + reason);
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            Log.i(TAG, "onPlaybackParametersChanged---playbackParameters=" + playbackParameters);
        }

        @Override
        public void onSeekProcessed() {
            Log.i(TAG, "onSeekProcessed");
        }
    }
}
