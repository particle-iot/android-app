package io.spark.core.android.ui.corelist;

import io.spark.core.android.R;
import io.spark.core.android.app.DeviceState;
import io.spark.core.android.cloud.ApiFacade;
import io.spark.core.android.cloud.api.Device;
import io.spark.core.android.ui.tinker.TinkerFragment;
import io.spark.core.android.ui.util.Ui;

import org.solemnsilence.util.EZ;
import org.solemnsilence.util.Py;
import org.solemnsilence.util.Py.Ranger.IntValue;
import org.solemnsilence.util.TLog;

import android.app.Activity;
import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;



/**
 * A list fragment representing a list of Cores. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon
 * selection. This helps indicate which item is currently being viewed in a
 * {@link TinkerFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class CoreListFragment extends ListFragment {


	private static final TLog log = new TLog(CoreListFragment.class);


	/**
	 * The fragment's current callback object, which is notified of list item
	 * clicks.
	 */
	private Callbacks mCallbacks = sDummyCallbacks;

	/**
	 * The current activated item position. Only used on tablets.
	 */
	private int mActivatedPosition = ListView.INVALID_POSITION;

	/**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of item
	 * selections.
	 */
	public interface Callbacks {

		/**
		 * Callback for when an item has been selected.
		 */
		public void onItemSelected(String id);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks() {

		@Override
		public void onItemSelected(String id) {
		}
	};

	DeviceListAdapter deviceAdapter;
	DevicesUpdatedReceiver updatesReceiver;
	LocalBroadcastManager broadcastMgr;
	ApiFacade api;

	String selectedDeviceId = null;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public CoreListFragment() {
	}


	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException("Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
		broadcastMgr = LocalBroadcastManager.getInstance(activity);
		api = ApiFacade.getInstance(activity);

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		updatesReceiver = new DevicesUpdatedReceiver();
		deviceAdapter = new DeviceListAdapter(getActivity());
		setListAdapter(deviceAdapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_core_list, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		updateDevicesList();
	}

	@Override
	public void onStart() {
		super.onStart();
		broadcastMgr.registerReceiver(updatesReceiver, updatesReceiver.getIntentFilter());
	}

	@Override
	public void onStop() {
		broadcastMgr.unregisterReceiver(updatesReceiver);
		super.onStop();
	}

	@Override
	public void onDetach() {
		super.onDetach();
		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.core_list, menu);
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		super.onListItemClick(listView, view, position, id);

		Device selectedDevice = deviceAdapter.getItem(position);
		// Notify the active callbacks interface (the activity, if the
		// fragment is attached to one) that an item has been selected.
		mCallbacks.onItemSelected(selectedDevice.id);
		mActivatedPosition = position;
	}

	public void setActivatedItem(String id) {
		Device device = DeviceState.getDeviceById(id);
		if (device == null) {
			log.w("Device was null when trying to set active list item??");
			return;
		}
		selectedDeviceId = device.id;
		mActivatedPosition = deviceAdapter.getPosition(device);
		setActiveCoreView();
	}

	private void updateDevicesList() {
		log.d("updateDevicesList()");
		deviceAdapter.clear();
		deviceAdapter.addAll(DeviceState.getKnownDevices());
		if (selectedDeviceId != null) {
			setActivatedItem(selectedDeviceId);
			setActiveCoreView();
		}
	}

	private void setActiveCoreView() {
		// This is just *ugly*, but all of this seems to be necessary to
		// *always* show the colored stripe next to the selected core.
		for (IntValue intValue : Py.range(getListView().getChildCount())) {
			View child = getListView().getChildAt(intValue.value);
			if (intValue.value == mActivatedPosition) {
				Ui.findView(child, R.id.active_core_stripe).setVisibility(View.VISIBLE);
				child.setActivated(true);
			} else {
				Ui.findView(child, R.id.active_core_stripe).setVisibility(View.INVISIBLE);
			}
		}

		EZ.runOnMainThread(new Runnable() {

			@Override
			public void run() {
				ListView lv = null;
				try {
					lv = getListView();
					if (getActivity() == null || getActivity().isFinishing() || lv == null) {
						return;
					}
				} catch (IllegalStateException e) {
					return;
				}
				for (IntValue intValue : Py.range(lv.getChildCount())) {
					View child = lv.getChildAt(intValue.value);
					if (intValue.value == mActivatedPosition) {
						Ui.findView(child, R.id.active_core_stripe).setVisibility(View.VISIBLE);
						child.setActivated(true);
					} else {
						Ui.findView(child, R.id.active_core_stripe).setVisibility(View.INVISIBLE);
					}
				}
			}
		});
	}

	class DevicesUpdatedReceiver extends BroadcastReceiver {

		IntentFilter getIntentFilter() {
			return new IntentFilter(ApiFacade.BROADCAST_DEVICES_UPDATED);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			updateDevicesList();
		}
	}

}
