package io.spark.core.android.smartconfig;


import static org.solemnsilence.util.Py.set;
import io.spark.core.android.app.AppConfig;
import io.spark.core.android.app.DeviceState;
import io.spark.core.android.cloud.ApiFacade;
import io.spark.core.android.util.Strings;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.solemnsilence.util.EZ;
import org.solemnsilence.util.TLog;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.integrity_project.smartconfiglib.FirstTimeConfig;
import com.integrity_project.smartconfiglib.FirstTimeConfigListener;


/**
 * Service for handling SmartConfig operations. Can be easily started and
 * stopped via the static convenience methods startSmartConfig() and
 * stopSmartConfig()
 * 
 */
public class SmartConfigService extends Service implements FirstTimeConfigListener {

	private static final TLog log = new TLog(SmartConfigService.class);


	public static final String EXTRA_SSID = "EXTRA_SSID";
	public static final String EXTRA_WIFI_PASSWORD = "EXTRA_WIFI_PASSWORD";
	public static final String EXTRA_GATEWAY_IP = "EXTRA_GATEWAY_IP";
	public static final String EXTRA_AES_KEY = "EXTRA_AES_KEY";

	public static final String ACTION_START_SMART_CONFIG = "ACTION_START_SMART_CONFIG";
	public static final String ACTION_STOP_SMART_CONFIG = "ACTION_STOP_SMART_CONFIG";


	public static void startSmartConfig(Context ctx, String ssid, String wifiPassword,
			String gatewayIP, String aesKey) {
		if (aesKey == null || aesKey.length() != 16) {
			aesKey = AppConfig.getSmartConfigDefaultAesKey();
			log.i("Using default AES key for SmartConfig");
		}
		Intent intent = new Intent(ctx, SmartConfigService.class)
				.setAction(SmartConfigService.ACTION_START_SMART_CONFIG)
				.putExtra(EXTRA_SSID, ssid)
				.putExtra(EXTRA_WIFI_PASSWORD, wifiPassword)
				.putExtra(EXTRA_GATEWAY_IP, gatewayIP)
				.putExtra(EXTRA_AES_KEY, aesKey);
		ctx.startService(intent);
	}

