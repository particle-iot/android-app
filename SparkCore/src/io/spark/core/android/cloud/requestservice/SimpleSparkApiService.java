package io.spark.core.android.cloud.requestservice;

import static org.solemnsilence.util.Py.list;
import static org.solemnsilence.util.Py.truthy;
import io.spark.core.android.R;
import io.spark.core.android.app.AppConfig;
import io.spark.core.android.cloud.ApiFacade;
import io.spark.core.android.cloud.ApiFacade.ApiResponseReceiver;
import io.spark.core.android.cloud.ApiUrlHelper;
import io.spark.core.android.cloud.WebHelpers;
import io.spark.core.android.cloud.login.TokenRequest;
import io.spark.core.android.cloud.login.TokenResponse;
import io.spark.core.android.cloud.login.TokenTool;
import io.spark.core.android.storage.Prefs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.solemnsilence.util.TLog;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;


/**
 * IntentService which performs the actual HTTP calls to talk to the Spark
 * Cloud.
 * 
 * You probably only need to poke around in here to look at the post/put/get
 * methods, or if you're debugging.
 * 
 */
public class SimpleSparkApiService extends ClearableIntentService {


	/**
	 * Key to retrieve the API response from the Bundle for the ResultReceiver
	 */
	public static final String EXTRA_API_RESPONSE_JSON = "EXTRA_API_RESPONSE_JSON";

	public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";
	public static final String EXTRA_ERROR_MSG = "EXTRA_ERROR_MSG";

	/**
	 * The status code returned when a request could not be made, i.e.: an
	 * IOException was raised because a socket couldn't be opened, etc.
	 */
	public static final int REQUEST_FAILURE_CODE = -1;


	/**
	 * Perform a POST request with the given args -- see {@link ApiFacade} for
	 * examples.
	 * 
	 * @param ctx
	 *            any Context
	 * @param resourcePathSegments
	 *            the URL path as a String array (not including version string).
	 *            e.g.: if your path was
	 *            "/v1/devices/0123456789abcdef01234567/myFunction", you'd use:
	 *            new String[] { "devices", "0123456789abcdef01234567",
	 *            "myFunction" }
	 * @param formEncodingBodyData
	 *            the data to post, as key-value pairs to be encoded as form
	 *            data.
	 * @param resultReceiver
	 *            Optional; specifies the ResultReceiver instance to use for
	 *            receiving the result. Using a subclass of
	 *            {@link ApiResponseReceiver} here is recommended for
	 *            simplicity.
	 * @param broadcastName
	 *            Optional; specifies the "action" string for a broadcast to be
	 *            sent via {@link LocalBroadcastManager}. See
	 *            {@link #processResponse(Response, Intent)} for more info.
	 */
	public static void post(Context ctx, String[] resourcePathSegments,
			Bundle formEncodingBodyData, ResultReceiver resultReceiver, String broadcastName) {
		ctx.startService(
				buildRestRequestIntent(ctx, resourcePathSegments, formEncodingBodyData,
						resultReceiver, broadcastName)
						.setAction(ACTION_POST));
	}

	/**
	 * Perform a PUT request with the given args -- see {@link ApiFacade} for
	 * examples
	 * 
	 * @param ctx
	 *            any Context
	 * @param resourcePathSegments
	 *            the URL path as a String array (not including version string).
	 *            e.g.: if your path was
	 *            "/v1/devices/0123456789abcdef01234567/myFunction", you'd use:
	 *            new String[] { "devices", "0123456789abcdef01234567",
	 *            "myFunction" }
	 * @param params
	 *            the data to PUT, as key-value pairs in a Bundle
	 * @param resultReceiver
	 *            Optional; specifies the ResultReceiver instance to use for
	 *            receiving the result. Using a subclass of
	 *            {@link ApiResponseReceiver} here is recommended for
	 *            simplicity.
	 * @param broadcastName
	 *            Optional; specifies the "action" string for a broadcast to be
	 *            sent via {@link LocalBroadcastManager}. See
	 *            {@link #processResponse(Response, Intent)} for more info.
	 */
	public static void put(Context ctx, String[] resourcePathSegments, Bundle params,
			ResultReceiver resultReceiver, String broadcastName) {
		ctx.startService(
				// null post data
				buildRestRequestIntent(ctx, resourcePathSegments, params, resultReceiver,
						broadcastName)
						.setAction(ACTION_PUT));
	}

