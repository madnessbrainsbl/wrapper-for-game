package com.fmguides.fmfdef;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.io.File;

public class DownloadService extends Service {
    private static final String CHANNEL_ID = "download_channel";
    private static final int NOTIF_ID = 101;
    private boolean isDownloading = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String url = intent.getStringExtra("url");
        String dest = intent.getStringExtra("dest");
        if (!isDownloading && url != null && dest != null) {
            isDownloading = true;
            startForeground(NOTIF_ID, buildNotification("Скачивание кэша...", 0));
            downloadAndUnzip(url, dest);
        }
        return START_NOT_STICKY;
    }

    /**
     * @param intent The Intent that was used to bind to this service,
     *               as given to {@link android.content.Context#bindService
     *               Context.bindService}.  Note that any extras that were included with
     *               the Intent at that point will <em>not</em> be seen here.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void downloadAndUnzip(String url, String dest) {
        File destFile = new File(dest);
        File targetDir = getFilesDir();
        new Thread(() -> {
            try {
                // Скачивание файла с прогрессом
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();
                try (okhttp3.Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        sendProgress(0, false, true, "Ошибка при скачивании");
                        stopSelf();
                        return;
                    }
                    java.io.InputStream is = response.body().byteStream();
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(destFile);
                    byte[] buffer = new byte[4096];
                    long total = response.body().contentLength();
                    long downloaded = 0;
                    int read;
                    int lastPercent = 0;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                        downloaded += read;
                        int percent = total > 0 ? (int) (downloaded * 100 / total) : 0;
                        if (percent != lastPercent) {
                            lastPercent = percent;
                            updateProgress(percent, "Скачивание: " + percent + "%");
                            sendProgress(percent, false, false, "Скачивание: " + percent + "%");
                        }
                    }
                    fos.close();
                    is.close();
                }
                // Распаковка zip-файла
                updateProgress(100, "Распаковка...");
                sendProgress(100, false, false, "Распаковка...");
                try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.FileInputStream(destFile))) {
                    java.util.zip.ZipEntry entry;
                    byte[] unzipBuffer = new byte[4096];
                    while ((entry = zis.getNextEntry()) != null) {
                        File outFile = new File(targetDir, entry.getName());
                        if (entry.isDirectory()) {
                            outFile.mkdirs();
                        } else {
                            outFile.getParentFile().mkdirs();
                            try (java.io.FileOutputStream out = new java.io.FileOutputStream(outFile)) {
                                int len;
                                while ((len = zis.read(unzipBuffer)) > 0) {
                                    out.write(unzipBuffer, 0, len);
                                }
                            }
                        }
                        zis.closeEntry();
                    }
                }
                updateProgress(100, "Готово");
                sendProgress(100, true, false, "Готово");
                stopSelf();
            } catch (Exception e) {
                sendProgress(0, false, true, "Ошибка при скачивании или распаковке");
                stopSelf();
            }
        }).start();
    }

    private void sendProgress(int percent, boolean done, boolean error, String message) {
        Intent intent = new Intent("DOWNLOAD_PROGRESS");
        intent.putExtra("progress", percent);
        intent.putExtra("done", done);
        intent.putExtra("error", error);
        intent.putExtra("message", message);
        sendBroadcast(intent);
    }

    @SuppressLint("NotificationPermission")
    private void updateProgress(int percent, String msg) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIF_ID, buildNotification(msg, percent));
    }

    private Notification buildNotification(String msg, int percent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Загрузка кэша", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Загрузка кэша")
                .setContentText(msg)
                .setSmallIcon(0x1e)
                .setProgress(100, percent, false)
                .setOngoing(true);
        return builder.build();
    }
}
