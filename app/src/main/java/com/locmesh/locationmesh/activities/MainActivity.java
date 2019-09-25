package com.locmesh.locationmesh.activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.locmesh.locationmesh.R;
import com.locmesh.locationmesh.adapters.MainRecyclerAdapter;
import com.locmesh.locationmesh.broadcastReceivers.WifiReciever;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;



public class MainActivity extends AppCompatActivity {

    private final String TAG="MyActivity";
    private RecyclerView recyclerView;
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION=12;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiReciever mReceiver;
    private Button discoverBtn,connectBtn,sendBtn;
    private TextView testTv,locTv,nameTv;
    String NICK_NAME="no model";
    String MY_ID;
    boolean isStarted=false;
    boolean isConnected=false;

    String FILE_NAME="location_table.txt";


    HashMap<String,String[]> locTable=new HashMap<>();
    public boolean isSocketThreadCreated =false;

    Channel channel;
    WifiManager wifiManager;
    WifiP2pManager wifiP2pManager;
    List <WifiP2pDevice> peers = new ArrayList<>();
    String [] deviceNameArray;
    WifiP2pDevice[] deviceArray;

    LocationManager locationManager;

    static final int MSG_READ=1;
    private Handler discoverPeerHandler;

    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive;

    LocationListener locationListenerGPS=new LocationListener() {
        @Override
        public void onLocationChanged(android.location.Location location) {
            setMyLoc(location.getLatitude(),location.getLongitude());
            Log.d(TAG, "location changed" );
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

            Log.d(TAG, "status changed" );
        }

        @Override
        public void onProviderEnabled(String provider) {

            Log.d(TAG, "provider disturbed" );
        }

        @Override
        public void onProviderDisabled(String provider) {

            Log.d(TAG, "provider disabled" );

        }
    };
    public void setMyLoc(double latitude,double longitude){
        String msg="Lat: "+latitude + "\nLon: "+longitude;
        locTv.setText(msg);
//        myLoc=latitude+"x"+longitude;
        locTable.put(MY_ID,new String[]{NICK_NAME,latitude+"",""+longitude});
        setRecycler();
//        printLocTable();
        Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_LONG).show();
        Log.d(TAG, "setMyLoc executed" );


    }
    public ArrayList<String[]> setRecycler(){

        ArrayList<String[]> valuesList = new ArrayList<>(locTable.values());
        Log.d(TAG,"recycle size : "+valuesList.size());
        recyclerView.swapAdapter(new MainRecyclerAdapter(valuesList),false);

        return valuesList;
    }
    public String stringizeLocTable(){

        String locTableString="";
        for (String id : locTable.keySet())
        {
            // search  for value
            String info [] = locTable.get(id);
            locTableString+=id+"$"+info[0]+"$" +info[1]+"$"+info[2]+"$";
        }
        return locTableString;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = (RecyclerView) findViewById(R.id.main_recycler_view);



        discoverPeerHandler = new Handler();
        nameTv=findViewById(R.id.name_tv);
        locTv=findViewById(R.id.loc_tv);
        BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();

        String msg="Lat: "+0 + "\nLon: "+0;
        locTv.setText(msg);
        NICK_NAME = myDevice.getName();
        MY_ID= Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);




        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);




        mAdapter = new MainRecyclerAdapter(new ArrayList<>(locTable.values()));
        recyclerView.setAdapter(mAdapter);

        setupWifi();
        discoverBtn= findViewById(R.id.discover_button);


        load();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);


        nameTv.setText(NICK_NAME);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
            Log.d(TAG,"startloc update");
        }

        discoverBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isStarted) {
                    isStarted = false;
                    Toast.makeText(getApplicationContext(), "Service stopped", Toast.LENGTH_SHORT).show();

                    stopRepeatingTask();
                } else {
                    isStarted = true;
                    Toast.makeText(getApplicationContext(), "Service started", Toast.LENGTH_SHORT).show();


                    startRepeatingTask();

                }
            }
        });

        connectBtn= findViewById(R.id.connect_button);
        connectBtn.setVisibility(View.INVISIBLE);
        connectBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToPeers();
            }
        });

        testTv= findViewById(R.id.test_tv);


        sendBtn= findViewById(R.id.send_button);
        sendBtn.setVisibility(View.INVISIBLE);
        sendBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg="yeah boi";
                Log.d(TAG,"Send click");

                sendReceive.write(msg.getBytes());
            }
        });




    }

    int discoveryCounter=0;
    Runnable periodicalDiscovery = new Runnable() {
        @Override
        public void run() {
            if(discoveryCounter==30) {
                discoveryCounter=0;
                resumeExecAfterInterval();
                stopRepeatingTask();
                alreadyConnectedAddresses.clear();
                Log.d(TAG,"5 min over Pausing");
                Toast.makeText(getApplicationContext(), "5 min over Pausing", Toast.LENGTH_SHORT).show();


            }
            else{
                discoveryCounter++;
                try {
                    Log.d(TAG, "periodicalDiscovery");
                    if (isStarted && !isConnected) {
                        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
//                            Toast.makeText(getApplicationContext(), "Discovering peers", Toast.LENGTH_SHORT).show();
                                // Code for when the discovery initiation is successful goes here.
                                // No services have actually been discovered yet, so this meth
                                Log.d(TAG, "Discovering peers");
                                connectToPeers();
                            }

                            @Override
                            public void onFailure(int reasonCode) {
                                Toast.makeText(getApplicationContext(), "Discovery Failed: " + reasonCode, Toast.LENGTH_SHORT).show();
                                // Code for when the discovery initiation fails goes here.
                                // Alert the user that something went wrong.
                                Log.d(TAG, "Discovering Failed : " + reasonCode);
                            }
                        });

                    }
//                updateStatus(); //this function can change value of mInterval.
                } finally {
                    // 100% guarantee that this always happens, even if
                    // your update method throws an exception
                    discoverPeerHandler.postDelayed(periodicalDiscovery, 10000);
                }
            }
        }
    };

    void startRepeatingTask() {
        periodicalDiscovery.run();
    }

    void stopRepeatingTask() {
        discoverPeerHandler.removeCallbacks(periodicalDiscovery);
    }

    void resumeExecAfterInterval(){

        final Handler resumeExecutionHandler = new Handler();
        resumeExecutionHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startRepeatingTask();
                Log.d(TAG,"Resuming");
                Toast.makeText(getApplicationContext(), "Resuming after 2 min", Toast.LENGTH_SHORT).show();

            }
        }, 120000);

    }

    boolean tryingToConnect=false;
    HashSet<String>alreadyConnectedAddresses=new HashSet<>();
    public void connectToPeers(){
        tryingToConnect=true;
        Random r = new Random();
        int delayDuration= (r.nextInt(10)+10)*100;
        boolean isNotFirst=false;
        for (final WifiP2pDevice device :peers) {

            if (!alreadyConnectedAddresses.contains(device.deviceAddress) ){
                if(isNotFirst){
                    delayDuration+= 5000;

                }
                isNotFirst=true;


                Toast.makeText(getApplicationContext(), "Waiting for "+delayDuration, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Waiting for "+delayDuration);


                final Handler delayConnectHandler = new Handler();
                delayConnectHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        final WifiP2pConfig config = new WifiP2pConfig();
                        config.deviceAddress = device.deviceAddress;
                        Log.d(TAG, "Trying to connect to " + device.deviceName);

                        if(!isConnected) {

                            wifiP2pManager.connect(channel, config, new ActionListener() {
                                @Override
                                public void onSuccess() {
                                    isConnected = true;

                                    Toast.makeText(getApplicationContext(), "Connected to " + device.deviceName, Toast.LENGTH_SHORT).show();
                                    isSocketThreadCreated = false;
                                    alreadyConnectedAddresses.add(config.deviceAddress);
                                    Log.d(TAG, "Connected to ");
                                }

                                @Override
                                public void onFailure(int i) {

                                    Toast.makeText(getApplicationContext(), "Failed to connect" + device.deviceName, Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "Failed to connect");
                                }
                            });
                        }
                        else{
                            Toast.makeText(getApplicationContext(), "Already connected not trying to connect to" + device.deviceName, Toast.LENGTH_SHORT).show();
                        }

                    }
                }, delayDuration);

            }

        }
        Log.d(TAG,"End of for");
        tryingToConnect=false;
        }

    public void startLocationUpdates(){
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(getApplicationContext(), "startlocationupdate", Toast.LENGTH_LONG).show();

            Location location=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(location!=null){

                setMyLoc(location.getLatitude(),location.getLongitude());
            }
            else
                setMyLoc(19.08934,72.87665);

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 600000, 10, locationListenerGPS);
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {

            case MY_PERMISSION_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "ACCESS_FINE_LOCATION permission was granted", Toast.LENGTH_LONG).show();
                    startLocationUpdates();
                    Log.d(TAG,"rewq accept stat loc uuopdate");
                } else {
                    Toast.makeText(getApplicationContext(), "ACCESS_FINE_LOCATION denied", Toast.LENGTH_LONG).show();
                    Log.d(TAG,"rewq denied stat loc uuopdate");
                }

                break;
        }
    }
    public void setupWifi(){

        // Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);


        wifiManager= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(this, getMainLooper(), null);

        if(wifiManager.isWifiEnabled()){
//            peerDiscover();
        }
        else{
            wifiManager.setWifiEnabled(true);
        }

        mReceiver = new WifiReciever(wifiP2pManager, channel, this);
        registerReceiver(mReceiver, intentFilter);


    }

    Handler handler= new Handler(new Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case MSG_READ:
                    byte[] readBuff=(byte[])msg.obj;
                    String locTableString=new String (readBuff,0,msg.arg1);
                    Log.d(TAG,"msg reci:"+locTableString);
                    String [] locTableArr= locTableString.split("\\$");
                    for (int i=0;i<locTableArr.length;i+=4){
                        locTable.put(locTableArr[i],new String[]{locTableArr[i+1],locTableArr[i+2],locTableArr[i+3]});

                        Log.d(TAG,"received id :"+locTableArr[i]);

                    }
                    setRecycler();
                    break;

            }
            return false;
        }
    });
    public void peerDiscover(){
        Log.d(TAG,"PeerDiscover");
        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(),"Discovering peers",Toast.LENGTH_SHORT).show();
                // Code for when the discovery initiation is successful goes here.
                // No services have actually been discovered yet, so this meth\
            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(getApplicationContext(),"Discovery Failed",Toast.LENGTH_SHORT).show();
                // Code for when the discovery initiation fails goes here.
                // Alert the user that something went wrong.
            }
        });
    }

    public PeerListListener peerListListener = new PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if (!tryingToConnect) {

                List<WifiP2pDevice> refreshedPeers = new ArrayList<>(peerList.getDeviceList());
                Toast.makeText(getApplicationContext(), "Peer list size " + refreshedPeers.size(), Toast.LENGTH_SHORT).show();
                if (!peerList.getDeviceList().equals(peers)) {
                    peers.clear();
                    peers.addAll(refreshedPeers);

                    int index = 0;
                    deviceArray = new WifiP2pDevice[refreshedPeers.size()];
                    deviceNameArray = new String[refreshedPeers.size()];

                    for (WifiP2pDevice device : refreshedPeers) {
                        deviceArray[index] = device;

                        deviceNameArray[index++] = device.deviceName;
//                        Toast.makeText(getApplicationContext(), "FOUND" + device.deviceName, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Found peer " + device.deviceAddress);


                    }


                }
                if (peers.size() == 0) {

//                    Toast.makeText(getApplicationContext(), "No device found", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "No peers ");
                }


            }
            else{
                Log.d(TAG,"Peer list change ignored");
            }


        }
    };
