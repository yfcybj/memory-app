package com.memory.app;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.util.Locale;

/**
 * 应用内更新插件：
 * 1. getAppInfo()          读取当前 versionName / versionCode
 * 2. downloadAndInstall()  用系统 DownloadManager 下载 APK，完成后自动拉起安装界面
 *
 * 需要 AndroidManifest 声明 REQUEST_INSTALL_PACKAGES，
 * 并且 file_paths.xml 里有 external-files-path（APK 保存在应用专属下载目录）。
 */
@CapacitorPlugin(name = "AppUpdater")
public class AppUpdaterPlugin extends Plugin {

    private BroadcastReceiver downloadReceiver;
    private long downloadId = -1L;
    private String apkFileName = null;

    @PluginMethod
    public void getAppInfo(PluginCall call) {
        try {
            Context ctx = getContext();
            PackageInfo info = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            long code = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    ? info.getLongVersionCode()
                    : info.versionCode;
            JSObject ret = new JSObject();
            ret.put("versionName", info.versionName == null ? "" : info.versionName);
            ret.put("versionCode", code);
            ret.put("packageName", ctx.getPackageName());
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("读取版本信息失败：" + e.getMessage());
        }
    }

    @PluginMethod
    public void downloadAndInstall(PluginCall call) {
        String url = call.getString("url");
        if (url == null || url.isEmpty()) {
            call.reject("缺少下载地址");
            return;
        }

        String fileName = call.getString("fileName", "update.apk");
        if (fileName == null || fileName.isEmpty()) {
            fileName = "update.apk";
        }
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".apk")) {
            fileName = fileName + ".apk";
        }

        Context ctx = getContext();

        try {
            File dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (dir == null) {
                dir = ctx.getCacheDir();
            }
            if (!dir.exists() && !dir.mkdirs()) {
                call.reject("无法创建下载目录");
                return;
            }

            File apk = new File(dir, fileName);
            if (apk.exists() && !apk.delete()) {
                call.reject("无法覆盖旧的安装包");
                return;
            }
            apkFileName = fileName;

            DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
            req.setTitle("艾记助手更新");
            req.setDescription("正在下载新版本…");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalFilesDir(ctx, Environment.DIRECTORY_DOWNLOADS, fileName);
            req.setMimeType("application/vnd.android.package-archive");
            req.addRequestHeader("User-Agent", "MemoryApp-Updater");

            downloadId = dm.enqueue(req);
            ensureReceiver();

            JSObject ret = new JSObject();
            ret.put("started", true);
            ret.put("downloadId", downloadId);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("启动下载失败：" + e.getMessage());
        }
    }

    private void ensureReceiver() {
        if (downloadReceiver != null) {
            return;
        }
        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
                if (id != downloadId) {
                    return;
                }
                handleDownloadFinished();
            }
        };
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        try {
            ContextCompat.registerReceiver(getContext(), downloadReceiver, filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED);
        } catch (Exception e) {
            try {
                getContext().registerReceiver(downloadReceiver, filter);
            } catch (Exception ignored) {
            }
        }
    }

    private void handleDownloadFinished() {
        Context ctx = getContext();
        try {
            DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
            Cursor cursor = dm.query(query);
            int status = -1;
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                }
                cursor.close();
            }

            if (status != DownloadManager.STATUS_SUCCESSFUL) {
                JSObject err = new JSObject();
                err.put("message", "下载失败（状态码 " + status + "）");
                notifyListeners("updateError", err);
                return;
            }
            installApk();
        } catch (Exception e) {
            JSObject err = new JSObject();
            err.put("message", "处理下载结果失败：" + e.getMessage());
            notifyListeners("updateError", err);
        }
    }

    private void installApk() {
        Context ctx = getContext();
        File dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) {
            dir = ctx.getCacheDir();
        }
        File apk = new File(dir, apkFileName == null ? "update.apk" : apkFileName);

        if (!apk.exists()) {
            JSObject err = new JSObject();
            err.put("message", "安装包不存在");
            notifyListeners("updateError", err);
            return;
        }

        // Android 8.0+ 需要用户授予「安装未知应用」权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !ctx.getPackageManager().canRequestPackageInstalls()) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + ctx.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
            } catch (Exception ignored) {
            }
            notifyListeners("needInstallPermission", new JSObject());
            return;
        }

        try {
            Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", apk);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
            notifyListeners("updateReady", new JSObject());
        } catch (Exception e) {
            JSObject err = new JSObject();
            err.put("message", "无法启动安装：" + e.getMessage());
            notifyListeners("updateError", err);
        }
    }

    @Override
    protected void handleOnDestroy() {
        if (downloadReceiver != null) {
            try {
                getContext().unregisterReceiver(downloadReceiver);
            } catch (Exception ignored) {
            }
            downloadReceiver = null;
        }
        super.handleOnDestroy();
    }
}
