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

// The Token Generator to generate token
// What does OSS Bucket mean?
// What does OSS Data mean?
// What does Taskhandler mean?

// TODO: Solved dependency issue while importing JAR
// Firstly create app/libs directory
// copy Jar files to app/libs
// performing Add as Library option on Jar file
// app/build.gradle will get updated with new dependencies


/**
 * Fetch picture from OSS and converting picture to byte array
 * Input: private OSS
 * Output: picture saved as byte stream
 * */
public class MainActivity extends Activity {

    // Access Key ID and Access Key Secrete
    static final String accessKey = "6kbI7MI3WMGz4Sig";
    static final String secretKey = "this-is-your-secrete-key";
    public Handler progressHandler = new ProgressHandler();
    public Handler successHandler = new SuccessHandler();
    public OSSBucket sampleBucket;
    // 记录object和显示控件的对应关系
    Map<String, ProgressBar> mMapBarAndObject = new HashMap<String, ProgressBar>();
    private ProgressBar mProgressBar;
    private TaskHandler mHandler;

    private static void makeToast(String operation, String key, Handler handler) {
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putString("operation", operation);
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

    // Get started from here
    static {
        // Global default token generator
        OSSClient.setGlobalDefaultTokenGenerator(new TokenGenerator() { // 设置全局默认加签器
            @Override
            public String generateToken(String httpMethod, String md5, String type, String date,
                                        String ossHeaders, String resource) {

                String content = httpMethod + "\n" + md5 + "\n" + type + "\n" + date + "\n" + ossHeaders
                        + resource;

                // Now start the progress of generating token
                // so what do you request to obtain token?
                // Access Key ID + Access Key Secrete
                return OSSToolKit.generateToken(accessKey, secretKey, content);
            }
        });

        // Setup Data center address
        OSSClient.setGlobalDefaultHostId("oss-cn-beijing.aliyuncs.com"); // 设置全局默认数据中心域名
        // Setup Access Control rule: private, public-read, public-read-write
        OSSClient.setGlobalDefaultACL(AccessControlList.PRIVATE); // 设置全局默认bucket访问权限
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);

        OSSClient.setApplicationContext(getApplicationContext()); // 传入应用程序context

        // 开始单个Bucket的设置
        sampleBucket = new OSSBucket("rockcafe-cn-beijing-photostream");
        sampleBucket.setBucketACL(AccessControlList.PRIVATE); // 如果这个Bucket跟全局默认的访问权限不一致，就需要单独设置
        // sampleBucket.setBucketHostId("oss-cn-beijing.aliyuncs.com"); // 如果这个Bucket跟全局默认的数据中心不一致，就需要单独设置
        // sampleBucket.setBucketTokenGen(new TokenGenerator() {...}); // 如果这个Bucket跟全局默认的加签方法不一致，就需要单独设置
        // sampleBucket.setBucketAccessRefer("your.refer.com"); // 如果这个Bucket开启了防盗链功能，就需要通过这个接口设置reference

        // 在map中记录文件和控件的对应关系
        mMapBarAndObject.put("family-photo/DSC00785_i.jpg", mProgressBar); // 建立进度条与下载文件的对应关系

        Button startDownload = (Button) findViewById(R.id.start_button);
        startDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // TODO: Fetching a simple picutre from OSS's bucket
                OSSData ossData = new OSSData(sampleBucket, "family-photo/DSC00785_i.jpg");
                mHandler = ossData.getInBackground(new GetBytesCallback() {

                    @Override
                    public void onSuccess(String objectKey, byte[] data) {

                        // Congratulations! you have successfully fetch the picture

                        Log.d("onSuccess", "Complete downloading picture. data length: " + data.length);
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

        Button cancelDownload = (Button) findViewById(R.id.cancel_button);
        cancelDownload.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mHandler != null) {
                    mHandler.cancel(); // 取消下载任务
                }
            }
        });
    }

    class ProgressHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            // So what do you wanna do with Progress Handler
            super.handleMessage(msg);
            Bundle b = msg.getData();
            int currentSize = b.getInt("current");
            int totalSize = b.getInt("total");
            String key = b.getString("objectKey");
            ProgressBar bar = mMapBarAndObject.get(key);
            bar.setProgress((int) (100 * (1.000 * currentSize / totalSize)));
        }
    }

    class SuccessHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();
            String operation = b.getString("operation");
            String key = b.getString("objectKey");
            super.handleMessage(msg);
            Toast.makeText(getApplicationContext(), operation + key + "成功!", Toast.LENGTH_SHORT).show();
        }
    }
}
