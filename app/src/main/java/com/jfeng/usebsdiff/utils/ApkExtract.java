package com.jfeng.usebsdiff.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 * Created by jfeng on 2018/3/24.
 */
public class ApkExtract
{
    /**
     * 获取文件的MD5值<br>
     * 注意此方法比较耗时，调用此方法放在线程中(异步任务)
     *
     * @param file
     * @return
     */
    public static String getFileMD5(File file)
    {
        if (!file.isFile())
        {
            return null;
        }

        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try
        {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1)
            {
                digest.update(buffer, 0, len);
            }
            in.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
        return bytesToHexString(digest.digest());
    }

    public static String bytesToHexString(byte[] src)
    {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0)
        {
            return null;
        }
        for (int i = 0; i < src.length; i++)
        {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2)
            {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    /**
     * 找出当前应用的原始安装apk文件所在路径
     *
     * @param context
     * @return
     */
    public static String extract(Context context)
    {
        context = context.getApplicationContext();
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        String apkPath = applicationInfo.sourceDir;
        Log.e("hongyang", apkPath);
        return apkPath;
    }

    /**
     * 安装apk文件
     *
     * @param context
     * @param apkPath
     */
    public static void install(Context context, String apkPath)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(context, "com.jfeng.ndkdemo.fileprovider", new File(apkPath));
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        }
        else
        {
            intent.setDataAndType(Uri.fromFile(new File(apkPath)), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        context.startActivity(intent);

        //android.os.Process.killProcess(android.os.Process.myPid());
    }
}
