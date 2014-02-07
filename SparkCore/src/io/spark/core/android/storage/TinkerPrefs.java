package io.spark.core.android.storage;

import android.content.Context;
import android.content.SharedPreferences;


public class TinkerPrefs {

	private static final String BUCKET_NAME = "tinkerPrefsBucket";

	private static final String KEY_IS_VISITED = "isVisited";

	private static TinkerPrefs instance = null;

	// making this a singleton to avoid having to pass around Context everywhere
	// that this info is needed. Kind of cheating, but in practice, it will be
	// fine here.
	public static TinkerPrefs getInstance() {
		return instance;
	}

	public static void initialize(Context ctx) {
		instance = new TinkerPrefs(ctx);
	}


	private final SharedPreferences prefs;


	private TinkerPrefs(Context context) {
		prefs = context.getApplicationContext()
				.getSharedPreferences(BUCKET_NAME, Context.MODE_PRIVATE);
	}

	public boolean isFirstVisit() {
		return !prefs.getBoolean(KEY_IS_VISITED, false);
	}

	public void setVisited(boolean isVisited) {
		prefs.edit().putBoolean(KEY_IS_VISITED, isVisited).commit();
	}

}
