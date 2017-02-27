package com.vincent.filepickersample;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by Vincent Woo
 * Date: 2015/10/29
 * Time: 15:31
 */

public class LocalFileUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private Context mContext;
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    public LocalFileUncaughtExceptionHandler(Context context, Thread.UncaughtExceptionHandler defaultHandler){
        this.mDefaultHandler = defaultHandler;
        this.mContext = context;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        Log.e("Crash", "Application crash", ex);
        writeFile(thread, ex);

        mDefaultHandler.uncaughtException(thread, ex);
    }

    private void writeFile(final Thread thread, final Throwable ex){
        try {
            OutputStream os = getLogStream();
            os.write(getExceptionInformation(thread, ex).getBytes("utf-8"));
            os.flush();
            os.close();

            android.os.Process.killProcess(android.os.Process.myPid());
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private OutputStream getLogStream() throws IOException {
        //crash_log_pkgname.log
//        String fileName = String.format("crash_%s.log", mContext.getPackageName());
        String fileName = "crash_multi_type_file_picker.log";
        File file  = new File(Environment.getExternalStorageDirectory(), fileName);

        if(!file.exists()){
            file.createNewFile();
        }

        return new FileOutputStream(file, true);
    }

    private String getExceptionInformation(Thread thread, Throwable ex){
        long current = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder().append('\n');
        sb.append("THREAD: ").append(thread).append('\n');
        sb.append("BOARD: ").append(Build.BOARD).append('\n');
        sb.append("BOOTLOADER: ").append(Build.BOOTLOADER).append('\n');
        sb.append("BRAND: ").append(Build.BRAND).append('\n');
        sb.append("CPU_ABI: ").append(Build.CPU_ABI).append('\n');
        sb.append("CPU_ABI2: ").append(Build.CPU_ABI2).append('\n');
        sb.append("DEVICE: ").append(Build.DEVICE).append('\n');
        sb.append("DISPLAY: ").append(Build.DISPLAY).append('\n');
        sb.append("FINGERPRINT: ").append(Build.FINGERPRINT).append('\n');
        sb.append("HARDWARE: ").append(Build.HARDWARE).append('\n');
        sb.append("HOST: ").append(Build.HOST).append('\n');
        sb.append("ID: ").append(Build.ID).append('\n');
        sb.append("MANUFACTURER: ").append(Build.MANUFACTURER).append('\n');
        sb.append("MODEL: ").append(Build.MODEL).append('\n');
        sb.append("PRODUCT: ").append(Build.PRODUCT).append('\n');
        sb.append("SERIAL: ").append(Build.SERIAL).append('\n');
        sb.append("TAGS: ").append(Build.TAGS).append('\n');
        sb.append("TIME: ").append(Build.TIME).append(' ').append(toDateString(Build.TIME)).append('\n');
        sb.append("TYPE: ").append(Build.TYPE).append('\n');
        sb.append("USER: ").append(Build.USER).append('\n');
        sb.append("VERSION.CODENAME: ").append(Build.VERSION.CODENAME).append('\n');
        sb.append("VERSION.INCREMENTAL: ").append(Build.VERSION.INCREMENTAL).append('\n');
        sb.append("VERSION.RELEASE: ").append(Build.VERSION.RELEASE).append('\n');
        sb.append("VERSION.SDK_INT: ").append(Build.VERSION.SDK_INT).append('\n');
        sb.append("LANG: ").append(mContext.getResources().getConfiguration().locale.getLanguage()).append('\n');
        sb.append("APP.VERSION.NAME: ").append(getVersionName()).append('\n');
        sb.append("APP.VERSION.CODE: ").append(getVersionCode()).append('\n');
        sb.append("CURRENT: ").append(current).append(' ').append(toDateString(current)).append('\n');

        sb.append(getErrorInformation(ex));

        return sb.toString();
    }

    private String getVersionName(){
        PackageManager packageManager = mContext.getPackageManager();
        PackageInfo packInfo = null;
        String version = null;
        try {
            packInfo = packageManager.getPackageInfo(mContext.getPackageName(),0);
            version = packInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return version;
    }

    private int getVersionCode(){
        PackageManager packageManager = mContext.getPackageManager();
        PackageInfo packInfo = null;
        int version = 0;
        try {
            packInfo = packageManager.getPackageInfo(mContext.getPackageName(),0);
            version = packInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return version;
    }

    private String getErrorInformation(Throwable t){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);
        t.printStackTrace(writer);
        writer.flush();
        String result = new String(baos.toByteArray());
        writer.close();

        return result;
    }

    private String toDateString(long timeMilli){
        Calendar calc = Calendar.getInstance();
        calc.setTimeInMillis(timeMilli);
        return String.format(Locale.CHINESE, "%04d.%02d.%02d %02d:%02d:%02d:%03d",
                calc.get(Calendar.YEAR), calc.get(Calendar.MONTH) + 1, calc.get(Calendar.DAY_OF_MONTH),
                calc.get(Calendar.HOUR_OF_DAY), calc.get(Calendar.MINUTE), calc.get(Calendar.SECOND), calc.get(Calendar.MILLISECOND));
    }

}
