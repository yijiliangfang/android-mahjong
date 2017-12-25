package com.main.mahjong;

/**
 * Created by admin on 2017/12/9.
 */

public class Card {
    private int id;
    private String name;

    public Card(int id, String name){
        this.id=id;
        this.name=name;

    }
    public void setId(int id){
        this.id=id;
    }
    public void setName(String name){
        this.name=name;
    }
    public int getId(){
        return id;
    }
    public String getName(){
        return name;
    }


}
