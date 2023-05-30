package com.tinyversespace.mtvapp;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.core.web.JsBridgeWebView;
import com.core.web.base.BaseWebViewClient;
//import helloworld.Hello;

public class MainActivity extends Activity {
    JsBridgeWebView webView;

    ProgressBar bar;
    final String mimeType = "text/html";
    final String encoding = "utf-8";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //将屏幕设置为全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //去掉标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.webview);

        bar = (ProgressBar) findViewById(R.id.progressBar1);
        webView = (JsBridgeWebView) findViewById(R.id.webView);
        //在此处添加提供给JS调用的接口，可以添加多个接口类每增加一个接口类：webView.addJavascriptInterface(new ToJSAPIClass(this))；
        //可以添加多行；
        webView.addJavascriptInterface(new JsCallMtvExample(this));
        String url = "https://mtv.tinyverse.space/";
        loadUrl(url);

        //String
        Toast.makeText(this,"调用原生无参数无回调方法",Toast.LENGTH_SHORT).show();

    }

    //重写onKeyDown(keyCode, event)方法 改写物理按键 返回的逻辑
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack();//返回上一页面
                return true;
            } else {
                System.exit(0);//退出程序
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void loadUrl(String url) {
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);//开启JavaScript支持
        ws.setDomStorageEnabled(true);//增加访问web storage支持
        webView.setWebViewClient(new BaseWebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                webView.loadUrl(request.getUrl().toString());
                return true;
            }

        });
        //添加进度条
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    bar.setVisibility(View.INVISIBLE);
                } else {
                    if (View.INVISIBLE == bar.getVisibility()) {
                        bar.setVisibility(View.VISIBLE);
                    }
                    bar.setProgress(newProgress);
                }
                super.onProgressChanged(view, newProgress);
            }
        });
        //出现net::ERR_CACHE_MISS错误提示
        //使用缓存的方式是基于导航类型。正常页面加载的情况下将缓存内容。当导航返回,
        //内容不会恢复（重新加载生成）,而只是从缓存中取回内容
        if (Build.VERSION.SDK_INT >= 19) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        webView.loadUrl(url);
    }
}