boolean isServerSocketCreated=false;
ServerSocket serverSocket;
    public WifiP2pManager.ConnectionInfoListener connectionInfoListener = new ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            Log.d(TAG,"connected info: "+wifiP2pInfo.toString());
            final InetAddress groupOwnerAddress =wifiP2pInfo.groupOwnerAddress;
            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner){
                isConnected=true;

                Log.d(TAG,"I am host ");
                if(!isServerSocketCreated){
                    try {
                        serverSocket = new ServerSocket(8888);
                        isServerSocketCreated=true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                if(!isSocketThreadCreated) {


                    serverClass = new ServerClass(serverSocket);
                    serverClass.start();
                    isSocketThreadCreated =true;

                }

            }else if(wifiP2pInfo.groupFormed){
                isConnected=true;

                Log.d(TAG,"I am client ");
                if(!isSocketThreadCreated) {
                    clientClass = new ClientClass(groupOwnerAddress);
                    clientClass.start();
                    isSocketThreadCreated =true;

                }
            }else if(!wifiP2pInfo.groupFormed){
                Log.d(TAG,"Disconnected(grp not formed)");
                isConnected=false;

            }

        }
    };
    public  void disconnect() {
        if (wifiP2pManager != null && channel != null) {
            wifiP2pManager.requestGroupInfo(channel, new GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && wifiP2pManager != null && channel != null
                            && group.isGroupOwner()) {
                        wifiP2pManager.removeGroup(channel, new ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "removeGroup onSuccess -");
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d(TAG, "removeGroup onFailure -" + reason);
                            }
                        });
                    }
                }
            });
        }
    }
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        save();

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"destroy");
        unregisterReceiver(mReceiver);
        stopRepeatingTask();

    }
