package top.gfdgdxi.wine.runner.android;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.FileOutputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.Arrays;

import android.app.Activity;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Environment;
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

    public void CleanTempFile(Context context)
    {
        executeCommand("rm -fv " + context.getFilesDir().getAbsolutePath() + "/*.tar.gz");
        executeCommand("rm -fv " + context.getFilesDir().getAbsolutePath() + "/*.tar.xz");
    }

    public String UnpackEnvironment(Context context)
    {
        // 获取资源文件路径
        copyFilesFromRaw(context, R.raw.proot_tar, "proot.tar.gz", context.getFilesDir().getAbsolutePath());
        String result = executeCommand("tar --no-same-owner --same-permissions -xvf " + context.getFilesDir().getAbsolutePath() + "/proot.tar.gz -C " + context.getFilesDir().getAbsolutePath() + "/..");
        executeCommand("rm -fv " + context.getFilesDir().getAbsolutePath() + "/proot.tar.gz");
        executeCommand("chmod 777 -R " + context.getFilesDir().getAbsolutePath() + "/usr/bin/");
        return result;
    }

    public String UnpackSystem(Context context)
    {
        String result = "";
        // 解压 opt 目录
        copyFilesFromRaw(context, R.raw.debian_opt_tar, "debian_opt.tar.xz", context.getFilesDir().getAbsolutePath());
        result += executeCommand("sh " + context.getFilesDir().getAbsolutePath() + "/../runprogram.sh tar -xvf " + context.getFilesDir().getAbsolutePath() + "/debian_opt.tar.xz -C " + context.getFilesDir().getAbsolutePath() + "/usr/var/lib/proot-distro/installed-rootfs");
        result += executeCommand("rm -fv " + context.getFilesDir().getAbsolutePath() + "/debian_opt.tar.xz");
        // 解压 usr/bin 目录
        copyFilesFromRaw(context, R.raw.debian_usr_bin_tar, "debian_usr_bin.tar.xz", context.getFilesDir().getAbsolutePath());
        result += executeCommand("sh " + context.getFilesDir().getAbsolutePath() + "/../runprogram.sh " + context.getFilesDir().getAbsolutePath() + "/usr/bin/tar --no-same-owner -xvf " + context.getFilesDir().getAbsolutePath() + "/debian_usr_bin.tar.xz -C " + context.getFilesDir().getAbsolutePath() + "/usr/var/lib/proot-distro/installed-rootfs");
        result += executeCommand("ls -l " + context.getFilesDir().getAbsolutePath() + "/usr/bin/tar");
        result += executeCommand("rm -fv " + context.getFilesDir().getAbsolutePath() + "/debian_usr_bin.tar.xz");
        // 解压 usr/lib 目录
        copyFilesFromRaw(context, R.raw.debian_usr_lib_tar, "debian_usr_lib.tar.xz", context.getFilesDir().getAbsolutePath());
        result += executeCommand("sh " + context.getFilesDir().getAbsolutePath() + "/../runprogram.sh " + context.getFilesDir().getAbsolutePath() + "/usr/bin/tar --no-same-owner -xvf " + context.getFilesDir().getAbsolutePath() + "/debian_usr_lib.tar.xz -C " + context.getFilesDir().getAbsolutePath() + "/usr/var/lib/proot-distro/installed-rootfs");
        result += executeCommand("rm -fv " + context.getFilesDir().getAbsolutePath() + "/debian_usr_lib.tar.xz");
        // 解压 usr/ 下的其它目录
        copyFilesFromRaw(context, R.raw.debian_usr_other_tar, "debian_usr_other.tar.xz", context.getFilesDir().getAbsolutePath());
        result += executeCommand("sh " + context.getFilesDir().getAbsolutePath() + "/../runprogram.sh " + context.getFilesDir().getAbsolutePath() + "/usr/bin/tar --no-same-owner -xvf " + context.getFilesDir().getAbsolutePath() + "/debian_usr_other.tar.xz -C " + context.getFilesDir().getAbsolutePath() + "/usr/var/lib/proot-distro/installed-rootfs");
        result += executeCommand("rm -fv " + context.getFilesDir().getAbsolutePath() + "/debian_usr_other.tar.xz");
        // 解压其它目录
        copyFilesFromRaw(context, R.raw.debian_other_tar, "debian_other.tar.gz", context.getFilesDir().getAbsolutePath());
        result += executeCommand("sh " + context.getFilesDir().getAbsolutePath() + "/../runprogram.sh " + context.getFilesDir().getAbsolutePath() + "/usr/bin/tar --no-same-owner -xvf " + context.getFilesDir().getAbsolutePath() + "/debian_other.tar.gz -C " + context.getFilesDir().getAbsolutePath() + "/usr/var/lib/proot-distro/installed-rootfs");
        result += executeCommand("rm -fv " + context.getFilesDir().getAbsolutePath() + "/debian_other.tar.gz");
        return result;
    }

    public String SetVNCPasswd(Context context, String password)
    {
        String result = "";
        // 写入密码设置脚本
        copyFilesFromRaw(context, R.raw.changevncpasswd, "ChangeVNCPasswd.sh", context.getFilesDir().getAbsolutePath() + "/usr/var/lib/proot-distro/installed-rootfs/debian/tmp");
        result += executeCommand("chmod 777 " + context.getFilesDir().getAbsolutePath() + "/usr/var/lib/proot-distro/installed-rootfs/debian/tmp/ChangeVNCPasswd.sh");
        try {
            FileWriter file = new FileWriter(context.getFilesDir().getAbsolutePath() + "/usr/var/lib/proot-distro/installed-rootfs/debian/tmp/password.txt");
            file.write(password);
            file.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        result += executeCommand("echo 114514");
        result += executeCommand("sh " + context.getFilesDir().getAbsolutePath() + "/../run.sh bash /tmp/ChangeVNCPasswd.sh /tmp/password.txt");
        File passFile = new File(context.getFilesDir().getAbsolutePath() + "/../cache/password.txt");
        passFile.delete();
        return result;
    }

    public String KernelVersion()
    {
        return executeCommand("uname -a").replace("\n", "");
    }

    public void Loging(Context context, int width, int height)
    {
        executeCommand("cp -v " + context.getFilesDir().getAbsolutePath() + "/../loader.sh " + context.getFilesDir().getAbsolutePath() + "/usr/var/lib/proot-distro/installed-rootfs/debian/");
        executeCommand("sh " + context.getFilesDir().getAbsolutePath() + "/../run.sh /loader.sh " + width + " " + height);
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
                Cout(line);
                result += line + "\n";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public void Cout(String data)
    {
        Log.d("RunCommand", data);
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
            FileOutputStream fos = new FileOutputStream(file);
            // 2.定义存储空间，每块文件 50MB
            byte[] buffer = new byte[50 * 1024 * 1024]; //new byte[inputStream.available()];
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
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
