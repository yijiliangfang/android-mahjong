package com.main.mahjong;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestActivity extends Activity {
    LinkedList<Card> cData;
    JSONArray jsonArray;
    int cardId;
    int countdownTimeout=10;
    Button countdownButton;
    Timer countdownTimer;
    List<String> myActiveCards =new ArrayList<String>();//活动牌列

    List<List<String>> myInactiveCardsGroup =new ArrayList<List<String>>();//非活动牌列组
    LinearLayout listUnableSelfCard,listSelfCard;
    LinearLayout listOperation;
    Boolean cardsClickable=false;//牌是否可点击

    //玩家操作倒计时
    TimerTask countdownTimerTask=new TimerTask() {
        @Override
        public void run() {
            countdownHandler.sendEmptyMessage(0);
        }
    };
    //玩家操作倒计时动态显示
    Handler countdownHandler=new Handler(){
        @Override
        public void handleMessage(Message message){
            if(countdownTimeout>0){

                countdownButton.setText((countdownTimeout--)+"");
            }else{
                //发送默认操作
                Toast.makeText(getApplicationContext(),"超时发送默认命令", Toast.LENGTH_SHORT).show();
                //清理
                countdownTimer.cancel();
                countdownTimerTask.cancel();
                countdownButton.setVisibility(View.GONE);
                listOperation.removeAllViews();
            }


        }
    };
    //玩家打牌
    private View.OnClickListener onCardClickListener=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            /*
            * {
                "cmdtype":"sockreq",
                "sockreq":"play-cards",
                "userid":124242,
                "roomid":123|0
                "cards:"["majiang_tong_2"]
              }
            * */
            String cardId=view.getId()+"";
            String sendCardJsonReq="{\"cmdtype\":\"sockreq\"," +
                    "\"sockreq\":\"play-cards\"," +
                    "\"userid\":111," +
                    "\"roomid\":888," +
                    "\"cards:\"[\""+cardId+"\"]}";
            //增加消息队列等待发出
            //mClientSocket.addSendMsgToQueue(sendCardJsonReq);
            //Toast.makeText(getApplicationContext(),view.getId()+"", Toast.LENGTH_SHORT).show();
            //删除活动牌中的该牌并重排不可点击
            removeCard(cardId);
            cardsClickable=false;
            displayCardsList(myActiveCards,listSelfCard,false);
            //计时器关闭
            countdownTimer.cancel();
            countdownTimerTask.cancel();
            countdownButton.setVisibility(View.GONE);
            Log.v("playerTag", "##  发送打牌/出牌成功"+cardId);
        }
    };

    //玩家操作
    private View.OnClickListener onActionClickListener=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            /*
            * {
                "cmdtype":"sockreq",
                "sockreq":"exe-cmd",  //执行命令
                "userid":124122,
                "roomid",123|0,
                "cmd":"peng", //此命令来自服务器推送的命令列表
                "cmd-data":"" //命令数据，预留 当cmd为hu, peng, gang,时cmd-data须为服务器push的牌点数
            }
            *
            * */

            Map<String,String> playerOperateMap=new HashMap();
            playerOperateMap=(Map<String,String>)view.getTag();
            Iterator entries = playerOperateMap.entrySet().iterator();
            String key=null;
            String value=null;
            while (entries.hasNext()) {

                Map.Entry entry = (Map.Entry) entries.next();
                key = (String)entry.getKey();
                value = (String)entry.getValue();
            }
            String sendPlayerActionJsonReq="{\"cmdtype\":\"sockreq\"," +
                    "\"sockreq\":\"exe-cmd\"," +
                    "\"userid\":111," +
                    "\"roomid\",888," +
                    "\"cmd\":\""+key+"\"," +
                    "\"cmd-data\":\"\" }";
            Log.v("playerTag", "##  发送操作命令成功："+sendPlayerActionJsonReq);
            //如果碰牌则重新排列牌序且设置碰牌不能点击
            if(key.equals("peng") || key.equals("gang")){
                setMyInactiveCardsGroup(value,key);
                displayCardsList(myInactiveCardsGroup,listUnableSelfCard,true);

                cardsClickable=true;
                displayCardsList(myActiveCards,listSelfCard,false);
                //清除操作列表按钮
                listOperation.removeAllViews();
                Log.v("playerTag", "##  牌符："+value);
            }

            if(key.equals("guo")){
                countdownTimer.cancel();
                countdownTimerTask.cancel();
                countdownButton.setVisibility(View.GONE);
                listOperation.removeAllViews();
            }

            //Log.v("playerTag", "##  玩家操作成功："+key);
            //Toast.makeText(getApplicationContext(),"玩家操作成功", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        String jsonStr="[{\"cmd\": \"peng\", \"cmd-param\": [16]},{\"cmd\": \"guo\", \"cmd-param\": null}]";
        listUnableSelfCard= (LinearLayout)findViewById(R.id.list_self_inactive_card);
        listSelfCard= (LinearLayout)findViewById(R.id.list_self_active_card);
        listOperation= (LinearLayout)findViewById(R.id.operate_list);
        countdownButton=(Button) findViewById(R.id.my_countdown);
        Button aaa=(Button) findViewById(R.id.aaa);
        myActiveCards.add("11");
        myActiveCards.add("12");
        myActiveCards.add("13");
        myActiveCards.add("16");
        myActiveCards.add("16");
        myActiveCards.add("17");
        aaa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message message = Message.obtain();
                message.obj = "[11,12,13,14,15,16,17,18,19,11,11,13,14]";
                mHandler.sendMessage(message);
            }
        });

        String pattern = "LXQ<\\(:([\\w\\W]*?):\\)>QXL";
        String line = "LXQ<(:aaabbbccc:)>QXL";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(line);
        while (m.find()){
            Log.v("jsonTag", "## "+m.group(1));
        }

        /*
        displayCardsList(myActiveCards,listSelfCard,false);
        try{
            JSONArray jsonArray =new JSONArray(jsonStr);

            for(int i=0;i<jsonArray.length();i++){
                JSONObject jsonObject =jsonArray.getJSONObject(i);
                Map<String,String> cmdOperateMap=new HashMap();
                //String[] cmdParamArr;
                String cmd=jsonObject.getString("cmd");
                String cmdParam=jsonObject.getString("cmd-param");


                cmdOperateMap.put(cmd,cmdParam);
                Button button=new Button(this);
                button.setText(cmd);//待处理成文字
                button.setTag(cmdOperateMap);
                button.setOnClickListener(this.onActionClickListener);
                listOperation.addView(button);

            }



        }catch (JSONException e){
            e.printStackTrace();
            Log.v("jsonTag", "## "+e.getMessage());
        }

        //计时器
        countdownTimeout=30;
        if(countdownTimeout>0){
            countdownTimer=new Timer();
            countdownTimer.schedule(countdownTimerTask,0,1000);
        }

        Log.v("playerTag", "##  收到玩家可操作动作:");

        */



    }
    Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            cardsClickable=false;
            displayCardsList(myActiveCards,listSelfCard,false);
        }
    };

    public  void toast(Context context, String info) {
        Toast.makeText(context, info, Toast.LENGTH_LONG).show();
    }

    //设置玩家非活动牌列
    private void setMyInactiveCardsGroup(String cardJsonStr,String operateTag){
        List<String> myInactiveCards =new ArrayList<String>();//非活动牌列
        try {
            JSONArray jsonArray =new JSONArray(cardJsonStr);
            int length=jsonArray.length();

            for(int i=0;i<length;i++){
                myInactiveCards.add(jsonArray.getString(i));
            }
            //碰或杠
            if(operateTag.equals("peng") || operateTag.equals("gang")){
                String pengCard=jsonArray.getString(0);
                Iterator<String> sListIterator = myActiveCards.iterator();
                while(sListIterator.hasNext()){
                    String e = sListIterator.next();
                    if(e.equals(pengCard)){
                        sListIterator.remove();
                        myInactiveCards.add(pengCard);
                    }
                }

            }
            //吃牌
            myInactiveCardsGroup.add(myInactiveCards);


        }catch (JSONException e){
            e.printStackTrace();
            Log.v("jsonTag", "## "+e.getMessage());
        }
    }
    //设置活动牌列
    private void setMyActiveCards(String cardJsonStr){
        //cardJsonStr=["19","12","16","24","35"]
        //myCards=["12","16","19","24","35"]<-["11"]
        try {
            JSONArray jsonArray =new JSONArray(cardJsonStr);
            int length=jsonArray.length();

            for(int i=0;i<length;i++){
                myActiveCards.add(jsonArray.getString(i));

            }

            int size=myActiveCards.size();

            //冒泡排序
            int temp;
            for(int i=0;i<size-1;i++){
                for(int j=0;j<size-1-i;j++){
                    int current=Integer.parseInt((String)myActiveCards.get(j));
                    int next=Integer.parseInt((String)myActiveCards.get(j+1));
                    if(current>next){
                        temp=current;
                        myActiveCards.set(j, next+"");
                        myActiveCards.set(j+1, temp+"");
                    }
                }
            }

        }catch (JSONException e){
            e.printStackTrace();
            Log.v("jsonTag", "## "+e.getMessage());
        }

    }

    private boolean displayCardsList(List<?> myCards,LinearLayout myView,boolean disClickable){
        //如果不能点击则不能点击
        boolean cClickable=(disClickable)?false:cardsClickable;

        //列牌

        Map<String,String> cardMap=getCardName();
        int size=myCards.size();
        if(size==0){
            return false;
        }
        myView.removeAllViews();
        if(myActiveCards.size()>0 && myCards.get(0).getClass()==myActiveCards.get(0).getClass()){
            for(int i=0;i<size;i++){
                Button button =new Button(this);
                //button.setText(jsonArray.get(i)+"");
                String key=myCards.get(i)+"";
                button.setText(cardMap.get(key));
                button.setClickable(cClickable);
                button.setId(Integer.parseInt(key));
                //cardId=Integer.parseInt(jsonArray.get(i).toString());
                if(cClickable){
                    button.setOnClickListener(this.onCardClickListener);//打牌触发
                }

                myView.addView(button);

            }
        }
        if(myInactiveCardsGroup.size()>0 && myCards.get(0).getClass()==myInactiveCardsGroup.get(0).getClass()){
            for(int i=0;i<size;i++){
                LinearLayout linearLayout=new LinearLayout(this);
                linearLayout.setBackgroundColor(Color.parseColor("#666666"));
                myView.addView(linearLayout);
                List<String> myInactiveCards=(List<String>)myCards.get(i);
                int max=myInactiveCards.size();
                for(int j=0;j<max;j++){
                    Button button =new Button(this);
                    //button.setText(jsonArray.get(i)+"");
                    String key=myInactiveCards.get(i)+"";
                    button.setText(cardMap.get(key));
                    button.setClickable(cClickable);
                    button.setId(Integer.parseInt(key));
                    if(cClickable){
                        button.setOnClickListener(this.onCardClickListener);//打牌触发
                    }

                    linearLayout.addView(button);
                }
            }
        }

        return true;


    }
    //从列表种移除一张牌
    public void removeCard(String cardNo){
        int size=myActiveCards.size();
        Iterator<String> sListIterator = myActiveCards.iterator();
        while(sListIterator.hasNext()){
            String e = sListIterator.next();
            if(e.equals(cardNo)){
                sListIterator.remove();
            }
        }
    }
    //获取牌面名字
    public Map getCardName(){
        Resources res =getResources();
        String[] cards=res.getStringArray(R.array.cards);
        Map<String,String> map=new HashMap();
        for(int i=0;i<cards.length;i++){
            String[] s=cards[i].split("\\|");

            String key=s[0];
            String value=s[1];
            //Log.v("playerTag",key+value+cards[i]);
            map.put(key,value);
        }
        return map;

    }
}
