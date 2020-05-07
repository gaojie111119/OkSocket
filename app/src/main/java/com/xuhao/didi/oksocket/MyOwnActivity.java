package com.xuhao.didi.oksocket;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xuhao.didi.core.iocore.interfaces.IPulseSendable;
import com.xuhao.didi.core.iocore.interfaces.ISendable;
import com.xuhao.didi.core.pojo.OriginalData;
import com.xuhao.didi.core.protocol.IReaderProtocol;
import com.xuhao.didi.oksocket.adapter.LogAdapter;
import com.xuhao.didi.oksocket.data.DefaultSendBean;
import com.xuhao.didi.oksocket.data.LogBean;
import com.xuhao.didi.oksocket.data.PulseBean;
import com.xuhao.didi.socket.client.impl.client.action.ActionDispatcher;
import com.xuhao.didi.socket.client.sdk.OkSocket;
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.client.sdk.client.action.SocketActionAdapter;
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager;
import com.xuhao.didi.socket.client.sdk.client.connection.NoneReconnect;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;


public class MyOwnActivity extends AppCompatActivity {

    private ConnectionInfo mInfo;

    public Button mConnect;
    public IConnectionManager mManager;
    public EditText mIPET;
    public EditText mPortET;
    private Button mRedirect;
    private EditText mFrequencyET;
    private Button mSetFrequency;
    private Button mMenualPulse;
    private Button mClearLog;
    private SwitchCompat mReconnectSwitch;

    public RecyclerView mSendList;
    public RecyclerView mReceList;

