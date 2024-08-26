/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.aospa.hub.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UpdateEngine;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import co.aospa.hub.R;
import co.aospa.hub.UpdateManager;
import co.aospa.hub.UpdaterState;
import co.aospa.hub.util.PackageFiles;
import co.aospa.hub.util.UpdateConfigs;
import co.aospa.hub.util.UpdateEngineErrorCodes;
import co.aospa.hub.util.UpdateEngineStatuses;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * UI for SystemUpdaterSample app.
 */
public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private static final int PICK_FILE_REQUEST = 15;
    public static final String SDCARD_DATA_PATH = "/data/media/0/ota/";

    private TextView mTextViewBuild;
    private Spinner mSpinnerPaths;
    private TextView mTextViewSelectPath;
    private Button mButtonApply;
    private Button mButtonStop;
    private Button mButtonReset;
    private ProgressBar mProgressBar;
    private TextView mTextViewUpdaterState;
    private TextView mTextViewEngineStatus;
    private TextView mTextViewEngineErrorCode;
    private TextView mTextViewVerifyStatus;

    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;

    private final UpdateManager mUpdateManager =
            new UpdateManager(new UpdateEngine(), new Handler());

    private Boolean isBackFromBrowseCopyFile = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mTextViewBuild = findViewById(R.id.textViewBuild);
        this.mSpinnerPaths = findViewById(R.id.spinnerPaths);
        this.mTextViewSelectPath = findViewById(R.id.textViewSelectPath);
        this.mButtonApply = findViewById(R.id.buttonApply);
        this.mButtonStop = findViewById(R.id.buttonStop);
        this.mButtonReset = findViewById(R.id.buttonReset);
        this.mProgressBar = findViewById(R.id.progressBar);
        this.mTextViewUpdaterState = findViewById(R.id.textViewUpdaterState);
        this.mTextViewEngineStatus = findViewById(R.id.textViewEngineStatus);
        this.mTextViewEngineErrorCode = findViewById(R.id.textViewEngineErrorCode);
        this.mTextViewVerifyStatus = findViewById(R.id.textViewVerifyStatus);

        this.mTextViewSelectPath.setText("");

        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);

        this.mUpdateManager.setOnStateChangeCallback(this::onUpdaterStateChange);
        this.mUpdateManager.setOnEngineStatusUpdateCallback(this::onEngineStatusUpdate);
        this.mUpdateManager.setOnEngineCompleteCallback(this::onEnginePayloadApplicationComplete);
        this.mUpdateManager.setOnProgressUpdateCallback(this::onProgressUpdate);

        loadSdcardFilePathsToSpinner();
        updateOtaStateBySharePreference();

        if (mUpdateManager.getUpdaterState() == UpdaterState.RUNNING) {
            acquireWakeLock();
        }
        mUpdateManager.bind();
    }

    @Override
    protected void onDestroy() {
        releaseWakeLock();
        this.mUpdateManager.setOnEngineStatusUpdateCallback(null);
        this.mUpdateManager.setOnProgressUpdateCallback(null);
        this.mUpdateManager.setOnEngineCompleteCallback(null);
        this.mUpdateManager.unbind();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateOtaStateBySharePreference();
        Log.d(TAG, "onResume state=" + mUpdateManager.getUpdaterState());
        if (mUpdateManager.getUpdaterState() == UpdaterState.RUNNING) {
            uiStateRunning();
        } else {
            uiResetWidgets();
        }
        if (isBackFromBrowseCopyFile) {
            mButtonApply.setEnabled(false);
            isBackFromBrowseCopyFile = false;
        }
    }

    @Override
    protected void onPause() {
        saveOtaStateBySharePreference();
        super.onPause();
    }

    private void acquireWakeLock() {
        if (mWakeLock == null) {
            mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MainActivity:mWakeLock");
        }
        Log.d(TAG, "acquireWakeLock mWakeLock isHeld=" + mWakeLock.isHeld() + " ,state=" + mUpdateManager.getUpdaterState());
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            Log.d(TAG, "releaseWakeLock mWakeLock isHeld=" + mWakeLock.isHeld());
            mWakeLock = null;
        }
    }

    /**
     * stop button clicked
     */
    public void onStopClick(View view) {
        new AlertDialog.Builder(this)
                .setTitle("Stop Update")
                .setMessage("Do you really want to cancel running update?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    releaseWakeLock();
                    if (mUpdateManager.getUpdaterState() == UpdaterState.RUNNING) {
                        cancelRunningUpdate();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private void cancelRunningUpdate() {
        mButtonApply.setEnabled(false);
        new Thread(() -> {
            try {
                mUpdateManager.cancelRunningUpdate();
            } catch (UpdaterState.InvalidTransitionException e) {
                Log.e(TAG, "Failed to cancel running update", e);
            } finally {
                runOnUiThread(() -> mButtonApply.setEnabled(true));
            }
        }).start();
    }

    /**
     * reset button clicked
     */
    public void onResetClick(View view) {
        new AlertDialog.Builder(this)
                .setTitle("Reset Update")
                .setMessage("Do you really want to cancel running update"
                        + " and restore old version?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    releaseWakeLock();
                    resetUpdate();
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private void resetUpdate() {
        mButtonApply.setEnabled(false);
        new Thread(() -> {
            try {
                mUpdateManager.resetUpdate();
            } catch (UpdaterState.InvalidTransitionException e) {
                Log.e(TAG, "Failed to reset update", e);
            } finally {
                runOnUiThread(() -> mButtonApply.setEnabled(true));
            }
        }).start();
    }

    /**
     * Invoked when SystemUpdaterSample app state changes.
     * Value of {@code state} will be one of the
     * values from {@link UpdaterState}.
     */
    private void onUpdaterStateChange(int state) {
        Log.i(TAG, "UpdaterStateChange state="
                + UpdaterState.getStateText(state)
                + "/" + state);
        runOnUiThread(() -> {
            setUiUpdaterState(state);

            if (state == UpdaterState.IDLE) {
                uiStateIdle();
            } else if (state == UpdaterState.RUNNING) {
                uiStateRunning();
            } else if (state == UpdaterState.PAUSED) {
                uiStatePaused();
            } else if (state == UpdaterState.ERROR) {
                uiStateError();
            } else if (state == UpdaterState.SLOT_SWITCH_REQUIRED) {
                uiStateSlotSwitchRequired();
            } else if (state == UpdaterState.REBOOT_REQUIRED) {
                handleUpdateComplete();
            }
        });
    }

    private void handleUpdateComplete() {
        try {
            mUpdateManager.setUpdaterStateIdle();
        } catch (Exception e) {
            Log.e(TAG, "onUpdaterStateChange error", e);
        } finally {
            releaseWakeLock();
            if (deleteOtaFile()) {
                createDialogToReboot();
            }
        }
    }

    /**
     * Invoked when {@link UpdateEngine} status changes. Value of {@code status} will
     * be one of the values from {@link UpdateEngine.UpdateStatusConstants}.
     */
    private void onEngineStatusUpdate(int status) {
        Log.i(TAG, "StatusUpdate - status="
                + UpdateEngineStatuses.getStatusText(status)
                + "/" + status);
        runOnUiThread(() -> {
            setUiEngineStatus(status);
        });
    }

    /**
     * Invoked when the payload has been applied, whether successfully or
     * unsuccessfully. The value of {@code errorCode} will be one of the
     * values from {@link UpdateEngine.ErrorCodeConstants}.
     */
    private void onEnginePayloadApplicationComplete(int errorCode) {
        final String completionState = UpdateEngineErrorCodes.isUpdateSucceeded(errorCode)
                ? "SUCCESS"
                : "FAILURE";
        Log.i(TAG,
                "PayloadApplicationCompleted - errorCode="
                        + UpdateEngineErrorCodes.getCodeName(errorCode) + "/" + errorCode
                        + " " + completionState);
        runOnUiThread(() -> {
            setUiEngineErrorCode(errorCode);
        });
    }

    /**
     * Invoked when update progress changes.
     */
    private void onProgressUpdate(double progress) {
        mProgressBar.setProgress((int) (100 * progress));
    }

    /** resets ui */
    private void uiResetWidgets() {
        mTextViewBuild.setText(getBuildNumber());
        mButtonStop.setEnabled(false);
        mButtonReset.setEnabled(false);
        mButtonApply.setEnabled(true);
    }

    private String getBuildNumber() {
        StringBuilder buildNumber = new StringBuilder(SystemProperties.get("ro.product.vendor.device", ""))
                .append("-")
                .append(Build.DISPLAY);
        String sku = SystemProperties.get("ro.boot.hardware.sku", "");
        if (!sku.equals("ROW")) {
            buildNumber.append("-").append(sku);
        }
        return buildNumber.toString();
    }

    private void uiResetEngineText() {
        mTextViewEngineStatus.setText(R.string.unknown);
        mTextViewEngineErrorCode.setText(R.string.unknown);
        // Note: Do not reset mTextViewUpdaterState; UpdateManager notifies updater state properly.
    }

    private void uiStateIdle() {
        uiResetWidgets();
        mButtonStop.setEnabled(true);
        mButtonReset.setEnabled(true);
    }

    private void uiStateRunning() {
        uiResetWidgets();
        mProgressBar.setEnabled(true);
        mButtonStop.setEnabled(true);
        mButtonApply.setEnabled(false);
    }

    private void uiStatePaused() {
        uiResetWidgets();
        mButtonReset.setEnabled(true);
        mProgressBar.setEnabled(true);
    }

    private void uiStateSlotSwitchRequired() {
        uiResetWidgets();
        mButtonReset.setEnabled(true);
        mProgressBar.setEnabled(true);
    }

    private void uiStateError() {
        uiResetWidgets();
        mButtonReset.setEnabled(true);
        mProgressBar.setEnabled(true);
    }

    private void uiStateRebootRequired() {
        uiResetWidgets();
        mButtonReset.setEnabled(true);
    }

    /**
     * @param status update engine status code
     */
    private void setUiEngineStatus(int status) {
        String statusText = UpdateEngineStatuses.getStatusText(status);
        mTextViewEngineStatus.setText(statusText + "/" + status);
    }

    /**
     * @param errorCode update engine error code
     */
    private void setUiEngineErrorCode(int errorCode) {
        String errorText = UpdateEngineErrorCodes.getCodeName(errorCode);
        mTextViewEngineErrorCode.setText(errorText + "/" + errorCode);
        if (errorCode == 0) {
            try {
                mUpdateManager.setUpdaterStateIdle();
            } catch (Exception e) {
                Log.e(TAG, "setUiEngineErrorCode", e);
            }
            releaseWakeLock();
            if (deleteOtaFile()) {
                createDialogToReboot();
            }
        }
    }

    /**
     * @param state updater sample state
     */
    private void setUiUpdaterState(int state) {
        String stateText = UpdaterState.getStateText(state);
        mTextViewUpdaterState.setText(stateText + "/" + state);
    }

    private void loadSdcardFilePathsToSpinner() {
        String[] spinnerArray = getHubUpdatesFilePaths();
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                spinnerArray);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        mSpinnerPaths.setAdapter(spinnerArrayAdapter);
        mSpinnerPaths.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mTextViewVerifyStatus.setText("");
                mTextViewSelectPath.setText((String) mSpinnerPaths.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void createDialogToReboot() {
        new AlertDialog.Builder(this)
                .setTitle("Update successful")
                .setMessage("Do you want to reboot device?"
                        + "If you choose to cancel, the update will not be applied.")
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    resetOtaStateBySharePreference();
                    mPowerManager.reboot(null);
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private boolean deleteOtaFile() {
        File file = new File(getOtaFilePathBySharePreference());
        boolean deleted = false;
        if (file.exists() && file.isFile()) {
            deleted = file.delete();
        }
        Log.d(TAG, "deleteOtaFile delete file:" + getOtaFilePathBySharePreference() + " ,status= " + deleted);
        return deleted;
    }

    private void saveOtaStateBySharePreference() {
        SharedPreferences.Editor editor = getSharedPreferences("ota_prefs", MODE_PRIVATE).edit();
        editor.putString("update_state", mTextViewUpdaterState.getText().toString());
        editor.putString("engine_state", mTextViewEngineStatus.getText().toString());
        editor.putString("engine_error_state", mTextViewEngineErrorCode.getText().toString());
        editor.apply();
    }

    private void resetOtaStateBySharePreference() {
        SharedPreferences.Editor editor = getSharedPreferences("ota_prefs", MODE_PRIVATE).edit();
        editor.putString("update_state", getString(R.string.unknown));
        editor.putString("engine_state", getString(R.string.unknown));
        editor.putString("engine_error_state", getString(R.string.unknown));
        editor.apply();
    }

    private void updateOtaStateBySharePreference() {
        SharedPreferences prefs = getSharedPreferences("ota_prefs", MODE_PRIVATE);
        String updateState = prefs.getString("update_state", getString(R.string.unknown));
        String stateText = updateState.split("/")[0];
        mTextViewUpdaterState.setText(updateState);
        mTextViewEngineStatus.setText(prefs.getString("engine_state", getString(R.string.unknown)));
        mTextViewEngineErrorCode.setText(prefs.getString("engine_error_state", getString(R.string.unknown)));
        Log.d(TAG, "updateOtaStateBySharePreference state=" + stateText);
        try {
            if (stateText.equals("RUNNING")) {
                mUpdateManager.setUpdaterStateRunning();
            } else {
                mUpdateManager.setUpdaterStateIdle();
            }
            saveOtaStateBySharePreference();
        } catch (Exception e) {
            Log.e(TAG, "updateOtaStateText error:", e);
        }
    }

    private String getOtaFilePathBySharePreference() {
        return getSharedPreferences("ota_prefs", MODE_PRIVATE).getString("file_path", "");
    }

    private void setOtaFilePathBySharePreference(String path) {
        SharedPreferences.Editor editor = getSharedPreferences("ota_prefs", MODE_PRIVATE).edit();
        editor.putString("file_path", path);
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        if (mUpdateManager.getUpdaterState() == UpdaterState.RUNNING) {
            Toast.makeText(this, "Please don\'t close the app when the update is in progress", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }

    public void onDirectApplyOTAClick(View view) {
        new AlertDialog.Builder(this)
                .setTitle("Directly apply update from select")
                .setMessage("Do you want to apply this update package?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    uiResetWidgets();
                    uiResetEngineText();
                    uiStateRunning();
                    directlyApplyUpdate();
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    public void onSdcardFileReloadClick(View view) {
        loadSdcardFilePathsToSpinner();
        if (!isSdcardPathContainFiles()) {
            mTextViewSelectPath.setText("");
        }
    }

    private boolean isSdcardPathContainFiles() {
        File directory = new File("/data/hub_updates/");
        File[] files = directory.listFiles();

        boolean containsFiles = files != null && files.length > 0;

        Log.d(TAG, "Directory /data/hub_updates/ contains files: " + containsFiles);
        if (!containsFiles) {
            Toast.makeText(this, "No files found in /data/hub_updates/", Toast.LENGTH_SHORT).show();
        }
        return containsFiles;
    }

    public static String[] getHubUpdatesFilePaths() {
        File directory = new File("/data/hub_updates/");
        File[] listFiles = directory.listFiles();
        List<String> filePaths = new ArrayList<>();

        if (listFiles != null) {
            for (File file : listFiles) {
                String filePath = file.getAbsolutePath();
                Log.d(TAG, "File Path: " + filePath);
                if (!filePath.isEmpty()) {
                    filePaths.add(filePath);
                }
            }
        } else {
            Log.d(TAG, "No files found in /data/hub_updates/ or directory doesn't exist");
        }

        return filePaths.toArray(new String[0]);
    }

    private void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        new CopyFileTask(sourceFile, destFile).execute();
    }

    public void onBrowseClick(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setDataAndType(Uri.parse(Environment.getExternalStorageDirectory().getPath() + "/Documents"), "*/*");
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    private void directlyApplyUpdate() {
        UpdateEngine updateEngine = new UpdateEngine();
        CharSequence selectedPath = mTextViewSelectPath.getText();
        ArrayList<String> headerKeyValuePairs = new ArrayList<>();
        HashMap<String, String> metadata = new HashMap<>();

        Log.i(TAG, "directlyApplyUpdate select path= " + selectedPath);
        String[] pathParts = mTextViewSelectPath.getText().toString().split("/");
        String filename = pathParts[pathParts.length - 1];

        try (ZipFile zipFile = new ZipFile(Paths.get(selectedPath.toString()).toFile())) {
            long payloadOffset = 0;
            long payloadSize = 0;
            long totalZipSize = 0;

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                long entrySize = entry.getExtra() != null ? entry.getExtra().length : 0;
                totalZipSize += entryName.length() + 30 + entrySize;

                if (!entry.isDirectory()) {
                    long compressedSize = entry.getCompressedSize();
                    if (PackageFiles.PAYLOAD_BINARY_FILE_NAME.equals(entryName)) {
                        if (entry.getMethod() != ZipEntry.STORED) {
                            throw new IOException("Invalid compression method.");
                        }
                        payloadSize = compressedSize;
                    } else if (PackageFiles.PAYLOAD_PROPERTIES_FILE_NAME.equals(entryName)) {
                        try (InputStream is = zipFile.getInputStream(entry);
                             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                headerKeyValuePairs.add(line);
                            }
                        }
                    } else if (PackageFiles.METADATA_FILE_PATH.equals(entryName)) {
                        try (InputStream is = zipFile.getInputStream(entry);
                             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                String[] parts = line.split("=");
                                if (parts.length > 1) {
                                    metadata.put(parts[0], parts[1]);
                                }
                            }
                        }
                    }
                    totalZipSize += compressedSize;
                }
            }

            payloadOffset = totalZipSize - payloadSize;

            int verificationResult = verifyMetaDataFile(metadata);
            Log.i(TAG, "meta verify state= " + verificationResult);

            if (isSdcardPathContainFiles() && !filename.isEmpty() && verificationResult == 0) {
                setOtaFilePathBySharePreference(selectedPath.toString());
                mUpdateManager.setUpdaterStateRunning();
                acquireWakeLock();
                updateEngine.applyPayload("file:///data/media/0/ota/" + filename, payloadOffset, payloadSize,
                        headerKeyValuePairs.toArray(new String[0]));
                mButtonApply.setEnabled(false);
            } else {
                uiStateError();
                Log.e(TAG, "Failed to verify metadata config error code=" + verificationResult);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply update", e);
            uiStateError();
        }
    }

    private int verifyMetaDataFile(HashMap<String, String> metadata) {
        String deviceProp = SystemProperties.get("ro.product.device", "");
        Log.i(TAG, "verifyMetaDataFile productProp=" + deviceProp);

        if (!metadata.getOrDefault("pre-device", " ").equals(deviceProp)) {
            mTextViewVerifyStatus.setText("pre-device is different from ro.product.device");
            return -1;
        }

        mTextViewVerifyStatus.setText("Verify metadata file success");
        return 0;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            String[] pathSegments = DocumentsContract.getDocumentId(data.getData()).split(":");
            String path = "primary".equalsIgnoreCase(pathSegments[0]) ?
                    Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + pathSegments[1] : "";

            if (!path.startsWith(Environment.getExternalStorageDirectory().getPath() + "/ota") && !path.isEmpty()) {
                String destPath = Environment.getExternalStorageDirectory().getPath() + "/ota/" + new File(path).getName();
                try {
                    copyFile(new File(path), new File(destPath));
                    mTextViewSelectPath.setText(destPath);
                } catch (IOException e) {
                    Log.e(TAG, "copy file fail", e);
                }
            } else {
                mTextViewSelectPath.setText(path);
            }
            mTextViewVerifyStatus.setText("");
        }
    }

    private class CopyFileTask extends AsyncTask<Void, Void, Void> {
        private static final int BUFFER_SIZE = 8192;
        private File mSourceFile;
        private File mDestFile;

        public CopyFileTask(File sourceFile, File destFile) {
            this.mSourceFile = sourceFile;
            this.mDestFile = destFile;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isBackFromBrowseCopyFile = true;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try (FileInputStream fis = new FileInputStream(mSourceFile);
                 FileOutputStream fos = new FileOutputStream(mDestFile);
                 FileChannel sourceChannel = fis.getChannel();
                 FileChannel destChannel = fos.getChannel()) {

                long size = sourceChannel.size();
                long transferred = 0;
                while (transferred < size) {
                    transferred += destChannel.transferFrom(sourceChannel, transferred, size - transferred);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error copying file", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mButtonApply.setEnabled(true);
            Toast.makeText(getApplicationContext(), "File copied to /sdcard/ota successfully", Toast.LENGTH_SHORT).show();
            isBackFromBrowseCopyFile = false;
        }
    }
}
