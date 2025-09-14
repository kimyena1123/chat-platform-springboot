package com.chatting.messageclient.service;

import com.chatting.messageclient.dto.domain.ChannelId;

public class UserService {

    //상태를 2개로 관리
    private enum Location {
        LOBBY, CHANNEL
    }

    private Location userLocation = Location.LOBBY;
    private String username = "";
    private ChannelId channelId = null;

    //Getter
    public boolean isInLobby(){
        return userLocation == Location.LOBBY;
    }

    public boolean isInChannel(){
        return userLocation == Location.CHANNEL;
    }

    public String getUsername() {
        return username;
    }

    public ChannelId getChannelId() {
        return channelId;
    }

    public void login(String username){
        this.username = username;
        moveToLobby();
    }

    public void logout(){
        this.username = "";
        moveToLobby();
    }

    //위치 정보가 바뀌었을 때
    public void moveToLobby() {
        userLocation = Location.LOBBY;
        this.channelId = null;
    }

    public void moveToChannel(ChannelId channelId) {
        userLocation = Location.CHANNEL;
        this.channelId = channelId;
    }
}