int counter=0;
    public class ServerClass extends Thread{
        Socket socket;
        ServerSocket serverSocket;

        ServerClass(ServerSocket serverSocket){
            this.serverSocket=serverSocket;

        }

        public void run(){
            try {
                Log.d(TAG,"Server run");

                if(serverSocket!=null){

                    Log.d(TAG,"isclosed:"+serverSocket.isClosed());

                    Log.d(TAG,"isbound:"+serverSocket.isBound());
                }
//                serverSocket= new ServerSocket(8888);

//                serverSocket.setReuseAddress(true);

//                serverSocket.bind(new InetSocketAddress(8888));
                counter++;
                Log.d(TAG,"counter:"+counter);


                socket=serverSocket.accept();
                sendReceive=new SendReceive(socket);
                sendReceive.start();

                sendReceive.write(stringizeLocTable().getBytes());
                Log.d(TAG,"Server end of run");

                Log.d(TAG,"writing: "+stringizeLocTable());
            } catch (IOException e) {
                Log.d(TAG,e.toString());
                e.printStackTrace();
            }
            catch (Exception e){

                Log.d(TAG,e.toString());

            }

        }
    }

    public class ClientClass extends Thread{

        Socket socket;
        String hostAdd;

        public ClientClass(InetAddress hostAddress){
            hostAdd=hostAddress.getHostAddress();
            socket = new Socket();
        }

        @Override
        public void run() {
            try {
                Log.d(TAG,"Client run");

                socket.connect(new InetSocketAddress(hostAdd,8888),500);
                counter++;
                Log.d(TAG,"counter:"+counter);
                sendReceive=new SendReceive(socket);

                sendReceive.start();

                sendReceive.write(stringizeLocTable().getBytes());

                Log.d(TAG,"writing: "+stringizeLocTable());
                Log.d(TAG,"Client end of run");

            } catch (IOException e) {
                Log.d(TAG,"Client exception");
                e.printStackTrace();
            }
        }
    }

    private class SendReceive extends Thread{
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;
        public SendReceive(Socket skt){
            socket =skt;
            try{
                inputStream=socket.getInputStream();
                outputStream=socket.getOutputStream();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte [] buffer= new byte[1024];
            int bytes;

            Log.d(TAG,"SendRecieve run");

            while(socket!=null){
                try {
                    bytes=inputStream.read(buffer);
                    Log.d(TAG,"Number of bytes received"+bytes);
                    if(bytes>0)
                    {

                        Log.d(TAG,"SendRecieve bytes>0");
                        handler.obtainMessage(MSG_READ,bytes,-1,buffer).sendToTarget();
                        Looper.prepare();
                        final Handler closeSocketHandler = new Handler();
                        closeSocketHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
//                                    socket.setSoLinger(true, 1);
//
//                                    socket.close();

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                socket=null;

                                disconnect();

                                isSocketThreadCreated =false;

                                Log.d(TAG,"Socket closed");
                            }
                        }, 2000);
                        Looper.loop();


                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
//        public void write(byte[] bytes){
//            try {
//                outputStream.write(bytes);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        public void disconnectFromPeer(){
//            disconnect();
//        }
        public void write(final byte[] bytes) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        outputStream.write(bytes);
                        Log.d(TAG,"written ");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }


    public void save(){
//        StringBuilder text = new StringBuilder("\n Start:");
        String locTableString=stringizeLocTable();
        String filePath;

//        for (String sub : savedList){
//            text.append(" ").append(sub);
//        }
        Log.d(TAG,"save reached");

        FileOutputStream fos=null;

        try {
            fos=openFileOutput(FILE_NAME,Context.MODE_APPEND);
            fos.write(locTableString.getBytes());

            filePath=getFilesDir()+"/"+FILE_NAME;

//            Toast.makeText(getApplicationContext(),"Added file to "+filePath,Toast.LENGTH_SHORT).show();
            Log.d(TAG,"Added file to "+filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos!=null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }




    }
    public void load(){
//        StringBuilder text = new StringBuilder("\n Start:");

        String filePath = getFilesDir() + "/" +FILE_NAME;

//        for (String sub : savedList){
//            text.append(" ").append(sub);
//        }


        FileInputStream fis = null;
        try {
            fis = openFileInput(FILE_NAME);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            String locTableString= sb.toString();

            Log.d(TAG,"file reci:"+locTableString);
            String [] locTableArr= locTableString.split("\\$");
            if(locTableArr.length>=4) {
                for (int i = 0; i < locTableArr.length; i += 4) {
                    locTable.put(locTableArr[i], new String[]{locTableArr[i + 1], locTableArr[i + 2], locTableArr[i + 3]});

                    Log.d(TAG, "received id :" + locTableArr[i]);

                }
                setRecycler();
            }
        } catch (FileNotFoundException e) {
            Log.d(TAG,"File not found");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (fis!=null){
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }



    }
}
