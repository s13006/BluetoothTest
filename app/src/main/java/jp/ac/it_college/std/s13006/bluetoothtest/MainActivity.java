package jp.ac.it_college.std.s13006.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;


public class MainActivity extends ActionBarActivity {
    private static final int REQUEST_ENABLE_BT = 0x01;
    private BluetoothAdapter btAdapter;
    private DeviceItemAdapter deviceItemAdapter;
    private boolean isDiscover = false;


    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                addItem(device);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        deviceItemAdapter = new DeviceItemAdapter(this , android.R.layout.simple_list_item_1);
        ListView deviceList = (ListView) findViewById(R.id.device_list);
        deviceList.setAdapter(deviceItemAdapter);
        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = deviceItemAdapter.getItem(position).getValue();
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                intent.putExtra("remote device", device);
                startActivity(intent);
                finish();
            }
        });

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onPostResume();
        deviceItemAdapter.clear();
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                addItem(device);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.discover:
                startDiscover();
                return true;
            case R.id.server:
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("make server", true);
                startActivity(intent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startDiscover() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        btAdapter.startDiscovery();
        isDiscover = true;

        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }

    private void stopDiscover() {
        if (isDiscover) {
            btAdapter.cancelDiscovery();
            unregisterReceiver(receiver);
            isDiscover = false;
        }
    }

    private void addItem(BluetoothDevice device) {
        deviceItemAdapter.add(new DeviceItem(device.getName() + "[" + device.getAddress() + "]", device));
    }

    public static class DeviceItem extends Pair<String, BluetoothDevice> {
        public DeviceItem(String first, BluetoothDevice second) {
            super(first, second);
        }

        public String getName() {
            return super.first;
        }

        public BluetoothDevice getValue() {
            return super.second;
        }
    }

    public static class DeviceItemAdapter extends ArrayAdapter<DeviceItem> {

        public DeviceItemAdapter(Context context, int resource) {
            super(context, resource);
        }

        public DeviceItemAdapter(Context context, int resource, DeviceItem[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getView(position, convertView, parent);
            view.setText(getItem(position).getName());
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getDropDownView(position, convertView, parent);
            view.setText(getItem(position).getName());
            return view;
        }
    }
}

