package com.compass.qq;

import android.util.Log;
import com.compass.qq.handler.UIHandler;
import com.compass.qq.tts.TtsModule;

public class QDownLinkMsgHelper {
    private String TAG = QDownLinkMsgHelper.class.getName();
    private QBluetoothDevice device = QBluetoothDevice.getInstance();
    private static QDownLinkMsgHelper instance = new QDownLinkMsgHelper();

    public static QDownLinkMsgHelper getInstance() {
        return instance;
    }

    /**
     * 接收DcsFramework发送过来的指令, 下发给硬件
     */
    public void handleDirective(String command) {

        switch (command) {
            case QInterestPoint.ACTION_CODE_INTEEMITTENT_LAMP: // 间歇灯
            case QInterestPoint.ACTION_CODE_DISTANCE: //测距
            case QInterestPoint.ACTION_CODE_TEMPERATURE: //测温度
            case QInterestPoint.ACTION_CODE_HUMIDITY: // 湿度
                device.sendMessage(command+"#");
                break;
            case QInterestPoint.ACTION_CODE_BLINDGUIDE://导盲模式，需要开启线程 持续发送命令
                enableBlindGuideMode();
                break;
            case QInterestPoint.ACTION_CODE_STOP://停止板子所有行为，停止展示
                disableBlindGuideMode();
                UIHandler.getInstance().sendMessage(Constants.MSG_ARDUINO_TEXT,"已停止".getBytes());
                break;
            default:
                break;
        }
    }

    private final double referenceDistance = 2.0;
    private double distance = 0;
    //TODO, 在开启盲人模式时应先设置 distance = 0
    public void setDistance(double distance){
        this.distance = distance;
    }
    private int triggerBuzzer(){
        if(distance <= referenceDistance){
            return (int)Math.round(distance*10/referenceDistance);
        }
        return -1;
    }

    /**
     * 导盲模式的定时任务
     */
    private Runnable runnableCode = new Runnable() {
        int sendCommendAccount = 30;
        int triggerBuzzerAccount = 0;
        int i = 0, j = 0;
        @Override
        public void run() {
            UIHandler.getInstance().postDelayed(this, 100);
            Log.i(TAG, "timer call in main thread");

            // 发送命令计数
            if(j < sendCommendAccount){
                j++;
            }else{
                device.sendMessage(QInterestPoint.ACTION_CODE_DISTANCE);
                j = 0;
            }

            // 触发蜂鸣器计数
            triggerBuzzerAccount = triggerBuzzer();
            if(i < triggerBuzzerAccount){
                i++;
            }else if(-1 != triggerBuzzerAccount){
                // 处理蜂鸣器
                UIHandler.getInstance().sendMessage(Constants.PLAY_SOUND, null);
                i = 0;
            }
        }
    };

    private void enableBlindGuideMode() {
        UIHandler.getInstance().sendMessage(Constants.MSG_ARDUINO_TEXT,"盲人模式已开启".getBytes());
        UIHandler.getInstance().post(runnableCode);
    }

    public void disableBlindGuideMode() {
        TtsModule.getInstance().stop();
        Log.d(TAG, "removeCallbacks(), remove the runnableCode.");
        UIHandler.getInstance().removeCallbacks(runnableCode);
    }
}
