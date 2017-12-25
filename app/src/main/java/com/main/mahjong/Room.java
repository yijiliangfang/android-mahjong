package com.main.mahjong;

/**
 * Created by admin on 2017/11/17.
 */

public class Room {
    private long rId;//房号
    private int baseScore=1;//底分
    private int roundNumber=8;//局数
    private int playerNumber=3;//人数
    private String payType="SPLIT_FARE";//房费支付方式:SPLIT_FARE 平摊，ROOMOWNER_PAY
    private String avoidCheat="IP";//防止作弊方式

    public Room(long rId,
                int baseScore,
                int roundNumber,
                int playerNumber){
        this.rId=rId;
        this.baseScore=baseScore;
        this.roundNumber=roundNumber;
        this.playerNumber=playerNumber;


    }
    public Room(){};

    public void setrId(long rId)
    {
        this.rId=rId;
    }
    public void setBaseScore(int baseScore){
        this.baseScore=baseScore;
    }
    public void setRoundNumber(int roundNumber){
        this.roundNumber=roundNumber;
    }
    public void setPlayerNumber(int playerNumber){
        this.playerNumber=playerNumber;
    }
    public void setPayType(String payType){
        this.payType=payType;
    }
    public void setAvoidCheat(String avoidCheat){
        this.avoidCheat=avoidCheat;
    }
    //--------------------------------------------
    public long getrId()
    {
        return rId;
    }
    public int getBaseScore(){
        return baseScore;
    }
    public int getRoundNumber(){
        return roundNumber;
    }
    public int getPlayerNumber(){
        return playerNumber;
    }
    public String getPayType(){
        return payType;
    }
    public String getAvoidCheat(){
        return avoidCheat;
    }

}
