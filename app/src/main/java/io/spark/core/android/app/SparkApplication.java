package io.spark.core.android.app;

import android.app.Application;

import io.spark.core.android.cloud.WebHelpers;
import io.spark.core.android.storage.Prefs;
import io.spark.core.android.storage.TinkerPrefs;

public class SparkApplication extends Application {

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
