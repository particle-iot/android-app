package io.spark.core.android.ui.tinker;

import static org.solemnsilence.util.Py.list;
import io.spark.core.android.R;
import io.spark.core.android.app.DeviceState;
import io.spark.core.android.cloud.ApiFacade;
import io.spark.core.android.cloud.api.Device;
import io.spark.core.android.cloud.api.TinkerResponse;
import io.spark.core.android.storage.TinkerPrefs;
import io.spark.core.android.ui.BaseActivity;
import io.spark.core.android.ui.BaseFragment;
import io.spark.core.android.ui.ErrorsDelegate;
import io.spark.core.android.ui.corelist.CoreListActivity;
import io.spark.core.android.ui.tinker.Pin.OnAnalogWriteListener;
import io.spark.core.android.ui.util.NamingHelper;
import io.spark.core.android.ui.util.Ui;

import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.solemnsilence.util.Py;
import org.solemnsilence.util.TLog;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.widget.SlidingPaneLayout;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.TextView;
import android.widget.ImageView;

/**
 * A fragment representing a single Core detail screen. This fragment is either
 * contained in a {@link CoreListActivity} in two-pane mode (on tablets) or a
 * {@link CoreDetailActivity} on handsets.
 */
public class TinkerFragment extends BaseFragment implements OnClickListener {

	private static final TLog log = new TLog(TinkerFragment.class);

	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_DEVICE_ID = "ARG_DEVICE_ID";

	List<Pin> aPins = Py.list();
	List<Pin> dPins = Py.list();
	List<Pin> allPins = Py.list();
	List<Pin> digitalReadPins = Py.list();
	List<Pin> digitalWritePins = Py.list();
	List<Pin> analogWritePins = Py.list();
	List<Pin> analogReadPins = Py.list();
	Map<String, Pin> pinsByName = Py.map();

	Pin selectedPin;
	AlertDialog selectDialog;

	private Device device;
	private TinkerReceiver tinkerReceiver;
	private NamingCompleteReceiver namingCompleteReceiver;
	private NamingFailedReceiver namingFailedReceiver;
	private NamingStartedReceiver namingStartedReceiver;

