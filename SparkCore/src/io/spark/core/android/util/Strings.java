package io.spark.core.android.util;


public class Strings {

	public static String getRedacted(String value) {
		return (value == null)
				? "<null>"
				: String.format("[  REDACTED  (length: %d) ]", value.length());
	}

}
