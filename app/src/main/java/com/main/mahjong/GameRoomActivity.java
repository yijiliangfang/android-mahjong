package com.main.mahjong;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.main.mahjong.util.ClientSocketThread;
import com.main.mahjong.util.ClientSocketThread.OnSocketRecieveCallBack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameRoomActivity extends Activity implements OnSocketRecieveCallBack {

    public static final String SERVER_NAME = "192.168.2.55";
    public static final int PORT = 9229;
    TextView show;
    Button testBtn;
    EditText myedit;

    Button selfSeat,leftSeat,rightSeat;
    Button selfBanker,leftBanker,rightBanker;
    Button selfDeal,leftDeal,rightDeal;
    Button leftOperate,rightOperate;
    Button roboterBtn;
    LinearLayout listUnableSelfCard,listSelfCard,listOppositeCard;
    LinearLayout listOperation;
    Button countdownButton;
    Timer countdownTimer;
    String userId;
    String roomId;
    int countdownTimeout=30;//玩家操作超时时间，单位：秒 默认10秒
    boolean cardsClickable=false;//牌是否可点击
    String roboterSwitch="yes";

    Map<Object,String> seatOrder= new HashMap();//座位顺序
    List<String> myActiveCards =new ArrayList<String>();//活动牌列
    List<List<String>> myInactiveCardsGroup =new ArrayList<List<String>>();//非活动牌列组
    ClientSocketThread mClientSocket;

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

                //小于10秒时候提示用户
                if(countdownTimeout<=10){
                    countdownButton.setVisibility(View.VISIBLE);
                    countdownButton.setText((countdownTimeout--)+"");
                }

            }else{
                //发送默认操作

                //清理计时器，任务，记时按钮，操作列表
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
                "cards:"[11]
              }
            * */
            String cardId=view.getId()+"";
            String sendCardJsonReq="{\"cmdtype\":\"sockreq\"," +
                    "\"sockreq\":\"play-cards\"," +
                    "\"userid\":"+userId+"," +
                    "\"roomid\":"+roomId+"," +
                    "\"cards\":["+cardId+"]}";
            //增加消息队列等待发出
            mClientSocket.addSendMsgToQueue(sendCardJsonReq);
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
                "roomid",123,
                "token":"YMQV7AJWINUMJCM4AIOQ5HTW2PDGHJQN"
                "cmd":"peng", //此命令来自服务器推送的命令列表
                "cmd-param":[13] //命令数据，预留 当cmd为hu, peng, gang,时cmd-data须为服务器push的牌点数
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
                    "\"userid\":"+userId+"," +
                    "\"roomid\","+roomId+"," +
                    "\"cmd\":\""+key+"\"," +
                    "\"cmd-param\":"+value+"}";
            mClientSocket.addSendMsgToQueue(sendPlayerActionJsonReq);
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
            if(key.equals("hu") || key.equals("zi mao")){

                Log.v("playerTag", "##  胡啦啦："+value);
            }
            Log.v("playerTag", "##  玩家操作成功："+key);
            //Toast.makeText(getApplicationContext(),"玩家操作成功", Toast.LENGTH_SHORT).show();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_room);

        //测试按钮
        testBtn=(Button) findViewById(R.id.test_btn);
        listOppositeCard= (LinearLayout)findViewById(R.id.opposite_player);

        //入座情况
        selfSeat=(Button) findViewById(R.id.self_seat);
        leftSeat=(Button) findViewById(R.id.left_seat);
        rightSeat=(Button) findViewById(R.id.right_seat);
        //庄家情况
        selfBanker=(Button) findViewById(R.id.self_banker);
        leftBanker=(Button) findViewById(R.id.left_banker);
        rightBanker=(Button) findViewById(R.id.right_banker);

        //各方打出牌显示
        selfDeal=(Button) findViewById(R.id.self_deal);
        leftDeal=(Button) findViewById(R.id.left_deal);
        rightDeal=(Button) findViewById(R.id.right_deal);
        //各方操作动作显示
        leftOperate=(Button) findViewById(R.id.left_operate);
        rightOperate=(Button) findViewById(R.id.right_operate);
        //自己的手里的牌列
        listUnableSelfCard= (LinearLayout)findViewById(R.id.list_self_inactive_card);
        listSelfCard= (LinearLayout)findViewById(R.id.list_self_active_card);
        //可操作动作列
        listOperation= (LinearLayout)findViewById(R.id.operate_list);
        //玩家倒计时
        countdownButton=(Button) findViewById(R.id.my_countdown);
        //玩家托管
        roboterBtn=(Button)findViewById(R.id.roboter);

        //开始socket 长连接
        mClientSocket = new ClientSocketThread(SERVER_NAME, PORT);
        mClientSocket.setOnSocketRecieveCallBack(this);
        mClientSocket.start();

        //请求入座
        Intent it=getIntent();
        String joinGameJsonReq=it.getStringExtra("myPlayer");
        roomId=it.getStringExtra("roomId");
        //Log.v("socketTag", "## msg connect : " + joinGameJsonReq.toString());
        //进入界面直接请求入座
        if(joinGameJsonReq != null){// && mClientSocket.isSocketConnected()
            mClientSocket.addSendMsgToQueue(joinGameJsonReq);
            Log.v("socketTag", "## add message queue Successed" );
        }
        //获取用户信息
        SharedPreferences sp = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        //userId = sp.getString("userId", null);//或者是token
        userId="111";
        //String picPath = sp.getString("picPath", null);
        //int balance = sp.getInt("balance", 0);
        //int createTime = sp.getInt("createTime", 0);


        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setMyActiveCards("[13,15,16,11,22]");
                displayCardsList(myActiveCards,listOppositeCard,false);
            }
        });




    }



    //玩家托管开关
    public void OnClickRoboter(View view){
        //roboterSwitch

        String sendRoboterJsonReq="{\"cmdtype\":\"sockreq\"," +
                "\"sockreq\":\"robot-play\"," +
                "\"robot-play\":\""+roboterSwitch+"\"," +
                "\"userid\":"+userId+"," +
                "\"Roomid\":"+roomId+"}";
        mClientSocket.addSendMsgToQueue(sendRoboterJsonReq);

        Log.v("playerTag", "##  发送玩家托管成功："+sendRoboterJsonReq);
    }
    /* 再次换新界面的时候重连
     *  {
        "cmdtype":"sockreq",
        "sockreq":"reconnect",
        "userid":123456,
        "gameid":324243,
        "roomid":324242
        }
    * */

    /*@Override
    protected void onResume(){
        super.onResume();
        String sendReconnectJsonReq="{\"cmdtype\":\"sockreq\"," +
                "\"sockreq\":\"reconnect\"," +
                "\"userid\":"+userId+"," +
                "\"gameid\":\"m1\"," +
                "\"roomid\":"+roomId+"}";
        mClientSocket.addSendMsgToQueue(sendReconnectJsonReq);
        Log.v("playerTag", "##  发送服务器重连命令成功："+sendReconnectJsonReq);

    }*/
