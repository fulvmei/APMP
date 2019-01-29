package com.chengfu.fuexoplayer.code.source.builder;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.chengfu.fuexoplayer.ExoMedia;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;

public abstract class MediaSourceBuilder {

    @NonNull
    public abstract MediaSource build(@NonNull Context context, @NonNull Uri uri, @NonNull String userAgent, @NonNull Handler handler, @Nullable TransferListener transferListener);

    @NonNull
    protected DataSource.Factory buildDataSourceFactory(@NonNull Context context, @NonNull String userAgent, @Nullable TransferListener listener) {
        ExoMedia.HttpDataSourceFactoryProvider provider = ExoMedia.Data.httpDataSourceFactoryProvider;
        HttpDataSource.BaseFactory dataSourceFactory = provider != null ? provider.provide(userAgent, listener) : new DefaultHttpDataSourceFactory(userAgent, listener);

        return new DefaultDataSourceFactory(context, listener, dataSourceFactory);
    }
}
