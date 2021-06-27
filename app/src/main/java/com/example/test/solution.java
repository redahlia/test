package com.example.test;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.hardware.Camera;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class solution extends AppCompatActivity {
    private ImageView img;
    private final int SUBMIT = 1;
    private final int GET = 2;
    private File file_img;
    private TextView answer;
    private String response_answer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solution);
        init();
        String path = getIntent().getStringExtra("path");
        Log.e(path, "onCreate: " );
        if(path != null){
            img.setImageURI(Uri.fromFile(new File(path)));
            file_img = new File(path);
        }

    }
    private void init(){
        answer = (TextView)findViewById(R.id.answer);
        img = (ImageView)findViewById(R.id.image);
        img.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                popMenu(v);
                return true;
            }
        });

    }
    private void popMenu(View v){
        final PopupMenu myMenu = new PopupMenu(this,v);
        myMenu.getMenuInflater().inflate(R.menu.photo,myMenu.getMenu());
        myMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getTitle().equals("解析")){
                    http2();
                }else if(item.getTitle().equals("重新拍照")){
                    Intent intent = new Intent(solution.this,MainActivity.class);
                    startActivity(intent);
                }
                return false;
            }
        });
        myMenu.show();
    }
    public void http() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject json = new JSONObject();
                int i =0;
                try {
                    json.put("img",file_img);
                    URL url = new URL("http://192.168.31.232:8081/getNum");
                    HttpURLConnection connect = (HttpURLConnection) url.openConnection();
                    connect.setDoInput(true);
                    connect.setDoOutput(true);
                    connect.setRequestMethod("POST");
                    connect.setUseCaches(false);
                    connect.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    connect.connect();
                    OutputStream outputStream = connect.getOutputStream();
                    outputStream.write(json.toString().getBytes());
                    outputStream.flush();
                    outputStream.close();

                    int response = connect.getResponseCode();
                    if (response == HttpURLConnection.HTTP_OK) {
                        System.out.println(response);
                        InputStream input = connect.getInputStream();
                        BufferedReader in = new BufferedReader(new InputStreamReader(input));
                        JSONObject js = streamToJson(input);
                        String question = js.getString("question");
                        Log.e("json",question);
//                        String line = null;
//                        StringBuffer sb = new StringBuffer();
//                        while ((line = in.readLine()) != null) {
//                            sb.append(line);
//                            Log.d("line:",line);
//                        }
                    } else {
                        System.out.println(response);
                    }
                } catch (Exception e) {
                    Log.e("e:", String.valueOf(e));
                }
            }
        }).start();

    }
    public void http2(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //MediaType mediaType = MediaType.parse("multipart/form-data; charset=utf-8");
                OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
                RequestBody requestBody = RequestBody.create(MediaType.parse("image/png"), file_img);
                MultipartBody body = new MultipartBody.Builder()
                                    .setType(MultipartBody.FORM)
                                    .addFormDataPart("pic",file_img.getName()+".png",requestBody)
                                    .build();
                final Request request = new Request.Builder()
                                .url("http://192.168.31.232:8081/upPic")
                                .post(body)
                                .build();
                Call call = okHttpClient.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.d("fail", "onFailure: "+e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        response_answer  = response.body().string();
                        Message msg = new Message();
                        msg.what = GET;
                        handler.sendMessage(msg);
                    }
                });
            }
        }).start();

    }
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == GET) {
                answer.setText(response_answer);
            }
        }

    };
    private JSONObject streamToJson(InputStream inputStream) throws Exception {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream,"UTF-8"));
        String temp = "";
        StringBuilder stringBuilder = new StringBuilder();
        while ((temp = bufferedReader.readLine()) != null) {
            stringBuilder.append(temp);
        }
        JSONObject json = new JSONObject(stringBuilder.toString().trim());
        return json;
    }
}
