package io.spark.core.android.cloud.login;

import io.spark.core.android.util.Strings;


public class TokenResponse {

	// only available when request is successful (HTTP 200)
	public final String accessToken;
	public final String tokenType;
	public final int expiresIn;

	// all(?) other responses
	public final String errorDescription;

	private int statusCode;


	public TokenResponse(String accessToken, String tokenType, int expiresIn,
			String errorDescription) {
		this.accessToken = accessToken;
		this.tokenType = tokenType;
		this.expiresIn = expiresIn;
		this.errorDescription = errorDescription;
	}

	public TokenResponse() {
		this(null, null, -1, null);
	}

	@Override
	public String toString() {
		return "LoginResponse [accessToken=" + Strings.getRedacted(accessToken) + ", tokenType="
				+ tokenType + ", expiresIn=" + expiresIn + ", errorDescription=" + errorDescription
				+ "]";
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

}