	public static void stopSmartConfig(Context ctx) {
		ctx.startService(new Intent(ctx, SmartConfigService.class)
				.setAction(SmartConfigService.ACTION_STOP_SMART_CONFIG));
	}



	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);


	private LocalBroadcastManager broadcastMgr;
	private ApiFacade api;

	private FirstTimeConfig firstTimeConfig;
	private HelloListener helloListener;
	private Future<?> postOnNoHellosReceivedFuture;

	private boolean isStarted = false;
	private boolean receivedHello = false;


	@Override
	public void onCreate() {
		super.onCreate();
		broadcastMgr = LocalBroadcastManager.getInstance(this);
		api = ApiFacade.getInstance(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null) {
			log.d("onStartCommand() - intent arg was null, intentionally doing nothing until receving an intent with an action attached.");

		} else {
			if (ACTION_START_SMART_CONFIG.equals(intent.getAction())) {
				startSmartConfig(intent);

			} else if (ACTION_STOP_SMART_CONFIG.equals(intent.getAction())) {
				stopSmartConfig();
			}
		}

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onFirstTimeConfigEvent(FtcEvent ftcEvent, Exception error) {
		log.i("onFirstTimeConfigEvent(): " + ftcEvent);
		if (error != null) {
			log.e("Error during first time config: ", error);
		}
	}


	@Override
	public IBinder onBind(Intent intent) {
		// this method must be present but doesn't need to do anything.
		return null;
	}

	@Override
	public void onDestroy() {
		log.d("onDestroy()");
		super.onDestroy();
	}

	private void startSmartConfig(Intent intent) {
		log.i("startSmartConfig()");
		if (isStarted) {
			log.d("Smart config already started, ignoring new request to start it gain.");
			return;
		}
		try {
			if (firstTimeConfig != null) {
				firstTimeConfig.stopTransmitting();
			}
			if (helloListener != null) {
				helloListener.stopListener();
			}
			if (postOnNoHellosReceivedFuture != null) {
				postOnNoHellosReceivedFuture.cancel(true);
			}

			postOnNoHellosReceivedFuture = executor.schedule(new Runnable() {

				@Override
				public void run() {
					if (!receivedHello) {
						log.i("No Hello messages heard, making API request for the IDs any Cores we should attempt to claim.");
						api.requestUnheardCores();
					}
				}
			}, 60, TimeUnit.SECONDS);

			receivedHello = false;
			isStarted = true;
			firstTimeConfig = buildFirstTimeConfig(this, intent);
			helloListener = new HelloListener();
			helloListener.startListener();
			firstTimeConfig.transmitSettings();


		} catch (Exception e) {
			log.e("Error while transmitting settings: ", e);
		}
	}

	private void stopSmartConfig() {
		log.i("stopSmartConfig()");
		if (firstTimeConfig != null) {
			try {
				firstTimeConfig.stopTransmitting();
				helloListener.stopListener();
				if (postOnNoHellosReceivedFuture != null) {
					postOnNoHellosReceivedFuture.cancel(true);
				}
			} catch (Exception e) {
				log.e("Error trying to stop transmitting: ", e);
			}
			firstTimeConfig = null;
			helloListener = null;
			postOnNoHellosReceivedFuture = null;
		}
		isStarted = false;
		receivedHello = false;
		stopSelf();
	}


	private void onHelloIdReceived(final String hexId) {
		log.i("Core ID received via CoAP 'Hello': " + hexId);
		receivedHello = true;

		if (SmartConfigState.getClaimedButPossiblyUnnamedDeviceIds().contains(hexId)) {
			log.i("Already claimed and named this Core: " + hexId);
			return;
		}

		// See if this is a device we already know about
		if (DeviceState.getDeviceById(hexId) != null) {
			log.i("Device is alerady claimed by us but not yet offered for rename:" + hexId);
			SmartConfigState.addClaimedButPossiblyUnnamedDeviceId(hexId);
			broadcastMgr.sendBroadcast(new Intent(ApiFacade.BROADCAST_CORE_CLAIMED));

		} else {
			int delay = 2000;
			log.i("New core found, will attempt to claim in " + delay / 1000 + " seconds.");
			// HACK: wait for 2 seconds after receiving HELLO CoAP
			EZ.runOnMainThreadDelayed(new Runnable() {

				@Override
				public void run() {
					api.claimCore(hexId);
				}
			}, delay);
		}
	}

	private FirstTimeConfig buildFirstTimeConfig(FirstTimeConfigListener listener, Intent intent)
			throws Exception {
		Bundle extras = intent.getExtras();

		String ssid = extras.getString(EXTRA_SSID);
		String wifiPassword = extras.getString(EXTRA_WIFI_PASSWORD);
		String gatewayIP = extras.getString(EXTRA_GATEWAY_IP);
		String aesKey = extras.getString(EXTRA_AES_KEY);
		byte[] transmissionKey = aesKey.getBytes();

		// AES key isn't being redacted below because it's public knowledge.
		log.d("FirstTimeConfig params: SSID=" + ssid + ", wifiPassword="
				+ Strings.getRedacted(wifiPassword) + ", gatewayIP=" + gatewayIP + ", aesKey="
				+ aesKey);

		return new FirstTimeConfig(listener, wifiPassword, transmissionKey, gatewayIP, ssid);
	}

	class HelloListener {

		final AtomicBoolean shouldContinue = new AtomicBoolean(true);
		Set<String> hexIdsHeard = set();
		MulticastSocket socket;
		Future<?> future;


		void startListener() {
			final String addr = AppConfig.getSmartConfigHelloListenAddress();
			final int port = AppConfig.getSmartConfigHelloListenPort();

			try {
				socket = new MulticastSocket(port);
			} catch (IOException e1) {
				log.d("Error while listening for Hello messages", e1);
				return;
			}

			this.future = executor.submit(new Runnable() {

				@Override
				public void run() {
					try {
						socket.joinGroup(InetAddress.getByName(addr));

						// I assume this is sufficient
						byte[] buffer = new byte[1024];
						DatagramPacket dgram = new DatagramPacket(buffer, buffer.length);

						log.d("Listening for CoAP Hello messages on " + addr + ":" + port);

						while (shouldContinue.get()) {
							// blocks until a datagram is received
							socket.receive(dgram);
							readCoreId(dgram);
							dgram.setLength(buffer.length);
						}
					} catch (UnknownHostException e) {
						// only log when we were intending to continue,
						// otherwise we always show an exception in the log when
						// shutting down the socket
						if (shouldContinue.get()) {
							log.d("Error while listening for Hello messages", e);
						}

					} catch (IOException e) {
						// (see above)
						if (shouldContinue.get()) {
							log.d("Error while listening for Hello messages", e);
						}
					}
				}
			});
		}

		void stopListener() {
			shouldContinue.set(false);
			if (socket != null) {
				socket.close();
				socket = null;
			}
			if (future != null) {
				future.cancel(true);
				future = null;
			}
			hexIdsHeard.clear();
		}

		void readCoreId(DatagramPacket dgram) {
			log.d("Received " + dgram.getLength() + " byte datagram from " + dgram.getAddress());
			if (dgram.getLength() != AppConfig.getSmartConfigHelloMessageLength()) {
				log.w("Received datagram with a payload having a length of " + dgram.getLength()
						+ ", ignoring.");
				return;
			}
			byte[] idAsBytes = Arrays.copyOfRange(dgram.getData(), 7, 19);
			String asString = bytesToHexString(idAsBytes);
			if (!hexIdsHeard.contains(asString)) {
				hexIdsHeard.add(asString);
				onHelloIdReceived(asString);
			}
		}
	}

	public static String bytesToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		Formatter formatter = new Formatter(sb);
		for (byte b : bytes) {
			formatter.format("%02x", b);
		}
		return sb.toString();
	}


}
