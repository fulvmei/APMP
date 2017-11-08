package com.chengfu.fuexoplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.app.NotificationBuilderWithBuilderAccessor;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.chengfu.fuexoplayer.util.ResourceHelper;

/**
 * 音频播放Notificatio管理器
 * Created by ChengFu on 2017/9/26.
 */

public class AudioNotificationManager extends BroadcastReceiver {
    private static final String TAG = "AudioNotification";

    private static final int NOTIFICATION_ID = 666;
    private static final int REQUEST_CODE = 100;

    public static final String ACTION_PLAY = "com.chengfu.fuexoplayer.play";
    public static final String ACTION_PAUSE = "com.chengfu.fuexoplayer.pause";
    public static final String ACTION_CLOSE = "com.chengfu.fuexoplayer.close";

    private final Service mService;
    private final NotificationManagerCompat mNotificationManager;

    private final PendingIntent mPlayIntent;
    private final PendingIntent mPauseIntent;
    private final PendingIntent mCloseIntent;

    private final int mNotificationColor;

    private boolean mStarted = false;

    public AudioNotificationManager(Service service) {
        mService = service;

        mNotificationColor = ResourceHelper.getThemeColor(mService, R.attr.colorPrimary,
                Color.DKGRAY);

        mNotificationManager = NotificationManagerCompat.from(service);

        String pkg = mService.getPackageName();
        mPlayIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPauseIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mCloseIntent = PendingIntent.getBroadcast(mService, REQUEST_CODE,
                new Intent(ACTION_CLOSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);

        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        mNotificationManager.cancelAll();
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before {@link #stopNotification} is called.
     */
    public void startNotification() {
        if (!mStarted) {
            // The notification must be updated after setting started to true
            Notification notification = createNotification();
            if (notification != null) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_PLAY);
                filter.addAction(ACTION_PAUSE);
                filter.addAction(ACTION_CLOSE);
                mService.registerReceiver(this, filter);

                mService.startForeground(NOTIFICATION_ID, notification);
                mStarted = true;
            }
        }
    }

    public void stopNotification() {
        if (mStarted) {
            mStarted = false;
            try {
                mNotificationManager.cancel(NOTIFICATION_ID);
                mService.unregisterReceiver(this);
            } catch (IllegalArgumentException ex) {
                // ignore if the receiver is not registered.
            }
            mService.stopForeground(true);
        }
    }

    private Notification createNotification() {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mService, null);
        notificationBuilder.addAction(R.drawable.ic_default_play, "播放", mPlayIntent);
        notificationBuilder.addAction(R.drawable.ic_default_clear, "关闭", mCloseIntent);

        Bitmap art = BitmapFactory.decodeResource(mService.getResources(),
                R.drawable.ic_default_art);
        notificationBuilder
                .setStyle(new MediaStyle())
                .setColor(mNotificationColor)
                .setSmallIcon(R.drawable.ic_launcher)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setUsesChronometer(true)
                .setContentIntent(null)
                .setContentTitle("这是一个标题1111111111111111111111111111111111111111111111111111111111111111111111111")
                .setContentInfo("这是内容22222222222222222222222222222222222222222222222222222222222222222222222222222")
                .setLargeIcon(art);
        return notificationBuilder.build();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.d(TAG, "Received intent with action " + action);
        switch (action) {
            case ACTION_PLAY:

                break;
            case ACTION_PAUSE:

                break;
            case ACTION_CLOSE:

                break;
            default:
        }
    }

    public static class MediaStyle extends NotificationCompat.Style {
        @Override
        public RemoteViews makeContentView(NotificationBuilderWithBuilderAccessor builder) {
            RemoteViews remoteViews = new RemoteViews(mBuilder.mContext.getPackageName(),
                    R.layout.notification_default_audio_play);
            remoteViews.setImageViewBitmap(R.id.media_imgViewIocn, mBuilder.mLargeIcon);
            remoteViews.setTextViewText(R.id.media_tvTitle, mBuilder.mContentTitle);
            remoteViews.setTextViewText(R.id.media_tvInfo, mBuilder.mContentInfo);

            return remoteViews;
        }
    }


    public static class MediaStyle1 {

//        private static final int MAX_MEDIA_BUTTONS_IN_COMPACT = 3;
//        private static final int MAX_MEDIA_BUTTONS = 5;
//
//        int[] mActionsToShowInCompact = null;
//        MediaSessionCompat.Token mToken;
//        boolean mShowCancelButton;
//        PendingIntent mCancelButtonIntent;

//        public MediaStyle() {
//        }
//
//        public MediaStyle(android.support.v4.app.NotificationCompat.Builder builder) {
//            setBuilder(builder);
//        }

