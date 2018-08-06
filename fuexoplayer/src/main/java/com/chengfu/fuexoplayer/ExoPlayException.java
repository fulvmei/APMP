package com.chengfu.fuexoplayer;

import android.support.annotation.IntDef;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.util.Assertions;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class ExoPlayException extends Exception {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_SOURCE, TYPE_RENDERER, TYPE_UNEXPECTED})
    public @interface Type {
    }

    public static final int TYPE_SOURCE = 0;

    public static final int TYPE_RENDERER = 1;

    public static final int TYPE_UNEXPECTED = 2;

    @ExoPlayException.Type
    public final int type;

    public final int rendererIndex;

    public static ExoPlayException create(@Type int type, Throwable cause, int rendererIndex) {
        switch (type) {
            case TYPE_SOURCE:
                return new ExoPlayException(TYPE_SOURCE, null, cause, C.INDEX_UNSET);
            case TYPE_RENDERER:
                return new ExoPlayException(TYPE_RENDERER, null, cause, rendererIndex);
            case TYPE_UNEXPECTED:
                return new ExoPlayException(TYPE_UNEXPECTED, null, cause, C.INDEX_UNSET);
        }
        return new ExoPlayException(TYPE_UNEXPECTED, null, cause, C.INDEX_UNSET);
    }

    public static ExoPlayException createForRenderer(Exception cause, int rendererIndex) {
        return new ExoPlayException(TYPE_RENDERER, null, cause, rendererIndex);
    }

    public static ExoPlayException createForSource(IOException cause) {
        return new ExoPlayException(TYPE_SOURCE, null, cause, C.INDEX_UNSET);
    }

    public static ExoPlayException createForUnexpected(RuntimeException cause) {
        return new ExoPlayException(TYPE_UNEXPECTED, null, cause, C.INDEX_UNSET);
    }

    private ExoPlayException(@Type int type, String message, Throwable cause,
                             int rendererIndex) {
        super(message, cause);
        this.type = type;
        this.rendererIndex = rendererIndex;
    }

    /**
     * Retrieves the underlying error when {@link #type} is {@link #TYPE_SOURCE}.
     *
     * @throws IllegalStateException If {@link #type} is not {@link #TYPE_SOURCE}.
     */
    public IOException getSourceException() {
        Assertions.checkState(type == TYPE_SOURCE);
        return (IOException) getCause();
    }

    /**
     * Retrieves the underlying error when {@link #type} is {@link #TYPE_RENDERER}.
     *
     * @throws IllegalStateException If {@link #type} is not {@link #TYPE_RENDERER}.
     */
    public Exception getRendererException() {
        Assertions.checkState(type == TYPE_RENDERER);
        return (Exception) getCause();
    }

    /**
     * Retrieves the underlying error when {@link #type} is {@link #TYPE_UNEXPECTED}.
     *
     * @throws IllegalStateException If {@link #type} is not {@link #TYPE_UNEXPECTED}.
     */
    public RuntimeException getUnexpectedException() {
        Assertions.checkState(type == TYPE_UNEXPECTED);
        return (RuntimeException) getCause();
    }
}
