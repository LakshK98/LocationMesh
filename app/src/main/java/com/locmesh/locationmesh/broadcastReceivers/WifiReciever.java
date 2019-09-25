package com.locmesh.locationmesh.broadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.util.Log;
import android.widget.Toast;

import com.locmesh.locationmesh.activities.MainActivity;

/**
 * Created by lakshkotian on 15/03/19.
 */

public class WifiReciever extends BroadcastReceiver {


    WifiP2pManager wP2pManager;
    Channel wP2pChannel;
    MainActivity mActivity;

    public WifiReciever(WifiP2pManager argManager,Channel argChannel,MainActivity argActivity){

        wP2pManager = argManager;
        wP2pChannel = argChannel;
        mActivity = argActivity;



    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

//            Log.d("MyActivity",thisDeviceName);

            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Toast.makeText(context,"Wifi is on",Toast.LENGTH_SHORT).show();
                Log.d("MyActivity","Wifi on");

//                mActivity.peerDiscover();
            } else {
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
//            Toast.makeText(context,"Peer List Changed",Toast.LENGTH_SHORT).show();
            Log.d("MyActivity","Peer list changed");


            if(wP2pManager!=null){
                wP2pManager.requestPeers(wP2pChannel,mActivity.peerListListener);
            }


        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (wP2pManager==null){
                Log.d("MyActivity","Manager null");

                return;
            }

            wP2pManager.requestConnectionInfo(wP2pChannel,mActivity.connectionInfoListener);

            NetworkInfo networkInfo= (NetworkInfo)intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            Log.d("MyActivity","BO  info: "+intent.getExtras().toString());

            Log.d("MyActivity","YO info: "+networkInfo.toString());
            mActivity.isSocketThreadCreated =false;
            if (networkInfo.isConnected()){
                wP2pManager.requestConnectionInfo(wP2pChannel,mActivity.connectionInfoListener);
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {


        }
    }
}