    private LogAdapter mSendLogAdapter = new LogAdapter();
    private LogAdapter mReceLogAdapter = new LogAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complex);
        findViews();
        initData();
        setListener();
        mInfo = new ConnectionInfo("47.93.22.113", 8801);
        open(getApplicationContext(),this);
        mManager=initData(mInfo);

    }

    private void findViews() {
        mSendList = findViewById(R.id.send_list);
        mReceList = findViewById(R.id.rece_list);
        mClearLog = findViewById(R.id.clear_log);
        mSetFrequency = findViewById(R.id.set_pulse_frequency);
        mFrequencyET = findViewById(R.id.pulse_frequency);
        mConnect = findViewById(R.id.connect);
        mIPET = findViewById(R.id.ip);
        mPortET = findViewById(R.id.port);
        mRedirect = findViewById(R.id.redirect);
        mMenualPulse = findViewById(R.id.manual_pulse);
        mReconnectSwitch = findViewById(R.id.switch_reconnect);
    }

    private void initData(){
        mIPET.setEnabled(false);
        mPortET.setEnabled(false);

        LinearLayoutManager manager1 = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mSendList.setLayoutManager(manager1);
        mSendList.setAdapter(mSendLogAdapter);

        LinearLayoutManager manager2 = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mReceList.setLayoutManager(manager2);
        mReceList.setAdapter(mReceLogAdapter);


    }


    private void setListener() {
        //mManager.registerReceiver(adapter);

        mReconnectSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    if (!(mManager.getReconnectionManager() instanceof NoneReconnect)) {
                        mManager.option(new OkSocketOptions.Builder(mManager.getOption()).setReconnectionManager(new NoneReconnect()).build());
                        logSend("关闭重连管理器(Turn Off The Reconnection Manager)");
                    }
                } else {
                    if (mManager.getReconnectionManager() instanceof NoneReconnect) {
                        mManager.option(new OkSocketOptions.Builder(mManager.getOption()).setReconnectionManager(OkSocketOptions.getDefault().getReconnectionManager()).build());
                        logSend("打开重连管理器(Turn On The Reconnection Manager)");
                    }
                }
            }
        });

        mConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mManager == null) {
                    return;
                }
                if (!mManager.isConnect()) {
                    mConnect.setText("Connecting");
                    mManager.connect();
                } else {
                    mConnect.setText("DisConnecting");
                    mManager.disconnect();
                }
            }
        });

        mClearLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mReceLogAdapter.getDataList().clear();
                mSendLogAdapter.getDataList().clear();
                mReceLogAdapter.notifyDataSetChanged();
                mSendLogAdapter.notifyDataSetChanged();
            }
        });

        mRedirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mManager == null) {
                    return;
                }
                String ip = mIPET.getText().toString();
                String portStr = mPortET.getText().toString();
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("cmd", 57);
                jsonObject.addProperty("data", ip + ":" + portStr);
                DefaultSendBean bean = new DefaultSendBean();
                bean.setContent(new Gson().toJson(jsonObject));
                mManager.send(bean);
            }
        });

        mSetFrequency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mManager == null) {
                    return;
                }
                String frequencyStr = mFrequencyET.getText().toString();
                long frequency = 0;
                try {
                    frequency = Long.parseLong(frequencyStr);
                    OkSocketOptions okOptions = new OkSocketOptions.Builder(mManager.getOption())
                            .setPulseFrequency(frequency)
                            .build();
                    mManager.option(okOptions);
                } catch (NumberFormatException e) {
                }
            }
        });

        mMenualPulse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mManager == null) {
                    return;
                }
                mManager.getPulseManager().trigger();
            }
        });
    }

    public void logSend(String log) {
        LogBean logBean = new LogBean(System.currentTimeMillis(), log);
        mSendLogAdapter.getDataList().add(0, logBean);
        mSendLogAdapter.notifyDataSetChanged();
    }

    public void logRece(String log) {
        LogBean logBean = new LogBean(System.currentTimeMillis(), log);
        mReceLogAdapter.getDataList().add(0, logBean);
        mReceLogAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mManager != null) {
            mManager.disconnect();
            //mManager.unRegisterReceiver(adapter);
        }
    }


    private Context context= null;
    private MyOwnActivity activity=null;

    public void open(Context context,MyOwnActivity activity)
    {
        this.context = context;
        this.activity=activity;

    }

    private ConnectionInfo info = new ConnectionInfo("47.93.22.113", 8801);
    private int pulseCount = 1;
    public IConnectionManager initData(ConnectionInfo minfo) {
        IConnectionManager manager;
        if(minfo!=null)
            manager= OkSocket.open(minfo);
        else
            manager= OkSocket.open(info);
        //获得当前连接通道的参配对象
        //获得当前连接通道的参配对象
        final Handler handler = new Handler(Looper.getMainLooper());
        //OkSocketOptions.Builder builder = new OkSocketOptions.Builder();
//        builder.setReconnectionManager(new NoneReconnect());
//        builder.setCallbackThreadModeToken(new OkSocketOptions.ThreadModeToken() {
//            @Override
//            public void handleCallbackEvent(ActionDispatcher.ActionRunnable runnable) {
//                handler.post(runnable);
//            }
//        });

        OkSocketOptions.Builder optionsBuilder  = new OkSocketOptions.Builder();
        optionsBuilder.setReconnectionManager(new NoneReconnect());
        optionsBuilder.setCallbackThreadModeToken(new OkSocketOptions.ThreadModeToken() {
            @Override
            public void handleCallbackEvent(ActionDispatcher.ActionRunnable runnable) {
                handler.post(runnable);
            }
        });
        optionsBuilder.setWritePackageBytes(1024);
        optionsBuilder.setReadPackageBytes(1024);
        optionsBuilder.setReaderProtocol(new IReaderProtocol(){
            @Override
            public int getHeaderLength() {
                return 6;
            }

            @Override
            public int getBodyLength(byte[] header, ByteOrder byteOrder) {
                if (header == null || header.length < getHeaderLength()) {
                    return 0;
                }
                ByteBuffer bb = ByteBuffer.wrap(header,2,4);
                bb.order(byteOrder);
                return bb.getInt();
            }
        });

        //基于当前参配对象构建一个参配建造者类
        OkSocketOptions.Builder builder = new OkSocketOptions.Builder(optionsBuilder.build());
        //修改参配设置(其他参配请参阅类文档)
        //建造一个新的参配对象并且付给通道
        manager.option(builder.build());


        //注册Socket行为监听器,SocketActionAdapter是回调的Simple类,其他回调方法请参阅类文档
        manager.registerReceiver(new SocketActionAdapter() {
            @Override
            public void onSocketDisconnection(ConnectionInfo info, String action, Exception e) {
                Log.d("PCSocket", "onSocketDisconnection: action=$action  info=$info e=$e");
                if (e != null) {
                    if (e instanceof RedirectException) {
                        activity.logSend("正在重定向连接(Redirect Connecting)...");
                        activity.mManager.switchConnectionInfo(((RedirectException) e).redirectInfo);
                        activity.mManager.connect();
                        activity.mIPET.setEnabled(true);
                        activity.mPortET.setEnabled(true);
                    } else {
                        activity.logSend("异常断开(Disconnected with exception):" + e.getMessage());
                        activity.mIPET.setEnabled(false);
                        activity.mPortET.setEnabled(false);
                    }
                } else {
                    activity.logSend("正常断开(Disconnect Manually)");
                    activity.mIPET.setEnabled(false);
                    activity.mPortET.setEnabled(false);
                }
                activity.mConnect.setText("Connect");

            }

            @Override
            public void onSocketConnectionFailed(ConnectionInfo info, String action, Exception e) {
                activity.logSend("连接失败(Connecting Failed)");
                activity.mConnect.setText("Connect");
                activity.mIPET.setEnabled(false);
                activity.mPortET.setEnabled(false);

                Log.d("PCSocket", "onSocketConnectionFailed: action=$action  info=$info e=$e");
            }

            @Override
            public void onSocketReadResponse(ConnectionInfo info, String action, OriginalData data) {

                byte[] bytes = data.getBodyBytes();
                String str=new String(bytes, Charset.forName("utf-8"));
                activity.logRece(str);
                Log.d("PCSocket", "onSocketReadResponse: action="+action+"  data="+str);
                activity.mManager.getPulseManager().feed();

            }

            @Override
            public void onSocketWriteResponse(ConnectionInfo info, String action, ISendable data) {

                byte[] bytes = data.parse();
                String str=new String(bytes, Charset.forName("utf-8"));
                activity.logSend(str);
                Log.d("PCSocket", "onSocketWriteResponse:   str:"+str);
            }

            @Override
            public void onPulseSend(ConnectionInfo info, com.xuhao.didi.core.iocore.interfaces.IPulseSendable data) {
//            byte[] bytes = data.parse();
//            bytes = Arrays.copyOfRange(bytes, 4, bytes.length);
//            String str = new String(bytes, Charset.forName("utf-8"));
//            JsonObject jsonObject = new JsonParser().parse(str).getAsJsonObject();
//            int cmd = jsonObject.get("cmd").getAsInt();
//            if (cmd == 14) {
//                logSend("发送心跳包(Heartbeat Sending)");
//            }

                Log.d("PCSocket", "onPulseSend: data=$data  info=$info");
            }

            @Override
            public void onSocketConnectionSuccess(ConnectionInfo info, String action) {
                activity.logSend("连接成功(Connecting Sucess)");
                activity.mConnect.setText("DisConnect");
                OkSocket.open(info).getPulseManager().setPulseSendable(packageData(0, packagePulse())).pulse();
                Log.d("PCSocket", "onSocketConnectionSuccess: action=$action");
            }
            //Action from com/xuhao/didi/socket/client/impl/client/ConnectionManagerImpl.java
            //disconnect->DisconnectThread.run()->finally sendBroadcast(IAction.ACTION_DISCONNECTION, mException);
//            override fun onSocketDisconnection(info: ConnectionInfo?, action: String?, e: Exception?) {
//                //super.onSocketDisconnection(info, action, e)
//                Log.d("PCSocket", "onSocketDisconnection: action=$action  info=$info e=$e")
//            }

        });

        //调用通道进行连接
        //manager.connect();
        return manager;
    }

    private String packagePulse() {
    JsonObject jsonObject =new JsonObject();
        jsonObject.addProperty(
        "imei", Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID)
        );
        jsonObject.addProperty("index", pulseCount);
        increasePulse();
        return jsonObject.toString();
        }

    private ParseData packageData(int type ,String data)  {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", type);
        jsonObject.addProperty("version", "1.0");
        jsonObject.addProperty("package", data);
        jsonObject.addProperty("length", data.length());
        jsonObject.addProperty("stamp", System.currentTimeMillis());

        return new ParseData(jsonObject.toString());
        }

    private void increasePulse() {
        pulseCount++;
        }
}

