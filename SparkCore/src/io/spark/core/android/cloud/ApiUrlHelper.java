package io.spark.core.android.cloud;

import static org.solemnsilence.util.Py.truthy;
import io.spark.core.android.app.AppConfig;

import java.net.MalformedURLException;
import java.net.URL;

import org.solemnsilence.util.TLog;

import android.net.Uri;


public class ApiUrlHelper {

	// all Uris just end up getting built from this one anyhow, no need to keep
	// reconstructing them completely
	private static Uri baseUri;


	public static Uri.Builder buildUri(String authToken, String... pathSegments) {
		Uri.Builder builder = getBaseUriBuilder().appendPath(AppConfig.getApiVersion());
		for (String segment : pathSegments) {
			builder.appendPath(segment);
		}

		if (truthy(authToken)) {
			builder.appendQueryParameter(AppConfig.getApiParamAccessToken(), authToken);
		}
		return builder;
	}

	public synchronized static Uri.Builder getBaseUriBuilder() {
		if (baseUri == null) {
			baseUri = new Uri.Builder()
					.scheme(AppConfig.getApiUrlScheme())
					.encodedAuthority(AppConfig.getApiHostname() + ":" + AppConfig.getApiHostPort())
					.build();
		}
		return baseUri.buildUpon();
	}

	public static URL convertToURL(Uri.Builder uriBuilder) {
		Uri builtUri = uriBuilder.build();
		try {
			return new URL(builtUri.toString());
		} catch (MalformedURLException e) {
			// Not printing exception here since I don't know for sure if this
			// could ever include the URL itself, which in the case of a GET
			// request would include the token(!)
			log.e("Unable to build URL from Uri");
			return null;
		}
	}


	private static final TLog log = new TLog(ApiUrlHelper.class);

}