	/**
	 * Perform a GET request -- see {@link ApiFacade} for examples
	 * 
	 * @param ctx
	 *            any Context
	 * @param resourcePathSegments
	 *            the URL path as a String array (not including version string).
	 *            e.g.: if your path was
	 *            "/v1/devices/0123456789abcdef01234567/myFunction", you'd use:
	 *            new String[] { "devices", "0123456789abcdef01234567",
	 *            "myFunction" }
	 * @param params
	 *            the URL params, as key-value pairs in a Bundle
	 * @param resultReceiver
	 *            Optional; specifies the ResultReceiver instance to use for
	 *            receiving the result. Using a subclass of
	 *            {@link ApiResponseReceiver} here is recommended for
	 *            simplicity.
	 * @param broadcastName
	 *            Optional; specifies the "action" string for a broadcast to be
	 *            sent via {@link LocalBroadcastManager}. See
	 *            {@link #processResponse(Response, Intent)} for more info.
	 */
	public static void get(Context ctx, String[] resourcePathSegments, Bundle params,
			ResultReceiver resultReceiver, String broadcastName) {
		ctx.startService(
				buildRestRequestIntent(ctx, resourcePathSegments, params, resultReceiver,
						broadcastName)
						.setAction(ACTION_GET));
	}


	// Logging in is handled a little differently, since it requires a number of
	// different behaviors
	public static void logIn(Context ctx, String username, String password) {
		Intent intent = new Intent(ctx, SimpleSparkApiService.class)
				.putExtra("username", username)
				.putExtra("password", password)
				.setAction(ACTION_LOG_IN);
		ctx.startService(intent);
	}



	private static Intent buildRestRequestIntent(Context ctx, String[] resourcePathSegments,
			Bundle params, ResultReceiver resultReceiver, String broadcastAction) {

		Intent intent = new Intent(ctx, SimpleSparkApiService.class)
				.putExtra(EXTRA_RESOURCE_PATH_SEGMENTS, resourcePathSegments);

		if (params != null) {
			intent.putExtra(EXTRA_REQUEST_QUERY_PARAMS, params);
		}

		if (resultReceiver != null) {
			intent.putExtra(EXTRA_RESULT_RECEIVER, resultReceiver);
		}

		if (broadcastAction != null) {
			intent.putExtra(EXTRA_BROADCAST_ACTION, broadcastAction);
		}

		return intent;
	}


	private static final String NS = SimpleSparkApiService.class.getCanonicalName() + ".";
	private static final String ACTION_GET = NS + "ACTION_GET";
	private static final String ACTION_POST = NS + "ACTION_POST";
	private static final String ACTION_PUT = NS + "ACTION_PUT";

	private static final String ACTION_LOG_IN = NS + "ACTION_LOG_IN";

	private static final String EXTRA_RESOURCE_PATH_SEGMENTS = NS + "EXTRA_RESOURCE_PATH_SEGMENTS";
	private static final String EXTRA_REQUEST_QUERY_PARAMS = NS + "EXTRA_REQUEST_QUERY_PARAMS";
	private static final String EXTRA_RESULT_RECEIVER = NS + "EXTRA_RESULT_RECEIVER";
	private static final String EXTRA_BROADCAST_ACTION = NS + "EXTRA_BROADCAST_ACTION";

	Gson gson;
	Prefs prefs;
	OkHttpClient okHttpClient;
	LocalBroadcastManager localBroadcastManager;
	// IntentServices are always single-threaded, so it's safe to just keep
	// re-using the same output stream for capturing responses
	ByteArrayOutputStream reusableResponseStream = new ByteArrayOutputStream(8192);
	TokenTool tokenTool;

	boolean authFailed = false;
	String authToken = null;



