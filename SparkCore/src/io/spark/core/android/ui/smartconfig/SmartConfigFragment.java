package io.spark.core.android.ui.smartconfig;

import static org.solemnsilence.util.Py.list;
import io.spark.core.android.R;
import io.spark.core.android.cloud.ApiFacade;
import io.spark.core.android.smartconfig.SmartConfigService;
import io.spark.core.android.ui.BaseFragment;
import io.spark.core.android.ui.util.Ui;
import io.spark.core.android.util.NetConnectionHelper;

import org.solemnsilence.util.TLog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.danh32.fontify.EditText;


public class SmartConfigFragment extends BaseFragment {


	NetConnectionHelper wifiHelper;
	SmartConfigFoundSomethingReceiver receiver;

	Button connectButton;
	EditText customKeyEntry;
	CheckBox customKeyCheckbox;

	OnClickListener startConfigClickListener;
	Runnable smartConfigFailedRunnable;
	Handler failHandler;

	boolean stopService = true;

	@Override
	public int getContentViewLayoutId() {
		return R.layout.fragment_smart_config;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		wifiHelper = new NetConnectionHelper(activity);
		receiver = new SmartConfigFoundSomethingReceiver();
		failHandler = new Handler();

		smartConfigFailedRunnable = new Runnable() {

			@Override
			public void run() {
				getActivity().startActivity(new Intent(getActivity(), NoCoresFoundActivity.class));
				getActivity().finish();
			}
		};
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		connectButton = Ui.findView(this, R.id.connect_button);
		customKeyEntry = Ui.findView(this, R.id.custom_aes_key_text);
		customKeyCheckbox = Ui.findView(this, R.id.custom_aes_key_checkbox);

		startConfigClickListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				startSmartConfig();
			}
		};
		connectButton.setOnClickListener(startConfigClickListener);

		TextView wifiPassView = Ui.findView(this, R.id.wifi_credentials_text);
		wifiPassView.setOnEditorActionListener(new TextView.OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.connect || id == EditorInfo.IME_NULL) {
					startSmartConfig();
					return true;
				}
				return false;
			}
		});

		setUpCustomKeyCheckbox();
		setUpTextInputFieldWatchers();
	}

	@Override
	public void onStart() {
		super.onStart();
		broadcastMgr.registerReceiver(receiver, receiver.getFilter());
		Ui.setText(this, R.id.ssid_text, wifiHelper.getSSID());
		if (!wifiHelper.isConnectedViaWifi()) {
			connectButton.setEnabled(false);
			showWifiRequiredDialog();
		}
	}

	@Override
	public void onStop() {
		if (stopService) {
			stopSmartConfig();
		}
		failHandler.removeCallbacks(smartConfigFailedRunnable);
		broadcastMgr.unregisterReceiver(receiver);
		super.onStop();
	}

	private void setUpTextInputFieldWatchers() {
		for (final Integer viewId : list(R.id.custom_aes_key_text, R.id.wifi_credentials_text)) {
			EditText field = Ui.findView(this, viewId);
			field.addTextChangedListener(new TextWatcher() {

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					// NO-OP
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					// NO-OP
				}

				@Override
				public void afterTextChanged(Editable s) {
					validateForm(viewId);
				}
			});
		}
	}

	private void validateForm(int viewId) {
		EditText wifiCredentialsText = Ui.findView(this, R.id.wifi_credentials_text);

		CharSequence customKey = customKeyEntry.getText();
		CharSequence wifiCreds = wifiCredentialsText.getText();

		float alpha = 0.5f;
		boolean enabled = false;

		if ((customKey.length() == 16 || !customKeyCheckbox.isChecked())
				&& (wifiCreds.length() == 0 || (wifiCreds.length() >= 8 && wifiCreds.length() <= 32))) {
			alpha = 1.0f;
			enabled = true;
		}
		connectButton.setEnabled(enabled);
		connectButton.setAlpha(alpha);

		// "greater than" shouldn't be possible, but...
		if (wifiCreds.length() >= 32 && viewId == R.id.wifi_credentials_text) {
			new AlertDialog.Builder(getActivity())
					.setMessage(
							R.string.the_spark_core_can_only_accept_passwords_up_to_32_characters_)
					.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					})
					.create()
					.show();
		}
	}

	private void setUpCustomKeyCheckbox() {
		final ImageView aesCbImage = Ui.findView(this, R.id.custom_aes_key_checkbox_image);
		customKeyCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				customKeyEntry.setVisibility((isChecked) ? View.VISIBLE : View.GONE);
				aesCbImage.setImageResource((isChecked)
						? R.drawable.aes_checkbox_checked_temp
						: R.drawable.aes_checkbox_unchecked_temp);
				validateForm(buttonView.getId());
			}
		});
		View aesBoxParent = Ui.findView(this, R.id.aes_parent);
		aesBoxParent.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				customKeyCheckbox.toggle();
			}
		});
	}

	private void configureConnectButton(final boolean startingSmartConfig) {
		connectButton.setText((startingSmartConfig) ? R.string.stop : R.string.connect);
		connectButton.setBackgroundResource((startingSmartConfig)
				? R.drawable.red_button_selector
				: R.drawable.blue_button_selector);
		showProgress(R.id.progress_indicator, startingSmartConfig);
	}

	private void startSmartConfig() {
		configureConnectButton(true);

		String customKey = (customKeyCheckbox.isChecked())
				? customKeyEntry.getText().toString()
				: "";

		SmartConfigService.startSmartConfig(getActivity(),
				Ui.getText(this, R.id.ssid_text, false),
				Ui.getText(this, R.id.wifi_credentials_text, false),
				wifiHelper.getGatewayIp(),
				customKey);

		connectButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				stopSmartConfig();
			}
		});

		int noCoresTimeoutMillis = 90 * 1000;
		failHandler.postDelayed(smartConfigFailedRunnable, noCoresTimeoutMillis);
	}

	private void stopSmartConfig() {
		configureConnectButton(false);
		failHandler.removeCallbacks(smartConfigFailedRunnable);
		SmartConfigService.stopSmartConfig(SmartConfigFragment.this.getActivity());
		connectButton.setOnClickListener(startConfigClickListener);
	}

	private void showWifiRequiredDialog() {
		new AlertDialog.Builder(getActivity())
				.setMessage("SmartConfig requires a WiFi connection.")
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.setNegativeButton("WiFi Settings", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
					}
				})
				.create()
				.show();
	}


	class SmartConfigFoundSomethingReceiver extends BroadcastReceiver {

		IntentFilter getFilter() {
			return new IntentFilter(ApiFacade.BROADCAST_CORE_CLAIMED);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			log.i("Received BROADCAST_CORE_CLAIMED, found at least 1 core.");
			stopService = false;
			getActivity().startActivity(new Intent(getActivity(), NamingActivity.class));
			getActivity().finish();
		}
	}


	private static final TLog log = new TLog(SmartConfigFragment.class);

}
