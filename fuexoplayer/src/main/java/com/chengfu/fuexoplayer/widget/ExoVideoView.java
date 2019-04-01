package com.chengfu.fuexoplayer.widget;

import com.chengfu.fuexoplayer.ExoMediaPlayer;
import com.chengfu.fuexoplayer.IVideoController;
import com.chengfu.fuexoplayer.code.source.builder.MediaSourceBuilder;
import com.chengfu.fuexoplayer.video.IVideoPlay;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.FrameLayout;

import java.io.IOException;

/**
 * 基于ExoPlayer的视频播放控件
 *
 * @author ChengFu
 */
public class ExoVideoView extends FrameLayout implements IVideoPlay {

    // STATIC
    public static final String TAG = "ExoVideoView";

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
//    private PowerManager.WakeLock mWakeLock = null;


    private ExoMediaPlayer mExoMediaPlayer;
    private final ExoPlayerListener mExoPlayerListener;
    private IVideoController mVideoController;
    private VideoTextureView mVideoTextureView;
    private Surface mSurface;
    private String mVideoPath;

    private boolean mReleaseOnDetachFromWindow;
    private boolean mPlayRequested;
    private boolean mShowControllerWhenPrepared;

    private AudioManager mAudioManager;
    private boolean mPlayOnFocusGain;
    private int mCurrentAudioFocusState = AUDIO_NO_FOCUS_NO_DUCK;

    private ExoVideoView.ScaleType mScaleType = ExoVideoView.ScaleType.FIT_CENTER;

    public enum ScaleType {
        FIT_CENTER(0), FIT_XY(1), CENTER(2), CENTER_CROP(3), CENTER_INSIDE(4);

        ScaleType(int ni) {
            nativeInt = ni;
        }

        final int nativeInt;
    }

    private VideoListener mVideoListener;

    public interface VideoListener {

        void onReady();

        void onPlayPauseChanged(boolean play);

        void onLoadingChanged(boolean isLoading);

        void onError(ExoPlaybackException error);

        void onCompletion();
    }

    public ExoVideoView(Context context) {
        this(context, null);
    }

    public ExoVideoView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExoVideoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mContext = context;

        mExoPlayerListener = new ExoPlayerListener();

        mAudioManager = (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

//        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
//        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);

        initTextureView();


        initPlayer();
    }

    private void initTextureView() {
        mVideoTextureView = new VideoTextureView(mContext);

        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);
        mVideoTextureView.setLayoutParams(lp);
        mVideoTextureView.setScaleType(mScaleType);

        mVideoTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        addView(mVideoTextureView, 0);
    }

    private void initPlayer() {

        mExoMediaPlayer = new ExoMediaPlayer(mContext);

        mExoMediaPlayer.addVideoRendererEventListener(mExoPlayerListener);
        mExoMediaPlayer.addListener(mExoPlayerListener);

        mExoMediaPlayer.setPlayWhenReady(mPlayRequested);

    }

    public void setVideoListener(VideoListener videoListener) {
        mVideoListener = videoListener;
    }

    public VideoListener getVideoListener() {
        return mVideoListener;
    }

    public void setVideoController(IVideoController controller) {
        if (mVideoController != null) {
            mVideoController.hide();
        }
        mVideoController = controller;
        attachVideoController();
    }

    private void attachVideoController() {
        if (mVideoController != null) {
            mVideoController.setMediaPlayer(this);
            mVideoController.setEnabled(isInPlaybackState());
        }
    }

    private void toggleVideoControlsVisiblity() {
        if (mVideoController.isShowing()) {
            mVideoController.hide();
        } else {
            mVideoController.show();
        }
    }

    public boolean isInPlaybackState() {
        return mExoMediaPlayer.getPlaybackState() == Player.STATE_READY;
    }

    public void setScaleType(ExoVideoView.ScaleType scaleType) {
        if (scaleType == null) {
            throw new NullPointerException();
        }
        if (mScaleType != scaleType) {
            mScaleType = scaleType;
            if (mVideoTextureView != null) {
                mVideoTextureView.setScaleType(mScaleType);
            }
        }
    }

    public ExoVideoView.ScaleType getScaleType() {
        return mScaleType;
    }

    public void setReleaseOnDetachFromWindow(boolean releaseOnDetachFromWindow) {
        mReleaseOnDetachFromWindow = releaseOnDetachFromWindow;
    }

    public boolean getReleaseOnDetachFromWindow() {
        return mReleaseOnDetachFromWindow;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!isInEditMode() && mReleaseOnDetachFromWindow) {
            release();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isInPlaybackState() && mVideoController != null) {
            toggleVideoControlsVisiblity();
        }
        return false;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        if (isInPlaybackState() && mVideoController != null) {
            toggleVideoControlsVisiblity();
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_VOLUME_UP
                && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN && keyCode != KeyEvent.KEYCODE_VOLUME_MUTE
                && keyCode != KeyEvent.KEYCODE_MENU && keyCode != KeyEvent.KEYCODE_CALL
                && keyCode != KeyEvent.KEYCODE_ENDCALL;
        if (isInPlaybackState() && isKeyCodeSupported && mVideoController != null) {
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (isPlaying()) {
                    pause();
                    mVideoController.show();
                } else {
                    start();
                    mVideoController.hide();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                if (!isPlaying()) {
                    start();
                    mVideoController.hide();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                if (isPlaying()) {
                    pause();
                    mVideoController.show();
                }
                return true;
            } else {
                toggleVideoControlsVisiblity();
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    public void setShowControllerWhenPrepared(boolean showControllerWhenPrepared) {
        mShowControllerWhenPrepared = showControllerWhenPrepared;
    }

    public boolean getShowControllerWhenPrepared() {
        return mShowControllerWhenPrepared;
    }

//    public void keepScreenOn(boolean keep) {
//        if (keep) {
//            if (null != mWakeLock && (!mWakeLock.isHeld())) {
//                mWakeLock.acquire();
//            }
//        } else {
//            if (mWakeLock != null && mWakeLock.isHeld()) {
//                mWakeLock.release();
//            }
//        }
//    }

    public void setMediaSourceBuilder(MediaSourceBuilder builder) {

    }

    @Override
    public void setVideoPath(String path) {
        setVideoPath(path, null);
    }

    public void setVideoPath(String path, MediaSourceBuilder builder) {
        if (path == null) {
            stopPlayback(true);
            mVideoTextureView.clearSurface();
            if (mVideoListener != null) {
                mVideoListener.onError(ExoPlaybackException.createForSource(new IOException("VideoPath is null")));
            }
            return;
        }
        mVideoPath = path;
        mExoMediaPlayer.setPlayWhenReady(false);
        mExoMediaPlayer.prepare(path, builder);
        mVideoTextureView.clearSurface();
        if (mVideoController != null) {
            mVideoController.hideNow();
        }
        if (mSurface != null) {
            mExoMediaPlayer.setVideoSurface(mSurface);
        }
    }

    @Override
    public String getVideoPath() {
        return mVideoPath;
    }

    @Override
    public void start() {
//        setKeepScreenOn(true);
//        tryToGetAudioFocus();

        mExoMediaPlayer.setPlayWhenReady(true);
        mPlayRequested = true;
    }

    @Override
    public void pause() {
//        setKeepScreenOn(false);

        mExoMediaPlayer.setPlayWhenReady(false);
        mPlayRequested = false;
        if (mVideoListener != null) {
            mVideoListener.onPlayPauseChanged(isPlaying());
        }
    }

    @Override
    public boolean restart() {
        if (mVideoPath == null) {
            return false;
        }
        setVideoPath(mVideoPath);
        seekTo(0);
        start();
        return true;
    }

    @Override
    public void suspend() {
//        setKeepScreenOn(false);
//        giveUpAudioFocus();

        mExoMediaPlayer.release();
        mPlayRequested = false;
    }

    @Override
    public void stopPlayback(boolean clearSurface) {
        pause();
//        setKeepScreenOn(false);
//        giveUpAudioFocus();
        mExoMediaPlayer.stop();
        mPlayRequested = false;
        if (clearSurface) {
            mExoMediaPlayer.clearVideoSurface();
        }
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
        stopPlayback(true);
        mVideoPath = null;
    }

    @Override
    public void release() {
        mVideoController = null;
        mVideoPath = null;
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

    private final SurfaceTextureListener mSurfaceTextureListener = new SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            Log.i(TAG, "onSurfaceTextureAvailable---surfaceTexture=" + surfaceTexture + ",width=" + width + ",height="
                    + height);
            mSurface = new Surface(surfaceTexture);
            mExoMediaPlayer.setVideoSurface(mSurface);
            if (mPlayRequested) {
                mExoMediaPlayer.setPlayWhenReady(mPlayRequested);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            Log.i(TAG, "onSurfaceTextureSizeChanged---surfaceTexture=" + surfaceTexture + ",width=" + width + ",height="
                    + height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            Log.i(TAG, "onSurfaceTextureDestroyed---surfaceTexture=" + surfaceTexture);
            mSurface = null;
            mExoMediaPlayer.clearVideoSurface();
            surfaceTexture.release();
//            stopPlayback(true);
//            pause();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            // Log.i(TAG, "onSurfaceTextureUpdated---surfaceTexture=" +
            // surfaceTexture);
        }

    };

    private void updateScreenOn(boolean playWhenReady, int playbackState) {
        boolean keepScreenOn = false;
        if (playbackState != Player.STATE_IDLE
                && playbackState != Player.STATE_ENDED && playWhenReady) {
            keepScreenOn = true;
        }
        setKeepScreenOn(keepScreenOn);
    }

    private void updateAudioFocus(boolean playWhenReady, int playbackState) {
        if (playbackState == Player.STATE_READY
                && playWhenReady) {
            tryToGetAudioFocus();
        }
        if (playbackState == Player.STATE_IDLE
                || playbackState == Player.STATE_ENDED
                || !playWhenReady) {
            giveUpAudioFocus();
        }
    }

    public class ExoPlayerListener
            implements VideoRendererEventListener, Player.EventListener {

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            Log.i(TAG, "onTracksChanged---trackGroups=" + trackGroups + ",trackSelections=" + trackSelections);
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            Log.i(TAG, "onLoadingChanged---isLoading=" + isLoading);
//            if (mVideoListener != null) {
//                mVideoListener.onLoadingChanged(isLoading);
//            }
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            Log.i(TAG, "onPlayerStateChanged---playWhenReady=" + playWhenReady + ",playbackState=" + playbackState);
            updateScreenOn(playWhenReady, playbackState);
            updateAudioFocus(playWhenReady, playbackState);
            if (playbackState == Player.STATE_BUFFERING && mVideoListener != null) {
                mVideoListener.onLoadingChanged(true);
            }
            if (playbackState == Player.STATE_READY) {

                if (mVideoListener != null) {
                    mVideoListener.onReady();
                    mVideoListener.onPlayPauseChanged(isPlaying());
                    mVideoListener.onLoadingChanged(false);
                }
                if (mVideoController != null) {
                    mVideoController.setEnabled(true);
                    if (mShowControllerWhenPrepared) {
                        mVideoController.show();
                    }
                }
            } else if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                if (mVideoController != null) {
                    mVideoController.setEnabled(false);
                }
            }
            if (playbackState == Player.STATE_ENDED) {
                setKeepScreenOn(false);
                if (mVideoListener != null) {
                    mVideoListener.onCompletion();
                }
            }
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {

        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Log.i(TAG, "onPlayerError---error=" + error);
//            setKeepScreenOn(false);
            if (mVideoListener != null) {
                mVideoListener.onError(error);
//                if (error != null) {
//                    mVideoListener.onError(ExoPlayException.create(error.type, error.getCause(), error.rendererIndex));
//                } else {
//                    mVideoListener.onError(ExoPlayException.createForUnexpected(new RuntimeException("播放出错")));
//                }
            }
        }

        @Override
        public void onPositionDiscontinuity(int reason) {
            Log.i(TAG, "onPositionDiscontinuity  reason=" + reason);
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            Log.i(TAG, "onPlaybackParametersChanged---playbackParameters=" + playbackParameters);
        }

        @Override
        public void onSeekProcessed() {

        }

        @Override
        public void onVideoEnabled(DecoderCounters counters) {
            Log.i(TAG, "onVideoEnabled---counters=" + counters);
        }

        @Override
        public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs,
                                              long initializationDurationMs) {
            Log.i(TAG, "onVideoDecoderInitialized---decoderName=" + decoderName + ",initializedTimestampMs="
                    + initializedTimestampMs + ",initializationDurationMs=" + initializationDurationMs);
        }

        @Override
        public void onVideoInputFormatChanged(Format format) {
            Log.i(TAG, "onVideoInputFormatChanged---format=" + Format.toLogString(format));
        }

        @Override
        public void onDroppedFrames(int count, long elapsedMs) {
            Log.i(TAG, "onDroppedFrames---count=" + count + ",elapsedMs=" + elapsedMs);
        }

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                       float pixelWidthHeightRatio) {
            mVideoTextureView.updateVideoSize(width, height);
            Log.i(TAG, "onVideoSizeChanged---width=" + width + ",height=" + height + ",unappliedRotationDegrees="
                    + unappliedRotationDegrees + ",pixelWidthHeightRatio=" + pixelWidthHeightRatio);
        }

        @Override
        public void onRenderedFirstFrame(Surface surface) {
            Log.i(TAG, "onRenderedFirstFrame---surface=" + surface);
        }

        @Override
        public void onVideoDisabled(DecoderCounters counters) {
            Log.i(TAG, "onVideoDisabled---counters=" + counters);
        }
    }
}
