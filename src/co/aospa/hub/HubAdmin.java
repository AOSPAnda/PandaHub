package co.aospa.hub;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class HubAdmin extends DeviceAdminReceiver {
    @Override
    public void onEnabled(Context context, Intent intent) {
        Utils.showToast(context, R.string.admin_enabled);
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        Utils.showToast(context, R.string.admin_disabled);
    }
}
