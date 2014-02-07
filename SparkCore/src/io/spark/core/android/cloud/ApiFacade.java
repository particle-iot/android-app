package io.spark.core.android.cloud;

import io.spark.core.android.app.DeviceState;
import io.spark.core.android.cloud.api.Device;
import io.spark.core.android.cloud.api.SimpleResponse;
import io.spark.core.android.cloud.api.TinkerResponse;
import io.spark.core.android.cloud.requestservice.SimpleSparkApiService;
import io.spark.core.android.smartconfig.SmartConfigState;
import io.spark.core.android.ui.tinker.DigitalValue;

import java.lang.reflect.Type;
import java.util.List;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.solemnsilence.util.TLog;
import org.solemnsilence.util.Toaster;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;

import com.google.gson.reflect.TypeToken;


/**
 * A simple interface for making requests and handling responses from the Spark
 * API.
 * 
 * If you want to work with the Spark API from Android, this is the place to
 * start. See examples below like {@link #nameCore(String, String)},
 * {@link #digitalWrite(String, String, DigitalValue, DigitalValue)}, etc, for
 * templates to work from.
 * 
 */
public class ApiFacade {

	private static final TLog log = new TLog(ApiFacade.class);


	public static final int REQUEST_FAILURE_CODE = -1;

	// broadcast receiver actions
	public static final String BROADCAST_SIGN_UP_FINISHED = "BROADCAST_SIGN_UP_FINISHED";
	public static final String BROADCAST_DEVICES_UPDATED = "BROADCAST_DEVICES_UPDATED";
	public static final String BROADCAST_LOG_IN_FINISHED = "BROADCAST_LOG_IN_FINISHED";
	public static final String BROADCAST_CORE_CLAIMED = "BROADCAST_CORE_CLAIMED";
	public static final String BROADCAST_CORE_NAMING_REQUEST_COMPLETE = "BROADCAST_CORE_NAMING_REQUEST_COMPLETE";
	public static final String BROADCAST_TINKER_RESPONSE_RECEIVED = "BROADCAST_TINKER_RESPONSE_RECEIVED";
	public static final String BROADCAST_SHOULD_LOG_OUT = "BROADCAST_SHOULD_LOG_OUT";

	public static final String BROADCAST_SERVICE_API_ERROR = "BROADCAST_SERVICE_API_ERROR";

	public static final String EXTRA_ERROR_CODE = "EXTRA_ERROR_CODE";
	public static final String EXTRA_TINKER_RESPONSE = "EXTRA_TINKER_RESPONSE";


	private static ApiFacade instance = null;


	public static ApiFacade getInstance(Context context) {
		if (instance == null) {
			instance = new ApiFacade(context.getApplicationContext());
		}
		return instance;
	}


	public static int getResultCode(Intent broadcastIntent) {
		int resultcode = broadcastIntent.getIntExtra(SimpleSparkApiService.EXTRA_RESULT_CODE,
				SimpleSparkApiService.REQUEST_FAILURE_CODE);
		return resultcode;
	}


	final Context ctx;
	final Handler handler;
	final LocalBroadcastManager broadcastMgr;


	private ApiFacade(Context context) {
		this.ctx = context.getApplicationContext();
		this.handler = new Handler();
		this.broadcastMgr = LocalBroadcastManager.getInstance(this.ctx);
	}


	public void signUp(String username, String password) {
		Bundle b = new Bundle();
		b.putString("username", username);
		b.putString("password", password);
		SimpleSparkApiService.post(ctx, new String[] { "users" }, b,
				new SignUpResponseReceiver(handler, username, password),
				null);
	}

	public void logIn(String username, String password) {
		SimpleSparkApiService.logIn(ctx, username, password);
	}

	public void claimCore(String coreId) {
		log.i("Making request to claim core: " + coreId);
		Bundle b = new Bundle();
		b.putString("id", coreId);
		SimpleSparkApiService.post(ctx, new String[] { "devices" }, b,
				new ClaimCoreResponseReceiver(handler, coreId), null);
	}

	public void requestUnheardCores() {
		SimpleSparkApiService.post(ctx, new String[] { "devices" }, null,
				new UnheardCoreCoreResponseReceiver(handler),
				null);
	}

	public void startSignalling(String coreId) {
		log.i("Making request for " + coreId + " to start signalling.");
		Bundle b = new Bundle();
		b.putInt("signal", 1);
		SimpleSparkApiService.put(ctx, new String[] { "devices", coreId }, b,
				new SignalingResponseReceiver(handler, coreId, 1),
				null);
	}

