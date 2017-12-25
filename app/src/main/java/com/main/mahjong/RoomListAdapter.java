package com.main.mahjong;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.LinkedList;

/**
 * Created by admin on 2017/11/17.
 */

public class RoomListAdapter extends BaseAdapter {
    private Context rContext;
    private LinkedList<Room> rData;
    private OnClickListener onClickListener;
    public RoomListAdapter(LinkedList<Room> rData, OnClickListener onClickListener, Context rContext){
        this.rData = rData;
        this.onClickListener=onClickListener;
        this.rContext = rContext;

    }
    @Override
    public int getCount() {
        return rData.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = LayoutInflater.from(rContext).inflate(R.layout.list_item,parent,false);
        TextView txt_rId=(TextView)convertView.findViewById(R.id.rId);
        TextView txt_score=(TextView) convertView.findViewById(R.id.baseScore);
        TextView txt_roundNumber=(TextView) convertView.findViewById(R.id.roundNumber);
        TextView txt_playerNumber=(TextView) convertView.findViewById(R.id.playerNumber);
        Button btn_invite_friend=(Button)convertView.findViewById(R.id.invite_friend);

        //RoomListModel roomListModel=rData.get(position);

        txt_rId.setText(rData.get(position).getrId()+"");
        txt_score.setText(rData.get(position).getBaseScore()+"");//String.valueOf(tData.get(position).gettAge())
        txt_roundNumber.setText(rData.get(position).getRoundNumber()+"");
        txt_playerNumber.setText("/"+rData.get(position).getPlayerNumber());
        btn_invite_friend.setOnClickListener(this.onClickListener);
//        img_icon.setBackgroundResource(tData.get(position).getaIcon());
//        txt_aName.setText(tData.get(position).getaName());
//        txt_aSpeak.setText(tData.get(position).getaSpeak());
        return convertView;
    }
}