//======================================接收到消息的各种处理=================================

    //收到服务器的信息显示
    Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {

            String jsonStr=msg.obj.toString();
            Map<String,Object> map=new HashMap();


            /*
            *    解析json 数据
            * */
            try{
                JSONObject jsonObject=new JSONObject(jsonStr);
                Iterator it=jsonObject.keys();
                while(it.hasNext()){
                    String key = (String) it.next();
                    String value=jsonObject.getString(key);
                    map.put(key,value);
                }


            }catch(JSONException e){
                e.printStackTrace();
                Log.v("jsonTag", e.getMessage());
            }
            //Log.v("playerTag", "##  ----------> " + map.get("sockpush"));
            Log.v("ValueTag", "## 是否能出牌-------------->"+cardsClickable);
            /*
            * 入座
            * {
                "cmdtype":"sockresp"
                "sockresp":"join-game",
                "result":"OK"
                "errmsg":""
                "result-data":{"state":""}
              }
            *说明：成功时：state 取值：“等待新玩家“，”组局成功，马上开始“，”智能匹配玩家“ 等
            * */
            if(map.get("cmdtype").equals("sockresp") && map.get("sockresp").equals("join-game")){

                if(map.get("result").equals("OK")){
                    selfSeat.setText("已座");
                    seatOrder.put(userId,"self");
                    //Log.v("playerTag", "##  收到入座成功");
                }else{
                    //toast(getApplicationContext(),map.get("errmsg").toString());
                    //bundle.putString("toast",map.get("errmsg").toString());
                    Log.v("playerTag", "##  收到入座失败:"+map.get("errmsg").toString());
                }

                Log.v("playerTag", "##  收到入座请求返回:"+map.get("result").toString());
            }

            /*
            * 组局成功：玩家信息
            *
            * {
                "cmdtype":"sockpush",
                "sockpush":"game-players",
                "players": [{"userid": 111}, {"userid": 222}, {"userid": 333}] //玩家按照入场顺序排序
               }
            * */

            if(map.get("cmdtype").equals("sockpush") && map.get("sockpush").equals("game-players")){
                //从列表找到玩家本身，如果没有入座则入座，从列表中把玩家本身之外的小伙伴入座到左右两边
                //userId
                //String[] seatPosition={"slef","right","left"};
                //Object[] otherUserid=new String[2];

                //List linkedList =new LinkedList();
                //ArrayList arrayList =new ArrayList();
                //ArrayMap arrayMap=new ArrayMap();
                //{1=left, 3=self, 7=right}
                try{
                    JSONArray jsonArray=new JSONArray(map.get("players").toString());
                    int count=jsonArray.length();
                    String[] userIdArr=new String[count];
                    //解析用户json
                    for(int i=0;i<count;i++){
                        JSONObject jsonObject=jsonArray.getJSONObject(i);
                        Iterator it=jsonObject.keys();
                        while (it.hasNext()){
                            String key=(String)it.next();
                            String value=jsonObject.get(key).toString();
                            if(key.equals("userid")){
                                userIdArr[i]=value;
                            }
                        }
                    }
                    //按排座位次序

                    /*if(count==1){
                        seatOrder.put(userIdArr[0],"self");
                        Log.v("playerTag", "##  收到第一次组局:"+userIdArr[0]);
                    }*/

                    if(count==2){
                        if(userIdArr[0].equals(userId)){
                            seatOrder.put(userIdArr[1],"right");
                            rightSeat.setText("已座");
                        }
                        if(userIdArr[1].equals(userId)){

                            seatOrder.put(userIdArr[0],"left");
                            leftSeat.setText("已坐");
                        }
                        //Log.v("playerTag", "##  收到第二次组局:"+userIdArr[0]+"-"+userIdArr[1]);
                    }
                    if(count==3){
                        if(userIdArr[0].equals(userId)){
                            seatOrder.put(userIdArr[2],"left");
                            leftSeat.setText("已坐");
                        }
                        if(userIdArr[1].equals(userId)){
                            seatOrder.put(userIdArr[2],"right");
                            rightSeat.setText("已坐");
                        }
                        if(userIdArr[2].equals(userId)){

                            seatOrder.put(userIdArr[0],"right");
                            seatOrder.put(userIdArr[1],"left");
                            leftSeat.setText("已坐");
                            rightSeat.setText("已坐");
                        }
                        //Log.v("playerTag", "##  收到第三次组局:"+userIdArr[0]+"-"+userIdArr[1]+"-"+userIdArr[2]);
                    }

                    /*for(int i=0;i<userIdArr.length;i++){
                        if(userIdArr[i].equals(userId)){
                            //自己
                            seatOrder.put(userId,"self");
                            //selfSeat.setText("已座");
                            //确定下家
                            if((i+1)<max){

                                seatOrder.put(userIdArr[i+1],"right");
                            }else{
                                seatOrder.put(userIdArr[0],"right");
                            }

                            //确定上家
                            if((i-1)>=0){

                                seatOrder.put(userIdArr[i-1],"left");
                            }else{
                                seatOrder.put(userIdArr[max-1],"left");
                            }
                        }
                    }*/
                }catch (JSONException e){
                    e.printStackTrace();
                    Log.v("jsonTag", "## "+e.getMessage());
                }


                Log.v("playerTag", "##  收到的玩家列表:"+map.get("players").toString());
            }

            /*
            * 定庄 {"cmdtype":"sockpush","sockpush":"new-banker","userid":122443}
            * */

            if(map.get("cmdtype").equals("sockpush") && map.get("sockpush").equals("new-banker")){

                String newBanker=seatOrder.get(map.get("userid").toString());
                if(newBanker.equals("left")){
                    leftBanker.setText("庄");
                    leftBanker.setVisibility(View.VISIBLE);

                }
                if(newBanker.equals("right")){
                    rightBanker.setText("庄");
                    rightBanker.setVisibility(View.VISIBLE);
                }
                if(newBanker.equals("self")){
                    selfBanker.setText("庄");
                    selfBanker.setVisibility(View.VISIBLE);
                    cardsClickable=true;//庄家可以点击
                }

                Log.v("playerTag", "##  收到的新庄家:"+map.get("userid").toString());
            }

            /*
            * 发牌/摸牌 {"cmdtype":"sockpush","sockpush":"deal-cards","cards":[11，12，15，18,13]}
            * */
            if(map.get("cmdtype").equals("sockpush") && map.get("sockpush").equals("deal-cards")){

                String cardJsonStr=map.get("cards").toString();

                setMyActiveCards(cardJsonStr);
                displayCardsList(myActiveCards,listSelfCard,false);

                Log.v("playerTag", "##  收到的发牌数据:"+map.get("cards").toString());
            }

            /*
             *   玩家操作命令列表
             *   {
             *       "cmdtype":"sockpush",
             *       "sockpush":"cmd-opts",
             *       "cmd-opts":
             *       [
                        {"cmd": "peng", "cmd-param": [14]},
                        {"cmd": "guo", "cmd-param": null}
		             ],
             *       "resp-timeout":30 //用户响应超时时间，单位：秒，值为-1时为没有超时，一直等待用户响应
             *       "def-cmd":"hu",
             *       "cmd-param":""   //当def-opt为chupai时，此参数为使用逗号分隔的牌
                }
            * */
            if(map.get("cmdtype").equals("sockpush") && map.get("sockpush").equals("cmd-opts")){

                try{
                    JSONArray jsonArray =new JSONArray(map.get("cmd-opts").toString());

                    for(int i=0;i<jsonArray.length();i++){
                        JSONObject jsonObject =jsonArray.getJSONObject(i);
                        Map<String,String> cmdOperateMap=new HashMap();
                        String cmd=jsonObject.getString("cmd");
                        String cmdParam=jsonObject.getString("cmd-param");

                        cmdOperateMap.put(cmd,cmdParam);
                        Button button=new Button(GameRoomActivity.this);
                        button.setText(cmd);//待处理成文字
                        button.setTag(cmdOperateMap);
                        button.setOnClickListener(onActionClickListener);
                        listOperation.addView(button);
                    }

                }catch (JSONException e){
                    e.printStackTrace();
                    Log.v("jsonTag", "## "+e.getMessage());
                }

                //计时器
                int serverTimeout=Integer.parseInt(map.get("resp-timeout").toString());
                if(serverTimeout>0){
                    countdownTimeout=serverTimeout;
                }
                if(countdownTimeout>0){

                    countdownTimer=new Timer();
                    countdownTimer.schedule(countdownTimerTask,0,1000);
                }

                Log.v("playerTag", "##  收到玩家可操作动作:"+map.get("cmd-opts").toString());
            }

            /*
            * 玩家出牌，此用于通知其他玩家
            *{
            *    "cmdtype":"sockpush",
            *    "sockpush":"play-cards",
            *    "cards":["majiang_wan_3"],
            *    "player-state":"normal|offline|robot-play" //玩家状态，normal:正常，offline：离线，robot-play:机器人托管，normal与offline是互斥出现的，robot-play可与normal或offline组合出现，中间以竖线“|”分隔。
            *    "userid":11234.  #出牌玩家的userid
            }
            * */
            if(map.get("cmdtype").equals("sockpush") && map.get("sockpush").equals("play-cards")){
                String playerPosition=seatOrder.get(map.get("userid").toString());
                String card=map.get("cards").toString();
                if(playerPosition.equals("left")){
                    leftDeal.setVisibility(View.VISIBLE);
                    leftDeal.setText(card);
                }
                if(playerPosition.equals("right")){
                    rightDeal.setVisibility(View.VISIBLE);
                    rightDeal.setText(card);
                }
                if(playerPosition.equals("self")){
                    selfDeal.setVisibility(View.VISIBLE);
                    selfDeal.setText(card);
                }
                //玩家状态提醒（待开发）

                Log.v("playerTag", "##  收到其他玩家打出的牌:"+map.get("cards").toString());
            }

           /*
            * 一局游戏结束，广播胜负各方
            *{
            *    "cmdtype":"sockpush",
            *    "sockpush":"game-result",
            *    "winners":[{"userid":1224, "score":12},]
            *    "losers":[{"userid":122,"score":12}]
            }
            * */
            if(map.get("cmdtype").equals("sockpush") && map.get("sockpush").equals("game-result")){
                String showStr=null;
                try{
                    JSONArray winJsonArray=new JSONArray(map.get("winners").toString());
                    JSONArray losJsonArray=new JSONArray(map.get("losers").toString());
                    //map playerInfo("111",new player())
                    //playerInfo.get("111").getName();
                    for(int i=0;i<winJsonArray.length();i++){
                        JSONObject jsonObject=winJsonArray.getJSONObject(i);
                        showStr+="胜："+jsonObject.get("userid")+"，"+jsonObject.get("score")+"分/n";
                    }
                    for(int i=0;i<losJsonArray.length();i++){
                        JSONObject jsonObject=losJsonArray.getJSONObject(i);
                        showStr+="负："+jsonObject.get("userid")+"，"+jsonObject.get("score")+"分/n";
                    }
                    //后期优化弹窗

                    toast(getApplicationContext(), showStr);

                }catch (JSONException e){
                    e.printStackTrace();
                    Log.v("jsonTag", "## "+e.getMessage());
                }
                Log.v("playerTag", "##  胜负结局:"+map.get("winners").toString());
            }

           /*
            * 一轮8局结束，广播各玩家累积的分数
            {
                "cmdtype":"sockpush",
                "sockpush":"scores",
                "scores":[{"userid":1224, "score":12},{"userid":1223, "score":-12},]
            }
            * */
            if(map.get("cmdtype").equals("sockpush") && map.get("sockpush").equals("scores")){
                String showStr=null;
                try{
                    JSONArray scoresJsonArray=new JSONArray(map.get("scores").toString());
                    for (int i=0;i<scoresJsonArray.length();i++){
                        JSONObject jsonObject=scoresJsonArray.getJSONObject(i);
                        showStr+=jsonObject.get("userid")+", "+jsonObject.get("score")+"分/n";
                    }
                    toast(getApplicationContext(), showStr);

                }catch (JSONException e){
                    e.printStackTrace();
                    Log.v("jsonTag", "## "+e.getMessage());
                }
                Log.v("playerTag", "##  本轮牌局积分:"+map.get("scores").toString());
            }


           /*
            * 广播玩家的操作
            {
                "cmdtype":"sockpush",
                "sockpush":"exed-cmd",
                "exed-cmd":"hu"
                "cmd-param":null
                "userid":123, #执行操作的用户ID
            }
            * */
            if(map.get("cmdtype").equals("sockpush") && map.get("sockpush").equals("exed-cmd")){
                String playerPosition=seatOrder.get(map.get("userid").toString());
                String exedCmd=map.get("exed-cmd").toString();
                if(playerPosition.equals("left")){
                    leftOperate.setVisibility(View.VISIBLE);
                    leftOperate.setText(exedCmd);//待完善动画或其他
                }
                if(playerPosition.equals("right")){
                    rightOperate.setVisibility(View.VISIBLE);
                    rightOperate.setText(exedCmd);
                }

                Log.v("playerTag", "##  其他玩家做出的操作:"+map.get("exed-cmd").toString());
            }

           /*
            * 广播游戏的状态
            {
                "cmdtype":"sockpush",
                "sockpush":"game-status",
                "game-status":""
                "state-data":"1". #state-data作为扩展使用，预留
            }
            * */
            if(map.get("cmdtype").equals("sockpush") && map.get("sockpush").equals("game-status")){

                Log.v("playerTag", "##  收到游戏的状态:"+map.get("game-status").toString());
            }

           /*
            * 玩家设置机器人托管
            {
                "cmdtype":"sockresp"
                "sockresp":"robot-play",
                "result":"OK"
                "errmsg":""
                "result-data":{"player-state":"normal|offline|robot-play"} #player-state: 玩家状态，normal:正常，offline：离线，robot-play:机器人托管，normal与offline是互斥出现的，robot-play可与normal或offline组合出现，中间以竖线“|”分隔。

            }
            * */
            if(map.get("cmdtype").equals("sockresp") && map.get("sockresp").equals("robot-play")){
                String result=map.get("result").toString();
                String roboterBtnText=(roboterSwitch.equals("yes"))?"取消托管":"托管";
                if(result.equals("OK")){
                    roboterBtn.setText(roboterBtnText);
                    roboterSwitch=(roboterSwitch.equals("yes"))?"no":"yes";
                    //玩家不可有任何操作？
                }
                if(result.equals("ERROR")){
                    toast(getApplicationContext(), map.get("errmsg").toString());
                }

                Log.v("playerTag", "##  收到玩家托管请求"+map.get("result").toString());
            }

        }
    };

    /*
     *  接收到消息的回调处理方法
     *  部分是否需要做到新的线程中？
     */

    @Override
    public void OnRecieveFromServerMsg(String msg) {
        Log.v("playerTag", "##  客户端收到的信息: " + msg);
        //正则过滤字符
        String pattern = "LXQ<\\(:([\\w\\W]*?):\\)>QXL";
        //String line = "LXQ<(:aaabbbccc:)>QXL";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(msg);
        while (m.find()){
            Log.v("jsonTag", "## 正则的JSON:"+m.group(1));
            String jsonStr=m.group(1);
            Message message = Message.obtain();
            message.obj = jsonStr;
            mHandler.sendMessage(message);
        }



    }

