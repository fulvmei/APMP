package com.chengfu.fuexoplayer;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import com.chengfu.fuexoplayer.code.renderer.RendererProvider;
import com.chengfu.fuexoplayer.code.source.MediaSourceProvider;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.PlayerMessage;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataRenderer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.SurfaceHolder.Callback;

/**
 * 基于ExoPlayer的音视频播放器
 *
 * @author ChengFu
 */
public class ExoMediaPlayer implements ExoPlayer {

    // STATIC
    public static final String TAG = "ExoMediaPlayer";

    private final Context mContext;
    private final Handler mMainHandler;

    private Surface mSurface;
    private boolean mOwnsSurface;
    private SurfaceHolder mSurfaceHolder;
    private TextureView mTextureView;
    private TextRenderer.Output mTextOutput;
    private MetadataRenderer.Output mMetadataOutput;

    @C.VideoScalingMode
    private int mVideoScalingMode;

    private List<Renderer> mRendererList = new LinkedList<>();
    private final int mVideoRendererCount;
    private final int mAudioRendererCount;
    private DefaultTrackSelector mTrackSelector;
    private final AdaptiveTrackSelection.Factory mAdaptiveTrackSelectionFactory;
    private final MediaSourceProvider mMediaSourceProvider;
    private final DefaultBandwidthMeter mBandwidthMeter;

    private final ExoPlayer mExoPlayer;
    private final ComponentListener mComponentListener;
    private final CopyOnWriteArraySet<AudioRendererEventListener> mAudioRendererEventListeners;
    private final CopyOnWriteArraySet<VideoRendererEventListener> mVideoRendererEventListeners;

    private DecoderCounters mVideoDecoderCounters;
    private DecoderCounters mAudioDecoderCounters;
    private int mAudioSessionId;
    private Format mVideoFormat;
    private Format mAudioFormat;
    private int mAudioStreamType;
    private float mAudioVolume;

    public ExoMediaPlayer(Context context) {
        mContext = context;

        mAudioRendererEventListeners = new CopyOnWriteArraySet<>();
        mVideoRendererEventListeners = new CopyOnWriteArraySet<>();

        mMainHandler = new Handler();

        mComponentListener = new ComponentListener();
        RendererProvider rendererProvider = new RendererProvider(context, mMainHandler, mComponentListener,
                mComponentListener, mComponentListener, mComponentListener);
        mRendererList = rendererProvider.generate();

        mMediaSourceProvider = new MediaSourceProvider();
        mBandwidthMeter = new DefaultBandwidthMeter();

        int videoRendererCount = 0;
        int audioRendererCount = 0;
        for (Renderer renderer : mRendererList) {
            switch (renderer.getTrackType()) {
                case C.TRACK_TYPE_VIDEO:
                    videoRendererCount++;
                    break;
                case C.TRACK_TYPE_AUDIO:
                    audioRendererCount++;
                    break;
            }
        }
        mVideoRendererCount = videoRendererCount;
        mAudioRendererCount = audioRendererCount;

        mAudioVolume = 1;
        mAudioSessionId = C.AUDIO_SESSION_ID_UNSET;
        mAudioStreamType = C.STREAM_TYPE_DEFAULT;
        mVideoScalingMode = C.VIDEO_SCALING_MODE_DEFAULT;

        mAdaptiveTrackSelectionFactory = new AdaptiveTrackSelection.Factory(mBandwidthMeter);
        mTrackSelector = new DefaultTrackSelector(mAdaptiveTrackSelectionFactory);

        LoadControl loadControl = ExoMedia.Data.loadControl != null ? ExoMedia.Data.loadControl
                : new DefaultLoadControl();
        mExoPlayer = ExoPlayerFactory.newInstance(mRendererList.toArray(new Renderer[mRendererList.size()]),
                mTrackSelector, loadControl);
    }

    public void setVideoScalingMode(@C.VideoScalingMode int videoScalingMode) {
        mVideoScalingMode = videoScalingMode;
        ExoPlayerMessage[] messages = new ExoPlayerMessage[mVideoRendererCount];
        int count = 0;
        for (Renderer renderer : mRendererList) {
            if (renderer.getTrackType() == C.TRACK_TYPE_VIDEO) {
                messages[count++] = new ExoPlayerMessage(renderer, C.MSG_SET_SCALING_MODE, videoScalingMode);
            }
        }
        mExoPlayer.sendMessages(messages);
    }

