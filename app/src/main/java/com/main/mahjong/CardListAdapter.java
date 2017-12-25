package com.main.mahjong;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;

import java.util.LinkedList;

/**
 * Created by admin on 2017/12/9.
 */

public class CardListAdapter extends BaseAdapter {
    private Context cContext;
    private LinkedList<Card> cData;

    public CardListAdapter(LinkedList<Card> cData, Context cContext){
        this.cData=cData;
        this.cContext=cContext;
    }
    @Override
    public int getCount() {
        return cData.size();
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
        convertView = LayoutInflater.from(cContext).inflate(R.layout.list_card,parent,false);

        Button btn_my_card=(Button)convertView.findViewById(R.id.my_card);
        btn_my_card.setText(cData.get(position).getName());
        //RoomListModel roomListModel=rData.get(position);
        //btn_invite_friend.setOnClickListener(this.onClickListener);

        return convertView;
    }

}
