package io.spark.core.android.util;

import static org.solemnsilence.util.Py.truthy;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.format.Formatter;


public class NetConnectionHelper {

	final Context context;
	final WifiManager wifiManager;
	final ConnectivityManager connManager;

	public NetConnectionHelper(Context context) {
		// avoid retaining any context but the application context unless truly
		// necessary
		this.context = context.getApplicationContext();
		this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		this.connManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	public String getSSID() {
		WifiInfo wifiInfo = getWifiInfo();
		if (wifiInfo == null || !isConnectedViaWifi()) {
			return "";
		}

		return removeQuotes(wifiInfo.getSSID());
	}

	public boolean hasDataConnection() {
		NetworkInfo activeNetworkInfo = connManager.getActiveNetworkInfo();
		if (activeNetworkInfo != null) {
			return activeNetworkInfo.isConnected();
		} else {
			return false;
		}
	}

	public boolean isConnectedViaWifi() {
		NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (networkInfo == null) {
			return false;
		} else {
			return networkInfo.isConnected();
		}
	}

	private WifiInfo getWifiInfo() {
		return wifiManager.getConnectionInfo();
	}

	public String getGatewayIp() {
		DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
		if (dhcpInfo == null) {
			return null;
		} else {
			return Formatter.formatIpAddress(dhcpInfo.gateway);
		}
	}

	// in Jellybean, SSIDs can have quotes around them
	private static String removeQuotes(String ssid) {
		if (!truthy(ssid)) {
			return "";
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
				ssid = ssid.substring(1, ssid.length() - 1);
			}
		}
		return ssid;
	}


}
