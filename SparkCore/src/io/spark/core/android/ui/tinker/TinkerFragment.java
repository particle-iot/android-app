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
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.TextView;


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
		setHasOptionsMenu(true);
		if (getArguments().containsKey(ARG_DEVICE_ID)) {
			device = DeviceState.getDeviceById(getArguments().getString(ARG_DEVICE_ID));
		}
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
		broadcastMgr.registerReceiver(tinkerReceiver, tinkerReceiver.getFilter());
		broadcastMgr.registerReceiver(namingCompleteReceiver, namingCompleteReceiver.getFilter());
		broadcastMgr.registerReceiver(namingFailedReceiver, namingFailedReceiver.getFilter());
		broadcastMgr.registerReceiver(namingStartedReceiver, namingStartedReceiver.getFilter());
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

		allPins.addAll(aPins);
		allPins.addAll(dPins);

		for (Pin pin : allPins) {
			pinsByName.put(pin.name, pin);
			pin.setConfiguredAction(
					prefs.getPinFunction(device.id, pin.name));
		}

		digitalWritePins.addAll(allPins);
		digitalReadPins.addAll(allPins);
		analogWritePins.addAll(Py.list(aPins.get(0), aPins.get(1),
				aPins.get(4), aPins.get(5), aPins.get(6), aPins.get(7),
				dPins.get(0), dPins.get(1)));
		analogReadPins.addAll(aPins);

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
		Ui.findView(this, R.id.tinker_main).setOnClickListener(this);
	}

	private void showInstructions() {
		View instructions = Ui.findView(this, R.id.tinker_instructions);

		// set cyan on "D7" text
		TextView instructions3 = Ui.findView(instructions, R.id.tinker_instructions_3);
		String d7 = "D7";
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
		toggleViewVisibilityWithFade(R.id.tinker_logo, false);

		final View selectDialogView = getActivity().getLayoutInflater().inflate(
				R.layout.tinker_select, null);

		selectDialog = new AlertDialog.Builder(getActivity(),
				R.style.AppTheme_DialogNoDimBackground)
				.setView(selectDialogView)
				.setCancelable(true)
				.setOnCancelListener(new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						dialog.dismiss();
					}
				})
				.create();
		selectDialog.setCanceledOnTouchOutside(true);
		selectDialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				unmutePins();
				toggleViewVisibilityWithFade(R.id.tinker_logo, true);
				selectDialog = null;
			}
		});

		final View analogRead = Ui.findView(selectDialogView, R.id.tinker_button_analog_read);
		final View analogWrite = Ui.findView(selectDialogView, R.id.tinker_button_analog_write);
		final View digitalRead = Ui.findView(selectDialogView, R.id.tinker_button_digital_read);
		final View digitalWrite = Ui.findView(selectDialogView, R.id.tinker_button_digital_write);
		final List<View> allButtons = list(analogRead, analogWrite, digitalRead, digitalWrite);

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

		digitalWrite.setOnClickListener(this);
		digitalRead.setOnClickListener(this);
		analogRead.setOnClickListener(this);
		analogWrite.setOnClickListener(this);

		if (!digitalWritePins.contains(pin)) {
			digitalWrite.setVisibility(View.INVISIBLE);
		} else {
			digitalWrite.setVisibility(View.VISIBLE);
		}
		if (!digitalReadPins.contains(pin)) {
			digitalRead.setVisibility(View.INVISIBLE);
		} else {
			digitalRead.setVisibility(View.VISIBLE);
		}
		if (!analogReadPins.contains(pin)) {
			analogRead.setVisibility(View.INVISIBLE);
		} else {
			analogRead.setVisibility(View.VISIBLE);
		}
		if (!analogWritePins.contains(pin)) {
			analogWrite.setVisibility(View.INVISIBLE);
		} else {
			analogWrite.setVisibility(View.VISIBLE);
		}

		((TextView) selectDialogView.findViewById(R.id.tinker_select_pin)).setText(pin.name);

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
		}

		selectDialog.show();

		View decorView = selectDialog.getWindow().getDecorView();
		noIReallyMeanItIWantThisToBeTransparent(decorView);
	}

	private void setTinkerSelectButtonSelected(View selectButtonView, List<View> allButtons) {
		for (View button : allButtons) {
			Ui.findView(button, R.id.tinker_button_color)
					.setVisibility((button == selectButtonView) ? View.VISIBLE : View.INVISIBLE);
			button.setBackgroundResource(
					(button == selectButtonView)
							? R.color.tinker_selection_overlay_item_selected_bg
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
		view.animate()
				.setDuration(shortAnimTime)
				.alpha(show ? 1 : 0)
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
		toggleViewVisibilityWithFade(R.id.tinker_logo, true);
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
		toggleViewVisibilityWithFade(R.id.tinker_logo, true);

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
		toggleViewVisibilityWithFade(R.id.tinker_logo, false);
		pin.showAnalogWrite(new OnAnalogWriteListener() {

			@Override
			public void onAnalogWrite(int value) {
				api.analogWrite(device.id, pin.name, pin.getAnalogValue(), value);
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
		DigitalValue newValue = (currentValue == DigitalValue.HIGH)
				? DigitalValue.LOW
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
			ErrorsDelegate errorsDelegate = ((BaseActivity) getActivity()).getErrorsDelegate();
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
		if (response.responseValue < 0 || response.responseValue > 4095) {
			log.e("TinkerResponse: bad response value");
			return false;
		}
		return true;
	}



	private class TinkerReceiver extends BroadcastReceiver {

		IntentFilter getFilter() {
			return new IntentFilter(ApiFacade.BROADCAST_TINKER_RESPONSE_RECEIVED);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			TinkerResponse response = intent.getParcelableExtra(ApiFacade.EXTRA_TINKER_RESPONSE);
			onTinkerResponse(response);
		}

	}


	private class NamingFailedReceiver extends BroadcastReceiver {

		IntentFilter getFilter() {
			return new IntentFilter(ApiFacade.BROADCAST_CORE_NAMING_REQUEST_COMPLETE);
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
			String previousName = (device.name == null)
					? getString(R.string._unnamed_core_)
					: device.name;
			device = newDevice;

			if (!previousName.equals(device.name) && device.name != null) {
				BaseActivity activity = (BaseActivity) getActivity();
				activity.setCustomActionBarTitle(device.name);
			}
		}
	}

}
