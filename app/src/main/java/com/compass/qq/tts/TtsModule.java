package com.compass.qq.tts;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.baidu.tts.auth.AuthInfo;
import com.baidu.tts.chainofresponsibility.logger.LoggerProxy;
import com.baidu.tts.client.SpeechError;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by ezfanbi on 6/15/2018.
 * 语音合成，播放
 */
public class TtsModule implements SpeechSynthesizerListener {
    private static final String TAG = "TtsModule";

    // ================== 初始化参数设置开始 ==========================
    private static final String APP_ID = "11189849";
    private static final String APP_KEY = "yoLqWgEbzNgKKprVT7iSh9YK";
    private static final String SECRET_KEY = "cf7fb720d813411714ec85a5429ea75f";

    // TtsMode.MIX; 离在线融合，在线优先； TtsMode.ONLINE 纯在线； 没有纯离线
    // 离线时只支持2种发音, 离线时只有普通女声和普通男声。即无特别男声、度逍遥和度丫丫
    private static final TtsMode TTS_MODE = TtsMode.ONLINE;

    private static final String SAMPLE_DIR_NAME = "baiduTTS";
    // m15 离线男声 / f7 离线女声 / yyjw 度逍遥 / as 度丫丫
    private static final String TTS_SPEECH_F7_MODEL_FILE = "bd_etts_common_speech_f7_mand_eng_high_am-mix_v3.0.0_20170512.dat";
    private static final String TTS_SPEECH_YYJW_MODEL_FILE = "bd_etts_common_speech_yyjw_mand_eng_high_am-mix_v3.0.0_20170512.dat";
    private static final String TTS_TEXT_MODEL_FILE = "bd_etts_text.dat";

    private final String mSampleDirPath;

    private String ttsTextModelFile;
    private String ttsSpeechF7ModelFile;
    private String ttsSpeechYyjwModelFile;

    private static SpeechSynthesizer speechSynthesizer;
    private Context context;

    private static volatile TtsModule instance;

    private TtsModule() {
        String sdcardPath = Environment.getExternalStorageDirectory().toString();
        mSampleDirPath = sdcardPath + "/" + SAMPLE_DIR_NAME;

        ttsTextModelFile = mSampleDirPath + "/" + TTS_TEXT_MODEL_FILE;
        ttsSpeechF7ModelFile = mSampleDirPath + "/" + TTS_SPEECH_F7_MODEL_FILE;
        ttsSpeechYyjwModelFile = mSampleDirPath + "/" + TTS_SPEECH_YYJW_MODEL_FILE;
    }

    public static TtsModule getInstance() {
        if (instance == null) {
            instance = new TtsModule();
        }
        return instance;
    }

    public void setContext(Context context) {
        this.context = context;
        speechSynthesizer = initTts();
    }

    /**
     * 可以测试离线合成功能，首次使用请联网
     * 其中initTTS方法需要在新线程调用，否则引起UI阻塞
     * 纯在线请修改代码里ttsMode为TtsMode.ONLINE， 没有纯离线
     */
    private SpeechSynthesizer initTts() {
        LoggerProxy.printable(true); // 日志打印在logcat中

        // 1. 获取实例
        speechSynthesizer = SpeechSynthesizer.getInstance();
        speechSynthesizer.setContext(context);

        // 2. 设置appId，APP_KEY.SECRET_KEY
        checkResult(speechSynthesizer.setAppId(APP_ID), "setAppId");
        checkResult(speechSynthesizer.setApiKey(APP_KEY, SECRET_KEY), "setApiKey");

        // 3. 填参数
        if (TTS_MODE.equals(TtsMode.ONLINE)) {
            setOnlineTtsParams(speechSynthesizer);
        } else if (checkOfflineResources() && checkAuth()) {
            setMixTtsParams(speechSynthesizer);
        }

        // 4. 初始化
        checkResult(speechSynthesizer.initTts(TTS_MODE), "initTts");
        return speechSynthesizer;
    }