	public SimpleSparkApiService() {
		super(SimpleSparkApiService.class.getSimpleName());
		gson = WebHelpers.getGson();
		okHttpClient = WebHelpers.getOkClient();
		tokenTool = new TokenTool(gson, okHttpClient);
		prefs = Prefs.getInstance();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		localBroadcastManager = LocalBroadcastManager.getInstance(this);
		// Don't redeliver intents, it's not critical for this app, and it's a
		// great way to cause crash loops if something is wrong with your
		// Intents.
		setIntentRedelivery(false);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (ACTION_LOG_IN.equals(intent.getAction())) {
			String username = intent.getStringExtra("username");
			String password = intent.getStringExtra("password");
			logIn(username, password);
			return;
		}

		String token = getAuthToken();
		if (!truthy(token)) {
			log.d("Making request without token...");
		}

		Bundle extras = intent.getExtras();
		String action = intent.getAction();
		Response response = null;

		if (ACTION_GET.equals(action)) {
			URL url = buildGetUrl(extras, token);
			response = get(url);

		} else if (ACTION_POST.equals(action)) {
			URL url = buildPostUrl(extras, token);
			String postData = getPostData(extras);
			response = post(url, postData);

		} else if (ACTION_PUT.equals(action)) {
			URL url = buildPutUrl(extras);
			Bundle queryParams = extras.getBundle(EXTRA_REQUEST_QUERY_PARAMS);
			queryParams.putString(AppConfig.getApiParamAccessToken(), token);
			String putString = URLEncodedUtils.format(
					bundleParamsToNameValuePairs(queryParams),
					HTTP.UTF_8);
			response = put(url, putString);

		} else {
			log.wtf("Received intent with unrecognized action: " + action);
		}

		processResponse(response, intent);
	}

	// returns the status code of the request
	private int logIn(String username, String password) {
		TokenRequest tokenRequest = new TokenRequest(username, password);
		TokenResponse response = tokenTool.requestToken(tokenRequest);
		log.d("Token response received, status code: " + response.getStatusCode());

		Intent bcast = new Intent(ApiFacade.BROADCAST_LOG_IN_FINISHED);
		if (response.getStatusCode() == -1 || response.getStatusCode() >= 300) {
			log.e("Error requesting token: " + response.errorDescription);
			bcast.putExtra(EXTRA_ERROR_MSG, response.errorDescription);
			bcast.putExtra(EXTRA_RESULT_CODE, response.getStatusCode());

		} else {
			prefs.saveUsername(username);
			prefs.saveCompletedFirstLogin(true);
			prefs.saveToken(response.accessToken);
			bcast.putExtra(EXTRA_RESULT_CODE, response.getStatusCode());
			authFailed = false;
		}
		localBroadcastManager.sendBroadcast(bcast);

		return response.getStatusCode();
	}

	void processResponse(Response response, Intent intent) {
		Bundle extras = intent.getExtras();

		Bundle resultBundle = new Bundle();
		int resultCode = REQUEST_FAILURE_CODE;

		String error = getString(R.string.error_communicating_with_server);
		if (response != null) {
			resultCode = response.responseCode;
			resultBundle.putString(EXTRA_API_RESPONSE_JSON, response.apiResponse);
			resultBundle.putInt(EXTRA_RESULT_CODE, resultCode);
		} else {
			resultBundle.putString(EXTRA_ERROR_MSG, error);
		}

		ResultReceiver receiver = extras.getParcelable(EXTRA_RESULT_RECEIVER);
		if (receiver != null) {
			receiver.send(resultCode, resultBundle);
		}

		String bcastAction = extras.getString(EXTRA_BROADCAST_ACTION);
		if (truthy(bcastAction)) {
			Intent responseIntent = new Intent()
					.replaceExtras(resultBundle)
					.setAction(bcastAction);
			localBroadcastManager.sendBroadcast(responseIntent);
		}
	}

	Response post(URL url, String stringData) {
		return performRequestWithInputData(url, "POST", stringData);
	}

	Response put(URL url, String stringData) {
		return performRequestWithInputData(url, "PUT", stringData);
	}

