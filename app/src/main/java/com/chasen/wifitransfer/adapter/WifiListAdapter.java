package com.chasen.wifitransfer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.chasen.wifitransfer.R;

import java.net.Socket;
import java.util.List;

/**
 *
 * 发送端，已连接上的设备列表适配器
 *
 * @Author Chasen
 * @Data 2018/10/9
 */

public class WifiListAdapter extends BaseAdapter {

    private Context mContext;
    private List<Socket> mSockets;

    public WifiListAdapter(Context context, List<Socket> sockets) {
        this.mContext = context;
        this.mSockets = sockets;
    }

    @Override
    public int getCount() {
        return mSockets.size();
    }

    @Override
    public Object getItem(int position) {
        return mSockets.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Socket socket = mSockets.get(position);
        ViewHolder viewHolder = null;
        if (socket == null) {
            return null;
        }

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_wifi_list, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.ipTv = convertView.findViewById(R.id.tv_connected_ip);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.ipTv.setText(socket.getInetAddress().toString().replace("/", ""));
        return convertView;
    }

    class ViewHolder {
        TextView ipTv;
    }
}