    public
    @C.VideoScalingMode
    int getVideoScalingMode() {
        return mVideoScalingMode;
    }

    public void clearVideoSurface() {
        setVideoSurface(null);
    }

    public void setVideoSurface(Surface surface) {
        removeSurfaceCallbacks();
        setVideoSurfaceInternal(surface, false);
    }

    public void clearVideoSurface(Surface surface) {
        if (surface != null && surface == mSurface) {
            setVideoSurface(null);
        }
    }

    public void setVideoSurfaceHolder(SurfaceHolder surfaceHolder) {
        removeSurfaceCallbacks();
        mSurfaceHolder = surfaceHolder;
        if (surfaceHolder == null) {
            setVideoSurfaceInternal(null, false);
        } else {
            setVideoSurfaceInternal(surfaceHolder.getSurface(), false);
            surfaceHolder.addCallback(mComponentListener);
        }
    }

    public void clearVideoSurfaceHolder(SurfaceHolder surfaceHolder) {
        if (surfaceHolder != null && surfaceHolder == mSurfaceHolder) {
            setVideoSurfaceHolder(null);
        }
    }

    public void setVideoSurfaceView(SurfaceView surfaceView) {
        setVideoSurfaceHolder(surfaceView == null ? null : surfaceView.getHolder());
    }

    public void clearVideoSurfaceView(SurfaceView surfaceView) {
        clearVideoSurfaceHolder(surfaceView == null ? null : surfaceView.getHolder());
    }

    public void setVideoTextureView(TextureView textureView) {
        removeSurfaceCallbacks();
        mTextureView = textureView;
        if (textureView == null) {
            setVideoSurfaceInternal(null, true);
        } else {
            if (textureView.getSurfaceTextureListener() != null) {
                Log.w(TAG, "Replacing existing SurfaceTextureListener.");
            }
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            setVideoSurfaceInternal(surfaceTexture == null ? null : new Surface(surfaceTexture), true);
            textureView.setSurfaceTextureListener(mComponentListener);
        }
    }

    public void clearVideoTextureView(TextureView textureView) {
        if (textureView != null && textureView == mTextureView) {
            setVideoTextureView(null);
        }
    }

    public void setAudioStreamType(@C.StreamType int audioStreamType) {
        mAudioStreamType = audioStreamType;
        ExoPlayerMessage[] messages = new ExoPlayerMessage[mAudioRendererCount];
        int count = 0;
        for (Renderer renderer : mRendererList) {
            if (renderer.getTrackType() == C.TRACK_TYPE_AUDIO) {
                messages[count++] = new ExoPlayerMessage(renderer, C.MSG_SET_AUDIO_ATTRIBUTES, audioStreamType);
            }
        }
        mExoPlayer.sendMessages(messages);
    }

    public
    @C.StreamType
    int getAudioStreamType() {
        return mAudioStreamType;
    }

    public void setVolume(float audioVolume) {
        mAudioVolume = audioVolume;
        ExoPlayerMessage[] messages = new ExoPlayerMessage[mAudioRendererCount];
        int count = 0;
        for (Renderer renderer : mRendererList) {
            if (renderer.getTrackType() == C.TRACK_TYPE_AUDIO) {
                messages[count++] = new ExoPlayerMessage(renderer, C.MSG_SET_VOLUME, audioVolume);
            }
        }
        mExoPlayer.sendMessages(messages);
    }

    public float getVolume() {
        return mAudioVolume;
    }

    @TargetApi(23)
    public void setPlaybackParams(@Nullable PlaybackParams params) {
        PlaybackParameters playbackParameters;
        if (params != null) {
            params.allowDefaults();
            playbackParameters = new PlaybackParameters(params.getSpeed(), params.getPitch());
        } else {
            playbackParameters = null;
        }
        setPlaybackParameters(playbackParameters);
    }

    public Format getVideoFormat() {
        return mVideoFormat;
    }

    public Format getAudioFormat() {
        return mAudioFormat;
    }

    public int getAudioSessionId() {
        return mAudioSessionId;
    }

    public DecoderCounters getVideoDecoderCounters() {
        return mVideoDecoderCounters;
    }

    public DecoderCounters getAudioDecoderCounters() {
        return mAudioDecoderCounters;
    }

    public void setTextOutput(TextRenderer.Output output) {
        mTextOutput = output;
    }

