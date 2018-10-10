package com.chasen.wifitransfer.activities;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chasen.wifitransfer.R;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.DecimalFormat;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SendFileActivity extends AppCompatActivity {

    //-------------------Const Start-------------------//
    public static final String TAG = "SendFileActivity";
    public static final String PATH_KEY = "path_key";
    //-------------------Const  End -------------------//

    //-------------------view Start-------------------//
    @BindView(R.id.tv_status)
    TextView mStatusTv;
    @BindView(R.id.tv_file_name)
    TextView mFileNameTv;
    @BindView(R.id.tv_file_size)
    TextView mFileSizeTv;
    @BindView(R.id.progress)
    ProgressBar mPb;
    //-------------------view  End -------------------//

    //-------------------Field Start-------------------//
    private File mFile;
    private String mPath;
    private long mSize;
    private long mSentLength = 0;
    private static Socket mSocket;
    private SendFileTask mTask;

    private boolean isSendInitInfo = false;

    //-------------------Field  End -------------------//
    /**
     * SendFileActivity入口方法
     * @param context Context
     * @param path 需要传递一个路径过来
     */
    public static void startActivity(Context context, String path, Socket socket) {
        Intent intent = new Intent(context, SendFileActivity.class);
        intent.putExtra(PATH_KEY, path);
        context.startActivity(intent);
        mSocket = socket;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_file);
        ButterKnife.bind(this);
        init();
        sendInit();
    }

    /**
     * 先发送文件信息给接收方
     */
    private void sendInit() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                PrintWriter pw = null;
                try {
                    pw = new PrintWriter(mSocket.getOutputStream());
                    pw.print(mFile.getName()+","+String.valueOf(mSize)+"\n");
                    pw.flush();
                    mTask = new SendFileTask(mSocket, mFile);
                    mTask.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void init() {
        mStatusTv.setText("发送中...");
        Intent intent = getIntent();
        if (intent != null) {
            mPath = intent.getStringExtra(PATH_KEY);
        }
        if (!TextUtils.isEmpty(mPath)) {
            mFile = new File(mPath);
            mSize = mFile.length();
            mFileNameTv.setText(String.valueOf(mFile.getName()));
            mFileSizeTv.setText(formatLength(mSize));
            mPb.setMax(100);
        }
    }

    /**
     * 格式化文件大小显示
     * @param length 文件大小
     * @return 格式化后的文件大小
     */
    public static String formatLength(long length) {
        DecimalFormat df = new DecimalFormat("0.00");
        String s;
        double size;
        if (length >= 1024*1024*1024) { // G
            size = (length/(1024*1024))/1024D;
            s = df.format(size)+"G";
        } else if (length >= 1024 * 1024) { // M
            size = (length/1024)/1024D;
            s = df.format(size)+"M";
        } else if (length >= 1024) { // K
            size = length/1024D;
            s = df.format(size)+"Kb";
        } else {  // B
            size = length;
            s = df.format(size)+"B";
        }
        return s;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
            mTask.cancel(true);
        }
    }

    class SendFileTask extends AsyncTask<Void, Long, Boolean> {

        private Socket socket;
        private File file;

        public SendFileTask(Socket socket, File file) {
            this.socket = socket;
            this.file = file;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            OutputStream os = null;
            FileInputStream fis = null;
            byte[] buf = new byte[1024 * 4];
            int len = 0;
            try {
                os = socket.getOutputStream();
                fis = new FileInputStream(file);
                long sTime = SystemClock.currentThreadTimeMillis();
                long eTime = 0;
                while ((len = fis.read(buf)) != -1) {
                    os.write(buf, 0, len);
                    mSentLength += len;
                    eTime = SystemClock.currentThreadTimeMillis();
                    if (eTime - sTime >= 200) {
                        publishProgress(mSentLength);
                        sTime = eTime;
                        Log.e(TAG, "sent:"+ mSentLength);
                    }
                }
                os.flush();
                publishProgress(mSize);
                return true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
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
            mPb.setProgress((int) (values[0] / mSize) * 100);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean) {
                Toast.makeText(SendFileActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SendFileActivity.this, "发送失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
