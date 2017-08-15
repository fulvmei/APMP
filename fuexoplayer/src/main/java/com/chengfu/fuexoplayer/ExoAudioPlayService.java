package com.chengfu.fuexoplayer;

import static android.media.AudioManager.AUDIOFOCUS_GAIN;
import static android.media.AudioManager.AUDIOFOCUS_LOSS;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;

import com.chengfu.fuexoplayer.audio.IAudioPlay;
import com.chengfu.fuexoplayer.util.FuPlayerUtil;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayer.EventListener;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * 音频播放服务
 * 
 * @author ChengFu
 *
 */
public class ExoAudioPlayService extends Service implements IAudioPlay {

	public static final String TAG = "ExoAudioPlayService";// tag

	public static final String NOTIF_ACTION = "com.chengfu.exoplayer.ExoAudioPlayService";
	public static final int NOTIF_ID = 10000;
	public static final int NOTIF_ACTION_PLAY_PAUSE = 10001;
	public static final int NOTIF_ACTION_CANCEL = 10002;

	private String mAudioPath;
	private ExoMediaPlayer mExoMediaPlayer;
	private ExoPlayerListener mExoPlayerListener;

	private final IBinder mBinder = new LocalBinder();

	private WifiManager.WifiLock mWifiLock;// WIFI休眠唤醒锁
	private AudioManager mAudioManager;

	private Notification mNotification;
	private RemoteViews mRemoteViews;

	private AudioListener mAudioListener;

	private boolean mAudioFocusPause;

	public static interface AudioListener {
		public void onLoadingChanged(boolean isLoading);

		public void onPlayPauseChanged(boolean play);

		public void onError(ExoPlaybackException error);

		public void onCompletion();
	}

	private OnCancelListener mOnCancelListener;

	public interface OnCancelListener {
		void onCancel();
	}

	public void setOnCancelListener(OnCancelListener onCancelListener) {
		this.mOnCancelListener = onCancelListener;
	}

	public void setAudioListener(AudioListener audioListener) {
		mAudioListener = audioListener;
	}

	public AudioListener getAudioListener() {
		return mAudioListener;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// 注册Notification点击广播
		IntentFilter filter = new IntentFilter();
		filter.addAction(NOTIF_ACTION);
		registerReceiver(mNotificationReceiver, filter);

		// 音频管理器
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		// 获取WIFI休眠唤醒锁
		mWifiLock = ((WifiManager) getSystemService(WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, TAG);

		mAudioManager.requestAudioFocus(mAudioFocusChangeListener, AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN);

		
		initMediaPlayer();
		
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class LocalBinder extends Binder {
		public ExoAudioPlayService getService() {
			return ExoAudioPlayService.this;
		}
	}

	private void initMediaPlayer() {
		if (mExoMediaPlayer == null) {
			mExoPlayerListener = new ExoPlayerListener();
			mExoMediaPlayer = new ExoMediaPlayer(this);

			mExoMediaPlayer.addAudioRendererEventListener(mExoPlayerListener);
			mExoMediaPlayer.addListener(mExoPlayerListener);

			mExoMediaPlayer.setPlayWhenReady(false);
		}
	}

	protected RemoteViews initRemoteViews() {
		RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_default_audio_play);
		remoteViews.setImageViewResource(R.id.media_imgViewIocn, FuPlayerUtil.getAppIcon(this));
		remoteViews.setTextViewText(R.id.media_tvTitle, mAudioPath);
		remoteViews.setTextViewText(R.id.media_tvInfo, "");

		Intent intentPlayPause = new Intent(NOTIF_ACTION);
		intentPlayPause.putExtra(NOTIF_ACTION, NOTIF_ACTION_PLAY_PAUSE);
		PendingIntent pendingIntentPlayPause = PendingIntent.getBroadcast(this, 0, intentPlayPause,
				PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.media_imgBtnPlayPause, pendingIntentPlayPause);

		Intent intentCancel = new Intent(NOTIF_ACTION);
		intentCancel.putExtra(NOTIF_ACTION, NOTIF_ACTION_CANCEL);
		PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(this, 1, intentCancel,
				PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.media_imgBtnCancel, pendingIntentCancel);
		return remoteViews;
	}

	protected Notification initNotification() {
		Log.i(TAG, "初始化Notifition");
		NotificationCompat.Builder mNotificationBuilder = new NotificationCompat.Builder(this);
		mNotificationBuilder.setSmallIcon(FuPlayerUtil.getAppIcon(this));

		mRemoteViews = initRemoteViews();
		if (mRemoteViews != null) {
			mNotificationBuilder.setContent(mRemoteViews);
		}

		PendingIntent pendingActivity = initNotificationClickIntent();
		if (pendingActivity != null) {
			mNotificationBuilder.setContentIntent(pendingActivity);
		}
		Notification notification = mNotificationBuilder.build();
		notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT | Notification.FLAG_INSISTENT;
		return notification;
	}

	protected PendingIntent initNotificationClickIntent() {
		return null;
	}

	private void updataNotifition() {
		if (mNotification == null) {
			mNotification = initNotification();
		}
		if (mNotification == null) {
			return;
		}
		Log.i(TAG, "更新Notifition");
		updataNotifitionData(mRemoteViews);
		startForeground(NOTIF_ID, mNotification);
	}

	protected void updataNotifitionData(RemoteViews remoteViews) {
		remoteViews.setImageViewResource(R.id.media_imgViewIocn, FuPlayerUtil.getAppIcon(this));
		remoteViews.setTextViewText(R.id.media_tvTitle, mAudioPath);
		remoteViews.setTextViewText(R.id.media_tvInfo, "");
		if (isPlaying()) {
			remoteViews.setImageViewResource(R.id.media_imgBtnPlayPause, R.mipmap.ic_default_pause);
		} else {
			remoteViews.setImageViewResource(R.id.media_imgBtnPlayPause, R.mipmap.ic_default_play);
		}
	}

	public boolean isInPlaybackState() {
		return mExoMediaPlayer.getPlaybackState() == ExoPlayer.STATE_READY;
	}

	@Override
	public void setAudioPath(String path) {
		mAudioPath = path;
		mExoMediaPlayer.prepare(path);
	}

	@Override
	public String getAudioPath() {
		return mAudioPath;
	}

	@Override
	public void start() {
		mExoMediaPlayer.setPlayWhenReady(true);
		updataNotifition();
		if (mAudioListener != null) {
			mAudioListener.onPlayPauseChanged(isPlaying());
		}
		mAudioManager.requestAudioFocus(mAudioFocusChangeListener, AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN);

	}

	@Override
	public void pause() {
		mExoMediaPlayer.setPlayWhenReady(false);
		updataNotifition();
		if (mAudioListener != null) {
			mAudioListener.onPlayPauseChanged(isPlaying());
		}
	}

//	@Override
//	public boolean restart() {
//		int playbackState = mExoMediaPlayer.getPlaybackState();
//		if (playbackState != ExoPlayer.STATE_IDLE && playbackState != ExoPlayer.STATE_ENDED) {
//			return false;
//		}
//		return true;
//	}
//
//	@Override
//	public void suspend() {
//		mExoMediaPlayer.release();
//	}
//
//	@Override
//	public void stopPlayback(boolean clearSurface) {
//		mExoMediaPlayer.stop();
//	}

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

//	@Override
//	public boolean canPause() {
//		return false;
//	}
//
//	@Override
//	public boolean canSeekBackward() {
//		return false;
//	}
//
//	@Override
//	public boolean canSeekForward() {
//		return false;
//	}

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
//		stopPlayback(true);
		setAudioPath(null);
	}

