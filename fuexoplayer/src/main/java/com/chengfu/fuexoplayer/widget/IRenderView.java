package com.chengfu.fuexoplayer.widget;

import com.google.android.exoplayer2.ExoPlayer;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;

public interface IRenderView {
	View getView();

	boolean shouldWaitForResize();

	void setVideoSize(int videoWidth, int videoHeight);

	void setVideoSampleAspectRatio(int videoSarNum, int videoSarDen);

	void setVideoRotation(int degree);

	void setAspectRatio(int aspectRatio);

	Bitmap getScreenshot();

	void addRenderCallback(IRenderCallback callback);

	void removeRenderCallback(IRenderCallback callback);

	interface ISurfaceHolder {
		void bindToMediaPlayer(ExoPlayer mp);

		IRenderView getRenderView();

		SurfaceHolder getSurfaceHolder();

		Surface openSurface();

		SurfaceTexture getSurfaceTexture();
	}

	interface IRenderCallback {
		/**
		 * @param holder
		 * @param width
		 *            could be 0
		 * @param height
		 *            could be 0
		 */
		void onSurfaceCreated(ISurfaceHolder holder, int width, int height);

		/**
		 * @param holder
		 * @param format
		 *            could be 0
		 * @param width
		 * @param height
		 */
		void onSurfaceChanged(ISurfaceHolder holder, int format, int width, int height);

		void onSurfaceDestroyed(ISurfaceHolder holder);
	}
}
