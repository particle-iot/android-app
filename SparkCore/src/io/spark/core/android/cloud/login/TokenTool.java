package io.spark.core.android.cloud.login;

import io.spark.core.android.app.AppConfig;
import io.spark.core.android.cloud.ApiUrlHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.protocol.HTTP;
import org.solemnsilence.util.TLog;

import android.net.Uri;
import android.util.Base64;

import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;


public class TokenTool {

	private static final TLog log = new TLog(TokenTool.class);

	private static final String[] PATH_SEGMENTS = new String[] { "oauth", "token" };


	private final Gson gson;
	private final OkHttpClient okHttpclient;

	public TokenTool(Gson gson, OkHttpClient okHttpclient) {
		this.gson = gson;
		this.okHttpclient = okHttpclient;
	}


	public TokenResponse requestToken(TokenRequest tokenRequest) {
		// URL url = ApiUrlHelper.buildUrlNoVersion(PATH);
		Uri.Builder uriBuilder = ApiUrlHelper.getBaseUriBuilder();
		for (String pathSegment : PATH_SEGMENTS) {
			uriBuilder.appendPath(pathSegment);
		}
		URL url = ApiUrlHelper.convertToURL(uriBuilder);
		HttpURLConnection urlConnection = null;
		try {
			urlConnection = okHttpclient.open(url);
			return requestTokenPrivate(urlConnection, tokenRequest);

		} catch (Exception e) {
			log.e("Error when logging in");
			return null;

		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}
	}

	private TokenResponse requestTokenPrivate(HttpURLConnection urlConnection,
			TokenRequest tokenRequest) {

		TokenResponse response = new TokenResponse();
		int responseCode = -1;

		urlConnection.setDoOutput(true);
		urlConnection.setConnectTimeout(5000);
		urlConnection.setReadTimeout(15000);
		urlConnection.setRequestProperty("Authorization", getBasicAuthString());

		try {
			OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
			out.write(tokenRequest.asFormEncodedData().getBytes(HTTP.UTF_8));
			out.close();

			responseCode = urlConnection.getResponseCode();

			InputStream in = new BufferedInputStream(urlConnection.getInputStream());
			String responseStr = readStream(in);
			in.close();
			if (responseStr == null) {
				log.e("Error logging in, response was null.  HTTP response: " + responseCode);
				return null;
			} else {
				response = gson.fromJson(responseStr, TokenResponse.class);
			}
		} catch (IOException e) {
			log.e("Error requesting token");
		}

		response.setStatusCode(responseCode);
		return response;
	}

	private String getBasicAuthString() {
		try {
			byte[] asBytes = AppConfig.getSparkTokenCreationCredentials().getBytes(HTTP.UTF_8);
			return "Basic " + Base64.encodeToString(asBytes, Base64.NO_WRAP);
		} catch (UnsupportedEncodingException e) {
			log.e("Error encoding String as UTF-8 bytes: ", e);
			return "";
		}
	}

	static String readStream(InputStream in) throws IOException {
		StringBuilder strBuilder = new StringBuilder();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				strBuilder.append(line).append("\n");
			}
			return strBuilder.toString();

		} finally {
			in.close();
		}
	}

}
