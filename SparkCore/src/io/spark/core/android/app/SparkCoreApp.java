package io.spark.core.android.app;

import io.spark.core.android.cloud.WebHelpers;
import io.spark.core.android.storage.Prefs;
import io.spark.core.android.storage.TinkerPrefs;
import android.app.Application;


public class SparkCoreApp extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		AppConfig.initialize(this);
		Prefs.initialize(this);
		TinkerPrefs.initialize(this);
		WebHelpers.initialize(this);
		DeviceState.initialize(this);
	}

}
