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
import android.content.SharedPreferences;
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
import java.util.IllegalFormatCodePointException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "testTag";
    private final static int RENAME = 2;
    final static int SPEAK = 1;
    //??????UI??????
    private Button btn_camera;
    private SurfaceView imageHolder;
    private Camera camera = null;
    private TextView testShow;
    public String result_voice;
    //????????????
    private MyRecognizer myRecognizer;
    final Map<String,Object> recogparams  = new HashMap<>();
    private RecogEventAdapter recogEventAdapter;
    private boolean enableOffline=false;
    //????????????
    private WakeupEventAdapter wakeupAdapter;
    private Map<String,Object> wakeupParams = new HashMap<>();
    private MyWakeup myWakeup;
    //??????/??????flag
    private boolean wakeupFlag = false;
    //????????????????????????5s???????????????????????????flag,????????????????????????
    private long currenTime ;
    private Thread timeThread;

    private IWakeupListener wakeupListener= new IWakeupListener() {
        @Override
        public void onSuccess(String word, WakeUpResult result) {
            //????????????
            Log.d(TAG, "onSuccess: wakeUpsuccess");
            //??????????????????
            myWakeup.stop();
            //?????????????????????
            initRecog();
            //???????????????
            SpeakVoiceUtil.getInstance(getApplicationContext()).speak("?????????????????????????????????????????????");
            wakeupFlag = true;
            //???????????????????????????
            currenTime = System.currentTimeMillis();
            timeThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean flag = true;
                    while(flag){
                        long t = System.currentTimeMillis();
                        //????????????
                        if (t-currenTime>5000){
                            SpeakVoiceUtil.getInstance(getApplicationContext()).speak("???????????????");
                            //??????????????????
                            timeThread.interrupt();
                            //???????????????
                            myRecognizer.release();

                            myWakeup.stop();
                            //??????????????????
                            flag = false;
                            myWakeup.start(wakeupParams);
                        }
                    }
                    Log.d(TAG, "run: ???????????????");
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
            //?????????????????????????????????????????????????????????????????????
            //????????????currentTime????????????
            //???????????????5s?????????????????????????????????????????????
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
            String best_result = asRresponse.getBest_result().replace('???', ' ').trim();
            Log.d(TAG, "onAsrPartialResult2: " + best_result);
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
                case RENAME:
                    break;
            }

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //???????????????
        init_component();
        //???????????????
        initPermission();
        //???????????????
        initParams();
//        myRecognizer.start(recogparams);
       myWakeup.start(wakeupParams);
    }
    //??????????????????
    public void initRecog(){
        recogEventAdapter = new RecogEventAdapter(iRecogListener);
        myRecognizer = new MyRecognizer(MainActivity.this,recogEventAdapter);
    }
    public void initParams(){
        //?????????????????????
        recogparams.put("accept-audio-data",true);
        recogparams.put("disable-punctuation",false);
        recogparams.put("accept-audio-volume",true);
        recogparams.put("pid",1537);
        //?????????????????????
       wakeupParams.put(SpeechConstant.WP_WORDS_FILE, "assets://WakeUp.bin");
    }
    //???????????????
    public void init_component() {
        //?????????UI??????
        btn_camera = (Button) findViewById(R.id.photo);
        imageHolder = (SurfaceView) findViewById(R.id.imageholder);
        imageHolder.getHolder().addCallback(cpHolderCalback);
        testShow = (TextView)findViewById(R.id.showText);
//        //????????????????????????
        wakeupAdapter = new WakeupEventAdapter(wakeupListener);
        myWakeup = new MyWakeup(this,wakeupAdapter);
        //??????????????????
        btn_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //??????????????????
                camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        //??????
                        camera.takePicture(null, null, new Camera.PictureCallback() {
                            @Override
                            public void onPictureTaken(byte[] data, Camera camera) {
                                String path = "";
                                if ((path = saveFile(data)) != null) {
                                    //???????????????????????????activity??????????????????????????????
                                    Intent it = new Intent(MainActivity.this, solution.class);
                                    it.putExtra("path", path);
                                    startActivity(it);
                                } else {
                                    Toast.makeText(MainActivity.this, "??????????????????", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });
            }
        });

    }
    //?????????surfaceHolder
    private SurfaceHolder.Callback cpHolderCalback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            //?????????View
            startPreview();
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    Log.d("11111", "onAutoFocus: "+"????????????"+success);

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
    //????????????
    @Override
    protected void onStart(){
        if (!network.getInstance().isNetworkAvailable(this)){
            network.getInstance().showSetNetworkUI(this);
        }
        super.onStart();
    }
    //??????????????????
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction()==MotionEvent.ACTION_DOWN){
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    Log.d("11111", "onAutoFocus: "+"????????????"+success);

                }
            });
        }
        return super.onTouchEvent(event);
    }
    //??????????????????
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
            camera.setDisplayOrientation(90);   //???????????????90???
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onDestroy() {

        // ?????????????????????myRecognizer.loadOfflineEngine()??? release()????????????????????????????????????
        //        // ??????DEMO5.1 ??????????????????(???????????????) release()?????????????????????????????????????????????
        //        // ??????DEMO???5.2 ?????????????????????
        myRecognizer.release();
        // BluetoothUtil.destory(this); // ????????????

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
                // ?????????????????????????????????.
                Toast.makeText(this,"????????????",Toast.LENGTH_SHORT).show();
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