    private void setOnlineTtsParams(SpeechSynthesizer speechSynthesizer) {
        // 设置在线发声音人： 0 普通女声（默认） 1 普通男声 2 特别男声 3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0");
        // 设置合成的音量，0-9 ，默认 5
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "6");
        // 设置合成的语速，0-9 ，默认 5
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "5");
        // 设置合成的语调，0-9 ，默认 5
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "5");

        // 设置Mix模式的合成策略，默认MIX_MODE_DEFAULT, 其它参数请参考文档
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_HIGH_SPEED_NETWORK);
        // 设置音频格式,默认AUDIO_ENCODE_AMR
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_AUDIO_ENCODE, SpeechSynthesizer.AUDIO_ENCODE_AMR);
        // 设置比特率,默认AUDIO_BITRATE_AMR_15K
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_AUDIO_RATE, SpeechSynthesizer.AUDIO_BITRATE_AMR_12K65);


        // 该参数设置为TtsMode.MIX生效。即纯在线模式不生效。
        // MIX_MODE_DEFAULT 默认 ，wifi状态下使用在线，非wifi离线。在线状态下，请求超时6s自动转离线
        // MIX_MODE_HIGH_SPEED_SYNTHESIZE_WIFI wifi状态下使用在线，非wifi离线。在线状态下， 请求超时1.2s自动转离线
        // MIX_MODE_HIGH_SPEED_NETWORK ， 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
        // MIX_MODE_HIGH_SPEED_SYNTHESIZE, 2G 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_DEFAULT);
    }

    private void setMixTtsParams(SpeechSynthesizer speechSynthesizer) {
        // 文本模型文件路径 (离线引擎使用)， 注意TEXT_FILENAME必须存在并且可读
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, ttsTextModelFile);
        // 声学模型文件路径 (离线引擎使用)， 注意TEXT_FILENAME必须存在并且可读
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, ttsSpeechF7ModelFile);

        setOnlineTtsParams(speechSynthesizer);
    }

    /**
     * 检查appId ak sk 是否填写正确，另外检查官网应用内设置的包名是否与运行时的包名一致。本demo的包名定义在build.gradle文件中
     *
     * @return
     */
    private boolean checkAuth() {
        AuthInfo authInfo = speechSynthesizer.auth(TTS_MODE);
        if (!authInfo.isSuccess()) {
            // 离线授权需要网站上的应用填写包名
            String errorMsg = authInfo.getTtsError().getDetailMessage();
            Log.e(TAG, "[error]鉴权失败 errorMsg=" + errorMsg);
            return false;
        } else {
            Log.i(TAG, "验证通过，离线正式授权文件存在。");
            return true;
        }
    }

    /**
     * 检查 ttsTextModelFile, ttsSpeechF7ModelFile 这2个文件是否存在，不存在请自行从assets目录里手动复制
     *
     * @return
     */
    private boolean checkOfflineResources() {
        File file = new File(mSampleDirPath);
        if (!file.exists()) {
            file.mkdirs();
        }

        copyFromAssetsToSdcard(false, TTS_TEXT_MODEL_FILE, ttsTextModelFile);
        copyFromAssetsToSdcard(false, TTS_SPEECH_F7_MODEL_FILE, ttsSpeechF7ModelFile);
        copyFromAssetsToSdcard(false, TTS_SPEECH_YYJW_MODEL_FILE, ttsSpeechYyjwModelFile);

        String[] filenames = {ttsTextModelFile, ttsSpeechF7ModelFile};
        for (String path : filenames) {
            file = new File(path);
            if (!file.canRead()) {
                Log.e(TAG, "[ERROR] 初始化失败！！！文件不存在或者不可读取，请从assets目录复制同名文件到：" + path);
                return false;
            }
        }

        Log.i(TAG, "离线资源存在并且可读, 目录：" + mSampleDirPath);
        return true;
    }

    public void speak(String msg) {
        /* 以下参数每次合成时都可以修改
         *  mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0");
         *  设置在线发声音人： 0 普通女声（默认） 1 普通男声 2 特别男声 3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
         *  mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "5"); 设置合成的音量，0-9 ，默认 5
         *  mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "5"); 设置合成的语速，0-9 ，默认 5
         *  mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "5"); 设置合成的语调，0-9 ，默认 5
         *
         *  mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_DEFAULT);
         *  MIX_MODE_DEFAULT 默认 ，wifi状态下使用在线，非wifi离线。在线状态下，请求超时6s自动转离线
         *  MIX_MODE_HIGH_SPEED_SYNTHESIZE_WIFI wifi状态下使用在线，非wifi离线。在线状态下， 请求超时1.2s自动转离线
         *  MIX_MODE_HIGH_SPEED_NETWORK ， 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
         *  MIX_MODE_HIGH_SPEED_SYNTHESIZE, 2G 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
         */
        if (speechSynthesizer == null) {
            Log.e(TAG, "[ERROR] 初始化失败");
            return;
        }

        checkResult(speechSynthesizer.speak(msg), "speak");
    }

    private void checkResult(int result, String method) {
        if (result != 0) {
            Log.e(TAG, "error code :" + result + " method:" + method + ", 错误码文档:http://yuyin.baidu.com/docs/tts/122 ");
        }
    }

    public void stop() {
        Log.d(TAG,"停止说话");
        int result = speechSynthesizer.stop();
        checkResult(result, "stop");
    }


    public static void onDestroy() {
        Log.i(TAG,"Call onDestroy()");
        if (speechSynthesizer != null) {
            speechSynthesizer.stop();
            speechSynthesizer.release();
            speechSynthesizer = null;
            Log.i(TAG,"释放资源成功");
        }
    }

    /**
     * 将工程需要的资源文件拷贝到SD卡中使用（授权文件为临时授权文件，请注册正式授权）
     *
     * @param isCover 是否覆盖已存在的目标文件
     * @param source
     * @param dest
     */
    private void copyFromAssetsToSdcard(boolean isCover, String source, String dest) {
        File file = new File(dest);
        if (isCover || (!isCover && !file.exists())) {
            InputStream is = null;
            FileOutputStream fos = null;
            try {
                is = context.getAssets().open(source);
                String path = dest;
                fos = new FileOutputStream(path);
                byte[] buffer = new byte[1024];
                int size = 0;
                while ((size = is.read(buffer, 0, 1024)) >= 0) {
                    fos.write(buffer, 0, size);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    public void onSynthesizeStart(String s) {
    }

    @Override
    public void onSynthesizeDataArrived(String s, byte[] bytes, int i) {
    }

    @Override
    public void onSynthesizeFinish(String s) {
    }

    @Override
    public void onSpeechStart(String s) {
    }

    @Override
    public void onSpeechProgressChanged(String s, int i) {
    }

    @Override
    public void onSpeechFinish(String s) {
    }

    @Override
    public void onError(String s, SpeechError speechError) {
    }
}