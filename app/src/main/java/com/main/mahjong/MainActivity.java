package com.main.mahjong;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.main.mahjong.util.CheckNetwork;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends Activity {

    private static final String TAG_EXIT = "exit";
    private int expiredTime=60*60*60*24*7;//7天
    private String resultData = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //检查网络状态，无网络自动退出
        if(!CheckNetwork.isNetworkConnected(getApplicationContext())){
            show(getApplicationContext(),"网络异常，请检查网络");
            Intent intent = new Intent(this,MainActivity.class);
            intent.putExtra(MainActivity.TAG_EXIT, true);
            startActivity(intent);
            return;
        }

        //检查服务器连接状态(心跳机制)
        //如果没有登陆或过期，显示游客登陆按钮
        SharedPreferences sp = getSharedPreferences("userInfo", Context.MODE_PRIVATE);

        String userId = sp.getString("userId", null);//或者是token
        int createTime = sp.getInt("createTime", 0);
        //判断帐户是不是过期
        if((System.currentTimeMillis()/1000)>(createTime+expiredTime)){
            Log.e("ttt",""+System.currentTimeMillis()/1000+"-"+createTime+"-"+expiredTime);
            SharedPreferences.Editor editor=sp.edit();
            editor.clear();
            editor.commit();
        }

        if(userId==null){
            ImageButton loginButton= (ImageButton) findViewById(R.id.login_button);
            loginButton.setVisibility(View.VISIBLE);

            //点击登陆按钮访问服务器获取游客身份信息
            loginButton.setOnClickListener(new View.OnClickListener(){
                  @Override
                  public void onClick(View v){

                      new Thread(runnable).start();
                      //Log.e("ttt",resultData);

                  }
            });
        //如果已经登陆则跳转到房间页面
        }else{
            Intent it= new Intent(MainActivity.this,MyHomeActivity.class);
            it.putExtra("name","hello!liangfang");//传递一个值到新的activity
            startActivity(it);

        }




    }
    //检查登陆，网络状态，若已登陆则跳转到房间

   /* @Override
    protected void onResume(){
        super.onResume();
        CheckNetwork checkNetwork=new CheckNetwork();
        if(!checkNetwork.isNetworkConnected(getApplicationContext())){
            show(getApplicationContext(),"网络异常，请检查网络");
        }



    }*/
    //禁用后退返回键
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return false;
    }
     //获取游客身份信息

     Handler handler =new Handler(){
         Map<String,Object> map=new HashMap();
         Map<String,Object> user=new HashMap();
         @Override
         public void handleMessage(Message msg) {
             Bundle bundle = msg.getData();
             String data = bundle.getString("resultData");//读出数据
             String netError=bundle.getString("netError");
             if(netError!=null){
                 Toast.makeText(getApplicationContext(),netError, Toast.LENGTH_SHORT).show();
                 return;
             }
             //解析json
             try {
                 JSONObject jsonObject = new JSONObject(data);

                 Iterator it = jsonObject.keys();
                 while (it.hasNext()) {
                     String key = (String) it.next();
                     String value=jsonObject.getString(key);
                     map.put(key,value);
                     if(key.equals("data")){

                         JSONObject jsobj = new JSONObject(value);
                         Iterator its=jsobj.keys();
                         while (its.hasNext()){
                             String k = (String) its.next();
                             String v=jsobj.getString(k);
                             user.put(k,v);
                         }

                     }

                     //Log.e("ddd",key+value);
                 }
             }catch(JSONException e){
                 e.printStackTrace();

             }
             //判断数据结果
             if(map.get("return_code").equals("FAIL")){
                 Toast.makeText(getApplicationContext(),map.get("return_msg").toString(), Toast.LENGTH_SHORT).show();
                 return;
             }
             //存储用户数据
             SharedPreferences sp = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
             SharedPreferences.Editor editor=sp.edit();
             editor.putString("userId",user.get("user_id").toString());
             editor.putString("picPath",user.get("pic_path").toString());
             editor.putInt("balance",Integer.parseInt(user.get("balance").toString()));
             editor.putInt("createTime",Integer.parseInt(user.get("create_time").toString()));
             editor.commit();
             //跳转
             Intent it= new Intent(MainActivity.this,MyHomeActivity.class);
             startActivity(it);
             Log.e("ddd",map.get("return_code").toString());

         };

     };
    //请求网络获取游客身份信息
    Runnable runnable = new Runnable(){
        HttpURLConnection httpUrlConnection=null;

        @Override
        public void run() {

            Bundle bundle=new Bundle();
            Message message=new Message();
            try {
                URL url = new URL(getResources().getString(R.string.get_user_url));
                httpUrlConnection = (HttpURLConnection) url.openConnection();
                httpUrlConnection.setUseCaches(false);
                //httpUrlConnection.connect();
                if(200==httpUrlConnection.getResponseCode()){
                    InputStream is=httpUrlConnection.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader bufferReader = new BufferedReader(isr);
                    String inputLine  = "";
                    while((inputLine = bufferReader.readLine()) != null){
                        resultData += inputLine + "\n";
                    }
                    bundle.putString("resultData",resultData);
                    Log.e("ddd",resultData);
                }else{

                    bundle.putString("netError","请求失败-"+httpUrlConnection.getResponseCode());
                }
                //发送消息
                message.setData(bundle);
                handler.sendMessage(message);


            }catch(MalformedURLException e){
                e.printStackTrace();
            }catch(IOException e){
                e.printStackTrace();
            }finally {
                if (httpUrlConnection != null) {
                    httpUrlConnection.disconnect();
                }
            }
        }
    };
    //重写退出
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            boolean isExit = intent.getBooleanExtra(TAG_EXIT, false);
            if (isExit) {
                this.finish();
            }
        }
    }
    //-----------公共函数有待整理

    public static void show(Context context, String info) {
        Toast.makeText(context, info, Toast.LENGTH_LONG).show();
    }
}
