package io.spark.core.android.app;

import io.spark.core.android.R;
import android.content.Context;

public class AppConfig {

	private static Context ctx;
	private static String apiUrlScheme = null;
	private static String apiHostname = null;
	private static int apiHostPort = -1;

	// Should be called when starting up the app, probably in
	// Application.onCreate()
	public static void initialize(Context context) {
		ctx = context.getApplicationContext();
	}

	public static String getApiUrlScheme() {
		// static UrlScheme - dynamic version via Prefs
		if (apiUrlScheme != null)
			return apiUrlScheme;
		else
			return ctx.getString(R.string.api_url_scheme);
	}

	public static void setApiUrlScheme(String urlscheme) {
		apiUrlScheme = urlscheme;
	}

	public static String getApiHostname() {
		// static Hostname - dynamic version via Prefs
		if (apiHostname != null)
			return apiHostname;
		else {
			int resId = useStaging() ? R.string.staging_hostname
					: R.string.prod_hostname;
			return ctx.getString(resId);
		}
	}

	public static void setApiHostname(String hostname) {
		apiHostname = hostname;
	}

	public static int getApiHostPort() {
		// static HostPort- dynamic version via Prefs
		if (apiHostPort > 0)
			return apiHostPort;
		else
			return ctx.getResources().getInteger(R.integer.api_host_port);
	}

	public static void setApiHostPort(int hostport) {
		apiHostPort = hostport;
	}

	public static String getSparkTokenCreationCredentials() {
		return ctx.getString(R.string.spark_token_creation_credentials);
	}

	public static boolean useStaging() {
		return ctx.getResources().getBoolean(R.bool.use_staging);
	}

	public static String getApiVersion() {
		return ctx.getString(R.string.api_version);
	}

	public static String getApiParamAccessToken() {
		return ctx.getString(R.string.api_param_access_token);
	}

	public static String getSmartConfigHelloListenAddress() {
		return ctx.getString(R.string.smart_config_hello_listen_address);
	}

	public static int getSmartConfigHelloListenPort() {
		return ctx.getResources().getInteger(R.integer.smart_config_hello_port);
	}

	public static int getSmartConfigHelloMessageLength() {
		return ctx.getResources().getInteger(
				R.integer.smart_config_hello_msg_length);
	}

	public static String getSmartConfigDefaultAesKey() {
		return ctx.getString(R.string.smart_config_default_aes_key);
	}

}