        /**
         * Requests up to 3 actions (by index in the order of addition) to be shown in the compact
         * notification view.
         *
         * @param actions the indices of the actions to show in the compact notification view
         */
//        public MediaStyle setShowActionsInCompactView(int...actions) {
//            mActionsToShowInCompact = actions;
//            return this;
//        }
//
//        public MediaStyle setShowCancelButton(boolean show) {
//            if (Build.VERSION.SDK_INT < 21) {
//                mShowCancelButton = show;
//            }
//            return this;
//        }
//
//        public MediaStyle setCancelButtonIntent(PendingIntent pendingIntent) {
//            mCancelButtonIntent = pendingIntent;
//            return this;
//        }

//        /**
//         * @hide
//         */
//        @RestrictTo(LIBRARY_GROUP)
//        @Override
//        public void apply(NotificationBuilderWithBuilderAccessor builder) {
//            if (Build.VERSION.SDK_INT >= 21) {
//                builder.getBuilder().setStyle(
//                        fillInMediaStyle(new Notification.MediaStyle()));
//            } else if (mShowCancelButton) {
//                builder.getBuilder().setOngoing(true);
//            }
//        }
//
//        @RequiresApi(21)
//        Notification.MediaStyle fillInMediaStyle(Notification.MediaStyle style) {
//            if (mActionsToShowInCompact != null) {
//                style.setShowActionsInCompactView(mActionsToShowInCompact);
//            }
//            if (mToken != null) {
//                style.setMediaSession((MediaSession.Token) mToken.getToken());
//            }
//            return style;
//        }
//
//        /**
//         * @hide
//         */
//        @RestrictTo(LIBRARY_GROUP)
//        @Override
//        public RemoteViews makeContentView(NotificationBuilderWithBuilderAccessor builder) {
//            if (Build.VERSION.SDK_INT >= 21) {
//                // No custom content view required
//                return null;
//            }
//            return generateContentView();
//        }
//
//        RemoteViews generateContentView() {
//            RemoteViews view = applyStandardTemplate(false /* showSmallIcon */,
//                    getContentViewLayoutResource(), true /* fitIn1U */);
//
//            final int numActions = mBuilder.mActions.size();
//            final int numActionsInCompact = mActionsToShowInCompact == null
//                    ? 0
//                    : Math.min(mActionsToShowInCompact.length, MAX_MEDIA_BUTTONS_IN_COMPACT);
//            view.removeAllViews(R.id.media_actions);
//            if (numActionsInCompact > 0) {
//                for (int i = 0; i < numActionsInCompact; i++) {
//                    if (i >= numActions) {
//                        throw new IllegalArgumentException(String.format(
//                                "setShowActionsInCompactView: action %d out of bounds (max %d)",
//                                i, numActions - 1));
//                    }
//
//                    final android.support.v4.app.NotificationCompat.Action action =
//                            mBuilder.mActions.get(mActionsToShowInCompact[i]);
//                    final RemoteViews button = generateMediaActionButton(action);
//                    view.addView(R.id.media_actions, button);
//                }
//            }
//            if (mShowCancelButton) {
//                view.setViewVisibility(R.id.end_padder, View.GONE);
//                view.setViewVisibility(R.id.cancel_action, View.VISIBLE);
//                view.setOnClickPendingIntent(R.id.cancel_action, mCancelButtonIntent);
//                view.setInt(R.id.cancel_action, "setAlpha", mBuilder.mContext
//                        .getResources().getInteger(R.integer.cancel_button_image_alpha));
//            } else {
//                view.setViewVisibility(R.id.end_padder, View.VISIBLE);
//                view.setViewVisibility(R.id.cancel_action, View.GONE);
//            }
//            return view;
//        }
//
//        private RemoteViews generateMediaActionButton(
//                android.support.v4.app.NotificationCompat.Action action) {
//            final boolean tombstone = (action.getActionIntent() == null);
//            RemoteViews button = new RemoteViews(mBuilder.mContext.getPackageName(),
//                    R.layout.notification_media_action);
//            button.setImageViewResource(R.id.action0, action.getIcon());
//            if (!tombstone) {
//                button.setOnClickPendingIntent(R.id.action0, action.getActionIntent());
//            }
//            if (Build.VERSION.SDK_INT >= 15) {
//                button.setContentDescription(R.id.action0, action.getTitle());
//            }
//            return button;
//        }
//
//        int getContentViewLayoutResource() {
//            return R.layout.notification_template_media;
//        }
//
//        /**
//         * @hide
//         */
//        @RestrictTo(LIBRARY_GROUP)
//        @Override
//        public RemoteViews makeBigContentView(NotificationBuilderWithBuilderAccessor builder) {
//            if (Build.VERSION.SDK_INT >= 21) {
//                // No custom content view required
//                return null;
//            }
//            return generateBigContentView();
//        }
//
//        RemoteViews generateBigContentView() {
//            final int actionCount = Math.min(mBuilder.mActions.size(), MAX_MEDIA_BUTTONS);
//            RemoteViews big = applyStandardTemplate(false /* showSmallIcon */,
//                    getBigContentViewLayoutResource(actionCount), false /* fitIn1U */);
//
//            big.removeAllViews(R.id.media_actions);
//            if (actionCount > 0) {
//                for (int i = 0; i < actionCount; i++) {
//                    final RemoteViews button = generateMediaActionButton(mBuilder.mActions.get(i));
//                    big.addView(R.id.media_actions, button);
//                }
//            }
//            if (mShowCancelButton) {
//                big.setViewVisibility(R.id.cancel_action, View.VISIBLE);
//                big.setInt(R.id.cancel_action, "setAlpha", mBuilder.mContext
//                        .getResources().getInteger(R.integer.cancel_button_image_alpha));
//                big.setOnClickPendingIntent(R.id.cancel_action, mCancelButtonIntent);
//            } else {
//                big.setViewVisibility(R.id.cancel_action, View.GONE);
//            }
//            return big;
//        }
//
//        int getBigContentViewLayoutResource(int actionCount) {
//            return actionCount <= 3
//                    ? R.layout.notification_template_big_media_narrow
//                    : R.layout.notification_template_big_media;
//        }
    }
}
