package com.main.mahjong;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.main.mahjong.util.GetNetImageTask;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MyHomeActivity extends Activity implements AdapterView.OnItemClickListener{
    private List<Room> rData;
    private ListView listitem;
    private RoomListAdapter roomListAdapter;
    private EditText baseScore;
    private AlertDialog dialog;
    private int score;
    private HttpURLConnection httpUrlConnection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_home);
        //获取用户信息
        SharedPreferences sp = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        String userId = sp.getString("userId", null);//或者是token
        String picPath = sp.getString("picPath", null);
        int balance = sp.getInt("balance", 0);
        int createTime = sp.getInt("createTime", 0);
        TextView t_userid=(TextView) this.findViewById(R.id.userId);
        TextView t_userbalance=(TextView)this.findViewById(R.id.userBalance);
        ImageView headPortrait=(ImageView) this.findViewById(R.id.head_portrait);
        //赋值
        new GetNetImageTask(headPortrait,picPath).execute();
        t_userid.setText("ID:"+userId);
        t_userbalance.setText("余额:"+balance);


        listitem =(ListView)findViewById(R.id.list_room);
        //点击房间列表到指定的房间页面
        listitem.setOnItemClickListener(this);
        //要显示的数据（网络请求获取）
        new Thread(getRoomList).start();



    }
    //点击邀请，发送微信好友邀请链接
    private View.OnClickListener onClickListener=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Toast.makeText(getApplicationContext(),"待开发", Toast.LENGTH_SHORT).show();
        }
    };
    //点击房间列表到房间
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        //Toast.makeText(getApplicationContext(),"dddddd", Toast.LENGTH_SHORT).show();
        Intent it= new Intent(MyHomeActivity.this,GameRoomActivity.class);
        //it.putExtra("rId",rData.get(i).getrId());//传递一个值到新的activity

        //模拟三个玩家
        String[] myPlayer=new String[3];
        myPlayer[0]="{\"cmdtype\":\"sockreq\",\"sockreq\":\"join-game\",\"userid\":111,\"roomid\":888,\"gameid\":\"m1\"}";
        myPlayer[1]="{\"cmdtype\":\"sockreq\",\"sockreq\":\"join-game\",\"userid\":222,\"roomid\":888,\"gameid\":\"m1\"}";
        myPlayer[2]="{\"cmdtype\":\"sockreq\",\"sockreq\":\"join-game\",\"userid\":333,\"roomid\":888,\"gameid\":\"m1\"}";
        it.putExtra("myPlayer",myPlayer[i]);//传递一个值到新的activity
        it.putExtra("roomId","888");
        startActivity(it);
    }


    //点击“创建房间”触发
    public void createRoomClick(View view){
        View layout = LayoutInflater.from(this).inflate(R.layout.alert_create_room,null);

        final AlertDialog.Builder builder =new AlertDialog.Builder(MyHomeActivity.this,R.style.AlertDialog);
        builder.setView(layout);
        builder.setCancelable(false);
        dialog=builder.show();
        //关闭弹窗
        Button close_btn=(Button)layout.findViewById(R.id.layout_close_button);

        close_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                    dialog.dismiss();
                }


        });
        //服务器创建房间
        Button confirm_btn=(Button)layout.findViewById(R.id.layout_confirm_button);
        confirm_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //开启线程访问网络接口
                Toast.makeText(getApplicationContext(),"dddd", Toast.LENGTH_SHORT).show();
            }
        });

        //增加底分
        Button add_btn=(Button)layout.findViewById(R.id.add_score);
        baseScore=(EditText)layout.findViewById(R.id.baseScore);
        score= Integer.parseInt(baseScore.getText().toString());
        add_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Toast.makeText(getApplicationContext(),score+1+"", Toast.LENGTH_SHORT).show();
                //new Thread(runnable).start();
                if(score<5){
                    score=score+1;
                    baseScore.setText(score+"");
                }


            }
        });

        //减少底分
        Button lower_btn=(Button)layout.findViewById(R.id.lower_score);
        lower_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(score>1){
                    score=score-1;
                    baseScore.setText(score+"");
                }
            }
        });
        //Toast.makeText(getApplicationContext(),"dddd", Toast.LENGTH_SHORT).show();

    }

    //点击进入房间按钮
    public void joinRoomClick(View view){
        Intent it= new Intent(MyHomeActivity.this,TestActivity.class);
        //startActivity(it);
        startActivity(it);
        //Toast.makeText(getApplicationContext(),"dddd", Toast.LENGTH_SHORT).show();

    }

    //禁用后退返回键
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return false;
    }

    //处理房间请求数据
    Handler roomListHandler=new Handler(){
        Map<String,Object> resultMap=new HashMap();

        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String data = bundle.getString("resultData");//读出数据
            String netError=bundle.getString("netError");
            rData=new LinkedList<Room>();
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
                    resultMap.put(key,value);
                    if(key.equals("data")){

                        JSONObject jsobj = new JSONObject(value);
                        Iterator its=jsobj.keys();
                        while (its.hasNext()){

                            String k = (String) its.next();
                            String v=jsobj.getString(k);
                            JSONObject jo=new JSONObject(v);
                            //Log.e("ddd",jo.getLong("room_id")+"");
                            rData.add(new Room(
                                    jo.getLong("room_id"),
                                    jo.getInt("base_score"),
                                    jo.getInt("round_number"),
                                    jo.getInt("palyer_number")
                            ));

                            //roomData.put(k,v);
                        }

                    }


                }
            }catch(JSONException e){
                e.printStackTrace();

            }
            //判断数据结果
            if(resultMap.get("return_code").equals("FAIL")){
                Toast.makeText(getApplicationContext(),resultMap.get("return_msg").toString(), Toast.LENGTH_SHORT).show();
                return;
            }
            roomListAdapter=new RoomListAdapter((LinkedList<Room>) rData,onClickListener,MyHomeActivity.this);
            listitem.setAdapter(roomListAdapter);

        }
    };
    //获取房间列表
   Runnable getRoomList=new Runnable() {
       @Override
       public void run() {

           String resultData="";
           Bundle bundle=new Bundle();
           Message message=new Message();

           try {
               URL url = new URL(getResources().getString(R.string.get_room_url));
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
               roomListHandler.sendMessage(message);


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
}
