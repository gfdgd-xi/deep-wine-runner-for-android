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
        // 设置加载页
        webView1.loadUrl("file:///android_asset/LoadHTML/index.html");
        webView1.evaluateJavascript("javascript:UpdateInfo('1.1.0')", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {

            }
        });

        PRoot proot = new PRoot();
        proot.UnpackEnvironment(MainActivity.this);
        // 加载系统
        LoadSystem loadSystem = new LoadSystem();
        loadSystem.start();
        // 检测服务
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();  // 支持在主线程进行网络检测
        StrictMode.setThreadPolicy(policy);
        WebView webView = findViewById(R.id.systemGUI);
        webView.post(() -> {
            while (true) {
                if(telnetPort("127.0.0.1", 6080)) {
                    // 设置 NoVNC
                    webView1.loadUrl("http://127.0.0.1:6080/vnc.html");
                    break;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });


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
}