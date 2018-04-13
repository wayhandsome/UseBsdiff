package com.jfeng.usebsdiff;

import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
    EditText etOldApkDirectory;
    EditText etNewApk;
    TextView tvDestPatchDirectory;
    TextView tvDiffLog;

    private Handler logMsgHandler;
    private StringBuilder sb = new StringBuilder("");

    boolean working;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etOldApkDirectory = (EditText)findViewById(R.id.etoldapkdirectory);
        etNewApk = (EditText)findViewById(R.id.etnewapk);
        tvDestPatchDirectory = (TextView)findViewById(R.id.tvpatchdirectory);
        tvDiffLog = (TextView)findViewById(R.id.tvdifflog);

        etOldApkDirectory.setText(Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"DiffAPK");
        tvDestPatchDirectory.setText(etOldApkDirectory.getText().toString()+File.separator+"dest");
        etNewApk.setText(etOldApkDirectory.getText().toString());

        logMsgHandler = new Handler(){
            @Override
            public void handleMessage(Message msg)
            {
                super.handleMessage(msg);
                sb.append((String)msg.obj);
                sb.append("\n");

                tvDiffLog.setText(sb.toString());
            }
        };

        etOldApkDirectory.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count)
            {

            }

            @Override public void afterTextChanged(Editable s)
            {
                tvDestPatchDirectory.setText(etOldApkDirectory.getText().toString()+File.separator+"dest");
            }
        });
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

        if (TextUtils.isEmpty(oldapkPath)||TextUtils.isEmpty(oldapkPath.trim()))
        {
            Toast.makeText(this,"请输入历史版本APK所在目录",Toast.LENGTH_SHORT).show();
            return false;
        }

        File oldApkDirectory = new File(oldapkPath);

        if (!oldApkDirectory.exists())
        {
            Toast.makeText(this,"指定的历史版本APK所在目录不存在！",Toast.LENGTH_SHORT).show();
            return false;
        }

        String newApk = etNewApk.getText().toString();

        if (TextUtils.isEmpty(newApk)||TextUtils.isEmpty(newApk.trim()))
        {
            Toast.makeText(this,"请输入待发布APK完整路径!",Toast.LENGTH_SHORT).show();
            return false;
        }

        File newApkFile = new File(newApk);

        if (!newApkFile.exists())
        {
            Toast.makeText(this,"指定的待发布APK不存在！",Toast.LENGTH_SHORT).show();
            return false;
        }

        if (newApkFile.isDirectory())
        {
            Toast.makeText(this,"指定的待发布APK不是文件！",Toast.LENGTH_SHORT).show();
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

            if(sb.length() > 0)
            {
                sb.delete(0,sb.length());
            }

            tvDiffLog.setText("");

            String oldapkPath = etOldApkDirectory.getText().toString();
            String newApk = etNewApk.getText().toString();

            BsDiffTask task = new BsDiffTask(new File(oldapkPath),new File(newApk));
            task.execute();
        }
    }

    private class BsDiffTask extends AsyncTask<Void, Void, Boolean>
    {
        private File oldApkDirectory;
        private File newApk;

        public BsDiffTask(File oldApkDirectory, File newApk)
        {
            this.oldApkDirectory = oldApkDirectory;
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

            File destDirectory = new File(oldApkDirectory, "dest");

            if (destDirectory.exists())
            {//存放差异包的dest目录存在则清空dest目录下的文件
                clearDirectory(destDirectory);
            }
            else
            {//不存在则创建文件夹
                destDirectory.mkdir();
            }

            //同时在dest目录下创建result.txt文件
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

            //过滤出旧版本APK
            File[] subApkFiles = oldApkDirectory.listFiles(new FileFilter() {
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
                Log.d("jfeng","在目录 "+oldApkDirectory.getAbsolutePath()+" 找不到历史版本apk文件!");

                Message msg1 = new Message();
                msg1.obj = "在目录 "+oldApkDirectory.getAbsolutePath()+" 找不到历史版本apk文件!";
                logMsgHandler.sendMessage(msg1);
                return false;
            }

            for(File file:subApkFiles)
            {   //根据旧版本-新版本的命名规则生成差异包名称 vxxx-to-vyyy.patch
                String destPatchName = genPatchName(file.getName(),newApk.getName());

                String destPatch = destDirectory+File.separator+destPatchName;

                Log.d("jfeng", "begin to call bsdiff " + file.getName()+ " " + newApk.getName() + " "+destPatchName);

                Message msg1 = new Message();
                msg1.obj = "begin to call bsdiff " + file.getName()+ " " + newApk.getName() + " "+destPatchName;
                logMsgHandler.sendMessage(msg1);

                executeBsDiff(file.getAbsolutePath(),newApk.getAbsolutePath(),destPatch);

                Log.d("jfeng", "complete bsdiff, generate " + destPatch);

                Message msg2 = new Message();
                msg2.obj = "complete bsdiff, generate " + destPatch;
                logMsgHandler.sendMessage(msg2);

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

            return true;
        }

        private void executeBsDiff(String oldApk, String newApk, String patch)
        {
            //真实升级环境下这里第一个参数值换成ApkExtract.extract(this)
            BsDiff.bsdiff(oldApk,newApk,patch);
        }

        @Override protected void onPostExecute(Boolean aBoolean)
        {
            super.onPostExecute(aBoolean);

            working = false;

            if (aBoolean)
            {
                Log.d("jfeng", "complete bsdiff see result.txt");

                Message msg2 = new Message();
                msg2.obj = "complete bsdiff see detail result.txt";
                logMsgHandler.sendMessage(msg2);
            }
            else
            {
                Log.d("jfeng", "bsdiff interrupt for some errors !");

                Message msg2 = new Message();
                msg2.obj = "bsdiff interrupt for some errors !";
                logMsgHandler.sendMessage(msg2);
            }
        }
    }
}
