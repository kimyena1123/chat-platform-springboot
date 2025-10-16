package com.chatting.push_server.handler.kafka;


import com.chatting.push_server.dto.kafka.inbound.RecordInterface;


public interface BaseRecordHandler<T extends RecordInterface> {

    void handleRecord(T record);
}