//======================================公共的方法待整理到牌操作类中=================================
    /*
     *  相关牌操作的方法
     *  有待整理到牌操作类中
     */

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
        //cardJsonStr=[11,12,13,14,15,16,17,18,19,11,11,13,14]
        //myCards=["12","16","19","24","35"]<-["11"]
        try {
            JSONArray jsonArray =new JSONArray(cardJsonStr);
            int length=jsonArray.length();
            if(length==1){
                cardsClickable=true;
            }
            for(int i=0;i<length;i++){
                myActiveCards.add(jsonArray.get(i)+"");

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
                Button button =new Button(GameRoomActivity.this);
                //button.setText(jsonArray.get(i)+"");
                String key=myCards.get(i)+"";
                button.setText(cardMap.get(key));
                button.setClickable(cClickable);
                //button.setEnabled(cClickable);
                button.setId(Integer.parseInt(key));
                button.setBackgroundResource(R.drawable.card_selector);

                if(cClickable) {
                    button.setOnClickListener(onCardClickListener);//打牌触发
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
                        button.setOnClickListener(onCardClickListener);//打牌触发
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
    //创建计时器
    public void createCountdown(){
        if(countdownTimeout>0){
            countdownTimer=new Timer();
            countdownTimer.schedule(countdownTimerTask,0,1000);
        }
    }
    //关闭计时器
    public void shutCountdown(){
        countdownTimer.cancel();
        countdownTimerTask.cancel();
        countdownButton.setVisibility(View.GONE);
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
    public  void toast(Context context, String info) {
        Toast.makeText(context, info, Toast.LENGTH_LONG).show();
    }

}
