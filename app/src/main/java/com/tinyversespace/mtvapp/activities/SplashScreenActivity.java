package com.tinyversespace.mtvapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.tinyversespace.mtvapp.R;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置布局
        setContentView(R.layout.splash_screen);

        // 创建 Handler 对象
        Handler handler = new Handler(Looper.getMainLooper());
        // 延迟 3 秒后跳转到主页面
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, 3000);
    }
}
