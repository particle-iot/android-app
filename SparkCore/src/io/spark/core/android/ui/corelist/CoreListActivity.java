package io.spark.core.android.ui.corelist;

import static org.solemnsilence.util.Py.truthy;
import io.spark.core.android.R;
import io.spark.core.android.app.DeviceState;
import io.spark.core.android.cloud.api.Device;
import io.spark.core.android.smartconfig.SmartConfigState;
import io.spark.core.android.ui.BaseActivity;
import io.spark.core.android.ui.smartconfig.SmartConfigActivity;
import io.spark.core.android.ui.tinker.TinkerFragment;
import io.spark.core.android.ui.util.Ui;

import org.solemnsilence.util.TLog;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SlidingPaneLayout;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;



public class CoreListActivity extends BaseActivity implements CoreListFragment.Callbacks {

	private static final TLog log = new TLog(CoreListActivity.class);


	public static final String ARG_SKIP_TO_SMART_CONFIG = "ARG_SKIP_TO_SMART_CONFIG";
	public static final String ARG_ENTERING_FROM_LAUNCH = "ARG_ENTERING_FROM_LAUNCH";
	public static final String ARG_SELECT_DEVICE_ID = "ARG_SELECT_DEVICE_ID";

	private static final String STATE_SELECTED_DEVICE_ID = "STATE_SELECTED_DEVICE_ID";
	private static final String STATE_PANE_OPEN = "STATE_PANE_OPEN";


	private LayerDrawable actionBarBackgroundDrawable;
	private ActionBar actionBar;
	private SlidingPaneLayout slidingLayout;
	private String selectedItemId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String deviceIdToSelect = null;
		boolean openPane = true;

