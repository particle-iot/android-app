package io.spark.core.android.storage;

import static org.solemnsilence.util.Py.list;
import io.spark.core.android.app.AppConfig;
import io.spark.core.android.ui.tinker.PinAction;

import java.util.List;

import io.spark.core.android.R;
import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {

	private static final String BUCKET_NAME = "defaultPrefsBucket";

	private static final String KEY_URL_SCHEME = "urlscheme";
	private static final String KEY_HOST_NAME = "hostname";
	private static final String KEY_HOST_PORT = "hostport";
	private static final String KEY_USERNAME = "username";
	private static final String KEY_EXTTINKER = "exttinker";
	private static final String KEY_TOKEN = "token";
	private static final String KEY_COMPLETED_FIRST_LOGIN = "completedFirstLogin";
	private static final String KEY_CORES_JSON_ARRAY = "coresJsonArray";
	private static final String KEY_PIN_CONFIG_TEMPLATE = "corePinConfig_core-$1%s_pin-$2%s";

	private static Context ctx = null;
	private static Prefs instance = null;

	// making this a singleton to avoid having to pass around Context everywhere
	// that this info is needed. Kind of cheating, but in practice, it will be
	// fine here.
	public static Prefs getInstance() {
		return instance;
	}

	public static void initialize(Context context) {
		instance = new Prefs(context);
	}

	private final SharedPreferences prefs;

	private Prefs(Context context) {
		ctx = context.getApplicationContext();
		prefs = context.getApplicationContext().getSharedPreferences(
				BUCKET_NAME, Context.MODE_PRIVATE);

		getApiUrlScheme();
		getApiHostname();
		getApiHostPort();
	}

	public String getApiUrlScheme() {
		String urlscheme = prefs.getString(KEY_URL_SCHEME, null);
		if (urlscheme == null || urlscheme.length() <= 0) {
			urlscheme = ctx.getString(R.string.api_url_scheme);
		}
		AppConfig.setApiUrlScheme(urlscheme);
		return urlscheme;
	}

	public void saveApiUrlScheme(String urlscheme) {
		AppConfig.setApiUrlScheme(urlscheme);
		saveString(KEY_URL_SCHEME, urlscheme);
	}

	public String getApiHostname() {
		String hostname = prefs.getString(KEY_HOST_NAME, null);
		if (hostname == null || hostname.length() <= 0) {
			int resId = AppConfig.useStaging() ? R.string.staging_hostname
					: R.string.prod_hostname;
			hostname = ctx.getString(resId);
		}
		AppConfig.setApiHostname(hostname);
		return hostname;
	}

	public void saveApiHostname(String hostname) {
		AppConfig.setApiHostname(hostname);
		saveString(KEY_HOST_NAME, hostname);
	}

	public int getApiHostPort() {
		String hostport = prefs.getString(KEY_HOST_PORT, null);
		int portnumber = -1;
		if (hostport != null && hostport.length() > 0)
			portnumber = Integer.parseInt(hostport);
		else
			portnumber = ctx.getResources().getInteger(R.integer.api_host_port);

		AppConfig.setApiHostPort(portnumber);
		return portnumber;
	}

	public void saveApiHostPort(int hostport) {
		AppConfig.setApiHostPort(hostport);
		saveString(KEY_HOST_PORT, Integer.toString(hostport));
	}

	public void saveApiHostPort(String hostport) {
		int portnumber = -1;

		try // if hostport is parsable into integer
		{
			portnumber = Integer.parseInt(hostport);
		} catch (NumberFormatException nfe) { // otherwise use default
			hostport = Integer.toString(portnumber = ctx.getResources()
					.getInteger(R.integer.api_host_port));
		}

		AppConfig.setApiHostPort(portnumber);
		saveString(KEY_HOST_PORT, hostport);
	}

	public String getUsername() {
		return prefs.getString(KEY_USERNAME, null);
	}

	public void saveUsername(String username) {
		saveString(KEY_USERNAME, username);
	}

	public String getToken() {
		return prefs.getString(KEY_TOKEN, null);
	}

	public void saveToken(String token) {
		saveString(KEY_TOKEN, token);
	}

	public boolean getCompletedFirstLogin() {
		return prefs.getBoolean(KEY_COMPLETED_FIRST_LOGIN, false);
	}

	public void saveCompletedFirstLogin(boolean value) {
		prefs.edit().putBoolean(KEY_COMPLETED_FIRST_LOGIN, value).commit();
	}

	public String getCoresJsonArray() {
		return prefs.getString(KEY_CORES_JSON_ARRAY, "[]");
	}

	public void saveCoresJsonArray(String coresJson) {
		saveString(KEY_CORES_JSON_ARRAY, coresJson);
	}

	public PinAction getPinFunction(String coreId, String pinName) {
		String key = String.format(KEY_PIN_CONFIG_TEMPLATE, coreId, pinName);
		return PinAction.valueOf(prefs.getString(key, PinAction.NONE.name()));
	}

	public void savePinFunction(String coreId, String pinName,
			PinAction function) {
		String key = String.format(KEY_PIN_CONFIG_TEMPLATE, coreId, pinName);
		applyString(key, function.name());
	}

	public boolean getTinkerExtensions(String coreId) {
		if (prefs.contains(KEY_EXTTINKER + coreId))
			return prefs.getBoolean(KEY_EXTTINKER + coreId, false);
		else 
			return false;
	}

	public void saveTinkerExtensions(boolean exttinker, String coreId) {
		prefs.edit().putBoolean(KEY_EXTTINKER + coreId, exttinker).commit();
	}
	
	public void clearTinker(String coreId) {
		List<String> pinNames = list("A0", "A1", "A2", "A3", "A4", "A5", "A6",
				"A7", "D0", "D1", "D2", "D3", "D4", "D5", "D6", "D7", "TX",
				"RX");
		for (String pinName : pinNames) {
			savePinFunction(coreId, pinName, PinAction.NONE);
		}
	}

	public void clear() {
		boolean completed = getCompletedFirstLogin();

		String apiUrlScheme = getApiUrlScheme();
		String apiHostname = getApiHostname();
		int apiHostPort = getApiHostPort();

		String username = getUsername();

		prefs.edit().clear().commit();
		saveCompletedFirstLogin(completed);

		saveApiUrlScheme(apiUrlScheme);
		saveApiHostname(apiHostname);
		saveApiHostPort(apiHostPort);
		saveUsername(username);
	}

	private void saveString(String key, String value) {
		prefs.edit().putString(key, value).commit();
	}

	private void applyString(String key, String value) {
		prefs.edit().putString(key, value).apply();
	}

}