    public void clearTextOutput(TextRenderer.Output output) {
        if (mTextOutput == output) {
            mTextOutput = null;
        }
    }

    public void setMetadataOutput(MetadataRenderer.Output output) {
        mMetadataOutput = output;
    }

    public void clearMetadataOutput(MetadataRenderer.Output output) {
        if (mMetadataOutput == output) {
            mMetadataOutput = null;
        }
    }

    public void addVideoRendererEventListener(VideoRendererEventListener listener) {
        mVideoRendererEventListeners.add(listener);
    }

    public void removeVideoRendererEventListener(VideoRendererEventListener listener) {
        mVideoRendererEventListeners.remove(listener);
    }

    public void addAudioRendererEventListener(AudioRendererEventListener listener) {
        mAudioRendererEventListeners.add(listener);
    }

    public void removeAudioRendererEventListener(AudioRendererEventListener listener) {
        mAudioRendererEventListeners.remove(listener);
    }

    @Nullable
    @Override
    public VideoComponent getVideoComponent() {
        return null;
    }

    @Nullable
    @Override
    public TextComponent getTextComponent() {
        return null;
    }

    @Override
    public void addListener(Player.EventListener listener) {
        mExoPlayer.addListener(listener);
    }

    @Override
    public void removeListener(Player.EventListener listener) {
        mExoPlayer.removeListener(listener);
    }

    @Override
    public int getPlaybackState() {
        return mExoPlayer.getPlaybackState();
    }

    public void prepare(String mediaPath) {
        prepare(Uri.parse(mediaPath));
    }

    public void prepare(String mediaPath, boolean resetPosition, boolean resetState) {
        prepare(Uri.parse(mediaPath), resetPosition, resetState);
    }

    public void prepare(Uri mediaUri) {
        prepare(mediaUri != null ? mMediaSourceProvider.generate(mContext, mMainHandler, mediaUri, mBandwidthMeter)
                : null);
    }

    public void prepare(Uri mediaUri, boolean resetPosition, boolean resetState) {
        prepare(mediaUri != null ? mMediaSourceProvider.generate(mContext, mMainHandler, mediaUri, mBandwidthMeter)
                : null, resetPosition, resetState);
    }

    public boolean isPlaying() {
        return mExoPlayer.getPlaybackState() == Player.STATE_READY && mExoPlayer.getPlayWhenReady();
    }

    @Override
    public Looper getPlaybackLooper() {
        return mExoPlayer.getPlaybackLooper();
    }

    @Override
    public void prepare(MediaSource mediaSource) {
        mExoPlayer.prepare(mediaSource);
    }

    @Override
    public void prepare(MediaSource mediaSource, boolean resetPosition, boolean resetState) {
        mExoPlayer.prepare(mediaSource, resetPosition, resetState);
    }

    @Override
    public PlayerMessage createMessage(PlayerMessage.Target target) {
        return null;
    }

    @Override
    public void setPlayWhenReady(boolean playWhenReady) {
        mExoPlayer.setPlayWhenReady(playWhenReady);
    }

    @Override
    public boolean getPlayWhenReady() {
        return mExoPlayer.getPlayWhenReady();
    }

    @Override
    public void setRepeatMode(int repeatMode) {
        mExoPlayer.setRepeatMode(repeatMode);
    }

    @Override
    public int getRepeatMode() {
        return mExoPlayer.getRepeatMode();
    }

    @Override
    public void setShuffleModeEnabled(boolean shuffleModeEnabled) {

    }

    @Override
    public boolean getShuffleModeEnabled() {
        return false;
    }

    @Override
    public boolean isLoading() {
        return mExoPlayer.isLoading();
    }

    @Override
    public void seekToDefaultPosition() {
        mExoPlayer.seekToDefaultPosition();
    }

    @Override
    public void seekToDefaultPosition(int windowIndex) {
        mExoPlayer.seekToDefaultPosition(windowIndex);
    }

    @Override
    public void seekTo(long positionMs) {
        mExoPlayer.seekTo(positionMs);
    }

    @Override
    public void seekTo(int windowIndex, long positionMs) {
        mExoPlayer.seekTo(windowIndex, positionMs);
    }

    @Override
    public void setPlaybackParameters(PlaybackParameters playbackParameters) {
        mExoPlayer.setPlaybackParameters(playbackParameters);
    }

