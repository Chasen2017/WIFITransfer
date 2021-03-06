package com.chasen.wifitransfer.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author Chasen
 * @Data 2018/10/9
 * <p>
 * WIFI工具类
 */

public class WifiUtil {

    private static final String TAG = "WifiAPUtil";

    public static final int MESSAGE_AP_STATE_ENABLED = 1;
    public static final int MESSAGE_AP_STATE_FAILED = 2;
    //默认wifi密码
    private static final String DEFAULT_AP_PASSWORD = "12345678";
    private static Handler mHandler;
    private static Context mContext;
    private static WifiManager mWifiManager;
    //监听wifi热点的状态变化
    public static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
    public static final String EXTRA_WIFI_AP_STATE = "wifi_state";
    public static int WIFI_AP_STATE_DISABLING = 10;
    public static int WIFI_AP_STATE_DISABLED = 11;
    public static int WIFI_AP_STATE_ENABLING = 12;
    public static int WIFI_AP_STATE_ENABLED = 13;
    public static int WIFI_AP_STATE_FAILED = 14;

    public enum WifiSecurityType {
        WIFICIPHER_NOPASS, WIFICIPHER_WPA, WIFICIPHER_WEP, WIFICIPHER_INVALID, WIFICIPHER_WPA2
    }

    public static void init(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }


