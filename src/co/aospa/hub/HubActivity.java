package co.aospa.hub;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class HubActivity extends AppCompatActivity {

    private static final String TAG = "ParanoidHub";
    private static final int REQUEST_CODE_PICK_UPDATE_FILE = 1;

    private DevicePolicyManager devicePolicyManager;
    private ComponentName hubAdmin;
    private TextView statusTextView;
    private Button selectFileButton;
    private Button installUpdateButton;
    private Uri selectedFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub);

        initializeComponents();
        setupButtonListeners();
        updateAdminStatus();
    }

    private void initializeComponents() {
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        hubAdmin = new ComponentName(this, HubAdmin.class);

        statusTextView = findViewById(R.id.statusTextView);
        selectFileButton = findViewById(R.id.selectFileButton);
        installUpdateButton = findViewById(R.id.installUpdateButton);
    }

    private void setupButtonListeners() {
        selectFileButton.setOnClickListener(v -> selectUpdateFile());
        installUpdateButton.setOnClickListener(v -> installUpdate());
    }

    private void updateAdminStatus() {
        boolean isAdminActive = devicePolicyManager.isAdminActive(hubAdmin);
        statusTextView.setText(getString(isAdminActive ? R.string.admin_status_active : R.string.admin_status_inactive));
        installUpdateButton.setEnabled(isAdminActive);
    }

    private void selectUpdateFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_UPDATE_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_UPDATE_FILE && resultCode == RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            updateFileSelectionStatus();
        }
    }

    private void updateFileSelectionStatus() {
        String fileName = selectedFileUri != null ? selectedFileUri.getLastPathSegment() : getString(R.string.no_file_selected);
        statusTextView.setText(getString(R.string.file_selected, fileName));
    }

    private void installUpdate() {
        if (selectedFileUri == null) {
            showToast(R.string.select_file_first);
            return;
        }

        try {
            devicePolicyManager.installSystemUpdate(
                hubAdmin,
                selectedFileUri,
                ContextCompat.getMainExecutor(this),
                new DevicePolicyManager.InstallSystemUpdateCallback() {
                    @Override
                    public void onInstallUpdateError(int errorCode, String errorMessage) {
                        Log.e(TAG, "Update installation failed with error code: " + errorCode + ", message: " + errorMessage);
                        runOnUiThread(() -> showToast(getString(R.string.update_failed, errorMessage)));
                    }
                }
            );
        } catch (SecurityException | IllegalArgumentException e) {
            Log.e(TAG, "Error installing update: " + e.getMessage());
            showToast(getString(R.string.installation_error, e.getMessage()));
        }
    }

    private void showToast(int messageResId) {
        showToast(getString(messageResId));
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }
}
