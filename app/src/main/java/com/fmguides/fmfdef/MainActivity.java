package com.fmguides.fmfdef;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import android.text.ClipboardManager;
import android.view.Gravity;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.*;

import java.io.File;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;

public class MainActivity extends Activity {
    private String androidId;
    private String deviceModel;
    private ProgressBar progressBar;
    private TextView progressText;
    private AlertDialog progressDialog;
    private static final String CHECK_URL = "https://fmm24-cd160-default-rtdb.firebaseio.com/AndroidID.json"; 
    private static final String CACHE_URL = "https://yourdomain.com/gamecache.zip"; //заменить
    private static final int MAX_RETRIES = 3;
    private File cacheZipFile;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setBackgroundColor(0xFF18191A);
        FirebaseApp.initializeApp(this);
        androidId = getAndroidId();
        deviceModel = Build.MODEL;
        File cacheDir = getFilesDir();
        cacheZipFile = new File(getFilesDir(), "gamecache.zip");
        checkInternetAndId();
    }

    @SuppressLint("HardwareIds")
    private String getAndroidId() {
        try {
            String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            if (id == null || id.equals("null")) id = Build.SERIAL;
            if (id == null) id = "unknown";
            return id;
        } catch (Exception e) {
            return "unknown";
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    private void checkInternetAndId() {
        if (!isInternetAvailable()) {
            showNoInternetDialog();
        } else {
            checkAndroidId();
        }
    }

    private void checkAndroidId() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            showProgressDialog("Device check...");
        }
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("allowed_ids");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean found = false;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String id = String.valueOf(child.getValue());
                    if (androidId != null && androidId.equalsIgnoreCase(id)) {
                        found = true;
                        break;
                    }
                }
                hideProgressDialog();
                if (found) {
                    showDownloadDialog();
                } else {
                    showNotFoundDialog();
                }
            }
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideProgressDialog();
                showNoInternetDialog();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    private void showNoInternetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Нет интернета")
                .setMessage("Для проверки устройства необходимо включить интернет.")
                .setCancelable(false)
                .setPositiveButton("Обновить", (d, w) -> checkInternetAndId())
                .setNegativeButton("Выйти", (d, w) -> finishAffinity())
                .show();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    private void showNotFoundDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Устройство не прошло проверку")
                .setMessage("Модель: " + deviceModel + "\nID: " + androidId)
                .setCancelable(false)
                .setPositiveButton("Скопировать ID", (d, w) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setText(androidId);
                    Toast.makeText(this, "ID скопирован", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Связаться", (d, w) -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://vk.com"));
                    startActivity(browserIntent);
                })
                .setNegativeButton("Обновить", (d, w) -> checkInternetAndId())
                .setOnCancelListener(dialog -> finishAffinity())
                .setOnDismissListener(dialog -> {})
                .show();
    }

    private void showDownloadDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 60, 60, 60);
        layout.setBackgroundColor(0xFF23272A);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setIndeterminate(false);
        progressBar.setMinimumHeight(30);
        progressText = new TextView(this);
        progressText.setTextColor(0xFFFFFFFF);
        progressText.setText("Подготовка к скачиванию...");
        progressText.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.addView(progressBar);
        layout.addView(progressText);
        progressDialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Скачивание кэша")
                .setView(layout)
                .setCancelable(false)
                .create();
        progressDialog.show();
        startCacheDownload();
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void startCacheDownload() {
        Intent intent = new Intent(this, DownloadService.class);
        intent.putExtra("url", CACHE_URL);
        intent.putExtra("dest", cacheZipFile.getAbsolutePath());
        ContextCompat.startForegroundService(this, intent);
        registerReceiver(downloadReceiver, new IntentFilter("DOWNLOAD_PROGRESS"));
    }

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int progress = intent.getIntExtra("progress", 0);
            boolean done = intent.getBooleanExtra("done", false);
            boolean error = intent.getBooleanExtra("error", false);
            String message = intent.getStringExtra("message");
            if (progressBar != null) progressBar.setProgress(progress);
            if (progressText != null && message != null) progressText.setText(message);
            if (done) {
                unregisterReceiver(this);
                if (progressDialog != null) progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "Welcome", Toast.LENGTH_LONG).show();
                // TODO: запуск игры
            }
            if (error) {
                unregisterReceiver(this);
                if (progressDialog != null) progressDialog.dismiss();
                showDownloadDialog(); // повторная попытка
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    private void showProgressDialog(String msg) {
        progressDialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(msg)
                .setView(new ProgressBar(this))
                .setCancelable(false)
                .create();
        progressDialog.show();
    }
    private void hideProgressDialog() {
        if (progressDialog != null) progressDialog.dismiss();
    }
    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && 
                   (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || 
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }
    @SuppressLint("HardwareIds")
    private String K() {
        try {
            @SuppressLint("HardwareIds") String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            if (id == null || id.equals("null")) id = Build.SERIAL;
            if (id == null) id = "unknown";
            return id;
        } catch (Exception e) {
            return "unknown";
        }
    }
}

