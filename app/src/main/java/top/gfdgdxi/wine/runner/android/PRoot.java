package top.gfdgdxi.wine.runner.android;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import android.app.Activity;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.util.Log;

public class PRoot {
    // 数据目录
    String data = "/data/data/top.gfdgdxi.wine.runner.android/";

    public boolean SystemInstalled(Activity activity) {
        File file = new File(activity.getFilesDir().getAbsolutePath() + "/files/usr/var/lib/proot-distro/installed-rootfs/usr/bin/bash");
        return file.exists();
    }

    public boolean EnvironmentInstalled(Activity activity) {
        File file = new File(activity.getFilesDir().getAbsolutePath() + "/run.sh");
        return file.exists();
    }

    public String UnpackEnvironment(Context context)
    {
        // 获取资源文件路径
        copyFilesFromRaw(context, R.raw.proot_tar, "proot.tar.gz", context.getFilesDir().getAbsolutePath());
        String result = executeCommand("tar -xvf " + context.getFilesDir().getAbsolutePath() + "/proot.tar.gz -C " + context.getFilesDir().getAbsolutePath() + "/..");
        executeCommand("rm -fv " + context.getFilesDir().getAbsolutePath() + "/proot.tar.gz");
        return result;
    }

    public String UnpackSystem(Context context)
    {
        String result = "";
        // 解压 opt 目录
        copyFilesFromRaw(context, R.raw.debian_other_tar, "debian_opt.tar.gz", context.getFilesDir().getAbsolutePath());
        result += executeCommand("tar -xvf " + context.getFilesDir().getAbsolutePath() + "/debian_opt.tar.gz -C " + context.getFilesDir().getAbsolutePath() + "/usr/var/lib/proot-distro/installed-rootfs");
        result += executeCommand("rm -fv " + context.getFilesDir().getAbsolutePath() + "/debian_opt.tar.gz");
        // 解压其它目录
        copyFilesFromRaw(context, R.raw.debian_other_tar, "debian_other.tar.gz", context.getFilesDir().getAbsolutePath());
        result += executeCommand("tar -xvf " + context.getFilesDir().getAbsolutePath() + "/debian_other.tar.gz -C " + context.getFilesDir().getAbsolutePath() + "/usr/var/lib/proot-distro/installed-rootfs");
        result += executeCommand("rm -fv " + context.getFilesDir().getAbsolutePath() + "/debian_other.tar.gz");
        return result;
    }

    public String KernelVersion()
    {
        return executeCommand("uname -a").replace("\n", "");
    }

    private String executeCommand(String command) {
        String result = "";
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(command.split(" "));
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                Log.d("RunCommand", line);
                result += line + "\n";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }



    /**
     * 复制res/raw中的文件到指定目录
     *
     * @param context     上下文
     * @param id          资源ID R.raw.文件名
     * @param fileName    文件名
     * @param storagePath 目标文件夹的路径
     */
    private static void copyFilesFromRaw(Context context, int id, String fileName, String storagePath) {
        InputStream inputStream = context.getResources().openRawResource(id);
        File file = new File(storagePath);
        if (!file.exists()) {//如果文件夹不存在，则创建新的文件夹
            file.mkdirs();
        }
        readInputStream(storagePath + File.separator + fileName, inputStream);
    }

    /**
     * 读取输入流中的数据写入输出流
     *
     * @param storagePath 目标文件路径
     * @param inputStream 输入流
     */
    private static void readInputStream(String storagePath, InputStream inputStream) {
        File file = new File(storagePath);
        try {
            if (!file.exists()) {
                // 1.建立通道对象
                FileOutputStream fos = new FileOutputStream(file);
                // 2.定义存储空间
                byte[] buffer = new byte[inputStream.available()];
                // 3.开始读文件
                int lenght = 0;
                while ((lenght = inputStream.read(buffer)) != -1) {// 循环从输入流读取buffer字节
                    // 将Buffer中的数据写到outputStream对象中
                    fos.write(buffer, 0, lenght);
                }
                fos.flush();// 刷新缓冲区
                // 4.关闭流
                fos.close();
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
