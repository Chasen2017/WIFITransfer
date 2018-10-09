package com.chasen.wifitransfer.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

import com.chasen.wifitransfer.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.btn_send)
    void onSendFileClick() {
        OpenWifiApActivity.startActivity(this);
    }

    @OnClick(R.id.btn_receive)
    void onReceiveFileClick() {
        ConnectWifiApActivity.startActivity(this);
    }


}