	public void nameCore(String coreId, String name) {
		log.i("Renaming core " + coreId + " to " + name
				+ " and instructing Core to cease any shouting of rainbows it may be doing.");
		Bundle b = new Bundle();
		b.putInt("signal", 0);
		b.putString("name", name);
		SimpleSparkApiService.put(ctx, new String[] { "devices", coreId }, b,
				new CoreNamingResponseReceiver(handler, coreId, name),
				BROADCAST_CORE_NAMING_REQUEST_COMPLETE);
	}

	public void requestAllDevices() {
		log.d("Requesting update of all devices");
		SimpleSparkApiService.get(ctx, new String[] { "devices" }, null,
				new DevicesResponseReceiver(handler),
				BROADCAST_DEVICES_UPDATED);
	}

	public void requestDevice(String coreId) {
		log.i("Requesting update for individual device: " + coreId);
		SimpleSparkApiService.get(ctx, new String[] { "devices", coreId }, null,
				new SingleDeviceResponseReceiver(handler),
				BROADCAST_DEVICES_UPDATED);
	}

	public void reflashTinker(String coreId) {
		Bundle b = new Bundle();
		b.putString("app", "tinker");
		SimpleSparkApiService.put(ctx, new String[] { "devices", coreId }, b,
				new ReflashTinkerResponseReceiver(handler, coreId),
				null);
	}

	public void digitalRead(String coreId, String pinId, DigitalValue oldValue) {
		TinkerReadValueReceiver receiver = new TinkerReadValueReceiver(handler,
				TinkerResponse.REQUEST_TYPE_READ, coreId, pinId,
				TinkerResponse.RESPONSE_TYPE_DIGITAL, oldValue.asInt());

		Bundle args = new Bundle();
		args.putString("params", pinId);

		SimpleSparkApiService.post(ctx, new String[] { "devices", coreId, "digitalread" },
				args, receiver, null);
	}

	public void digitalWrite(String coreId, String pinId, DigitalValue oldValue,
			DigitalValue newValue) {
		TinkerWriteValueReceiver receiver = new TinkerWriteValueReceiver(handler,
				TinkerResponse.REQUEST_TYPE_WRITE, coreId, pinId,
				TinkerResponse.RESPONSE_TYPE_DIGITAL, oldValue.asInt(), newValue.asInt());

		Bundle args = new Bundle();
		args.putString("params", pinId + "," + newValue.name());

		SimpleSparkApiService.post(ctx, new String[] { "devices", coreId, "digitalwrite" },
				args, receiver, null);
	}

	public void analogRead(String coreId, String pinId, int oldValue) {
		TinkerReadValueReceiver receiver = new TinkerReadValueReceiver(handler,
				TinkerResponse.REQUEST_TYPE_READ, coreId, pinId,
				TinkerResponse.RESPONSE_TYPE_ANALOG, oldValue);

		Bundle args = new Bundle();
		args.putString("params", pinId);

		SimpleSparkApiService.post(ctx, new String[] { "devices", coreId, "analogread" },
				args, receiver, null);
	}

	public void analogWrite(String coreName, String pinId, int oldValue, int newValue) {
		TinkerWriteValueReceiver receiver = new TinkerWriteValueReceiver(handler,
				TinkerResponse.REQUEST_TYPE_WRITE, coreName, pinId,
				TinkerResponse.RESPONSE_TYPE_ANALOG, oldValue, newValue);
		Bundle args = new Bundle();
		args.putString("params", pinId + "," + newValue);

		SimpleSparkApiService.post(ctx, new String[] { "devices", coreName, "analogwrite" },
				args, receiver, null);
	}


	/**
	 * This class is just to give a more clear, semantic interface to
	 * ResultReceiver when using this Service, and provide a standard way of
	 * delivering failure messages
	 * 
	 */
	public static abstract class ApiResponseReceiver extends ResultReceiver {

		public abstract void onRequestResponse(int statusCode, String jsonData);

		public ApiResponseReceiver(Handler handler) {
			super(handler);
		}

		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) {
			if (resultCode == REQUEST_FAILURE_CODE || resultCode >= 300 && shouldReportErrors()) {
				sendFailureBroadcast(resultCode);
			}

			this.onRequestResponse(resultCode,
					resultData.getString(SimpleSparkApiService.EXTRA_API_RESPONSE_JSON));
		}

		private void sendFailureBroadcast(int errorCode) {
			String action = BROADCAST_SERVICE_API_ERROR;
			if (errorCode == 400) {
				// At least for now, 400 always means
				// "your token expired/was invalidated"
				action = BROADCAST_SHOULD_LOG_OUT;
			}
			Intent errIntent = new Intent(action)
					.putExtra(EXTRA_ERROR_CODE, errorCode);
			instance.broadcastMgr.sendBroadcast(errIntent);
		}

