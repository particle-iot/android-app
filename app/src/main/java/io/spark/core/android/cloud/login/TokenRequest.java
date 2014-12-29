package io.spark.core.android.cloud.login;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.util.List;

import io.spark.core.android.util.Strings;

import static org.solemnsilence.util.Py.list;


/**
 * Note: cannot be used with GSON like the other models, thus the placement in a
 * separate package *
 */
public class TokenRequest {

	public final String username;
	public final String password;


	public TokenRequest(String username, String password) {
		this.username = username;
		this.password = password;
	}


	@Override
	public String toString() {
		return "LoginRequest [username=" + username + ", " +
				"password=" + Strings.getRedacted(password) + "]";
	}


	public String asFormEncodedData() {
		List<BasicNameValuePair> pairs = list(
				new BasicNameValuePair("grant_type", "password"),
				new BasicNameValuePair("username", username),
				new BasicNameValuePair("password", password));
		return URLEncodedUtils.format(pairs, HTTP.UTF_8);
	}
}
