package com.jfeng.usebsdiff;

import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jfeng.bsdifflibrary.BsDiff;
import com.jfeng.usebsdiff.utils.ApkExtract;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class MainActivity extends AppCompatActivity
{
    TextView tvSdcardPath;
    EditText etOldApkDirectory;
    EditText etNewApk;

    boolean working;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvSdcardPath = (TextView)findViewById(R.id.tvsdcarddirectory);
        etOldApkDirectory = (EditText)findViewById(R.id.etoldapkdirectory);
        etNewApk = (EditText)findViewById(R.id.etnewapk);

        tvSdcardPath.setText(Environment.getExternalStorageDirectory().getAbsolutePath());
        etOldApkDirectory.setText(Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"DiffAPK");
    }

    public void generatePatch(View view)
    {
        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest
                .permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {//判断是否有sdcard读写权限,没有权限则动态申请
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android
                    .Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }
        else
        {//有sdcard读写权限则执行生成差异包
            if (checkPathValid())
            {
                doBsDiff();
            }
        }
    }

    private boolean checkPathValid()
    {
        String oldapkPath = etOldApkDirectory.getText().toString();
        String newApk = etNewApk.getText().toString();

        if (TextUtils.isEmpty(oldapkPath)||TextUtils.isEmpty(oldapkPath.trim()))
        {
            Toast.makeText(this,"请指定历史版本APK或所在目录",Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(newApk)||TextUtils.isEmpty(newApk.trim()))
        {
            Toast.makeText(this,"请指定新版本APK所在的目录",Toast.LENGTH_SHORT).show();
            return false;
        }

        File oldApkDirectory = new File(oldapkPath);

        if (!oldApkDirectory.exists())
        {
            Toast.makeText(this,"指定历史版本APK或所在目录不存在！",Toast.LENGTH_SHORT).show();
            return false;
        }

        File newApkFile = new File(newApk);

        if (!newApkFile.exists())
        {
            Toast.makeText(this,"指定新版本APK不存在！",Toast.LENGTH_SHORT).show();
            return false;
        }

        if (newApkFile.isDirectory())
        {
            Toast.makeText(this,"指定新版本APK不是文件！",Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 2)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                doBsDiff();
            }
        }
    }

    private void doBsDiff()
    {
        if (!working)
        {
            working = true;

            String oldapkPath = etOldApkDirectory.getText().toString();
            String newApk = etNewApk.getText().toString();

            BsDiffTask task = new BsDiffTask(new File(oldapkPath),new File(newApk));
            task.execute();
        }
    }

    private class BsDiffTask extends AsyncTask<Void, Void, Boolean>
    {
        private File oldApk;
        private File newApk;

        public BsDiffTask(File oldApk, File newApk)
        {
            this.oldApk = oldApk;
            this.newApk = newApk;
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();

            Log.d("jfeng", "begin to bsdiff oldApk newApk patch ... ");
        }

        private String genPatchName(String oldApkName, String newApkName)
        {
            if (TextUtils.isEmpty(oldApkName))
            {
                return "";
            }

            if (TextUtils.isEmpty(newApkName))
            {
                return "";
            }

            int fstUndLneIdx = oldApkName.indexOf("_");
            int lstUndLneIdx = oldApkName.lastIndexOf("_");
            String oldVersion = oldApkName.substring(fstUndLneIdx+1,lstUndLneIdx);
            oldVersion = oldVersion.replace(".","");

            fstUndLneIdx = newApkName.indexOf("_");
            lstUndLneIdx = newApkName.lastIndexOf("_");
            String newVersion = newApkName.substring(fstUndLneIdx+1,lstUndLneIdx);
            newVersion = newVersion.replace(".","");

            return oldVersion+"-to-"+newVersion+".patch";
        }

        private void clearDirectory(File destDirectory)
        {
            File[] subFiles = destDirectory.listFiles(new FileFilter() {
                @Override public boolean accept(File pathname)
                {
                    return pathname.isFile();
                }
            });

            if (subFiles != null && subFiles.length > 0)
            {
                for(File file:subFiles)
                {
                    file.delete();
                }
            }
        }

        @Override
        protected Boolean doInBackground(Void... params)
        {
            String md5NewApk = ApkExtract.getFileMD5(newApk);

            //过滤出旧版本APK
            File diffDirectory = new File(Environment.getExternalStorageDirectory(), "DiffAPK");

            if (!diffDirectory.exists())
            {
                diffDirectory.mkdir();
            }

            File destDirectory = new File(diffDirectory, "dest");

            if (destDirectory.exists())
            {//清空dest目录下的文件
                clearDirectory(destDirectory);
            }
            else
            {//不存在则创建文件夹
                destDirectory.mkdir();
            }

            File resultFile = new File(destDirectory,"result.txt");

            if (resultFile.exists())
            {
                resultFile.delete();
            }
            else
            {
                PrintWriter pw = null;
                try
                {
                    resultFile.createNewFile();

                    pw = new PrintWriter(new FileWriter(resultFile,true));

                    pw.append("MD5("+newApk.getAbsolutePath()+ ") = "+md5NewApk);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    if (pw != null)
                    {
                        pw.close();
                    }
                }
            }

            //根据旧版本-新版本的命名规则生成差异包名称
            //vxxx-to-vyyy.patch
            if (oldApk.isDirectory())
            {
               File[] subApkFiles = oldApk.listFiles(new FileFilter() {
                    @Override public boolean accept(File pathname)
                    {
                        boolean accepted =
                                (!pathname.getName().equals(newApk.getName()))
                                &&pathname.isFile()
                                &&(pathname.getName().lastIndexOf("apk")==(pathname.getName().length()-3));

                        return accepted;
                    }
                });

                if (subApkFiles == null || subApkFiles.length <= 0)
                {//没有旧APK
                    return false;
                }

                for(File file:subApkFiles)
                {
                    String destPatchName = genPatchName(file.getName(),newApk.getName());
                    String destPatch = destDirectory+File.separator+destPatchName;

                    executeBsDiff(file.getAbsolutePath(),newApk.getAbsolutePath(),destPatch);

                    PrintWriter pw = null;
                    try
                    {
                        pw = new PrintWriter(new FileWriter(resultFile,true));
                        pw.append(destPatch);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    finally
                    {
                        if (pw != null)
                        {
                            pw.close();
                        }
                    }
                }
            }
            else
            {//如果是文件则直接
                String destPatchName = genPatchName(oldApk.getName(),newApk.getName());
                String destPatch = destDirectory+File.separator+destPatchName;

                executeBsDiff(oldApk.getAbsolutePath(),newApk.getAbsolutePath(),destPatch);
            }

            return true;
        }

        private void executeBsDiff(String oldApk, String newApk, String patch)
        {
            Log.d("jfeng", "begin to call bsdiff " + oldApk+ " " + newApk + " "+patch);

            //真实升级环境下这里第一个参数值换成ApkExtract.extract(this)
            BsDiff.bsdiff(oldApk,newApk,patch);

            Log.d("jfeng", "complete bsdiff " + oldApk+ " " + newApk + " "+patch);
        }

        @Override protected void onPostExecute(Boolean aBoolean)
        {
            super.onPostExecute(aBoolean);

            working = false;

            if (aBoolean)
            {
                Log.d("jfeng", "complete bsdiff see result.txt");
            }
        }
    }
}
