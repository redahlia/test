package com.example.test;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.baidu.aip.asrwakeup3.core.recog.MyRecognizer;
import com.baidu.aip.asrwakeup3.core.recog.RecogResult;
import com.baidu.aip.asrwakeup3.core.recog.listener.IRecogListener;
import com.baidu.aip.asrwakeup3.core.recog.listener.RecogEventAdapter;
import com.baidu.aip.asrwakeup3.core.wakeup.MyWakeup;
import com.baidu.aip.asrwakeup3.core.wakeup.WakeUpResult;
import com.baidu.aip.asrwakeup3.core.wakeup.WakeupEventAdapter;
import com.baidu.aip.asrwakeup3.core.wakeup.listener.IWakeupListener;
import com.baidu.speech.EventListener;
import com.baidu.speech.asr.SpeechConstant;
import com.example.voicett.ASRresponse;
import com.example.voicett.AutoCheck;
import com.google.gson.Gson;
import com.setting.network;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "testTag";
    private final static int modifyStatus = 2;
    final static int SPEAK = 1;
    //界面UI控件
    private Button btn_camera;
    private SurfaceView imageHolder;
    private Camera camera = null;
    private TextView testShow;
    public String result_voice;
    //识别数据
    private MyRecognizer myRecognizer;
    final Map<String,Object> recogparams  = new HashMap<>();
    private RecogEventAdapter recogEventAdapter;
    private boolean enableOffline=false;
    //唤醒数据
    private WakeupEventAdapter wakeupAdapter;
    private Map<String,Object> wakeupParams = new HashMap<>();
    private MyWakeup myWakeup;
    //唤醒/熄灭flag
    private boolean wakeupFlag = false;
    //作为识别结果，若5s内没有变化，则更改flag,并且熄灭等待唤醒
    private long currenTime ;
    private Thread timeThread;
    private IWakeupListener wakeupListener= new IWakeupListener() {
        @Override
        public void onSuccess(String word, WakeUpResult result) {
            //唤醒成功
            Log.d(TAG, "onSuccess: wakeUpsuccess");
            //停止唤醒识别
            myWakeup.stop();
            //初始化识别控件
            recogEventAdapter = new RecogEventAdapter(iRecogListener);
            myRecognizer = new MyRecognizer(MainActivity.this,recogEventAdapter);
            //成功后操作
            SpeakVoiceUtil.getInstance(getApplicationContext()).speak("你好你好，有什么需要问我的吗？");
            wakeupFlag = true;
            //唤醒之后开一个线程
            currenTime = System.currentTimeMillis();
            timeThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean flag = true;
                    while(flag){
                        long t = System.currentTimeMillis();
                        //唤醒关闭
                        if (t-currenTime>5000){
                            SpeakVoiceUtil.getInstance(getApplicationContext()).speak("服务已退出");
                            //中断当前线程
                            timeThread.interrupt();
                            //释放识别器
                            myRecognizer.release();

                            myWakeup.stop();
                            //等待下次唤醒
                            flag = false;
                            myWakeup.start(wakeupParams);
                        }
                    }
                    Log.d(TAG, "run: 线程已退出");
                }
            });
            myRecognizer.start(recogparams);
        }
        @Override
        public void onStop() {

        }

        @Override
        public void onError(int errorCode, String errorMessge, WakeUpResult result) {

        }

        @Override
        public void onASrAudio(byte[] data, int offset, int length) {

        }
    };
    private IRecogListener iRecogListener= new IRecogListener() {
        @Override
        public void onAsrReady() {
        }
        @Override
        public void onAsrBegin() {

        }
        @Override
        public void onAsrEnd() {

        }
        @Override
        public void onAsrPartialResult(String[] results, RecogResult recogResult) {
            //在有语音输入的情况下，若线程没有开启则开启线程
            //通过更改currentTime进行判断
            //若时间超过5s后没有收到语音输入，则关闭唤醒
            currenTime = System.currentTimeMillis();
            if (!timeThread.isAlive()){
                timeThread.start();
            }
        }
        @Override
        public void onAsrOnlineNluResult(String nluResult) {
        }

        @Override
        public void onAsrFinalResult(String[] results, RecogResult recogResult) {
            Gson gson = new Gson();
            ASRresponse asRresponse = gson.fromJson(recogResult.getOrigalJson(), ASRresponse.class);
            String best_result = asRresponse.getBest_result().replace('，',' ').trim();
            Log.d(TAG, "onAsrPartialResult2: "+best_result);
        }

        @Override
        public void onAsrFinish(RecogResult recogResult) {
        }

        @Override
        public void onAsrFinishError(int errorCode, int subErrorCode, String descMessage, RecogResult recogResult) {

        }

        @Override
        public void onAsrLongFinish() {

        }

        @Override
        public void onAsrVolume(int volumePercent, int volume) {

        }

        @Override
        public void onAsrAudio(byte[] data, int offset, int length) {

        }

        @Override
        public void onAsrExit() {
            myRecognizer.start(recogparams);
            Log.d(TAG, "onAsrExit: exit");
        }

        @Override
        public void onOfflineLoaded() {

        }

        @Override
        public void onOfflineUnLoaded() {

        }
    };
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case SPEAK:
                    SpeakVoiceUtil.getInstance(getApplicationContext()).speak(result_voice);
                    testShow.setText(result_voice);
                    break;
                case modifyStatus:
                    break;
            }

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化组件
        init_component();
        //初始化权限
        initPermission();
        //初始化参数
        initParams();
        myWakeup.start(wakeupParams);
    }
    public void initParams(){
        //初始化识别参数
        recogparams.put("accept-audio-data",true);
        recogparams.put("disable-punctuation",false);
        recogparams.put("accept-audio-volume",true);
        recogparams.put("pid",1537);
        //初始化唤醒参数
        wakeupParams.put(SpeechConstant.WP_WORDS_FILE, "assets://WakeUp.bin");
    }
    //初始化组件
    public void init_component() {
        //初始化UI组件
        btn_camera = (Button) findViewById(R.id.photo);
        imageHolder = (SurfaceView) findViewById(R.id.imageholder);
        imageHolder.getHolder().addCallback(cpHolderCalback);
        testShow = (TextView)findViewById(R.id.showText);
        //初始化唤醒适配器
        wakeupAdapter = new WakeupEventAdapter(wakeupListener);
        myWakeup = new MyWakeup(this,wakeupAdapter);
        //点击屏幕拍照
        btn_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //开启自动对焦
                camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        //拍照
                        camera.takePicture(null, null, new Camera.PictureCallback() {
                            @Override
                            public void onPictureTaken(byte[] data, Camera camera) {
                                String path = "";
                                if ((path = saveFile(data)) != null) {
                                    //图片保存成功后跳转activity，并把图片路径传过去
                                    Intent it = new Intent(MainActivity.this, solution.class);
                                    it.putExtra("path", path);
                                    startActivity(it);
                                } else {
                                    Toast.makeText(MainActivity.this, "保存照片失败", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });
            }
        });

    }
    //初始化surfaceHolder
    private SurfaceHolder.Callback cpHolderCalback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            //初始化View
            startPreview();
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    Log.d("11111", "onAutoFocus: "+"聚焦成功"+success);

                }
            });
        }
        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            stopPreview();
        }
    };
    //网络判断
    @Override
    protected void onStart(){
        if (!network.getInstance().isNetworkAvailable(this)){
            network.getInstance().showSetNetworkUI(this);
        }
        super.onStart();
    }
    //点击屏幕对焦
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction()==MotionEvent.ACTION_DOWN){
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    Log.d("11111", "onAutoFocus: "+"聚焦成功"+success);

                }
            });
        }
        return super.onTouchEvent(event);
    }
    //保存临时文件
    private String saveFile(byte[] bytes){
        try {
            File file = File.createTempFile("img","");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.flush();
            fos.close();
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
    public void startPreview(){
        camera = Camera.open();
        Log.e("camera", "startPreview: ");
        try {
            camera.setPreviewDisplay(imageHolder.getHolder());
            camera.setDisplayOrientation(90);   //让相机旋转90度
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onDestroy() {

        // 如果之前调用过myRecognizer.loadOfflineEngine()， release()里会自动调用释放离线资源
        //        // 基于DEMO5.1 卸载离线资源(离线时使用) release()方法中封装了卸载离线资源的过程
        //        // 基于DEMO的5.2 退出事件管理器
        myRecognizer.release();
        // BluetoothUtil.destory(this); // 蓝牙关闭

        super.onDestroy();
    }
    public void start(){
        (new AutoCheck(getApplicationContext(), new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 100) {
                    AutoCheck autoCheck = (AutoCheck) msg.obj;
                    synchronized (autoCheck) {
                        String message = autoCheck.obtainErrorMessage(); // autoCheck.obtainAllMessage();
                        Log.w("AutoCheckMessage", message);
                    }
                }
            }
        }, enableOffline)).checkAsr(recogparams);
        Log.d("11111111", "start: 123123");
        myRecognizer.start(recogparams);
    }
    private void initPermission() {
        String permissions[] = {Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.ACCESS_WIFI_STATE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.
                Toast.makeText(this,"没有权限",Toast.LENGTH_SHORT).show();
            }
        }
        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }

    }
    protected void stop(){
        myRecognizer.stop();
    }
    protected void cancel() {

        myRecognizer.cancel();
    }
    public void stopPreview(){
        camera.stopPreview();
        camera.release();
        camera = null;
    }
}
