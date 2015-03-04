package com.example.android_example;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.aliyun.mbaas.oss.OSSClient;
import com.aliyun.mbaas.oss.callback.GetBytesCallback;
import com.aliyun.mbaas.oss.model.AccessControlList;
import com.aliyun.mbaas.oss.model.OSSException;
import com.aliyun.mbaas.oss.model.TokenGenerator;
import com.aliyun.mbaas.oss.storage.OSSBucket;
import com.aliyun.mbaas.oss.storage.OSSData;
import com.aliyun.mbaas.oss.storage.TaskHandler;
import com.aliyun.mbaas.oss.util.OSSToolKit;

import java.util.HashMap;
import java.util.Map;

// TODO: Solve dependency issue while importing JAR
// Firstly create app/libs directory
// copy Jar files to app/libs
// performing Add as Library option on Jar file
// app/build.gradle will get updated with new dependencies

public class MainActivity extends Activity {

    static final String accessKey = "Your AccessKey"; // 测试代码没有考虑AK/SK的安全性
    static final String screctKey = "Your SecretKey";

    // 记录object和显示控件的对应关系
    Map<String, ProgressBar> mapBarAndObject = new HashMap<String, ProgressBar>();
    public Handler progressHandler = new ProgressHandler();
    private ProgressBar progressBar;

    public Handler successHandler = new SucessHandler();
    public OSSBucket sampleBucket;

    private TaskHandler tHandler;

    class ProgressHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle b = msg.getData();
            int currentSize = b.getInt("current");
            int totalSize = b.getInt("total");
            String key = b.getString("objectKey");
            ProgressBar bar = mapBarAndObject.get(key);
            bar.setProgress((int) (100 * (1.000 * currentSize / totalSize)));
        }
    }

    class SucessHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();
            String oper = b.getString("operation");
            String key = b.getString("objectKey");
            super.handleMessage(msg);
            Toast.makeText(getApplicationContext(), oper + key + "成功!", Toast.LENGTH_SHORT).show();
        }
    }

    private static void makeToast(String oper, String key, Handler handler) {
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putString("operation", oper);
        b.putString("objectKey", key);
        msg.setData(b);
        handler.sendMessage(msg);
    }

    private static void updateBar(String objectKey, int current, int total, Handler handler) {
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putString("objectKey", objectKey);
        b.putInt("current", current);
        b.putInt("total", total);
        msg.setData(b);
        handler.sendMessage(msg);
    }

    static {
        OSSClient.setGlobalDefaultTokenGenerator(new TokenGenerator() { // 设置全局默认加签器
            @Override
            public String generateToken(String httpMethod, String md5, String type, String date,
                    String ossHeaders, String resource) {

                String content = httpMethod + "\n" + md5 + "\n" + type + "\n" + date + "\n" + ossHeaders
                        + resource;

                return OSSToolKit.generateToken(accessKey, screctKey, content);
            }
        });
        OSSClient.setGlobalDefaultHostId("oss-cn-hangzhou.aliyuncs.com"); // 设置全局默认数据中心域名
        OSSClient.setGlobalDefaultACL(AccessControlList.PRIVATE); // 设置全局默认bucket访问权限
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = (ProgressBar) findViewById(R.id.progressBar1);

        OSSClient.setApplicationContext(getApplicationContext()); // 传入应用程序context

        // 开始单个Bucket的设置
        sampleBucket = new OSSBucket("oss-example");
        sampleBucket.setBucketACL(AccessControlList.PUBLIC_READ_WRITE); // 如果这个Bucket跟全局默认的访问权限不一致，就需要单独设置
        // sampleBucket.setBucketHostId("oss-cn-hangzhou.aliyuncs.com"); // 如果这个Bucket跟全局默认的数据中心不一致，就需要单独设置
        // sampleBucket.setBucketTokenGen(new TokenGenerator() {...}); // 如果这个Bucket跟全局默认的加签方法不一致，就需要单独设置
        // sampleBucket.setBucketAccessRefer("your.refer.com"); // 如果这个Bucket开启了防盗链功能，就需要通过这个接口设置reference

        // 在map中记录文件和控件的对应关系
        mapBarAndObject.put("aliyun-logo.png", progressBar); // 建立进度条与下载文件的对应关系

        Button startDownload = (Button) findViewById(R.id.button1);
        startDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OSSData ossData = new OSSData(sampleBucket, "aliyun-logo.png");
                tHandler = ossData.getInBackground(new GetBytesCallback() {

                    @Override
                    public void onSuccess(String objectKey, byte[] data) {
                        Log.d("onSuccess", "complete downloading, data.length: " + data.length);
                        makeToast("download", objectKey, successHandler);
                    }

                    @Override
                    public void onProgress(String objectKey, int byteCount, int totalSize) {
                        Log.d("onProgress", String.format("%d %d\n", byteCount, totalSize));
                        updateBar(objectKey, byteCount, totalSize, MainActivity.this.progressHandler);
                    }

                    @Override
                    public void onFailure(String objectKey, OSSException ossException) {
                        Log.d("onFailure", ossException.toString());
                    }
                });
            }
        });

        Button cancelDownload = (Button) findViewById(R.id.button2);
        cancelDownload.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (tHandler != null) {
                    tHandler.cancel(); // 取消下载任务
                }
            }
        });
    }
}
