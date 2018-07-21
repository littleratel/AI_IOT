package com.compass.qq;

public interface QMessageListener {
    /**
     * 处理硬件发送过来的数据
     */
    void receiveMsg(String arduinoData);
}
