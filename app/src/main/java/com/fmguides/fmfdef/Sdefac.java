package com.fmguides.fmfdef;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Sdefac - A utility class for downloading ZIP files with progress updates.
 * This class provides me thods to download files using OkHttp and HttpsURLConnection,
 * with support for progress callbacks and retry logic.
 */

public class Sdefac {

    public static void fmguidesDefender(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public interface DownloadCallback {
        void onProgress(int percent);
        void onSuccess(File zipFile);
        void onError(Exception e);
    }
    
    /**
     * Downloads a ZIP file from the given URL and saves it to the specified destination file.
     * Uses OkHttp for downloading and provides progress updates via the callback.
     *
     * @param context   The application context.
     * @param url       The URL of the ZIP file to download.
     * @param destFile  The destination file where the ZIP will be saved.
     * @param callback  The callback to report progress and completion.
     */
    @SuppressLint("StaticFieldLeak")
    public void downloadZipWithRetry(Context context, String url, File destFile, int maxRetries, DownloadCallback callback) {
        new AsyncTask<Void, Integer, Exception>() {
            private File destFile;
            private DownloadCallback callback;

            @Override
            protected Exception doInBackground(Void... voids) {
                int attempt = 0;
                Exception lastException = null;
                while (attempt < maxRetries) {
                    try {
                        if (tryDownloadOkHttp(url, destFile, callback)) return null;
                    } catch (Exception e) {
                        lastException = e;
                    }
                    try {
                        if (tryDownloadHttps(url, destFile, callback)) return null;
                    } catch (Exception e) {
                        lastException = e;
                    }
                    attempt++;
                }
                return lastException;
            }
            @Override
            protected void onProgressUpdate(Integer... values) {
                if (callback != null && values.length > 0) callback.onProgress(values[0]);
            }
            @Override
            protected void onPostExecute(Exception e) {
                if (e == null) {
                    if (callback != null) callback.onSuccess(destFile);
                } else {
                    if (callback != null) callback.onError(e);
                }
            }
            private boolean tryDownloadOkHttp(String url, File destFile, DownloadCallback callback) throws IOException {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("OkHttp: Unexpected code " + response);
                assert response.body() != null;
                long total = response.body().contentLength();
                InputStream in = response.body().byteStream();
                return saveToFileWithProgress(in, destFile, total, callback);
            }
            private boolean tryDownloadHttps(String urlStr, File destFile, DownloadCallback callback) throws IOException {
                URL url = new URL(urlStr);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.connect();
                if (conn.getResponseCode() != 200) throw new IOException("HttpsURLConnection: Response code " + conn.getResponseCode());
                long total = conn.getContentLength();
                InputStream in = conn.getInputStream();
                return saveToFileWithProgress(in, destFile, total, callback);
            }
            private boolean saveToFileWithProgress(InputStream in, File destFile, long total, DownloadCallback callback) throws IOException {
                this.destFile = destFile;
                this.callback = callback;
                byte[] buffer = new byte[8192];
                int len;
                long downloaded = 0;
                try (in; FileOutputStream out = new FileOutputStream(destFile)) {
                    int lastPercent = 0;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                        downloaded += len;
                        if (total > 0) {
                            int percent = (int) (downloaded * 100 / total);
                            if (percent != lastPercent) {
                                publishProgress(percent);
                                lastPercent = percent;
                            }
                        }
                    }
                }
                return true;
            }
        }.execute();
    }

}
