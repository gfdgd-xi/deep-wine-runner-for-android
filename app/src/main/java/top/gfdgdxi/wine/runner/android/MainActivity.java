package top.gfdgdxi.wine.runner.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import top.gfdgdxi.wine.runner.android.databinding.ActivityMainBinding;
import top.gfdgdxi.wine.runner.android.PRoot;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 设置应用为横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);  //设置屏幕为横屏, 设置后会锁定方向
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);  // 设置隐藏标题栏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        // 设置 WebView 属性
        WebView webView1 = findViewById(R.id.systemGUI);
        WebSettings webSettings = webView1.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setDomStorageEnabled(true);  // 解决问题 Cannot read property getItem of null
        // 设置不允许选中文本
        webView1.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });

        // 设置加载页
        webView1.loadUrl("file:///android_asset/LoadHTML/index.html");
        webView1.evaluateJavascript("javascript:UpdateInfo('1.1.0')", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {

            }
        });

        // 判断系统是否释放
        // 如果没有
        if(true) {
            webView1.loadUrl("file:///android_asset/UnpackEnvironment/index.html");
            webView1.evaluateJavascript("javascript:UpdateInfo('1.1.0')", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {

                }
            });
            UnpackSystem unpackSystem = new UnpackSystem();
            unpackSystem.start();
        }
        else {
            RunSystem();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public boolean telnetPort(String ip, int port)
    {
        Socket socket = new Socket();
        boolean res = false;
        try {
            socket.connect(new InetSocketAddress(ip, port), 100);
            res = socket.isConnected();
        } catch (IOException e) {
            res = false;
        }
        return res;
    }

    public void RunSystem()
    {
        PRoot proot = new PRoot();
        proot.UnpackEnvironment(MainActivity.this);
        // 加载系统
        LoadSystem loadSystem = new LoadSystem();
        loadSystem.start();
        CheckPort checkPort = new CheckPort();
        checkPort.start();
        // 检测服务
        //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();  // 支持在主线程进行网络检测
        //StrictMode.setThreadPolicy(policy);

    }

    class CheckPort extends Thread {
        @Override
        public void run()
        {
            WebView webView = findViewById(R.id.systemGUI);
            while (true) {
                if(telnetPort("127.0.0.1", 6080)) {
                    // 设置 NoVNC
                    webView.post(() -> {
                        webView.loadUrl("http://127.0.0.1:6080/vnc.html");
                    });
                    break;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }

    class LoadSystem extends Thread {
        @Override
        public void run()
        {
            PRoot proot = new PRoot();
            // 获取屏幕分辨率
            WindowManager windowManager = getWindow().getWindowManager();
            Point point = new Point();
            windowManager.getDefaultDisplay().getRealSize(point);
            int width = point.x;
            int height = point.y;
            Log.d("Display", "run: " + width + " " + height);  // 为了在手机可以更好操作，设置分辨率只为一半
            proot.Loging(MainActivity.this, (int) (width * 0.55), (int) (height * 0.55));
        }
    }

    class PRootShowInfoToWebView extends PRoot {
        // 使用队列存储数据
        Queue<String> commandResult = new LinkedList<>();
        WebViewRefresh webViewThread;
        LogcatCout logcatThread;
        String nowResult;
        boolean stopThread = false;
        @Override
        public void Cout(String data)
        {
            // 不直接输出至 WebView 以提升性能
            // 存储至队列
            nowResult = data;
            commandResult.offer(data);
        }

        public void StartThread()
        {
            stopThread = false;
            // 开启刷新线程
            webViewThread = new WebViewRefresh();
            webViewThread.start();
            logcatThread = new LogcatCout();
            logcatThread.start();
        }

        public void StopThread()
        {
            stopThread = true;
        }
        class LogcatCout extends Thread {
            @Override
            public void run()
            {
                while(true) {
                    // 刷新次数：20次/s
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    // 判断队列内是否有数据
                    if (commandResult.isEmpty()) {
                        if(stopThread) {
                            break;
                        }
                        continue;
                    }
                    // 读取数据
                    String data = commandResult.poll();
                    Log.d("RunCommand", data);
                }
            }
        }
        // 另外开启一个线程以处理输出问题
        class WebViewRefresh extends Thread {
            @Override
            public void run()
            {
                while (true) {
                    // 刷新次数：20次/s
                    try {
                        Thread.sleep(90);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if(stopThread) {
                        break;
                    }
                    WebView webView1 = findViewById(R.id.systemGUI);
                    webView1.post(() -> {
                        webView1.evaluateJavascript("javascript:SetUnpackData('" + nowResult + "')", new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {}});
                    });
                }
            }
        }
    }

    class UnpackSystem extends Thread {
        @Override
        public void run()
        {
            WebView webView1 = findViewById(R.id.systemGUI);
            PRootShowInfoToWebView systemConfig = new PRootShowInfoToWebView();
            systemConfig.StartThread();
            systemConfig.CleanTempFile(MainActivity.this);
            // 解压文件
            webView1.post(() -> {
                webView1.evaluateJavascript("javascript:SetUnpackData('解压核心文件')", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {}});
            });
            if(!systemConfig.IsEnvironmentInstalled(MainActivity.this)) {
                // 不重复安装
                systemConfig.UnpackEnvironment(MainActivity.this);
            }
            webView1.post(() -> {
                webView1.evaluateJavascript("javascript:SetUnpackData('解压资源文件')", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {}}
                );
            });
            if(!systemConfig.IsSystemInstalled(MainActivity.this)) {
                // 不重复安装
                systemConfig.UnpackSystem(MainActivity.this);
                systemConfig.SetVNCPasswd(MainActivity.this, "123456");
            }
            systemConfig.StopThread();
            RunSystem();
        }
    }
}