class ParseData implements IPulseSendable {
    String str;
    public ParseData( String str)
    {
        this.str=str;
    }
    @Override
    public byte[] parse() {
        byte[]  header = "::".getBytes(Charset.defaultCharset());
        //byte[]  length = ByteUtil.intToBytes(str.length());
        byte[]  body = str.getBytes(Charset.defaultCharset());
        byte[]  foot = ";;".getBytes(Charset.defaultCharset());
        //        val merge = ByteUtil.addBytes(header, length, body, foot);
        ByteBuffer bb  = ByteBuffer.allocate(8 + body.length);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(header);
        bb.putInt(body.length);
        bb.put(body);
        bb.put(foot);
        return bb.array();
        }
}
/**
 * Created by kim on 2020/5/5.
 */
class ByteUtil {
    /**
     * int转byte{}
     */
    public static byte[] intToBytes(int value, ByteOrder mode) {
        byte[] src = new byte[4];
        if (mode == ByteOrder.LITTLE_ENDIAN) {
            src[3] = (byte) ((value >> 24) & 0xFF);
            src[2] = (byte) ((value >> 16) & 0xFF);
            src[1] = (byte) ((value >> 8) & 0xFF);
            src[0] = (byte) (value & 0xFF);
        } else {
            src[0] = (byte) ((value >> 24) & 0xFF);
            src[1] = (byte) ((value >> 16) & 0xFF);
            src[2] = (byte) ((value >> 8) & 0xFF);
            src[3] = (byte) (value & 0xFF);
        }
        return src;
    }
}