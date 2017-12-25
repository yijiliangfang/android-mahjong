package com.main.mahjong.util;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by admin on 2017/12/1.
 */

public class ClientSocketThread extends Thread{
    public static final String SOCKET_TAG = "socketTag";
    public static final long HEART_TIME = 10000;
    public static final long SEND_TIME = 1000;
    private static final String HEART_MSG = "heart";
    /**
     * 接收线程
     */
    private Thread mReciverThread = null;

    /**
     * 心跳时间
     */
    private long mHeartTime = 0;

    /**
     * client socket
     */
    private Socket mSocket = null;

    private InputStream in = null;

    private OutputStream  out = null;

    /**
     * 主机IP地址
     */
    private String serverUrl = null;

    /**
     * 主机端口
     */
    private int serverPort = 0;

    /**
     * 消息队列
     */
    private volatile List<String> mMsgQueue = null;

    /**
     * 接收到服务端返回信息回调接口
     */
    private OnSocketRecieveCallBack mOnSocketRecieveCallBack;

    /*********************************************
     *
     * gettter or setter
     *
     ********************************************/
    public Socket getSocket() {
        return mSocket;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public List<String> getMsgQueue() {
        return mMsgQueue;
    }

    public void setMsgQueue(List<String> mMsgQueue) {
        this.mMsgQueue = mMsgQueue;
    }

    public OnSocketRecieveCallBack getOnSocketRecieveCallBack() {
        return mOnSocketRecieveCallBack;
    }

    public void setOnSocketRecieveCallBack(OnSocketRecieveCallBack mOnSocketRecieveCallBack) {
        this.mOnSocketRecieveCallBack = mOnSocketRecieveCallBack;
    }

    /**
     * 构造函数
     */
    public ClientSocketThread(String hostName, int port){
        serverUrl = hostName;
        serverPort = port;
        mMsgQueue = new LinkedList<String>();

        mReciverThread = new Thread(){
            @Override
            public void run(){
                while(!this.isInterrupted()){
                    reciverMsgFromServer();
                }
            }
        };
        mReciverThread.start();
    }

    /***********************************************
     *
     * Methods
     *
     **********************************************/
    public String readFromInputStream(InputStream in){
        int count = 0;
        byte[] inDatas = null;
        try{
            while(count == 0){
                count = in.available();
            }
            inDatas = new byte[count];
            in.read(inDatas);
            return new String(inDatas);
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private void reciverMsgFromServer(){
        // 接收数据
        if(mSocket != null && mSocket.isConnected() && !mSocket.isInputShutdown() && in != null){
            try {
                String recieveMsg = readFromInputStream(in);
                if(mOnSocketRecieveCallBack != null)
                    mOnSocketRecieveCallBack.OnRecieveFromServerMsg(recieveMsg);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送心跳包
     */
    public void sendHeart(String msgHeart){
        ((LinkedList<String>)mMsgQueue).addFirst(msgHeart);
    }

    /**
     * socket 是否有断开连接
     * @return
     */
    public boolean isSocketConnected(){
        if(mSocket != null && mSocket.isConnected()) return true;
        return false;
    }
    /**
     * 添加发送消息到队列
     * @param msg
     */
    public void addSendMsgToQueue(String msg){
        mMsgQueue.add(msg);
    }

    /**
     * 判断消息队列是否为空
     * @return
     */
    public boolean isMsgQueueEmpty(){
        return mMsgQueue.isEmpty();
    }

    @Override
    public void run() {
        InetAddress inetAddress = null;
        try {
            mSocket = new Socket(InetAddress.getByName(serverUrl), serverPort);
            in = mSocket.getInputStream();
            out = mSocket.getOutputStream();
            mHeartTime = System.currentTimeMillis();
            Log.v(SOCKET_TAG, "client socket create successed");

            // 轮询发送消息列表中的数据
            while(true){
                // 判断是否要发送心跳包
                //if(Math.abs(mHeartTime - System.currentTimeMillis()) > HEART_TIME) sendHeart(HEART_MSG);

                Thread.sleep(SEND_TIME);
                // 判断client socket 是否连接上Server
                if(mSocket.isConnected()){
                    Log.v("socketStatus", "### client socket connected ###");
                    // 发送数据
                    if(!mSocket.isOutputShutdown() && !isMsgQueueEmpty()){
                        out.write(mMsgQueue.get(0).getBytes());
                        Log.v(SOCKET_TAG, "sned msg toServer: " + mMsgQueue.get(0));
                        Log.v(SOCKET_TAG, "## msg count : " + mMsgQueue.size());
                        // 将发送过的数据移除消息列表
                        mMsgQueue.remove(0);
                        mHeartTime = System.currentTimeMillis();
                    }
                }else{
                    // 重建连接
                    Log.v(SOCKET_TAG, "client socket disconnected");
                    if(!mSocket.isClosed()) mSocket.close();
                    inetAddress = InetAddress.getByName(serverUrl);
                    mSocket = new Socket(inetAddress, serverPort);
                }
            }
        }catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.v(SOCKET_TAG,e.getMessage());
        } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.v(SOCKET_TAG,e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            Log.v(SOCKET_TAG,e.getMessage());
        }finally{
            if(out != null)
                try {
                    out.close();
                } catch (IOException e2) {
                    // TODO Auto-generated catch block
                    e2.printStackTrace();
                }
            if(in != null){
                try {
                    in.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

            if(mSocket != null){
                try {
                    mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.v(SOCKET_TAG,e.getMessage());
                }
            }
            Log.v(SOCKET_TAG, "client socket close");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if(mReciverThread != null && !mReciverThread.isInterrupted()){
            mReciverThread.interrupt();
            mReciverThread = null;
        }

        if(mSocket != null && !mSocket.isClosed()){
            mSocket.close();
        }
        Log.v(SOCKET_TAG, "client socket destory");
    }

    /**
     * 接收到服务端返回信息回调接口
     * @author Administrator
     *
     */
    public interface OnSocketRecieveCallBack{
        public void OnRecieveFromServerMsg(String msg);
    }

}
