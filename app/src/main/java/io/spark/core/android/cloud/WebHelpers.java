package io.spark.core.android.cloud;

import io.spark.core.android.app.AppConfig;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import org.solemnsilence.util.TLog;

import android.content.Context;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;


public class WebHelpers {

	private static final TLog log = new TLog(WebHelpers.class);


	private static OkHttpClient okHttpClient;
	private static Gson gson;
	private static boolean initialized = false;


	// should be called during Application.onCreate() to ensure availability
	public static void initialize(Context ctx) {
		if (!initialized) {
			if (AppConfig.useStaging()) {
				okHttpClient = disableTLSforStaging();
			} else {
				okHttpClient = new OkHttpClient();
			}
			gson = new GsonBuilder()
					.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
					.create();
			initialized = true;
		}
	}

	public static Gson getGson() {
		return gson;
	}

	public static OkHttpClient getOkClient() {
		return okHttpClient;
	}


	private static OkHttpClient disableTLSforStaging() {

		log.e("WARNING: TLS DISABLED FOR STAGING!");

		OkHttpClient client = new OkHttpClient();
		client.setHostnameVerifier(new HostnameVerifier() {

			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		});

		try {
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, new X509TrustManager[] { new X509TrustManager() {

				public void checkClientTrusted(X509Certificate[] chain, String authType)
						throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] chain, String authType)
						throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}
			} }, new SecureRandom());

			client.setSslSocketFactory(context.getSocketFactory());

		} catch (Exception e) {
			e.printStackTrace();
		}

		return client;
	}


}
