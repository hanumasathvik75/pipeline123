package ai.ignition.android;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

/**
 * Custom Application class enabling Google Material You dynamic color theming
 * on supported Android 12+ (API 31+) hardware.
 */
public class IgnitionApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Enables Android wallpaper-derived dynamic coloring for activities
        if (DynamicColors.isDynamicColorAvailable()) {
            DynamicColors.applyToActivitiesIfAvailable(this);
        }
    }
}
