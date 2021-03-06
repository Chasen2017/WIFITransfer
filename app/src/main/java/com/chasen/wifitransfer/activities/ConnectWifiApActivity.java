package com.chasen.wifitransfer.activities;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chasen.wifitransfer.R;
import com.chasen.wifitransfer.utils.WifiUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 连接wifi热点Activity
 */
public class ConnectWifiApActivity extends AppCompatActivity {

    //-------------------Const Start-------------------//
    public static final String TAG = "ConnectWifiApActivity";
    public static final String SAVE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/WifiTransfer/";
    //-------------------Const  End -------------------//

    //-------------------View Start-------------------//
    @BindView(R.id.btn_connect_wifi)
    Button mConnectWifiBtn;
    @BindView(R.id.tv_status)
    TextView mStatusTv;
    @BindView(R.id.tv_file_name)
    TextView mFileNameTv;
    @BindView(R.id.tv_file_size)
    TextView mFileSizeTv;
    @BindView(R.id.progress)
    ProgressBar mPb;
    @BindView(R.id.item_layout)
    View view;
    //-------------------View  End -------------------//


    //-------------------Field Start-------------------//
    private Socket mSocket;
    private File mFile;
    private long mSize;
    private ReceiveTask mTask;
    //-------------------Field  End -------------------//

    /**
     * ConnectWifiApActivity入口
     *
     * @param context Context
     */
    public static void startActivity(Context context) {
        context.startActivity(new Intent(context, ConnectWifiApActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_wifi);
        setTitle("连接热点");
        ButterKnife.bind(this);
        requestPermission();
    }

    @OnClick(R.id.btn_connect_wifi)
    void onConnectWifiClick() {
        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // wifi连接上之后，尝试连接socket
        Log.e(TAG, "是否连接上了wifi了：" + WifiUtil.isConnected());
        if (WifiUtil.isConnected()) {
            connectSocket();
        }
    }

    /**
     * 尝试连接socket
     */
    private void connectSocket() {
        final String ip = "192.168.43.1";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mSocket = new Socket(ip, OpenWifiApActivity.PORT);
                    if (isConnectSocket(mSocket)) {
                        // 连接上了 ，等待发送
                        Log.e(TAG, "连接上了");
                        waitingReceive();
                        initReceive(mSocket);
                    } else {
                        // 连接错了或者是还没连接上
                        Log.e(TAG, "还没连接上了");
                        waitingConnect();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    /**
     * 初始化接收的文件信息
     *
     * @param socket Socket
     */
    private void initReceive(final Socket socket) {
        InputStream in = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            in = socket.getInputStream();
            isr = new InputStreamReader(in, "UTF-8");
            br = new BufferedReader(isr);
            String line = br.readLine();
            Log.e(TAG, "receive:" + line);
            final String[] receive = line.split(",");
            File dir = new File(SAVE_PATH);
            if (!dir.exists()) {
                dir.mkdir();
            }
            mFile = new File(SAVE_PATH + receive[0]);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mStatusTv.setText("接收中...");
                    view.setVisibility(View.VISIBLE);
                    mFileNameTv.setText(String.valueOf(receive[0]));
                    mSize = Long.parseLong(receive[1]);
                    mFileSizeTv.setText(SendFileActivity.formatLength(mSize));
                    mPb.setMax(100);
                    // 开启接收线程
                    mTask = new ReceiveTask(socket, mFile);
                    mTask.execute();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断是否连接上了
     *
     * @param socket Socket
     * @return 是返回true
     */
    private boolean isConnectSocket(Socket socket) {
        if (socket == null) {
            return false;
        }

        try {
            socket.sendUrgentData(0x01);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 等待接收文件
     */
    private void waitingReceive() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStatusTv.setVisibility(View.VISIBLE);
                mStatusTv.setText("等待接收文件中...");
                mConnectWifiBtn.setVisibility(View.GONE);
                setTitle("我的IP：" + intToIp(WifiUtil.getConnectingInfo().getIpAddress()));
            }
        });
    }

    /**
     * 没连接上热点
     */
    private void waitingConnect() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectWifiBtn.setText("连接热点");
                mConnectWifiBtn.setVisibility(View.VISIBLE);
                setTitle("连接热点");
                mStatusTv.setVisibility(View.GONE);
            }
        });
    }

    /**
     * 将int类型ip转成string类型
     *
     * @param ip int类型的ip
     * @return String类型的ip
     */
    private String intToIp(int ip) {
        return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mSocket != null) {
                mSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
            mTask.cancel(true);
        }
    }


    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            //验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                    return;
                }
            }
        }

    }

    // 接收的task
    class ReceiveTask extends AsyncTask<Void, Long, Boolean> {

        private Socket socket;
        private File file;

        public ReceiveTask(Socket socket, File file) {
            this.socket = socket;
            this.file = file;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            InputStream in = null;
            FileOutputStream fos = null;
            long received = 0;

            try {
                in = socket.getInputStream();
                fos = new FileOutputStream(file);
                int len = 0;
                byte[] buf = new byte[1024 * 2];
                long sTime = SystemClock.currentThreadTimeMillis();
                long eTime = 0;
                while ((len = in.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                    received += len;
                    // 更新ui
                    eTime = SystemClock.currentThreadTimeMillis();
                    if (eTime - sTime > 200) {
                        sTime = eTime;
                        publishProgress(received);
                    }
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            super.onProgressUpdate(values);
            if (mSize == 0) {
                return;
            }
            int progress = (int) (values[0] / mSize);
            mPb.setProgress(progress);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean) {
                Toast.makeText(ConnectWifiApActivity.this, "接收成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ConnectWifiApActivity.this, "接收失败", Toast.LENGTH_SHORT).show();
            }

        }

    }


}
