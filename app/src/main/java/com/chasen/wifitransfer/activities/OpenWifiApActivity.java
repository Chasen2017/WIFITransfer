package com.chasen.wifitransfer.activities;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.chasen.wifitransfer.R;
import com.chasen.wifitransfer.adapter.WifiListAdapter;
import com.chasen.wifitransfer.utils.FileUtils;
import com.chasen.wifitransfer.utils.WifiUtil;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 打开wifi热点的Activity
 */
public class OpenWifiApActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    //-------------------Const Start-------------------//
    public static final String TAG = "OpenWifiApActivity";
    // 端口号
    public static final int PORT = 8090;
    // 文件选择的request code
    public static final int PICK_FILE_REQUEST_CODE = 0X01;
    //-------------------Const End-------------------//


    //-------------------View Start-------------------//
    @BindView(R.id.btn_open_wifi_ap)
    Button mOpenApBtn;
    @BindView(R.id.list_view)
    ListView mConnectedLv;
    //-------------------View End-------------------//


    //-------------------Field Start-------------------//
    // 连接到热点的设备列表
    private ArrayList<Socket> mConnecteds = new ArrayList<>();
    // list view适配器
    private WifiListAdapter mAdapter;
    // ServerSocket
    private ServerSocket mServerSocket;
    // 当前选中的socket
    private Socket mCurSocket;
    //-------------------Field End-------------------//

    /**
     * OpenWifiApActivity入口
     *
     * @param context Context
     */
    public static void startActivity(Context context) {
        context.startActivity(new Intent(context, OpenWifiApActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_wifi_ap);
        ButterKnife.bind(this);
        setTitle("开启WIFI热点");
        initListView();
    }

    private void initListView() {
        mAdapter = new WifiListAdapter(this, mConnecteds);
        mConnectedLv.setAdapter(mAdapter);
        mConnectedLv.setOnItemClickListener(this);
    }

    @OnClick(R.id.btn_open_wifi_ap)
    void onTurnOnWifiApClick() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$TetherSettingsActivity"));
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, WifiUtil.getWifiAPState() + "");
        // TODO 是否开启了热点，是则开启端口显示连接的设备
        // 开启了热点，则开启socket并监听其状态，显示连接列表界面
        if (WifiUtil.isOpenAp()) {
            setViewVisible(true);
            initServerSocket();
        } else {
            setViewVisible(false);
        }

    }

    /**
     * 设置view的可见与否，开启热点和未开启热点显示的view不同
     *
     * @param isOpenAp 是否开启了热点
     */
    private void setViewVisible(boolean isOpenAp) {
        mOpenApBtn.setVisibility(isOpenAp ? View.GONE : View.VISIBLE);
        mConnectedLv.setVisibility(isOpenAp ? View.VISIBLE : View.GONE);
    }

    /**
     * 开启网络线程，打开ss并监听连接
     */
    private void initServerSocket() {
        Log.d(TAG, "initServerSocket");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mServerSocket != null) {
                        mServerSocket.close();
                        mServerSocket = null;
                    }
                    mServerSocket = new ServerSocket(PORT);
                    while (true) {
                        Socket socket = mServerSocket.accept();
                        if (!mConnecteds.contains(socket)) {
                            Log.d(TAG, "ss accept");
                            mConnecteds.add(socket);
                            refreshListView();
                            checkAlive(socket);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 刷新ListView
     */
    private void refreshListView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * 检测连接是否存活
     *
     * @param socket Socket
     */
    private void checkAlive(Socket socket) {
        Log.d(TAG, "checkAlive");
        new HeatBeatThread(socket).start();
    }

    /**
     * 心跳线程，用于检测连接是否存活
     */
    class HeatBeatThread extends Thread {

        private Socket mSocket;

        public HeatBeatThread(Socket socket) {
            this.mSocket = socket;
        }

        @Override
        public void run() {
            super.run();
            while (true) {
                // 已断开连接，从列表中移除，并通知更新
                if (!isConnecting(mSocket) && mConnecteds.contains(mSocket)) {
                    Log.d(TAG, "socket:" + mSocket.getInetAddress() + "disConnected");
                    mConnecteds.remove(mSocket);
                    refreshListView();
                    return;
                }
                // 还连接着，线程睡眠5s
                try {
                    Thread.sleep(5 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 判断是否还连接着
     *
     * @param socket socket
     * @return 是返回true
     */
    private boolean isConnecting(Socket socket) {
        Log.d(TAG, "send HeatBeat");
        try {
            socket.sendUrgentData(0x1);
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mCurSocket = mConnecteds.get(position);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == PICK_FILE_REQUEST_CODE && data != null) {
            Uri uri = data.getData();
            Log.e(TAG, "uri:" + data.getDataString());
            String path = FileUtils.getFilePathByUri(this, uri);
            SendFileActivity.startActivity(this, path, mCurSocket);
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mConnecteds.clear();
        try {
            if (mServerSocket != null) {
                mServerSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