	public static TinkerFragment newInstance(String deviceId) {
		Bundle arguments = new Bundle();
		arguments.putString(TinkerFragment.ARG_DEVICE_ID, deviceId);
		TinkerFragment fragment = new TinkerFragment();
		fragment.setArguments(arguments);
	return fragment;
	}

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public TinkerFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments().containsKey(ARG_DEVICE_ID)) {
			device = DeviceState.getDeviceById(getArguments().getString(
					ARG_DEVICE_ID));
		}
		setHasOptionsMenu(true); // needs device to be set, so it got moved here
		tinkerReceiver = new TinkerReceiver();
		namingCompleteReceiver = new NamingCompleteReceiver();
		namingFailedReceiver = new NamingFailedReceiver();
		namingStartedReceiver = new NamingStartedReceiver();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		loadViews();
		setupListeners();

		if (TinkerPrefs.getInstance().isFirstVisit()) {
			showInstructions();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		broadcastMgr.registerReceiver(tinkerReceiver,
				tinkerReceiver.getFilter());
		broadcastMgr.registerReceiver(namingCompleteReceiver,
				namingCompleteReceiver.getFilter());
		broadcastMgr.registerReceiver(namingFailedReceiver,
				namingFailedReceiver.getFilter());
		broadcastMgr.registerReceiver(namingStartedReceiver,
				namingStartedReceiver.getFilter());
	}

	@Override
	public void onStop() {
		broadcastMgr.unregisterReceiver(tinkerReceiver);
		broadcastMgr.unregisterReceiver(namingCompleteReceiver);
		broadcastMgr.unregisterReceiver(namingFailedReceiver);
		broadcastMgr.unregisterReceiver(namingStartedReceiver);
		super.onStop();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.tinker, menu);
		inflater.inflate(R.menu.core_row_overflow, menu);

		// first preset opposit state, and then toggle into desired state
		// not pretty, but safes me to wrap this in a standalone function
		menu.findItem(R.id.action_enable_extensions).setChecked(
				!prefs.getTinkerExtensions(device.id));
		menu.performIdentifierAction(R.id.action_enable_extensions, 0);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_rename_core:
			new NamingHelper(getActivity(), api).showRenameDialog(device);
			return true;

		case R.id.action_reflash_tinker:
			api.reflashTinker(device.id);
			return true;

		case R.id.action_clear_tinker:
			prefs.clearTinker(device.id);
			for (Pin pin : allPins) {
				pin.setConfiguredAction(PinAction.NONE);
				pin.reset();
			}
			return true;

		case R.id.action_enable_extensions:
			View v;
			item.setChecked(!item.isChecked());
			item.setIcon(item.isChecked() ? R.drawable.ic_action_ext
					: R.drawable.ic_action_noext);
			if ((v = Ui.findView(this, R.id.tinker_pins_ext)) != null)
				v.setVisibility(item.isChecked() ? View.VISIBLE : View.GONE);
			if (!item.isChecked()
					&& (v = Ui.findView(this, R.id.tinker_color_manipulation)) != null)
				v.setVisibility(View.GONE);
			prefs.saveTinkerExtensions(item.isChecked(), device.id);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void loadViews() {
		aPins.add(new Pin((TextView) Ui.findView(this, R.id.tinker_a0),
				PinType.A, "A0"));
		aPins.add(new Pin((TextView) Ui.findView(this, R.id.tinker_a1),
				PinType.A, "A1"));
		aPins.add(new Pin((TextView) Ui.findView(this, R.id.tinker_a2),
				PinType.A, "A2"));
		aPins.add(new Pin((TextView) Ui.findView(this, R.id.tinker_a3),
				PinType.A, "A3"));
		aPins.add(new Pin((TextView) Ui.findView(this, R.id.tinker_a4),
				PinType.A, "A4"));
		aPins.add(new Pin((TextView) Ui.findView(this, R.id.tinker_a5),
				PinType.A, "A5"));
		aPins.add(new Pin((TextView) Ui.findView(this, R.id.tinker_a6),
				PinType.A, "A6"));
		aPins.add(new Pin((TextView) Ui.findView(this, R.id.tinker_a7),
				PinType.A, "A7"));

		dPins.add(new Pin((TextView) Ui.findView(this, R.id.tinker_d0),
				PinType.D, "D0"));
		dPins.add(new Pin((TextView) Ui.findView(this, R.id.tinker_d1),
				PinType.D, "D1"));
		dPins.add(new Pin((TextView) Ui.findView(this, R.id.tinker_d2),
				PinType.D, "D2"));
		dPins.add(new Pin((TextView) Ui.findView(this, R.id.tinker_d3),
				PinType.D, "D3"));
		dPins.add(new Pin((TextView) Ui.findView(this, R.id.tinker_d4),
				PinType.D, "D4"));
		dPins.add(new Pin((TextView) Ui.findView(this, R.id.tinker_d5),
				PinType.D, "D5"));
		dPins.add(new Pin((TextView) Ui.findView(this, R.id.tinker_d6),
				PinType.D, "D6"));
		dPins.add(new Pin((TextView) Ui.findView(this, R.id.tinker_d7),
				PinType.D, "D7"));

		// additional digitalPins (rx/tx if not used for USART)
		dPins.add(new Pin((TextView) Ui.findView(this, R.id.tinker_rx),
				PinType.X, "RX"));
		dPins.add(new Pin((TextView) Ui.findView(this, R.id.tinker_tx),
				PinType.X, "TX"));
		// additional virtual Pin for RGB-LED
		aPins.add(new Pin((TextView) Ui.findView(this, R.id.tinker_cl),
				PinType.A, "CL"));

		allPins.addAll(aPins);
		allPins.addAll(dPins);

		for (Pin pin : allPins) {
			pinsByName.put(pin.name, pin);
			pin.setConfiguredAction(prefs.getPinFunction(device.id, pin.name));
		}

		digitalWritePins.addAll(allPins);
		digitalReadPins.addAll(allPins);
		analogWritePins.addAll(Py.list(aPins.get(0), aPins.get(1),
				aPins.get(4), aPins.get(5), aPins.get(6), aPins.get(7),
				dPins.get(0), dPins.get(1), aPins.get(8)));
		analogReadPins.addAll(aPins);

		// start of with RGB-LED control deactivated
		Ui.findView(this, R.id.tinker_rgb).setTag(false);
		Ui.findView(this, R.id.tinker_color_selector).setTag(0x00000000);
	}

	private void setupListeners() {
		// Set up pin listeners
		for (final Pin pin : allPins) {
			for (View view : list(pin.view, (ViewGroup) pin.view.getParent())) {
				view.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Pin writeModePin = getPinInWriteMode();
						if (writeModePin != null && !pin.equals(selectedPin)) {
							writeModePin.showAnalogWriteValue();
							unmutePins();
							return;
						}
						selectedPin = pin;
						onPinClick(pin);
					}
				});

				view.setOnLongClickListener(new OnLongClickListener() {

					@Override
					public boolean onLongClick(View v) {
						Pin writeModePin = getPinInWriteMode();
						if (writeModePin != null && !pin.equals(selectedPin)) {
							writeModePin.showAnalogWriteValue();
							unmutePins();
							return true;
						}
						selectedPin = pin;
						showTinkerSelect(pin);
						return true;
					}
				});
			}
		}

		// Set up other listeners
		Ui.findView(this, R.id.tinker_rgb).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						boolean bState = !(Boolean) v.getTag();

						View vChild = Ui.findView(v, R.id.tinker_logo_cogg_led);
						ViewGroup.MarginLayoutParams lp = (MarginLayoutParams) vChild
								.getLayoutParams();
						View vCS = Ui.findView((Activity) v.getContext(),
								R.id.tinker_color_selector);
						/*
						 * View vShade = Ui.findView((Activity) v.getContext(),
						 * R.id.tinker_shadow);
						 */
						int m = bState ? 0 : 2;
						int c = (Integer) vCS.getTag();

						if (bState) {
							c |= 0xFF000000;
							v.setBackgroundColor(c);
							vCS.setTag(c);
						} else {
							c &= 0x00FFFFFF;
							v.setBackgroundColor(0xFF000000);
							vCS.setTag(c);
							// vCS.setVisibility(View.GONE);
							// vShade.setVisibility(View.GONE);
							Ui.findView((Activity) v.getContext(),
									R.id.tinker_color_manipulation)
									.setVisibility(View.GONE);
						}

						((Vibrator) v.getContext().getSystemService(
								Service.VIBRATOR_SERVICE)).vibrate(25);
						api.analogWrite(device.id, "CL", 0, c & 0x01FFFFFF);

						lp.setMargins(m, m, m, m);
						vChild.setLayoutParams(lp);
						v.setTag(bState);
					}
				});

		Ui.findView(this, R.id.tinker_rgb).setOnLongClickListener(
				new OnLongClickListener() {

					@Override
					public boolean onLongClick(View v) {
						if (!(Boolean) v.getTag())
							v.callOnClick(); // make sure LED control is active

						View vCM = Ui.findView((Activity) v.getContext(),
								R.id.tinker_color_manipulation);
						View vCS = Ui.findView((Activity) v.getContext(),
								R.id.tinker_color_selector);
						View vShade = Ui.findView((Activity) v.getContext(),
								R.id.tinker_shadow);

						if (vCM.getVisibility() != View.VISIBLE) {
							vCM.setVisibility(View.VISIBLE);
							vShade.setX(vCS.getX() + 5);
							vShade.setY(vCS.getY() + 5);
							// vShade.setVisibility(View.VISIBLE);
						} else {
							// vCS.setVisibility(View.GONE);
							// vShade.setVisibility(View.GONE);
							vCM.setVisibility(View.GONE);
						}

						// View vCPV = Ui.findView((Activity) v.getContext(),
						// R.id.tinker_camera_preview);
						// vCPV.setVisibility(View.GONE);

						return true;
					}
				});

		/*
		 * Ui.findView(this, R.id.tinker_color_selector).setOnLongClickListener(
		 * new OnLongClickListener() {
		 * 
		 * @Override public boolean onLongClick(View v) {
		 * log.i("long touch start"); View vCPV = Ui.findView((Activity)
		 * v.getContext(), R.id.tinker_camera_preview);
		 * 
		 * if (vCPV.getVisibility() != View.VISIBLE) { log.i("long touch true");
		 * ((Vibrator) v.getContext().getSystemService(
		 * Service.VIBRATOR_SERVICE)).vibrate(25);
		 * 
		 * v.setVisibility(View.GONE);
		 * 
		 * ((SlidingPaneLayout) Ui.findView( (Activity) v.getContext(),
		 * R.id.sliding_pane_layout)) .requestDisallowInterceptTouchEvent(true);
		 * 
		 * vCPV.setVisibility(View.VISIBLE);
		 * 
		 * return true; } log.i("long touch end");
		 * 
		 * return false; } });
		 */

		Ui.findView(this, R.id.tinker_color_selector).setOnTouchListener(
				new OnTouchListener() {
					float alpha0 = 0;
					Matrix mx = new Matrix();
					Bitmap bmp = null;
					Rect r = new Rect();
					int color = 0;
					float rotation0;

					@Override
					public boolean onTouch(View v, MotionEvent event) {

						switch (event.getAction() & MotionEvent.ACTION_MASK) {
						case MotionEvent.ACTION_DOWN:
							// stop SlidingPane to slide during color selection
							((SlidingPaneLayout) Ui.findView(
									(Activity) v.getContext(),
									R.id.sliding_pane_layout))
									.requestDisallowInterceptTouchEvent(true);

							// get initial state of views
							v.getGlobalVisibleRect(r);
							alpha0 = (float) Math.toDegrees(Math.atan2(
									event.getRawX() - r.centerX(), r.centerY()
											- event.getRawY()));
							rotation0 = v.getRotation();

							// convert event coordinates to resized/rotated
							// image coordinates
							float[] xy = new float[] { event.getX(),
									event.getY() };
							bmp = ((BitmapDrawable) ((ImageView) v)
									.getDrawable()).getBitmap();
							((ImageView) v).getImageMatrix().invert(mx);
							mx.mapPoints(xy);
							if (xy[0] < 0)
								xy[0] = 0;
							else if (xy[0] > bmp.getWidth() - 1)
								xy[0] = bmp.getWidth() - 1;
							if (xy[1] < 0)
								xy[1] = 0;
							else if (xy[1] > bmp.getHeight() - 1)
								xy[1] = bmp.getHeight() - 1;

							color = bmp.getPixel((int) xy[0], (int) xy[1]);

							// accept only fully opaque colors
							if ((color & 0xFF000000) == 0xFF000000) {
								Ui.findView((Activity) v.getContext(),
										R.id.tinker_rgb).setBackgroundColor(
										color);
								v.setTag(color);

								View hsv = Ui.findView(
										(Activity) v.getContext(),
										R.id.tinker_hue_saturation_volume);
								hsv.setVisibility(View.VISIBLE);
								hsv.setRotation(alpha0);
								((ImageView) Ui.findView(
										(Activity) v.getContext(),
										R.id.tinker_hue)).setColorFilter(color);
							}

						case MotionEvent.ACTION_MOVE:
							// calculate angel between initial touch and now
							float alpha = (float) Math.toDegrees(Math.atan2(
									event.getRawX() - r.centerX(), r.centerY()
											- event.getRawY()))
									- alpha0;

							// normalize result to -180/+180 degrees
							if (alpha > 180)
								alpha -= 360;
							else if (alpha < -180)
								alpha += 360;

							// limit allowed rotation to -135/135 degrees
							// to fit the saturation/volume ring image
							if (Math.abs(alpha) <= 135) {
								// rotate color ring and its shadow accordingly
								v.setRotation(alpha + rotation0);
								Ui.findView((Activity) v.getContext(),
										R.id.tinker_shadow).setRotation(
										alpha + rotation0);

								// get selected color (hue)
								int c = (Integer) v.getTag();
								// split color into its RGB components
								int r = (c >> 16) & 0xFF;
								int g = (c >> 8) & 0xFF;
								int b = (c >> 0) & 0xFF;

								// do a rudimentary saturation/volume adjust
								if (alpha > 0) {
									r += (255 - r) * alpha / 135;
									g += (255 - g) * alpha / 135;
									b += (255 - b) * alpha / 135;
								} else {
									r *= (1 + alpha / 135);
									g *= (1 + alpha / 135);
									b *= (1 + alpha / 135);
								}

								// log.i("Move1: " + Integer.toHexString(c) +
								// " "
								// + Integer.toHexString(r) + " "
								// + Integer.toHexString(g) + " "
								// + Integer.toHexString(b));

								// put fully opaque color together again
								color = (c & 0xFF000000) | r << 16 | g << 8 | b;

								// log.i("Move2: " + Integer.toHexString(c) +
								// " "
								// + Integer.toHexString(r) + " "
								// + Integer.toHexString(g) + " "
								// + Integer.toHexString(b));

								// show result in RGB-LED view
								if ((color & 0xFF000000) == 0xFF000000) {
									Ui.findView((Activity) v.getContext(),
											R.id.tinker_rgb)
											.setBackgroundColor(color);
								}
							}
							return true;
						case MotionEvent.ACTION_UP:
							// give haptic feedback when finished
							((Vibrator) v.getContext().getSystemService(
									Service.VIBRATOR_SERVICE)).vibrate(25);

							// send selected color to Core
							// (alpha 0x01 causes RGB.control(true) on Core)
							api.analogWrite(device.id, "CL",
									(Integer) v.getTag() & 0x01FFFFFF,
									color & 0x01FFFFFF);
							v.setTag(color);
							
							// allow touch sliding again
							((SlidingPaneLayout) Ui.findView(
									(Activity) v.getContext(),
									R.id.sliding_pane_layout))
									.requestDisallowInterceptTouchEvent(false);
						case MotionEvent.ACTION_OUTSIDE:
						case MotionEvent.ACTION_CANCEL:
							// hide saturation/volume view
							Ui.findView((Activity) v.getContext(),
									R.id.tinker_hue_saturation_volume)
									.setVisibility(View.GONE);

							return true;
						}

						return false;
					}
				});

		Ui.findView(this, R.id.tinker_main).setOnClickListener(this);
	}

	private void showInstructions() {
		View instructions = Ui.findView(this, R.id.tinker_instructions);

		TextView instructions3 = Ui.findView(instructions,
				R.id.tinker_instructions_3);
		String d7 = "D7";

		pinsByName.get(d7).setConfiguredAction(PinAction.DIGITAL_WRITE);

		String instructions3Text = getString(R.string.tinker_instructions_3);
		int idx = instructions3Text.indexOf(d7);
		int cyan = getResources().getColor(R.color.cyan);
		Spannable str = (Spannable) instructions3.getText();
		str.setSpan(new ForegroundColorSpan(cyan), idx, idx + d7.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		// set visible and then set it to disappear when we're done. and then
		// never show up again.
		instructions.setVisibility(View.VISIBLE);
		instructions.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				v.setVisibility(View.GONE);
				pinsByName.get("D7").setConfiguredAction(
						prefs.getPinFunction(device.id, "D7"));
				TinkerPrefs.getInstance().setVisited(true);
			}
		});
	}

	private void onPinClick(Pin selectedPin) {
		if (selectedPin.getConfiguredAction() != PinAction.NONE) {
			// Perform requested action
			switch (selectedPin.getConfiguredAction()) {
			case ANALOG_READ:
				doAnalogRead(selectedPin);
				break;
			case ANALOG_WRITE:
				if (selectedPin.isAnalogWriteMode()) {
					selectedPin.showAnalogWriteValue();
					unmutePins();
				} else {
					doAnalogWrite(selectedPin);
				}
				break;
			case DIGITAL_READ:
				doDigitalRead(selectedPin);
				break;
			case DIGITAL_WRITE:
				doDigitalWrite(selectedPin);
				break;
			default:
				break;
			}
		} else {
			showTinkerSelect(selectedPin);
		}
	}

	private void showTinkerSelect(Pin pin) {
		// No current action on the pin
		mutePinsExcept(pin);
		toggleViewVisibilityWithFade(R.id.tinker_logo_cogg, false);

		final View selectDialogView = getActivity().getLayoutInflater()
				.inflate(R.layout.tinker_select, null);

		selectDialog = new AlertDialog.Builder(getActivity(),
				R.style.AppTheme_DialogNoDimBackground)
				.setView(selectDialogView).setCancelable(true)
				.setOnCancelListener(new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						dialog.dismiss();
					}
				}).create();
		selectDialog.setCanceledOnTouchOutside(true);
		selectDialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				unmutePins();
				toggleViewVisibilityWithFade(R.id.tinker_logo_cogg, true);
				selectDialog = null;
			}
		});

		final View analogRead = Ui.findView(selectDialogView,
				R.id.tinker_button_analog_read);
		final View analogWrite = Ui.findView(selectDialogView,
				R.id.tinker_button_analog_write);
		final View digitalRead = Ui.findView(selectDialogView,
				R.id.tinker_button_digital_read);
		final View digitalWrite = Ui.findView(selectDialogView,
				R.id.tinker_button_digital_write);

		final View hidePin = Ui.findView(selectDialogView,
				R.id.tinker_button_hide);
		final View resetPin = Ui.findView(selectDialogView,
				R.id.tinker_button_reset);

		final List<View> allButtons = list(analogRead, analogWrite,
				digitalRead, digitalWrite, hidePin, resetPin);

		analogRead.setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					setTinkerSelectButtonSelected(analogRead, allButtons);
				}
				return false;
			}
		});

		analogWrite.setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					setTinkerSelectButtonSelected(analogWrite, allButtons);
				}
				return false;
			}
		});

		digitalRead.setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					setTinkerSelectButtonSelected(digitalRead, allButtons);
				}
				return false;
			}
		});

		digitalWrite.setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					setTinkerSelectButtonSelected(digitalWrite, allButtons);
				}
				return false;
			}
		});

		hidePin.setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					setTinkerSelectButtonSelected(hidePin, allButtons);
				}
				return false;
			}
		});

		resetPin.setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					setTinkerSelectButtonSelected(resetPin, allButtons);
				}
				return false;
			}
		});

		digitalWrite.setOnClickListener(this);
		digitalRead.setOnClickListener(this);
		analogRead.setOnClickListener(this);
		analogWrite.setOnClickListener(this);
		hidePin.setOnClickListener(this);
		resetPin.setOnClickListener(this);

		if (pin.getConfiguredAction() == PinAction.HIDDEN) {
			hidePin.setVisibility(View.INVISIBLE);
		} else {
			hidePin.setVisibility(View.VISIBLE);
		}
		if (!digitalWritePins.contains(pin)
				|| pin.getConfiguredAction() == PinAction.HIDDEN) {
			digitalWrite.setVisibility(View.INVISIBLE);
		} else {
			digitalWrite.setVisibility(View.VISIBLE);
		}
		if (!digitalReadPins.contains(pin)
				|| pin.getConfiguredAction() == PinAction.HIDDEN) {
			digitalRead.setVisibility(View.INVISIBLE);
		} else {
			digitalRead.setVisibility(View.VISIBLE);
		}
		if (!analogReadPins.contains(pin)
				|| pin.getConfiguredAction() == PinAction.HIDDEN) {
			analogRead.setVisibility(View.INVISIBLE);
		} else {
			analogRead.setVisibility(View.VISIBLE);
		}
		if (!analogWritePins.contains(pin)
				|| pin.getConfiguredAction() == PinAction.HIDDEN) {
			analogWrite.setVisibility(View.INVISIBLE);
		} else {
			analogWrite.setVisibility(View.VISIBLE);
		}

		((TextView) selectDialogView.findViewById(R.id.tinker_select_pin))
				.setText(pin.name);

		PinAction action = pin.getConfiguredAction();
		switch (action) {
		case ANALOG_READ:
			setTinkerSelectButtonSelected(analogRead, allButtons);
			break;

		case ANALOG_WRITE:
			setTinkerSelectButtonSelected(analogWrite, allButtons);
			break;

		case DIGITAL_READ:
			setTinkerSelectButtonSelected(digitalRead, allButtons);
			break;

		case DIGITAL_WRITE:
			setTinkerSelectButtonSelected(digitalWrite, allButtons);
			break;

		case NONE:
			setTinkerSelectButtonSelected(null, allButtons);
			break;

		case HIDDEN:
			break;
		}

		selectDialog.show();

		View decorView = selectDialog.getWindow().getDecorView();
		noIReallyMeanItIWantThisToBeTransparent(decorView);
	}

	private void setTinkerSelectButtonSelected(View selectButtonView,
			List<View> allButtons) {
		for (View button : allButtons) {
			Ui.findView(button, R.id.tinker_button_color).setVisibility(
					(button == selectButtonView) ? View.VISIBLE
							: View.INVISIBLE);
			button.setBackgroundResource((button == selectButtonView) ? R.color.tinker_selection_overlay_item_selected_bg
					: R.color.tinker_selection_overlay_item_bg);
		}
	}

	private void noIReallyMeanItIWantThisToBeTransparent(View view) {
		if (view.getId() == R.id.tinker_select) {
			return;
		}
		view.setBackgroundColor(0);
		if (view instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup) view;
			for (int i = 0; i < vg.getChildCount(); i++) {
				noIReallyMeanItIWantThisToBeTransparent(vg.getChildAt(i));
			}
		}
	}

	private void toggleViewVisibilityWithFade(int viewId, final boolean show) {
		final View view = Ui.findView(this, viewId);
		int shortAnimTime = 150; // ms
		view.setVisibility(View.VISIBLE);
		view.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0)
				.setListener(new AnimatorListenerAdapter() {

					@Override
					public void onAnimationEnd(Animator animation) {
						view.setVisibility(show ? View.VISIBLE : View.GONE);
					}
				});
	}

	private void mutePinsExcept(Pin pin) {
		for (Pin currentPin : allPins) {
			if (!currentPin.equals(pin)) {
				currentPin.mute();
			}
		}
	}

	private void unmutePins() {
		// Unmute pins
		for (Pin pin : allPins) {
			pin.unmute();
		}
	}

	private void hideTinkerSelect() {
		// Reset tinker select dialog state
		toggleViewVisibilityWithFade(R.id.tinker_logo_cogg, true);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.tinker_button_analog_read:
			onFunctionSelected(selectedPin, PinAction.ANALOG_READ);
			break;
		case R.id.tinker_button_analog_write:
			onFunctionSelected(selectedPin, PinAction.ANALOG_WRITE);
			break;
		case R.id.tinker_button_digital_read:
			onFunctionSelected(selectedPin, PinAction.DIGITAL_READ);
			break;
		case R.id.tinker_button_digital_write:
			onFunctionSelected(selectedPin, PinAction.DIGITAL_WRITE);
			break;
		case R.id.tinker_button_hide:
			onFunctionSelected(selectedPin, PinAction.HIDDEN);
			break;
		case R.id.tinker_button_reset:
			onFunctionSelected(selectedPin, PinAction.NONE);
			break;
		case R.id.tinker_main:
			for (Pin pin : allPins) {
				if (pin.isAnalogWriteMode()) {
					pin.showAnalogWriteValue();
				}
			}
			unmutePins();
			// hideTinkerSelect();
			break;
		}
	}

	private Pin getPinInWriteMode() {
		for (Pin pin : allPins) {
			if (pin.isAnalogWriteMode()) {
				return pin;
			}
		}
		return null;
	}

	private void onFunctionSelected(Pin selectedPin, PinAction action) {
		if (selectDialog != null) {
			selectDialog.dismiss();
			selectDialog = null;
		}
		toggleViewVisibilityWithFade(R.id.tinker_logo_cogg, true);

		selectedPin.reset();
		selectedPin.setConfiguredAction(action);
		prefs.savePinFunction(device.id, selectedPin.name, action);
		// hideTinkerSelect();
		// unmutePins();
	}

	private void doAnalogRead(Pin pin) {
		pin.animateYourself();
		api.analogRead(device.id, pin.name, pin.getAnalogValue());
		// pin.showAnalogRead(850);
	}

	private void doAnalogWrite(final Pin pin) {
		mutePinsExcept(pin);
		toggleViewVisibilityWithFade(R.id.tinker_logo_cogg, false);
		pin.showAnalogWrite(new OnAnalogWriteListener() {

			@Override
			public void onAnalogWrite(int value) {
				api.analogWrite(device.id, pin.name, pin.getAnalogValue(),
						value);
				for (Pin pin : allPins) {
					if (pin.isAnalogWriteMode()) {
						pin.showAnalogWriteValue();
					}
				}
				unmutePins();
				hideTinkerSelect();
				pin.animateYourself();
			}
		});
	}

	private void doDigitalRead(Pin pin) {
		pin.animateYourself();
		api.digitalRead(device.id, pin.name, pin.getDigitalValue());
		// pin.showDigitalRead(DigitalValue.HIGH);

	}

	private void doDigitalWrite(Pin pin) {
		pin.animateYourself();
		DigitalValue currentValue = pin.getDigitalValue();
		DigitalValue newValue = (currentValue == DigitalValue.HIGH) ? DigitalValue.LOW
				: DigitalValue.HIGH;
		api.digitalWrite(device.id, pin.name, currentValue, newValue);
		// pin.showDigitalWrite(newValue);
	}

	@Override
	public int getContentViewLayoutId() {
		return R.layout.fragment_tinker;
	}

	private void onTinkerResponse(TinkerResponse response) {
		log.d("Tinker response received: " + response);

		if (!device.id.equals(response.coreId)) {
			log.i("Tinker resposne did not match core ID");
			return;
		}

		if (response.errorMakingRequest) {
			ErrorsDelegate errorsDelegate = ((BaseActivity) getActivity())
					.getErrorsDelegate();
			errorsDelegate.showTinkerError();
		}

		Pin pin = pinsByName.get(response.pin);
		if (!isValid(response)) {
			log.w("Invalid Tinker response: " + response);
			pin.stopAnimating();
			return;
		}

		if (pin.getConfiguredAction() == PinAction.NONE) {
			// received a response for a pin that has since been cleared
			pin.stopAnimating();
			return;
		}
		if (response.responseType == TinkerResponse.RESPONSE_TYPE_ANALOG) {
			pin.showAnalogValue(response.responseValue);
		} else {
			pin.showDigitalRead(DigitalValue.fromInt(response.responseValue));
		}
	}

	private boolean isValid(TinkerResponse response) {
		if (response.requestType != TinkerResponse.REQUEST_TYPE_READ
				&& response.requestType != TinkerResponse.REQUEST_TYPE_WRITE) {
			log.e("TinkerResponse: bad request type");
			return false;
		}
		if (response.responseType != TinkerResponse.RESPONSE_TYPE_ANALOG
				&& response.responseType != TinkerResponse.RESPONSE_TYPE_DIGITAL) {
			log.e("TinkerResponse: bad response type");
			return false;
		}
		if (!pinsByName.keySet().contains(response.pin)) {
			log.e("TinkerResponse: bad pin name");
			return false;
		}
		if (response.responseValue < 0 || response.responseValue > 4095
				&& response.pin != "CL") {
			log.e("TinkerResponse: bad response value");
			return false;
		}
		return true;
	}

	private class TinkerReceiver extends BroadcastReceiver {

		IntentFilter getFilter() {
			return new IntentFilter(
					ApiFacade.BROADCAST_TINKER_RESPONSE_RECEIVED);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			TinkerResponse response = intent
					.getParcelableExtra(ApiFacade.EXTRA_TINKER_RESPONSE);
			onTinkerResponse(response);
		}

	}

	private class NamingFailedReceiver extends BroadcastReceiver {

		IntentFilter getFilter() {
			return new IntentFilter(
					ApiFacade.BROADCAST_CORE_NAMING_REQUEST_COMPLETE);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			if ((ApiFacade.getResultCode(intent) != HttpStatus.SC_OK)) {
				BaseActivity activity = (BaseActivity) getActivity();
				activity.setCustomActionBarTitle(device.name);
				DeviceState.updateSingleDevice(device, true);
			}
		}

	}

	private class NamingStartedReceiver extends BroadcastReceiver {

		IntentFilter getFilter() {
			return new IntentFilter(NamingHelper.BROADCAST_NEW_NAME_FOUND);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			String newName = intent.getStringExtra(NamingHelper.EXTRA_NEW_NAME);
			if (newName != null) {
				BaseActivity activity = (BaseActivity) getActivity();
				activity.setCustomActionBarTitle(newName);
			}
		}

	}

	private class NamingCompleteReceiver extends BroadcastReceiver {

		IntentFilter getFilter() {
			return new IntentFilter(ApiFacade.BROADCAST_DEVICES_UPDATED);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			Device newDevice = DeviceState.getDeviceById(device.id);
			if (newDevice == null) {
				return;
			}
			// store previous name before switching out class level var
			String previousName = (device.name == null) ? getString(R.string._unnamed_core_)
					: device.name;
			device = newDevice;

			if (!previousName.equals(device.name) && device.name != null) {
				BaseActivity activity = (BaseActivity) getActivity();
				activity.setCustomActionBarTitle(device.name);
			}
		}
	}

}