	Response performRequestWithInputData(URL url, String httpMethod, String stringData) {
		HttpURLConnection connection = okHttpClient.open(url);
		OutputStream out = null;
		InputStream in = null;
		int responseCode = -1;
		String responseData = "";

		try {
			try {
				connection.setRequestMethod(httpMethod);
				connection.setDoOutput(true);

				out = connection.getOutputStream();
				out.write(stringData.getBytes(HTTP.UTF_8));
				out.close();

				responseCode = connection.getResponseCode();

				in = connection.getInputStream();
				responseData = readAsUtf8String(in);

			} finally {
				// Clean up.
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			}
		} catch (IOException e) {
			log.e("Error trying to make " + connection.getRequestMethod() + " request");
		}

		return new Response(responseCode, responseData);
	}

	Response get(URL url) {
		HttpURLConnection connection = okHttpClient.open(url);
		InputStream in = null;
		int responseCode = -1;
		String responseData = "";
		// Java I/O throws *exception*al parties!
		try {
			try {
				responseCode = connection.getResponseCode();
				in = connection.getInputStream();
				responseData = readAsUtf8String(in);
			} finally {
				if (in != null) {
					in.close();
				}
			}
		} catch (IOException e) {
			log.e("Error trying to make GET request");
		}

		return new Response(responseCode, responseData);
	}

	URL buildGetUrl(Bundle intentExtras, String token) {
		Uri.Builder uriBuilder = ApiUrlHelper.buildUri(token,
				intentExtras.getStringArray(EXTRA_RESOURCE_PATH_SEGMENTS));
		Bundle queryParams = intentExtras.getBundle(EXTRA_REQUEST_QUERY_PARAMS);
		if (queryParams != null) {
			for (NameValuePair param : bundleParamsToNameValuePairs(queryParams)) {
				uriBuilder.appendQueryParameter(param.getName(), param.getValue());
			}
		}

		return ApiUrlHelper.convertToURL(uriBuilder);
	}

	URL buildPutUrl(Bundle intentExtras) {
		Uri.Builder uriBuilder = ApiUrlHelper.buildUri(null,
				intentExtras.getStringArray(EXTRA_RESOURCE_PATH_SEGMENTS));
		return ApiUrlHelper.convertToURL(uriBuilder);
	}

	URL buildPostUrl(Bundle intentExtras, String token) {
		Uri.Builder uriBuilder = ApiUrlHelper.buildUri(token,
				intentExtras.getStringArray(EXTRA_RESOURCE_PATH_SEGMENTS));
		return ApiUrlHelper.convertToURL(uriBuilder);
	}


	List<NameValuePair> bundleParamsToNameValuePairs(Bundle params) {
		List<NameValuePair> paramList = list();
		for (String key : params.keySet()) {
			Object value = params.get(key);
			if (value != null) {
				paramList.add(new BasicNameValuePair(key, value.toString()));
			}
		}
		return paramList;
	}

	String getPostData(Bundle intentExtras) {
		String postString = "";

		Bundle queryParams = intentExtras.getBundle(EXTRA_REQUEST_QUERY_PARAMS);

		if (queryParams != null) {
			String putString = URLEncodedUtils.format(
					bundleParamsToNameValuePairs(queryParams),
					HTTP.UTF_8);
			postString = putString;
		}

		return postString;
	}

	String readAsUtf8String(InputStream in) throws IOException {
		reusableResponseStream.reset();
		byte[] buffer = new byte[1024];
		for (int count; (count = in.read(buffer)) != -1;) {
			reusableResponseStream.write(buffer, 0, count);
		}
		return reusableResponseStream.toString(HTTP.UTF_8);
	}


	String getAuthToken() {
		if (!truthy(authToken)) {
			authToken = prefs.getToken();
		}
		return authToken;
	}


	private static class Response {

		public final int responseCode;
		public final String apiResponse;

		public Response(int responseCode, String apiResponse) {
			this.responseCode = responseCode;
			this.apiResponse = apiResponse;
		}

		@Override
		public String toString() {
			return "RequestResult [resultCode=" + responseCode + ", apiResponse=" + apiResponse
					+ "]";
		}

	}


	private static final TLog log = new TLog(SimpleSparkApiService.class);

}
