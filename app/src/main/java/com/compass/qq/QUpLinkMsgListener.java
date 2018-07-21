package com.compass.qq;

import android.util.Log;
import com.compass.qq.handler.UIHandler;

/**
 * listen to message from hardware and deal it
 */
public class QUpLinkMsgListener implements QMessageListener {

    private String TAG = QUpLinkMsgListener.class.getName();

    public void receiveMsg(String arduinoData) {
        Log.i(TAG, String.format("receiving from Arduino: %s", arduinoData));

        String[] args = arduinoData.split("_");
        String command = args[0].toUpperCase();

        // 不同的模式展示不同的Text
        String showText = null;
        try{
            switch (command) {
                case QInterestPoint.ACTION_CODE_DISTANCE:
                    double distance = (double) Math.round(Double.valueOf(args[1])) / 100;
                    QDownLinkMsgHelper.getInstance().setDistance(distance);
                    showText = distance + "米";
                    break;
                case QInterestPoint.ACTION_CODE_HUMIDITY:
                    showText = (int) Math.round(Double.valueOf(args[1]))+"%";
                    break;
                case QInterestPoint.ACTION_CODE_TEMPERATURE:
                    showText = Double.valueOf(args[1]) + "℃";
                    break;
                case QInterestPoint.ACTION_CODE_FIRE_ALARM:
                    // 停止导盲
                    QDownLinkMsgHelper.getInstance().disableBlindGuideMode();
                    // 停止播放音乐、视频
                    UIHandler.getInstance().sendMessage(Constants.STOP_MUSIC,null);
                    showText = "火警警报";
                    break;
                default:
                    break;
            }
        }catch(Exception ex){
            Log.e(TAG, "exception was thrown when call receiveMsg()");
            ex.printStackTrace();
        }

        if (null != showText) {
            UIHandler.getInstance().sendMessage(Constants.MSG_ARDUINO_TEXT, showText.getBytes());
        }
    }
}
