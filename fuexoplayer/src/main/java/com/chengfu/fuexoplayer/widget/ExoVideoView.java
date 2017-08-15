package com.chengfu.fuexoplayer.widget;

import com.chengfu.fuexoplayer.ExoMediaPlayer;
import com.chengfu.fuexoplayer.IVideoController;
import com.chengfu.fuexoplayer.video.IVideoPlay;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayer.EventListener;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.FrameLayout;

/**
 * 基于ExoPlayer的视频播放控件
 *
 * @author ChengFu
 *
 */
public class ExoVideoView extends FrameLayout implements IVideoPlay {

    // STATIC
    public static final String TAG = "ExoVideoView";

    private final Context mContext;
    private final ExoPlayerListener mExoPlayerListener;

    private ExoMediaPlayer mExoMediaPlayer;
    private IVideoController mVideoController;
    private VideoTextureView mVideoTextureView;
    private Surface mSurface;

    private String mVideoPath;

    private boolean mReleaseOnDetachFromWindow;
    private boolean mPlayRequested;
    private boolean mShowControllerWhenPrepared;

    private AudioManager mAudioManager;
    private AudioFocusHelper mAudioFocusHelper;
    private boolean handleAudioFocus = true;

    private ExoVideoView.ScaleType mScaleType = ExoVideoView.ScaleType.FIT_CENTER;

    public enum ScaleType {
        FIT_CENTER(0), FIT_XY(1), CENTER(2), CENTER_CROP(3), CENTER_INSIDE(4);

        ScaleType(int ni) {
            nativeInt = ni;
        }

        final int nativeInt;
    }

    private VideoListener mVideoListener;

    public static interface VideoListener {
        public void onLoadingChanged(boolean isLoading);

        public void onError(ExoPlaybackException error);

        public void onCompletion();
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
        mAudioFocusHelper = new AudioFocusHelper();

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
        return mExoMediaPlayer.getPlaybackState() == ExoPlayer.STATE_READY;
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

    public void setHandleAudioFocus(boolean handleAudioFocus) {
        mAudioFocusHelper.abandonFocus();
        this.handleAudioFocus = handleAudioFocus;
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

    @Override
    public void setVideoPath(String path) {
        mVideoPath = path;
        mExoMediaPlayer.setPlayWhenReady(false);
        mExoMediaPlayer.prepare(path);
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
        setKeepScreenOn(true);
        mAudioFocusHelper.requestFocus();

        mExoMediaPlayer.setPlayWhenReady(true);
        mPlayRequested = true;

    }

    @Override
    public void pause() {
        setKeepScreenOn(false);
        mAudioFocusHelper.abandonFocus();

        mExoMediaPlayer.setPlayWhenReady(false);
        mPlayRequested = false;

    }

    @Override
    public boolean restart() {
        if (mVideoPath == null) {
            return false;
        }
        int playbackState = mExoMediaPlayer.getPlaybackState();
        if (playbackState != ExoPlayer.STATE_IDLE && playbackState != ExoPlayer.STATE_ENDED) {
            return false;
        }
        seekTo(0);
        start();
        return true;
    }

    @Override
    public void suspend() {
        setKeepScreenOn(false);
        mAudioFocusHelper.abandonFocus();

        mExoMediaPlayer.release();
        mPlayRequested = false;
    }

    @Override
    public void stopPlayback(boolean clearSurface) {
        setKeepScreenOn(false);
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
        return mExoMediaPlayer.getPlaybackState() == ExoPlayer.STATE_READY && mExoMediaPlayer.getPlayWhenReady();
    }

    @Override
    public int getBufferPercentage() {
        return mExoMediaPlayer.getBufferedPercentage();
    }

    @Override
    public void setVolume(int volume) {
        mExoMediaPlayer.setVolume(volume);
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

    protected class AudioFocusHelper implements AudioManager.OnAudioFocusChangeListener {
        protected boolean startRequested = false;
        protected boolean pausedForLoss = false;
        protected int currentFocus = 0;

        @Override
        public void onAudioFocusChange(int focusChange) {
            if (!handleAudioFocus || currentFocus == focusChange) {
                return;
            }

            currentFocus = focusChange;
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                    if (startRequested || pausedForLoss) {
                        start();
                        startRequested = false;
                        pausedForLoss = false;
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    if (isPlaying()) {
                        pausedForLoss = true;
                        pause();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (isPlaying()) {
                        pausedForLoss = true;
                        pause();
                    }
                    break;
            }
        }

        /**
         * Requests to obtain the audio focus
         *
         * @return True if the focus was granted
         */
        public boolean requestFocus() {
            if (!handleAudioFocus || currentFocus == AudioManager.AUDIOFOCUS_GAIN) {
                return true;
            }

            if (mAudioManager == null) {
                return false;
            }

            int status = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == status) {
                currentFocus = AudioManager.AUDIOFOCUS_GAIN;
                return true;
            }

            startRequested = true;
            return false;
        }

        /**
         * Requests the system to drop the audio focus
         *
         * @return True if the focus was lost
         */
        public boolean abandonFocus() {
            if (!handleAudioFocus) {
                return true;
            }

            if (mAudioManager == null) {
                return false;
            }

            startRequested = false;
            int status = mAudioManager.abandonAudioFocus(this);
            return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == status;
        }
    }

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
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            // Log.i(TAG, "onSurfaceTextureUpdated---surfaceTexture=" +
            // surfaceTexture);
        }

    };

    private final class ExoPlayerListener
            implements VideoRendererEventListener, EventListener {

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
            if (mVideoListener != null) {
                mVideoListener.onLoadingChanged(isLoading);
            }
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            Log.i(TAG, "onPlayerStateChanged---playWhenReady=" + playWhenReady + ",playbackState=" + playbackState);

            if (playbackState == ExoPlayer.STATE_READY && mVideoListener != null) {
                mVideoListener.onLoadingChanged(false);
            }
            if (mVideoController == null) {
                return;
            }
            if (playbackState == ExoPlayer.STATE_READY) {
                mVideoController.setEnabled(true);
                if (mShowControllerWhenPrepared) {
                    mVideoController.show();
                }
            } else if (playbackState == ExoPlayer.STATE_IDLE || playbackState == ExoPlayer.STATE_ENDED) {
                setKeepScreenOn(false);
                mVideoController.setEnabled(false);
            }
            if (playbackState == ExoPlayer.STATE_ENDED) {
                setKeepScreenOn(false);
                mVideoListener.onCompletion();
            }
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {

        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Log.i(TAG, "onPlayerError---error=" + error);
            setKeepScreenOn(false);
            if (mVideoListener != null) {
                mVideoListener.onError(error);
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
