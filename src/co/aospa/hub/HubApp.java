package co.aospa.hub;

import android.app.Application;
import com.google.android.material.color.DynamicColors;

public class HubApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
