package io.spark.core.android.app;

import static org.solemnsilence.util.Py.list;
import static org.solemnsilence.util.Py.map;
import static org.solemnsilence.util.Py.set;
import static org.solemnsilence.util.Py.truthy;
import io.spark.core.android.R;
import io.spark.core.android.cloud.WebHelpers;
import io.spark.core.android.cloud.api.Device;
import io.spark.core.android.storage.Prefs;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.solemnsilence.util.TLog;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.AsyncTask;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;


/**
 * Common access to {@link Device}s.
 * 
 * Uses an in-memory cache, backed by on-disk storage using SharedPreferences.
 * 
 */
public class DeviceState {

	static final TLog log = new TLog(DeviceState.class);


	private static final Map<String, Device> deviceMap = map();
	private static final Random random = new Random();

	private static Context appContext;


	public static synchronized void initialize(Context ctx) {
		appContext = ctx.getApplicationContext();
		String coresJsonArray = Prefs.getInstance().getCoresJsonArray();
		Type listType = new TypeToken<List<Device>>() {
		}.getType();
		List<Device> devices = WebHelpers.getGson().fromJson(coresJsonArray, listType);
		updateAllKnownDevices(devices);
	}

	public synchronized static List<Device> getKnownDevices() {
		return Lists.newArrayList(deviceMap.values());
	}

	public synchronized static Device getDeviceById(String deviceId) {
		return (deviceId == null) ? null : deviceMap.get(deviceId);
	}

	public synchronized static void updateAllKnownDevices(List<Device> updatedDevices) {
		log.d("Updating known devices with: " + updatedDevices);

		Set<String> updatedDeviceIds = set();
		for (Device updatedDevice : updatedDevices) {
			String updatedDeviceId = updateSingleDevice(updatedDevice, false);
			updatedDeviceIds.add(updatedDeviceId);
		}

		// now remove the devices which weren't in the update.
		for (String missingDeviceId : set(deviceMap.keySet()).getDifference(updatedDeviceIds)) {
			log.d("Removing device from local device store: " + missingDeviceId);
			deviceMap.remove(missingDeviceId);
		}

		saveDevices();
	}

	// returns the ID of the updated device.
	public synchronized static String updateSingleDevice(Device updatedDevice, boolean save) {
		Device oldDevice = deviceMap.get(updatedDevice.id);
		Device.Builder toInsert = updatedDevice.toBuilder();

		// ensure we never have a device with a
		if (oldDevice != null) {
			toInsert.fillInFalseyValuesFromOther(oldDevice);
		}

		if (!truthy(toInsert.getColor())) {
			toInsert.setColor(getRandomCoreColor());
		}
		if (!truthy(toInsert.getName())) {
			// don't allow null or empty string names
			toInsert.setName(appContext.getString(R.string._unnamed_core_));
		}

		Device built = toInsert.build();
		deviceMap.put(built.id, built);

		if (save) {
			saveDevices();
		}

		return updatedDevice.id;
	}

	public synchronized static void renameDevice(String coreId, String newName) {
		// Create a device with default values and let 'updateSingleDevice' do
		// the work. Kinda cheesy, but it works.
		Device device = deviceMap.get(coreId);
		if (device == null) {
			log.e("Cannot rename, no device found with ID: " + coreId);
			return;
		}
		updateSingleDevice(device.toBuilder()
				.setName(newName)
				.build(),
				true);
	}

	public synchronized static Set<String> getExistingCoreNames() {
		return set(Collections2.transform(deviceMap.values(),
				new Function<Device, String>() {

					@Override
					public String apply(Device device) {
						return device.name;
					}
				}));
	}


	private static int getRandomCoreColor() {
		TypedArray colors = appContext.getResources().obtainTypedArray(R.array.core_colors);
		int max = colors.length() - 1;
		int min = 0;
		int randomIdx = random.nextInt((max - min) + 1) + min;
		int color = colors.getColor(randomIdx, 0);
		colors.recycle();
		return color;
	}


	private synchronized static void saveDevices() {
		new DevicesSaver(list(deviceMap.values()))
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}


	private static class DevicesSaver extends AsyncTask<Void, Void, Void> {

		final Prefs prefs = Prefs.getInstance();
		final List<Device> devices;

		public DevicesSaver(List<Device> devices) {
			this.devices = devices;
		}

		@Override
		protected Void doInBackground(Void... params) {
			Type listType = new TypeToken<List<Device>>() {
			}.getType();
			String asJson = WebHelpers.getGson().toJson(devices, listType);
			prefs.saveCoresJsonArray(asJson);
			return null;
		}

	}


}