		public boolean shouldReportErrors() {
			return true;
		}

	}


	abstract class BaseTinkerValueReceiver extends ApiResponseReceiver {

		final int requestType;
		final String coreId;
		final String pinId;
		final int valueType;
		final int oldValue;

		abstract int getPinValue(int functionReturnValue);

		public BaseTinkerValueReceiver(Handler handler, int requestType, String coreId,
				String pinId, int valueType, int oldValue) {
			super(handler);
			this.requestType = requestType;
			this.coreId = coreId;
			this.pinId = pinId;
			this.valueType = valueType;
			this.oldValue = oldValue;
		}

		@Override
		public void onRequestResponse(int statusCode, String jsonData) {
			int newPinValue = oldValue;
			boolean hasErrors = false;
			if (statusCode == HttpStatus.SC_OK) {
				try {
					JSONObject json = new JSONObject(jsonData);
					if (json.has("return_value")) {
						int returnVal = json.getInt("return_value");
						newPinValue = getPinValue(returnVal);
					} else if (json.has("error")) {
						hasErrors = true;
					}
				} catch (JSONException e) {
					log.e("Unable to get result from response JSON");
					hasErrors = true;
				}

			} else {
				log.w("Pin value update failed!  Response code: " + statusCode);
			}

			TinkerResponse response = new TinkerResponse(requestType, coreId, pinId, valueType,
					newPinValue, hasErrors);
			Intent intent = new Intent(BROADCAST_TINKER_RESPONSE_RECEIVED)
					.putExtra(EXTRA_TINKER_RESPONSE, response);
			instance.broadcastMgr.sendBroadcast(intent);
		}
	}


	class TinkerReadValueReceiver extends BaseTinkerValueReceiver {

		public TinkerReadValueReceiver(Handler handler, int requestType, String coreId,
				String pinId, int valueType, int oldValue) {
			super(handler, requestType, coreId, pinId, valueType, oldValue);
		}

		@Override
		int getPinValue(int functionReturnValue) {
			if (functionReturnValue == -1) {
				return oldValue;
			} else {
				return functionReturnValue;
			}
		}

	}


	class TinkerWriteValueReceiver extends BaseTinkerValueReceiver {

		final int newValue;

		public TinkerWriteValueReceiver(Handler handler, int requestType, String coreId,
				String pinId, int valueType, int oldValue, int newValue) {
			super(handler, requestType, coreId, pinId, valueType, oldValue);
			this.newValue = newValue;
		}

		@Override
		int getPinValue(int functionReturnValue) {
			if (functionReturnValue == -1) {
				return oldValue;
			} else {
				return newValue;
			}
		}

	}


	class SignUpResponseReceiver extends ApiResponseReceiver {

		String username;
		String password;

		public SignUpResponseReceiver(Handler handler, String username, String password) {
			super(handler);
			this.username = username;
			this.password = password;
		}

		@Override
		public void onRequestResponse(int statusCode, String jsonData) {
			SimpleResponse simpleResponse = null;

			try {
				simpleResponse = WebHelpers.getGson().fromJson(jsonData, SimpleResponse.class);
			} catch (Exception e) {
				log.w("Error trying to read sign up response.");
			}

			int statusCodeToReport = SimpleSparkApiService.REQUEST_FAILURE_CODE;
			String errorMessageToReturn = "User " + username
					+ " exists, but the password is incorrect.";
			if (statusCode == HttpStatus.SC_OK && simpleResponse != null && simpleResponse.ok) {
				log.i("Sign up succeeded");
				statusCodeToReport = statusCode;
				errorMessageToReturn = null;

			} else {
				log.w("Registration failed!  Response code: " + statusCode);
			}

			if (simpleResponse != null) {
				Intent intent = new Intent(BROADCAST_SIGN_UP_FINISHED)
						.putExtra(SimpleSparkApiService.EXTRA_RESULT_CODE, statusCodeToReport);
				if (errorMessageToReturn != null) {
					intent.putExtra(SimpleSparkApiService.EXTRA_ERROR_MSG, errorMessageToReturn);
				}
				instance.broadcastMgr.sendBroadcast(intent);

				ApiFacade.instance.logIn(username, password);
			}
		}
	}


	class CoreNamingResponseReceiver extends ApiResponseReceiver {

		final String coreId;
		final String name;

		public CoreNamingResponseReceiver(Handler handler, String coreId, String name) {
			super(handler);
			this.coreId = coreId;
			this.name = name;
		}

		@Override
		public void onRequestResponse(int statusCode, String jsonData) {
			SmartConfigState.removeClaimedButPossiblyUnnamedDeviceIds(coreId);
			if (statusCode == HttpStatus.SC_OK) {
				log.i("Naming request succeeded: " + coreId + ", name: " + name);
				DeviceState.renameDevice(coreId, name);
				requestDevice(coreId);

			} else {
				log.w("Naming request failed: " + coreId + ", name: " + name
						+ ", response code: " + statusCode);
			}
		}
	}


	class SignalingResponseReceiver extends ApiResponseReceiver {

		final String coreId;
		final int signal;

		public SignalingResponseReceiver(Handler handler, String coreId, int signal) {
			super(handler);
			this.coreId = coreId;
			this.signal = signal;
		}

		@Override
		public void onRequestResponse(int statusCode, String jsonData) {
			if (statusCode == HttpStatus.SC_OK) {
				log.i("Signaling request succeeded: " + coreId + ", value: " + signal);

			} else {
				log.w("Signaling request failed: " + coreId + ", value: " + signal
						+ ", response code: " + statusCode);
			}
		}
	}


	class UnheardCoreCoreResponseReceiver extends ApiResponseReceiver {

		public UnheardCoreCoreResponseReceiver(Handler handler) {
			super(handler);
		}

		@Override
		public void onRequestResponse(int statusCode, String jsonData) {
			try {
				JSONObject json = new JSONObject(jsonData);
				JSONArray jsonArray = json.getJSONArray("devices");
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject idObj = jsonArray.getJSONObject(i);
					String unheardCoreId = idObj.getString("id");
					log.d("Got ID of core which was 'unheard' via mDNS/CoAP: " + unheardCoreId);
					SmartConfigState.addSmartConfigFoundId(unheardCoreId);
					instance.claimCore(unheardCoreId);
				}

			} catch (JSONException e) {
				log.e("Bad JSON response trying to get the IDs of cores which weren't heard from via mDNS/CoAP");
			}

		}

		@Override
		public boolean shouldReportErrors() {
			return false;
		}

	}


	class ClaimCoreResponseReceiver extends ApiResponseReceiver {

		final String coreId;

		public ClaimCoreResponseReceiver(Handler handler, String coreId) {
			super(handler);
			this.coreId = coreId;
		}

		@Override
		public void onRequestResponse(int statusCode, String jsonData) {
			if (statusCode == HttpStatus.SC_OK) {
				log.i("Claiming Core succeeded: " + coreId);
				SmartConfigState.addClaimedButPossiblyUnnamedDeviceId(coreId);
				broadcastMgr.sendBroadcast(new Intent(BROADCAST_CORE_CLAIMED));

			} else {
				log.w("Claiming Core failed!  Response code: " + statusCode);
			}
		}

		public boolean shouldReportErrors() {
			return false;
		}

	}


	class SingleDeviceResponseReceiver extends ApiResponseReceiver {

		public SingleDeviceResponseReceiver(Handler handler) {
			super(handler);
		}

		@Override
		public void onRequestResponse(int statusCode, String jsonData) {
			if (statusCode == HttpStatus.SC_OK) {

				try {
					Device updated = WebHelpers.getGson().fromJson(jsonData, Device.class);
					DeviceState.updateSingleDevice(updated, true);
				} catch (Exception e) {
					log.e("Error parsing resposne JSON from single device request.");
				}

			} else {
				log.w("Device request failed!  Response code: " + statusCode);
			}
		}
	}


	class DevicesResponseReceiver extends ApiResponseReceiver {

		public DevicesResponseReceiver(Handler handler) {
			super(handler);
		}

		@Override
		public void onRequestResponse(int statusCode, String jsonData) {
			if (statusCode == HttpStatus.SC_OK) {
				Type listType = new TypeToken<List<Device>>() {
				}.getType();
				List<Device> devices = WebHelpers.getGson().fromJson(jsonData, listType);
				DeviceState.updateAllKnownDevices(devices);
			} else {
				log.w("Device list failed!  Response code: " + statusCode);
			}
		}
	}


	class ReflashTinkerResponseReceiver extends ApiResponseReceiver {

		final String coreId;

		public ReflashTinkerResponseReceiver(Handler handler, String coreId) {
			super(handler);
			this.coreId = coreId;
		}

		@Override
		public void onRequestResponse(int statusCode, String jsonData) {
			if (statusCode == HttpStatus.SC_OK) {
				log.d("Request to reflash Tinker succeeded: " + coreId);
				Device d = DeviceState.getDeviceById(coreId);
				Toaster.s(ctx, "Re-flashing " + d.name + " with Tinker");

			} else {
				log.w("Request to reflash Tinker failed: " + coreId + ", response code: "
						+ statusCode);
			}
		}
	}

}
