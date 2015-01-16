package jp.ac.it_college.std.s13006.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class ChatActivity extends ActionBarActivity {
    private static final int MESSAGE_READ = 0x1;
    private static final int MESSAGE_CONNECTED = 0x2;
    //bluetooth(SPP)用
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String NAME = "ぶるーつーすちゃっとてすと";
    private BluetoothAdapter btAdapter;
    private BluetoothDevice remote;
    private boolean isServer = false;
    private Boolean isRunning = true;
    private AcceptThread acceptThread;
    private ConnectedThread connectedThread;
    private ConnectThread connectThread;
    private Handler handler;
    private ArrayAdapter<String> adapter;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        Intent intent = getIntent();
        isServer = intent.getBooleanExtra("make server", false);
        if (isServer) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        } else {
            remote = (BluetoothDevice) intent.getParcelableExtra("remote device");
            connectThread = new ConnectThread(remote);
            connectThread.start();
        }

        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_READ :
                        byte[] buf = (byte[]) msg.obj;
                        String text = new String(buf, 0, msg.arg1);
                        adapter.add("-->" + text);
                        break;
                    case MESSAGE_CONNECTED:
                        Toast.makeText(getApplicationContext(), "接続出来ました", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter = new ArrayAdapter<String> (this, android.R.layout.simple_list_item_1);
        ((ListView) findViewById(R.id.log)).setAdapter(adapter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        isRunning = false;
        try {
            if (acceptThread != null && acceptThread.isAlive()) {
                acceptThread.join();
            }

            if (connectThread != null && connectThread.isAlive()) {
                connectThread.join();
            }

            if (connectedThread != null && connectedThread.isAlive()) {
                connectedThread.join();
            }
        } catch (InterruptedException e) {

        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
        handler.obtainMessage(MESSAGE_CONNECTED).sendToTarget();
    }

    public void sendRemote(View v) {
        String text = ((TextView) findViewById(R.id.input_message)).getText().toString();
        connectedThread.write(text.getBytes());
        adapter.add("<--" + text);
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket ServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to ServerSocket,
            // because ServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = btAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) { }
            ServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (isRunning) {
                try {
                    socket = ServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket(socket);
                    try {
                        ServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                ServerSocket.close();
            } catch (IOException e) { }
        }
    }
    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to socket,
            // because socket is final
            BluetoothSocket tmp = null;
            this.device = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            socket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            btAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                socket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    socket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(socket);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream in;
        private final OutputStream out;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            in = tmpIn;
            out = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            while (isRunning) {
                try {
                    bytes = in.read(buffer);
                    handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                out.write(bytes);
            } catch (IOException e) { }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