    @Override
    public PlaybackParameters getPlaybackParameters() {
        return mExoPlayer.getPlaybackParameters();
    }

    @Override
    public void stop() {
        mExoPlayer.stop();
    }

    @Override
    public void stop(boolean reset) {

    }

    @Override
    public void release() {
        mExoPlayer.release();
        removeSurfaceCallbacks();
        if (mSurface != null) {
            if (mOwnsSurface) {
                mSurface.release();
            }
            mSurface = null;
        }
    }

    @Override
    public void sendMessages(ExoPlayerMessage... messages) {
        mExoPlayer.sendMessages(messages);
    }

    @Override
    public void blockingSendMessages(ExoPlayerMessage... messages) {
        mExoPlayer.blockingSendMessages(messages);
    }

    @Override
    public void setSeekParameters(@Nullable SeekParameters seekParameters) {

    }

    @Override
    public int getRendererCount() {
        return mExoPlayer.getRendererCount();
    }

    @Override
    public int getRendererType(int index) {
        return mExoPlayer.getRendererType(index);
    }

    @Override
    public TrackGroupArray getCurrentTrackGroups() {
        return mExoPlayer.getCurrentTrackGroups();
    }

    @Override
    public TrackSelectionArray getCurrentTrackSelections() {
        return mExoPlayer.getCurrentTrackSelections();
    }

    @Override
    public Timeline getCurrentTimeline() {
        return mExoPlayer.getCurrentTimeline();
    }

    @Override
    public Object getCurrentManifest() {
        return mExoPlayer.getCurrentManifest();
    }

    @Override
    public int getCurrentPeriodIndex() {
        return mExoPlayer.getCurrentPeriodIndex();
    }

    @Override
    public int getCurrentWindowIndex() {
        return mExoPlayer.getCurrentWindowIndex();
    }

    @Override
    public int getNextWindowIndex() {
        return 0;
    }

    @Override
    public int getPreviousWindowIndex() {
        return 0;
    }

    @Override
    public long getDuration() {
        return mExoPlayer.getDuration();
    }

    @Override
    public long getCurrentPosition() {
        return mExoPlayer.getCurrentPosition();
    }

    @Override
    public long getBufferedPosition() {
        return mExoPlayer.getBufferedPosition();
    }

    @Override
    public int getBufferedPercentage() {
        return mExoPlayer.getBufferedPercentage();
    }

    @Override
    public boolean isCurrentWindowDynamic() {
        return mExoPlayer.isCurrentWindowDynamic();
    }

    @Override
    public boolean isCurrentWindowSeekable() {
        return mExoPlayer.isCurrentWindowSeekable();
    }

    @Override
    public boolean isPlayingAd() {
        return mExoPlayer.isPlayingAd();
    }

    @Override
    public int getCurrentAdGroupIndex() {
        return mExoPlayer.getCurrentAdGroupIndex();
    }

    @Override
    public int getCurrentAdIndexInAdGroup() {
        return mExoPlayer.getCurrentAdIndexInAdGroup();
    }

    @Override
    public long getContentPosition() {
        return mExoPlayer.getContentPosition();
    }

    private void removeSurfaceCallbacks() {
        if (mTextureView != null) {
            if (mTextureView.getSurfaceTextureListener() != mComponentListener) {
                Log.w(TAG, "SurfaceTextureListener already unset or replaced.");
            } else {
                mTextureView.setSurfaceTextureListener(null);
            }
            mTextureView = null;
        }
        if (mSurfaceHolder != null) {
            mSurfaceHolder.removeCallback(mComponentListener);
            mSurfaceHolder = null;
        }
    }

    private void setVideoSurfaceInternal(Surface surface, boolean ownsSurface) {
        // Note: We don't turn this method into a no-op if the surface is being
        // replaced with itself
        // so as to ensure onRenderedFirstFrame callbacks are still called in
        // this case.
        ExoPlayerMessage[] messages = new ExoPlayerMessage[mVideoRendererCount];
        int count = 0;
        for (Renderer renderer : mRendererList) {
            if (renderer.getTrackType() == C.TRACK_TYPE_VIDEO) {
                messages[count++] = new ExoPlayerMessage(renderer, C.MSG_SET_SURFACE, surface);
            }
        }
        if (mSurface != null && mSurface != surface) {
            // If we created this surface, we are responsible for releasing it.
            if (mOwnsSurface) {
                mSurface.release();
            }
            // We're replacing a surface. Block to ensure that it's not accessed
            // after the method returns.
            mExoPlayer.blockingSendMessages(messages);
        } else {
            mExoPlayer.sendMessages(messages);
        }
        mSurface = surface;
        mOwnsSurface = ownsSurface;
    }

