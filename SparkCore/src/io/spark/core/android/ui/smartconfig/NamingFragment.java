package io.spark.core.android.ui.smartconfig;

import io.spark.core.android.R;
import io.spark.core.android.app.DeviceState;
import io.spark.core.android.cloud.ApiFacade;
import io.spark.core.android.cloud.api.Device;
import io.spark.core.android.cloud.requestservice.SimpleSparkApiService;
import io.spark.core.android.smartconfig.SmartConfigService;
import io.spark.core.android.smartconfig.SmartConfigState;
import io.spark.core.android.ui.BaseFragment;
import io.spark.core.android.ui.corelist.CoreListActivity;
import io.spark.core.android.ui.util.NamingHelper;
import io.spark.core.android.ui.util.Ui;
import io.spark.core.android.util.CoreNameGenerator;

import java.util.Iterator;

import org.apache.http.HttpStatus;
import org.solemnsilence.util.TLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class NamingFragment extends BaseFragment {

	private static final TLog log = new TLog(NamingFragment.class);


	private static final String STATE_CURRENT_RENAME_ATTEMPT_DEVICE_ID = "STATE_CURRENT_RENAME_ATTEMPT_DEVICE_ID";

	EditText coreNameText;
	Button okButton;

	SmartConfigFoundSomethingReceiver foundSomethingReceiver;
	MoreCoresClaimedReceiver moreCoresClaimedReceiver;

	String currentRenameAttemptHexId;


	@Override
	public int getContentViewLayoutId() {
		return R.layout.fragment_naming;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(STATE_CURRENT_RENAME_ATTEMPT_DEVICE_ID)) {
			currentRenameAttemptHexId = savedInstanceState.getString(
					STATE_CURRENT_RENAME_ATTEMPT_DEVICE_ID);
		}
		foundSomethingReceiver = new SmartConfigFoundSomethingReceiver();
		moreCoresClaimedReceiver = new MoreCoresClaimedReceiver();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		coreNameText = Ui.findView(this, R.id.core_name);
		coreNameText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.name_core || id == EditorInfo.IME_NULL) {
					nameCurrentCore();
					return true;
				}
				return false;
			}
		});
		okButton = Ui.findView(this, R.id.ok_button);
		okButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				nameCurrentCore();
			}
		});

		if (currentRenameAttemptHexId == null) {
			startNaming();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(STATE_CURRENT_RENAME_ATTEMPT_DEVICE_ID, currentRenameAttemptHexId);
	}

	@Override
	public void onStart() {
		super.onStart();
		broadcastMgr.registerReceiver(foundSomethingReceiver, foundSomethingReceiver.getFilter());
		broadcastMgr.registerReceiver(moreCoresClaimedReceiver,
				moreCoresClaimedReceiver.getFilter());
		setHeaderCoreCountMessage();
	}

	@Override
	public void onStop() {
		broadcastMgr.unregisterReceiver(foundSomethingReceiver);
		broadcastMgr.unregisterReceiver(moreCoresClaimedReceiver);
		SmartConfigService.stopSmartConfig(getActivity());
		super.onStop();
	}

	private String getNextFoundId() {
		Iterator<String> iter = SmartConfigState.getClaimedButPossiblyUnnamedDeviceIds().iterator();
		if (iter.hasNext()) {
			return iter.next();
		} else {
			return null;
		}
	}

	private void populateName() {
		Device device = DeviceState.getDeviceById(currentRenameAttemptHexId);
		String name = null;
		if (device != null) {
			// use existing name
			name = device.name;
		}

		// it could still be null if the device was never named
		if (name == null) {
			name = CoreNameGenerator.generateUniqueName(DeviceState.getExistingCoreNames());
		}

		coreNameText.setText(name);
	}

	private void startNaming() {
		String nextId = getNextFoundId();
		if (nextId == null) {
			stopNaming();
		} else {
			currentRenameAttemptHexId = nextId;
			api.startSignalling(currentRenameAttemptHexId);
			showProgress(false);
			setHeaderCoreCountMessage();
			populateName();
		}
	}

	private void setHeaderCoreCountMessage() {
		int count = SmartConfigState.getClaimedButPossiblyUnnamedDeviceIds().size();
		Resources res = getResources();
		String letsNameCoresMsg = res.getQuantityString(R.plurals.foundCoresMsg, count, count);
		Ui.setText(this, R.id.header_text, letsNameCoresMsg);
	}

	private void nameCurrentCore() {
		String name = coreNameText.getText().toString();
		Device existingDevice = DeviceState.getDeviceById(currentRenameAttemptHexId);

		// is this the name of an existing device?
		if (DeviceState.getExistingCoreNames().contains(name)) {
			// is it the current device?
			if (existingDevice == null || !name.equals(existingDevice.name)) {
				// No, it's not, which means this name belongs to another device
				// for this user, so it's a dupe name -- don't allow it.
				new NamingHelper(getActivity(), api).showDupeNameDialog(null);
				return;
			}
		}
		api.nameCore(currentRenameAttemptHexId, name);
		showProgress(true);
	}


	private void stopNaming() {
		Intent intent = new Intent(getActivity(), CoreListActivity.class)
				.putExtra(CoreListActivity.ARG_SELECT_DEVICE_ID, currentRenameAttemptHexId)
				.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
						| Intent.FLAG_ACTIVITY_SINGLE_TOP
						| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		getActivity().finish();
	}

	private void showProgress(final boolean show) {
		showProgress(R.id.progress_indicator, show);
		okButton.setEnabled(!show);
	}

	private void namingRequestFinished(boolean previousWasSuccessful, String errorMsg) {
		if (SmartConfigState.getClaimedButPossiblyUnnamedDeviceIds().isEmpty()) {
			// we're done
			stopNaming();
			return;
		}

		startNaming();
	}


	class MoreCoresClaimedReceiver extends BroadcastReceiver {

		IntentFilter getFilter() {
			return new IntentFilter(ApiFacade.BROADCAST_CORE_CLAIMED);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			log.i("Received BROADCAST_CORE_CLAIMED.");
			setHeaderCoreCountMessage();
		}
	}


	class SmartConfigFoundSomethingReceiver extends BroadcastReceiver {

		IntentFilter getFilter() {
			return new IntentFilter(ApiFacade.BROADCAST_CORE_NAMING_REQUEST_COMPLETE);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			log.i("Received BROADCAST_CORE_NAMING_REQUEST_COMPLETE.");
			namingRequestFinished((ApiFacade.getResultCode(intent) == HttpStatus.SC_OK),
					intent.getStringExtra(SimpleSparkApiService.EXTRA_ERROR_MSG));
		}
	}

}
