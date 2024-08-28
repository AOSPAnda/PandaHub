package co.aospa.hub;

import android.content.Context;
import android.widget.Toast;

public class Utils {
    public static void showToast(Context context, int messageResId) {
        Toast.makeText(context, context.getString(messageResId), Toast.LENGTH_SHORT).show();
    }
}
