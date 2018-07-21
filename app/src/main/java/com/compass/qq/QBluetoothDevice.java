package com.compass.qq;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Message;
import android.util.Log;

import com.compass.qq.handler.UIHandler;
import com.compass.qq.tts.TtsModule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

/**
 * one bluetooth device
 */
public class QBluetoothDevice {
    private String TAG = QBluetoothDevice.class.getName();
    private String macAddress;
    private InputStream inputStream;
    private OutputStream outputStream;
    private QMessageListener qMessageListener;
    private static final UUID UUID_SERIAL_PORT_SERVICE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static QBluetoothDevice instance = new QBluetoothDevice("AB:03:56:78:C1:3A", new QUpLinkMsgListener());

    public static QBluetoothDevice getInstance(){
        return instance;
    }

    /**
     * constructor
     * @param macAddress
     */
    QBluetoothDevice(String macAddress, QMessageListener qMessageListener){
        this.macAddress = macAddress;
        this.qMessageListener = qMessageListener;
    }

    /**
     * start listen to the device
     */
    public void startListening(){
        while (true){
            try{
                BluetoothSocket socket = btConnect();
                btReceive(socket);
            }
            catch (Exception e){
                Log.e(TAG, "btReceive fail", e);
            }
        }
    }

    /**
     * is connected
     * @return
     */
    public synchronized boolean isConnected() {
        return this.outputStream != null;
    }

    /**
     * send message to device
     */
    public synchronized void sendMessage(String message) {
        if(isConnected()){
            try {
                outputStream.write(message.getBytes());
            } catch (IOException e) {
                Log.e(TAG, "send downlink message failed", e);
            }
        }
    }

    private BluetoothSocket btConnect(){
        while (true){
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            adapter.enable();

            BluetoothDevice targetDevice = null;
            for (BluetoothDevice device : adapter.getBondedDevices()){
                if (macAddress.equals(device.getAddress())) {
                    targetDevice = adapter.getRemoteDevice(macAddress);
                    break;
                }
            }

            if(targetDevice != null){
                BluetoothSocket socket = null;
                try {
                    socket = targetDevice.createRfcommSocketToServiceRecord(UUID_SERIAL_PORT_SERVICE);
                    socket.connect();
                    return socket;
                } catch (IOException e) {
                    Log.e(TAG, "create or btConnect socket failed", e);
                    if(socket != null){
                        closeConnectedSocket(socket);
                    }
                }
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Log.i(TAG, "Thread.sleep() interrupted", e);
            }
        }
    }

    private void btReceive(BluetoothSocket socket) {
        BufferedReader bf = null;
        try {
            inputStream = socket.getInputStream();
            setOutputStream(socket.getOutputStream());
            bf = new BufferedReader(new InputStreamReader(inputStream));
            // 持续监听
            while (true) {
                try {
                    String words = bf.readLine();
                    // 直接将 words 交给情景模型处理
                    if (words != null && words.length() > 0) {
                        qMessageListener.receiveMsg(words);
                    }
                    if(words == null){
                        break;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "connection lost", e);
                    break;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "get streams failed", e);
        }
        finally {
            btClose(socket, bf);
        }
    }

    private void btClose(BluetoothSocket socket, BufferedReader bf){
        setOutputStream(null);
        closeBufferReader(bf);
        closeConnectedSocket(socket);
    }

    private void closeConnectedSocket(BluetoothSocket socket) {
        try {
            if(socket != null){
                socket.close();
            }
        } catch (IOException ex) {
            Log.e(TAG, "btClose() failed", ex);
        }
    }

    private void closeBufferReader(BufferedReader bf){
        try {
            if(bf != null){
                bf.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void setOutputStream(OutputStream outputStream){
        this.outputStream = outputStream;

        Message msg = UIHandler.getInstance().obtainMessage();
        msg.what = Constants.MSG_ARDUINO_LAMP_STATE;
        msg.obj = (null != outputStream);
        UIHandler.getInstance().sendMessage(msg);
    }
}