    private class ComponentListener implements VideoRendererEventListener, AudioRendererEventListener,
            TextRenderer.Output, MetadataRenderer.Output, Callback, SurfaceTextureListener {

        @Override
        public void onVideoEnabled(DecoderCounters counters) {
            mVideoDecoderCounters = counters;
            for (VideoRendererEventListener listener : mVideoRendererEventListeners) {
                listener.onVideoEnabled(counters);
            }
        }

        @Override
        public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs,
                                              long initializationDurationMs) {
            for (VideoRendererEventListener listener : mVideoRendererEventListeners) {
                listener.onVideoDecoderInitialized(decoderName, initializedTimestampMs,
                        initializationDurationMs);
            }
        }

        @Override
        public void onVideoInputFormatChanged(Format format) {
            mVideoFormat = format;
            for (VideoRendererEventListener listener : mVideoRendererEventListeners) {
                listener.onVideoInputFormatChanged(format);
            }
         }

        @Override
        public void onDroppedFrames(int count, long elapsedMs) {
            for (VideoRendererEventListener listener : mVideoRendererEventListeners) {
                listener.onDroppedFrames(count, elapsedMs);
            }
        }

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                       float pixelWidthHeightRatio) {
            for (VideoRendererEventListener listener : mVideoRendererEventListeners) {
                listener.onVideoSizeChanged(width, height, unappliedRotationDegrees,
                        pixelWidthHeightRatio);
            }
        }

        @Override
        public void onRenderedFirstFrame(Surface surface) {
            for (VideoRendererEventListener listener : mVideoRendererEventListeners) {
                listener.onRenderedFirstFrame(surface);
            }
        }

        @Override
        public void onVideoDisabled(DecoderCounters counters) {
            for (VideoRendererEventListener listener : mVideoRendererEventListeners) {
                listener.onVideoDisabled(counters);
            }
            mVideoFormat = null;
            mVideoDecoderCounters = null;
        }

        @Override
        public void onAudioEnabled(DecoderCounters counters) {
            mAudioDecoderCounters = counters;
            for (AudioRendererEventListener listener : mAudioRendererEventListeners) {
                listener.onAudioEnabled(counters);
            }
        }

        @Override
        public void onAudioSessionId(int audioSessionId) {
            mAudioSessionId = audioSessionId;
            for (AudioRendererEventListener listener : mAudioRendererEventListeners) {
                listener.onAudioSessionId(audioSessionId);
            }
        }

        @Override
        public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs,
                                              long initializationDurationMs) {
            for (AudioRendererEventListener listener : mAudioRendererEventListeners) {
                listener.onAudioDecoderInitialized(decoderName, initializedTimestampMs,
                        initializationDurationMs);
            }
        }

        @Override
        public void onAudioInputFormatChanged(Format format) {
            mAudioFormat = format;
            for (AudioRendererEventListener listener : mAudioRendererEventListeners) {
                listener.onAudioInputFormatChanged(format);
            }
        }

        @Override
        public void onAudioSinkUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
            for (AudioRendererEventListener listener : mAudioRendererEventListeners) {
                listener.onAudioSinkUnderrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
            }
        }

        @Override
        public void onAudioDisabled(DecoderCounters counters) {
            for (AudioRendererEventListener listener : mAudioRendererEventListeners) {
                listener.onAudioDisabled(counters);
            }
            mAudioFormat = null;
            mAudioDecoderCounters = null;
            mAudioSessionId = C.AUDIO_SESSION_ID_UNSET;
        }

        @Override
        public void onCues(List<Cue> cues) {
            if (mTextOutput != null) {
                mTextOutput.onCues(cues);
            }
        }

        @Override
        public void onMetadata(Metadata metadata) {
            if (mMetadataOutput != null) {
                mMetadataOutput.onMetadata(metadata);
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            setVideoSurfaceInternal(holder.getSurface(), false);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            setVideoSurfaceInternal(null, false);
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            setVideoSurfaceInternal(new Surface(surfaceTexture), true);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            setVideoSurfaceInternal(null, true);
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }

    }
}