		// The below is to try to present the user with the "best"
		// activity on launch, but still allowing them to return to the Core
		// list.
		// TODO: document further
		Intent intentToSkipTo = null;
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(STATE_SELECTED_DEVICE_ID)) {
				deviceIdToSelect = savedInstanceState.getString(STATE_SELECTED_DEVICE_ID);
			}
			if (savedInstanceState.containsKey(STATE_PANE_OPEN)) {
				openPane = savedInstanceState.getBoolean(STATE_PANE_OPEN);
			}

		} else if (getIntent().hasExtra(ARG_SKIP_TO_SMART_CONFIG)) {
			getIntent().removeExtra(ARG_SKIP_TO_SMART_CONFIG);
			intentToSkipTo = new Intent(this, SmartConfigActivity.class);

		} else if (getIntent().hasExtra(ARG_ENTERING_FROM_LAUNCH)) {
			log.i("Known devices count: " + DeviceState.getKnownDevices().size());
			if (DeviceState.getKnownDevices().isEmpty()) {
				intentToSkipTo = new Intent(this, SmartConfigActivity.class);

			} else if (DeviceState.getKnownDevices().size() == 1) {
				Device device = DeviceState.getKnownDevices().get(0);
				deviceIdToSelect = device.id;
			}

			getIntent().removeExtra(ARG_ENTERING_FROM_LAUNCH);

		} else if (getIntent().hasExtra(ARG_SELECT_DEVICE_ID)) {
			deviceIdToSelect = getIntent().getStringExtra(ARG_SELECT_DEVICE_ID);
			getIntent().removeExtra(ARG_SELECT_DEVICE_ID);
		}

		// NOTE EARLY RETURN!
		if (intentToSkipTo != null) {
			startActivity(intentToSkipTo);
			finish();
			return;
		}

		setContentView(R.layout.activity_core_list);

		actionBar = getActionBar();

		initActionBar();

		slidingLayout = (SlidingPaneLayout) findViewById(R.id.sliding_pane_layout);

		slidingLayout.setPanelSlideListener(new SliderListener());
		slidingLayout.getViewTreeObserver().addOnGlobalLayoutListener(new InitialLayoutListener());

		if (openPane) {
			slidingLayout.openPane();
		} else {
			slidingLayout.closePane();
		}

		if (deviceIdToSelect != null) {
			onItemSelected(deviceIdToSelect);
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (intent.hasExtra(ARG_SELECT_DEVICE_ID)) {
			String deviceIdToSelect = intent.getStringExtra(ARG_SELECT_DEVICE_ID);
			intent.removeExtra(ARG_SELECT_DEVICE_ID);
			onItemSelected(deviceIdToSelect);
		}
	}

	private void initActionBar() {
		// this is such a rad effect. Huge props to Cyril Mottier for his
		// "Pushing the ActionBar to the Next Level" article, which inspired the
		// basis of this
		actionBarBackgroundDrawable = (LayerDrawable) getResources().getDrawable(
				R.drawable.action_bar_layers);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
			actionBarBackgroundDrawable.setCallback(new Drawable.Callback() {

				@Override
				public void invalidateDrawable(Drawable who) {
					getActionBar().setBackgroundDrawable(who);
				}

				@Override
				public void scheduleDrawable(Drawable who, Runnable what, long when) {
				}

				@Override
				public void unscheduleDrawable(Drawable who, Runnable what) {
				}
			});
		}
		actionBar.setBackgroundDrawable(actionBarBackgroundDrawable);
		actionBarBackgroundDrawable.getDrawable(1).setAlpha(0);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(STATE_PANE_OPEN, slidingLayout.isOpen());
		if (selectedItemId != null) {
			outState.putString(STATE_SELECTED_DEVICE_ID, selectedItemId);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		SmartConfigState.clearSmartConfigData();
		api.requestAllDevices();
		if (selectedItemId == null && !DeviceState.getKnownDevices().isEmpty()) {
			onItemSelected(DeviceState.getKnownDevices().get(0).id);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

			case R.id.action_set_up_a_new_core:
				startActivity(new Intent(this, SmartConfigActivity.class));
				return true;

			case android.R.id.home:
				if (!slidingLayout.isOpen()) {
					slidingLayout.openPane();
					return true;
				}
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Callback method from {@link CoreListFragment.Callbacks} indicating that
	 * the item with the given ID was selected.
	 */
	@Override
	public void onItemSelected(String id) {
		// same item selected, just close the pane
		if (id.equals(selectedItemId)) {
			slidingLayout.closePane();
			return;
		}

		Device deviceById = DeviceState.getDeviceById(id);
		setCustomActionBarTitle(deviceById.name);

		selectedItemId = id;
		getFragmentManager()
				.beginTransaction()
				.replace(R.id.tinker_container, TinkerFragment.newInstance(id))
				.commit();

		CoreListFragment listFrag = Ui.findFrag(this, R.id.core_list);
		listFrag.setActivatedItem(selectedItemId);
		slidingLayout.closePane();
	}

	@Override
	public void onBackPressed() {
		if (!slidingLayout.isOpen()) {
			slidingLayout.openPane();
		} else {
			super.onBackPressed();
		}
	}

	protected boolean shouldShowUpButtonWhenDevicesListNotEmpty() {
		return false;
	}

	private void panelOpened() {
		Fragment tinkerFrag = Ui.findFrag(this, R.id.tinker_container);

		if (tinkerFrag == null) {
			log.v("Tinker fragment is null");
		}

		if (slidingLayout.isSlideable()) {
			Ui.findFrag(this, R.id.core_list).setHasOptionsMenu(true);
			if (tinkerFrag != null) {
				tinkerFrag.setHasOptionsMenu(false);
			}
		} else {
			Ui.findFrag(this, R.id.core_list).setHasOptionsMenu(true);
			if (tinkerFrag != null) {
				tinkerFrag.setHasOptionsMenu(true);
			}
		}

		actionBar.setHomeButtonEnabled(false);
		actionBar.setDisplayHomeAsUpEnabled(false);

		setCustomActionBarTitle(getString(R.string.app_name_lower));
	}

	private void panelClosed() {
		Ui.findFrag(this, R.id.core_list).setHasOptionsMenu(false);
		Fragment tinkerFrag = Ui.findFrag(this, R.id.tinker_container);
		if (tinkerFrag != null) {
			tinkerFrag.setHasOptionsMenu(true);
		}

		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);

		if (selectedItemId != null) {
			Device selectedDevice = DeviceState.getDeviceById(selectedItemId);
			if (selectedDevice != null && truthy(selectedDevice.name)) {
				setCustomActionBarTitle(selectedDevice.name);
			} else {
				setCustomActionBarTitle(getString(R.string._unnamed_core_));
			}
		} else {
			log.wtf("Selected item is null?");
		}
	}


	private class SliderListener extends SlidingPaneLayout.SimplePanelSlideListener {

		@Override
		public void onPanelOpened(View panel) {
			panelOpened();
		}

		@Override
		public void onPanelClosed(View panel) {
			panelClosed();
		}

		@Override
		public void onPanelSlide(View view, float v) {
			final int newAlpha = (int) (v * 255 * 0.5);
			actionBarBackgroundDrawable.getDrawable(1).setAlpha(newAlpha);
		}
	}


	private class InitialLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {

		@SuppressWarnings("deprecation")
		@SuppressLint("NewApi")
		@Override
		public void onGlobalLayout() {
			if (slidingLayout.isSlideable() && !slidingLayout.isOpen()) {
				panelClosed();
			} else {
				panelOpened();
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				slidingLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			} else {
				slidingLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
		}
	}

}
