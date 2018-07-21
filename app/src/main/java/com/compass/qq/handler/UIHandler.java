package com.compass.qq.handler;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.baidu.duer.dcs.androidapp.DcsSampleMainActivity;
import com.baidu.duer.dcs.systeminterface.IWebView;
import com.compass.qq.Constants;
import com.compass.qq.tts.TtsModule;
import java.nio.charset.Charset;

public class UIHandler extends Handler {

    private String TAG = UIHandler.class.getName();
    // HTML静态页面基本信息
    private static final String WEB_VIEW_HTM_STRING = "<html>" +
            "<head>" +
            "    <meta charset=\"utf-8\">" +
            "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
            "    <script src=\"http://duer.bdstatic.com/saiya/dcsview/main.e239b3.js\"></script>" +
            "    <style></style>" +
            "</head>" +
            "<body>" +
            "<div id=\"display\">" +
            "    <section data-from=\"server\" class=\"head p-box-out\">" +
            "        <div class=\"avatar\"></div>" +
            "        <div class=\"bubble-container\">" +
            "            <div class=\"bubble p-border text\">" +
            "                <div class=\"text-content text\">%s</div>" +
            "            </div>" +
            "        </div>" +
            "    </section>" +
            "</div>" +
            "</body>" +
            "</html>";

    private IWebView webView;
    private DcsSampleMainActivity mainActivity;
    private static UIHandler instance = new UIHandler();

    public static UIHandler getInstance() {
        return instance;
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        switch (msg.what) {
            case Constants.MSG_ARDUINO_TEXT:
                String message = new String((byte[]) msg.obj, Charset.defaultCharset());
                Log.i(TAG, "receiving uplink message: "+message);
                TtsModule.getInstance().speak(message);
                showInWebView(message);
                break;
            case Constants.MSG_ARDUINO_LAMP_STATE:
                if (mainActivity != null) {
                    mainActivity.updateBreathingLight((Boolean) msg.obj);
                }
                break;
            case Constants.PLAY_MUSIC:
                if (mainActivity != null) {
                    // 可以根据 msg.obj 播放不同的音乐
                    String musicName = new String((byte[]) msg.obj, Charset.defaultCharset());
                    mainActivity.playMusic(musicName);
                }
                break;
            case Constants.PLAY_SOUND:
                if (mainActivity != null) {
                    mainActivity.playSound();
                }
                break;
            case Constants.STOP_MUSIC:
                if (mainActivity != null) {
                    mainActivity.stopPlayMusic();
                }
                break;
            default:
                break;
        }
    }

    /**
     * 处理组合数据
     *
     * @param msg
     */
    public void showInWebView(String[] msg) {
        // index: 0:网址，1:音频标志，2:文本
        Log.d(TAG, "showing in WebView: " + msg[2]);
        if(msg[2].isEmpty()){
            return;
        }

        if(null == msg[0] || msg[0].isEmpty()){
            // 是一个文本
            webView.loadData(String.format(WEB_VIEW_HTM_STRING, msg[2]), "text/html; charset=UTF-8", null);
        }else if(null == msg[1] || msg[1].isEmpty()){
            // 是一个视频
            webView.loadUrl(msg[0]);
        }else{
            // 是一个图片
            webView.loadUrl(msg[0]);
            // 播放音乐
            sendMessage(Constants.PLAY_MUSIC,msg[1].getBytes() );
        }
    }
    private void showInWebView(String msg) {
        Log.d(TAG, "showing in WebView: " + msg);
        if(msg.isEmpty()){
            return;
        }
        webView.loadData(String.format(WEB_VIEW_HTM_STRING, msg), "text/html; charset=UTF-8", null);
    }

    /**
     * send message from sub-thread to main-thread
     *
     * @param what
     * @param obj
     */
    public void sendMessage(int what, Object obj){
        Message message = obtainMessage();
        message.what = what;
        message.obj = obj;
        sendMessage(message);
    }

    /**
     * 处理组合数据
     *
     * @param msg
     */
    public void speak(String msg){
        TtsModule.getInstance().speak(msg);
    }

    public void setWebView(IWebView webView) {
        this.webView = webView;
    }

    public void setContext(Context mContext) {
        this.mainActivity = (DcsSampleMainActivity) mContext;
    }
}