	@Override
	public void release() {
		mExoMediaPlayer.release();
	}

	private final class ExoPlayerListener implements AudioRendererEventListener, EventListener {

		@Override
		public void onTimelineChanged(Timeline timeline, Object manifest) {

		}

		@Override
		public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

		}

		@Override
		public void onLoadingChanged(boolean isLoading) {
			if (mAudioListener != null) {
				mAudioListener.onLoadingChanged(isLoading);
			}
		}

		@Override
		public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
			System.out.println("StateChanged playWhenReady=" + playWhenReady + " playbackState=" + playbackState);
			if (playbackState == ExoPlayer.STATE_READY && mAudioListener != null) {
				mAudioListener.onLoadingChanged(false);
			}
			if (playbackState == ExoPlayer.STATE_ENDED) {
				mAudioListener.onCompletion();
			}
		}

		@Override
		public void onPlayerError(ExoPlaybackException error) {
			if (mAudioListener != null) {
				mAudioListener.onError(error);
			}
		}

		@Override
		public void onRepeatModeChanged(int repeatMode) {

		}

		@Override
		public void onPositionDiscontinuity() {

		}

		@Override
		public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

		}

		@Override
		public void onAudioEnabled(DecoderCounters counters) {

		}

		@Override
		public void onAudioSessionId(int audioSessionId) {

		}

		@Override
		public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs,
				long initializationDurationMs) {

		}

		@Override
		public void onAudioInputFormatChanged(Format format) {

		}

		@Override
		public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {

		}

		@Override
		public void onAudioDisabled(DecoderCounters counters) {

		}

	}

	private final OnAudioFocusChangeListener mAudioFocusChangeListener = new OnAudioFocusChangeListener() {

		@Override
		public void onAudioFocusChange(int focusChange) {
			switch (focusChange) {
			case AUDIOFOCUS_GAIN:
				if (isInPlaybackState() && mAudioFocusPause) {
					start();
					mAudioFocusPause = false;
				}
				break;
			case AUDIOFOCUS_LOSS:
				mAudioFocusPause = isPlaying() || mAudioFocusPause;
				pause();
				break;
			case AUDIOFOCUS_LOSS_TRANSIENT:
				mAudioFocusPause = isPlaying() || mAudioFocusPause;
				pause();
				break;
			case AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
//				duck();
				break;
			}
		}
	};

	private final BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(NOTIF_ACTION)) {
				int notif_action = intent.getIntExtra(NOTIF_ACTION, NOTIF_ACTION_PLAY_PAUSE);
				switch (notif_action) {
				case NOTIF_ACTION_PLAY_PAUSE:
					Log.i(TAG, "Notifition播放暂停按钮点击");
					if (isPlaying()) {
						pause();
					} else {
						if (isInPlaybackState()) {
							start();
						} else {
							start();
							initMediaPlayer();
						}
					}
					break;
				case NOTIF_ACTION_CANCEL:
					Log.i(TAG, "Notifition取消按钮点击");
					if (mOnCancelListener != null) {
						mOnCancelListener.onCancel();
					}
					stopSelf();
					break;
				}
			}
		}
	};

}