    public static boolean turnOnWifiAp(String str, String password, WifiSecurityType Type) {
        String ssid = str;
        //配置热点信息。
        WifiConfiguration wcfg = new WifiConfiguration();
        wcfg.SSID = new String(ssid);
        wcfg.networkId = 1;
        wcfg.allowedAuthAlgorithms.clear();
        wcfg.allowedGroupCiphers.clear();
        wcfg.allowedKeyManagement.clear();
        wcfg.allowedPairwiseCiphers.clear();
        wcfg.allowedProtocols.clear();

        if (Type == WifiSecurityType.WIFICIPHER_NOPASS) {
            Log.d(TAG, "wifi ap----no password");
            wcfg.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN, true);
            wcfg.wepKeys[0] = "";
            wcfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wcfg.wepTxKeyIndex = 0;
        } else if (Type == WifiSecurityType.WIFICIPHER_WPA) {
            Log.d(TAG, "wifi ap----wpa");
            //密码至少8位，否则使用默认密码
            if (null != password && password.length() >= 8) {
                wcfg.preSharedKey = password;
            } else {
                wcfg.preSharedKey = DEFAULT_AP_PASSWORD;
            }
            wcfg.hiddenSSID = false;
            wcfg.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wcfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wcfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            //wcfg.allowedKeyManagement.set(4);
            wcfg.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wcfg.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wcfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wcfg.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        } else if (Type == WifiSecurityType.WIFICIPHER_WPA2) {
            Log.d(TAG, "wifi ap---- wpa2");
            //密码至少8位，否则使用默认密码
            if (null != password && password.length() >= 8) {
                wcfg.preSharedKey = password;
            } else {
                wcfg.preSharedKey = DEFAULT_AP_PASSWORD;
            }
            wcfg.hiddenSSID = true;
            wcfg.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wcfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wcfg.allowedKeyManagement.set(4);
            //wcfg.allowedKeyManagement.set(4);
            wcfg.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wcfg.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wcfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wcfg.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        }
        try {
            Method method = mWifiManager.getClass().getMethod("setWifiApConfiguration",
                    wcfg.getClass());
            Boolean rt = (Boolean) method.invoke(mWifiManager, wcfg);
            Log.d(TAG, " rt = " + rt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return setWifiApEnabled();
    }

    //获取热点状态
    public static int getWifiAPState() {
        int state = -1;
        try {
            Method method2 = mWifiManager.getClass().getMethod("getWifiApState");
            state = (Integer) method2.invoke(mWifiManager);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        Log.i("WifiAP", "getWifiAPState.state " + state);
        return state;
    }

    private static boolean setWifiApEnabled() {
        //开启wifi热点需要关闭wifi
        while (mWifiManager.getWifiState() != WifiManager.WIFI_STATE_DISABLED) {
            mWifiManager.setWifiEnabled(false);
            try {
                Thread.sleep(200);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                return false;
            }
        }
        // 确保wifi 热点关闭。
        while (getWifiAPState() != WIFI_AP_STATE_DISABLED) {
            try {
                Method method1 = mWifiManager.getClass().getMethod("setWifiApEnabled",
                        WifiConfiguration.class, boolean.class);
                method1.invoke(mWifiManager, null, false);

                Thread.sleep(200);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        //开启wifi热点
        try {
            Method method1 = mWifiManager.getClass().getMethod("setWifiApEnabled",
                    WifiConfiguration.class, boolean.class);
            method1.setAccessible(true);
            method1.invoke(mWifiManager, null, true);
            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    //关闭WiFi热点
    public void closeWifiAp() {
        if (getWifiAPState() != WIFI_AP_STATE_DISABLED) {
            try {
                Method method = mWifiManager.getClass().getMethod("getWifiApConfiguration");
                method.setAccessible(true);
                WifiConfiguration config = (WifiConfiguration) method.invoke(mWifiManager);
                Method method2 = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                method2.invoke(mWifiManager, config, false);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public void regitsterHandler(Handler handler) {
        mHandler = handler;
    }

    public void unregitsterHandler() {
        mHandler = null;
    }


    //获取热点ssid
    public String getValidApSsid() {
        try {
            Method method = mWifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration configuration = (WifiConfiguration) method.invoke(mWifiManager);
            return configuration.SSID;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    //获取热点密码
    public String getValidPassword() {
        try {
            Method method = mWifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration configuration = (WifiConfiguration) method.invoke(mWifiManager);
            return configuration.preSharedKey;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }

    }

    //获取热点安全类型
    public int getValidSecurity() {
        WifiConfiguration configuration;
        try {
            Method method = mWifiManager.getClass().getMethod("getWifiApConfiguration");
            configuration = (WifiConfiguration) method.invoke(mWifiManager);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return WifiSecurityType.WIFICIPHER_INVALID.ordinal();
        }

        Log.i(TAG, "getSecurity security=" + configuration.allowedKeyManagement);
        if (configuration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) {
            return WifiSecurityType.WIFICIPHER_NOPASS.ordinal();
        } else if (configuration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)) {
            return WifiSecurityType.WIFICIPHER_WPA.ordinal();
        } else if (configuration.allowedKeyManagement.get(4)) { //4 means WPA2_PSK
            return WifiSecurityType.WIFICIPHER_WPA2.ordinal();
        }
        return WifiSecurityType.WIFICIPHER_INVALID.ordinal();
    }

    /**
     * 获取已连接当前热点的设备的IP地址
     *
     * @return connectIpList
     * @throws IOException
     */
    public static ArrayList<String> getConnectIp() throws IOException {
        ArrayList<String> connectIpList = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
        String line;
        while ((line = br.readLine()) != null) {
            String[] split = line.split(" +");
            if (split != null && split.length >= 4) {
                String ip = split[0];
                if (ip.contains(".")) {
                    connectIpList.add(ip);
                }
            }
        }
        return connectIpList;
    }

    /**
     * 是否已经开启了热点
     * @return 是返回true
     */
    public static  boolean isOpenAp() {
        return getWifiAPState() == WIFI_AP_STATE_ENABLED;
    }

    /**
     * wifi是否已经连接上了
     * @return 是返回true
     */
    public static boolean isConnected() {
        return mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED;
    }

    /**
     * 获取当前正在连接的wifi的信息
     * @return WifiInfo
     */
    public static WifiInfo getConnectingInfo() {
        return mWifiManager.getConnectionInfo();
    }

    /**
     * 获取附近的Wifi列表
     * @return
     */
    public static List<String> getWifiList() {
        mWifiManager.startScan();
        List<ScanResult> scanResults = mWifiManager.getScanResults();
        ArrayList<String> wifiList = new ArrayList<>();
        for (ScanResult scanResult : scanResults) {
            if (!wifiList.contains(scanResult.SSID)) {
                wifiList.add(scanResult.SSID);
            }
        }
        return wifiList;
    }

}

