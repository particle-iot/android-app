package io.spark.core.android.ui.tinker;

import io.spark.core.android.R;
import io.spark.core.android.ui.util.Ui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.AttributeSet;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback,
		Camera.PreviewCallback, Camera.ErrorCallback, Camera.AutoFocusCallback,
		Camera.OnZoomChangeListener, Camera.PictureCallback,
		Camera.ShutterCallback {

	// private Paint paint = new Paint();
	private SurfaceHolder surfaceHolder = null;
	private Camera camera = null;
	private Camera.Parameters cameraParameters = null;
	private ImageView iv = null;

	int camdegrees = 0;

	public static final int STATE_UNINITIALIZED = 0;
	public static final int STATE_INITIALIZED = 1;
	public static final int STATE_INITIALIZATIONFAILED = -1;

	private int state = STATE_UNINITIALIZED;

	private boolean rotate = true;

	public CameraView(Context context) {
		super(context);

		initView();
	}

	public CameraView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		initView();
	}

	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);

		initView();
	}

	@SuppressWarnings("deprecation")
	private void initView() {
		camera = null;
		setFocusable(true);
		surfaceHolder = getHolder();
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		surfaceHolder.addCallback(this);
	}

	protected Camera openCameraImplementation() {
		return Camera.open();
	}

	@SuppressWarnings("deprecation")
	private int naturalOrientation(Context context) {

		WindowManager wm = ((WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE));
		int result = Configuration.ORIENTATION_LANDSCAPE;
		int rotation = wm.getDefaultDisplay().getRotation();
		int width = wm.getDefaultDisplay().getWidth();
		int height = wm.getDefaultDisplay().getHeight();

		if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
			if (width > height) {
				result = Configuration.ORIENTATION_LANDSCAPE;
			} else {
				result = Configuration.ORIENTATION_PORTRAIT;
			}
		} else {
			if (width > height) {
				result = Configuration.ORIENTATION_PORTRAIT;
			} else {
				result = Configuration.ORIENTATION_LANDSCAPE;
			}
		}

		return result;
	}

	private void openCamera() {
		if (camera == null) {
			camera = openCameraImplementation();
			cameraParameters = camera.getParameters();

			camdegrees = ((9 - naturalOrientation(getContext()) - ((WindowManager) getContext()
					.getSystemService(Context.WINDOW_SERVICE))
					.getDefaultDisplay().getRotation()) * 90) % 360;

			Display defaultDisplay = ((WindowManager) getContext()
					.getSystemService(Context.WINDOW_SERVICE))
					.getDefaultDisplay();
			// Rect size = null;
			// defaultDisplay.getRectSize(size);

			int rotation = defaultDisplay.getRotation();
			// int degrees = 0;

			// int w = size.width();
			// int h = size.height();

			if (naturalOrientation(getContext()) == Configuration.ORIENTATION_LANDSCAPE) {
				switch (rotation) {
				case Surface.ROTATION_0:
					// degrees = 0;
					camdegrees = 0;
					break;
				case Surface.ROTATION_90:
					// degrees = 90;
					camdegrees = 270;
					break;
				case Surface.ROTATION_180:
					// degrees = 180;
					camdegrees = 180;
					break;
				case Surface.ROTATION_270:
					// degrees = 270;
					camdegrees = 90;
					break;
				}
			} else {
				switch (rotation) {

				case Surface.ROTATION_0:
					// degrees = 0;
					camdegrees = 90;
					break;
				case Surface.ROTATION_90:
					// degrees = 90;
					camdegrees = 0;
					break;
				case Surface.ROTATION_180:
					// degrees = 180;
					camdegrees = 270;
					break;
				case Surface.ROTATION_270:
					// degrees = 270;
					camdegrees = 180;
					break;
				}
			}

			if (rotate)
				camera.setDisplayOrientation(camdegrees);

			// int orientation = getResources().getConfiguration().orientation;

			// DisplayMetrics outMetrics = new DisplayMetrics();
			// defaultDisplay.getMetrics(outMetrics);

			if (state == STATE_INITIALIZED) {
				try {
					camera.setPreviewDisplay(surfaceHolder);
					camera.setPreviewCallback(this);
				} catch (IOException e) {

				}
			}

		}
	}

	private void setPreviewParameter(Camera camera, Camera.Parameters params) {
		List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
		long mydiag2 = getHeight() * getHeight() + getWidth() * getWidth();
		Camera.Size psize = null;
		for (Camera.Size size : previewSizes) {
			long diag2 = size.height * size.height + size.width * size.width;
			if (diag2 >= mydiag2) {
				break;
			}
			psize = size;
		}
		if (psize != null) {

			params.setPreviewSize(psize.width, psize.height);

		}

		List<String> focusModes = params.getSupportedFocusModes();
		if (focusModes.contains(Camera.Parameters.FOCUS_MODE_EDOF)) {
			params.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);
		} else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
			params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		}

		camera.setParameters(params);
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

		if (holder.getSurface() == null) {
			return;
		}

		if (camera != null) {
			try {
				camera.stopPreview();
			} catch (Exception e) {

			}
			try {
				camera.setPreviewDisplay(holder);
				camera.startPreview();
			} catch (Exception e) {

			}
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {

		try {

			openCamera();
			if (camera != null) {

				camera.setPreviewDisplay(holder);
				camera.setPreviewCallback(this);
				setState(STATE_INITIALIZED);
			} else {
				setState(STATE_INITIALIZATIONFAILED);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block

		}

	}

	public void surfaceDestroyed(SurfaceHolder holder) {

		try {

			if (camera != null) {
				camera.setPreviewCallback(null);
				camera.setPreviewDisplay(null);
				camera.release();
				camera = null;
				surfaceHolder = null;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block

		}
	}

	public void startPreview() {
		openCamera();
		if (state == STATE_INITIALIZED) {
			if (camera != null) {
				setPreviewParameter(camera, cameraParameters);
				camera.startPreview();
			}
		} else {
			Thread delayedStart = new Thread() {
				@Override
				public void run() {
					while (state == STATE_UNINITIALIZED) {
						try {
							sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							break;
						}
					}

					if (state == STATE_INITIALIZED) {
						post(new Runnable() {

							public void run() {
								if (camera != null) {
									setPreviewParameter(camera,
											cameraParameters);
									camera.startPreview();
								}
							}
						});
					}
				}
			};

			delayedStart.start();
		}
	}

	public void stopPreview() {
		if (camera != null) {
			camera.stopPreview();
		}
	}

	public void releaseCamera() {
		if (camera != null) {
			camera.setPreviewCallback(null);
			camera.release();

			camera = null;
		}
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public Camera getCamera() {
		return camera;
	}

	// Callbacks
	// @Override
	public void onShutter() {

	}

	// @Override
	public void onPictureTaken(byte[] data, Camera camera) {

	}

	// @Override
	public void onZoomChange(int zoomValue, boolean stopped, Camera camera) {

	}

	// @Override
	public void onAutoFocus(boolean success, Camera camera) {

	}

	// @Override
	public void onError(int error, Camera camera) {

	}

	// @Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		try {
			Camera.Size size = cameraParameters.getPreviewSize();

			// Log.d(Globals.LOG_TAG, "CameraView.onPreviewFrame()");
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21,
					size.width, size.height, null);
			yuvImage.compressToJpeg(new Rect(0, 0, size.width, size.height),
					50, out);

			byte[] imageBytes = out.toByteArray();
			Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0,
					imageBytes.length);
			if (iv == null)
				iv = Ui.findView((Activity) this.getContext(),
						R.id.tinker_color_selector);

			if (iv != null) {
				iv.setVisibility(View.VISIBLE);
				iv.setImageBitmap(image);
				iv.setRotation(camdegrees);
				this.stopPreview();
				this.releaseCamera();
				this.setVisibility(View.GONE);
			}
		} catch (Exception e) {
			this.releaseCamera();
			this.setVisibility(View.GONE);
			View v = Ui.findView((Activity) this.getContext(),
					R.id.tinker_color_selector);
			v.setBackgroundResource(R.drawable.tinker_color_selector);
			v.setVisibility(View.VISIBLE);
		}
	}

	// ///////////////////
	// Keyboardhandling

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub

		return super.onKeyLongPress(keyCode, event);
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		// TODO Auto-generated method stub

		return super.onKeyMultiple(keyCode, repeatCount, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub

		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		if (camera != null) {
			camera.setOneShotPreviewCallback(this);
			return true;
		}
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		// TODO Auto-generated method stub

		return super.onTrackballEvent(event);
	}

	public float getHorizontalViewAngle() {
		if (camera == null)
			openCamera();
		if (cameraParameters != null) {
			return cameraParameters.getHorizontalViewAngle();
		}
		return 0.0f;
	}

	public float getVerticalViewAngle() {
		if (camera == null)
			openCamera();
		if (cameraParameters != null) {
			return cameraParameters.getVerticalViewAngle();
		}
		return 0.0f;
	}

	public boolean isRotate() {
		return rotate;
	}

	public void setRotate(boolean rotate) {
		this.rotate = rotate;
	}

	public Parameters getCameraParameters() {
		return this.cameraParameters;
	}

}
