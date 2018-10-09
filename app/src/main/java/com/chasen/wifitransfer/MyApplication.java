package com.chasen.wifitransfer;

import com.chasen.wifitransfer.utils.WifiUtil;

/**
 * @Author Chasen
 * @Data 2018/10/9
 */

public class MyApplication extends android.app.Application {

    public static MyApplication sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        // init
        WifiUtil.init(this);
    }
}
