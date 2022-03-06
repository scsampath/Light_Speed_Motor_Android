package com.example.lightspeedmotor;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.airbnb.lottie.LottieAnimationView;
import com.cardiomood.android.controls.gauge.SpeedometerGauge;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // Animation Variables
    private LottieAnimationView animationView;
    static int animationDelay = 0;

    // Toolbar Variables
    private Toolbar mToolbar;
    private Menu menu;
    private MenuItem bluetoothStatus;

    // Bluetooth Variables
    private BluetoothDevice btModule;
    private BluetoothSocket socket;
    private BluetoothSocket socketFallback;
    private static final UUID MY_UUID = UUID.fromString("0000110E-0000-1000-8000-00805F9B34FB");

    // Speedometer
    private SpeedometerGauge speedometer;
    private TextView speedTextView;
    private TextView modeTextView;

    // Seekbar
    private SeekBar throttle;

    // Shifters
    private ImageView leftShift;
    private ImageView rightShift;

    // Motor Logic
    private int mode = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Close Animation and make view visible
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // animation : invisible
                animationView.setVisibility(View.INVISIBLE);
                // toolbar : visible
                mToolbar.setVisibility(View.VISIBLE);
                // speedometer : visible
                speedometer.setVisibility(View.VISIBLE);
                speedTextView.setVisibility(View.VISIBLE);
                modeTextView.setVisibility(View.VISIBLE);
                // seekbar : visible
                throttle.setVisibility(View.VISIBLE);
                // shifters : visible
                leftShift.setVisibility(View.VISIBLE);
                rightShift.setVisibility(View.VISIBLE);
            }
        }, animationDelay);

        // Set up animation
        animationView = findViewById(R.id.animationView);

        // Set up toolbar
        mToolbar = findViewById(R.id.appToolbar);
        setSupportActionBar(mToolbar);

        // Set up speedometer
        speedometer = findViewById(R.id.speedometer);
        speedTextView = findViewById(R.id.speedTextView);
        modeTextView = findViewById(R.id.modeTextView);

        // Set up seekbar
        throttle = findViewById(R.id.throttle);
        throttle.setOnSeekBarChangeListener(seekBarChangeListener);

        // Set up shifters
        leftShift = findViewById(R.id.shiftLeftImage);
        rightShift = findViewById(R.id.shiftRightImage);
        leftShift.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                shiftModeLeft();
            }
        });
        rightShift.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                shiftModeRight();
            }
        });

        // configure value range and ticks
        speedometer.setMaxSpeed(6000);
        speedometer.setMajorTickStep(1000);
        speedometer.setMinorTicks(1);
        speedometer.setMinorTicks(5);
        speedometer.setLabelTextSize(1000);

        // Configure value range colors
        speedometer.addColoredRange(0, 3000, Color.GREEN);
        speedometer.addColoredRange(3000, 5000, Color.YELLOW);
        speedometer.addColoredRange(5000, 6000, Color.RED);
    }

    // SeekBar logic
    SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @SuppressLint("DefaultLocale")
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int speed = 6 * progress;
            String txData = String.format("%04d", speed);
            try {
                write(txData);
            } catch (IOException e) {
                e.printStackTrace();
            }
            speedometer.setSpeed(speed);
            speedTextView.setText(txData);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // called when the user first touches the SeekBar
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // called after the user finishes moving the SeekBar
        }
    };

    // Translate Mode number to String
    public String getMode(int modeNumber){
        switch(modeNumber) {
            case 0: return "R";

            case 2: return "L";

            case 3: return "M";

            case 4: return "H";

            default: return "S";
        }
    }

    public void shiftModeLeft(){
        if(mode != 0){
            mode -= 1;
            modeTextView.setText(getMode(mode));
        }
        if(mode == 0){
            leftShift.setColorFilter(R.color.black);
        }
        if(mode == 3){
            rightShift.clearColorFilter();
        }
    }

    public void shiftModeRight(){
        if(mode != 4){
            mode += 1;
            modeTextView.setText(getMode(mode));
        }
        if(mode == 4){
            rightShift.setColorFilter(R.color.black);
        }
        if(mode == 1){
            leftShift.clearColorFilter();
        }
    }

    // Toolbar button setup
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final Toolbar mToolbar = findViewById(R.id.appToolbar);
        mToolbar.inflateMenu(R.menu.main_activity_menu);
        mToolbar.setOnMenuItemClickListener(item -> onOptionsItemSelected(item));
        this.menu = menu;
        bluetoothStatus = menu.findItem(R.id.bluetoothStatus);
        return true;
    }

    // Toolbar button logic
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bluetoothSettings:
                Log.i("Bluetooth: ", "button pressed");
                getBluetoothAddresses();
                break;
            default:
                break;
        }
        return true;
    }

    public void getBluetoothAddresses(){
        ArrayList<String> deviceNames = new ArrayList<>();
        final ArrayList<String> devicesAddresses = new ArrayList<>();
        BluetoothAdapter btAdapter=BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices=btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0)
        {
            for (BluetoothDevice device : pairedDevices)
            {
                deviceNames.add(device.getName() + "\n" +device.getAddress());
                devicesAddresses.add(device.getAddress());
            }
        }
        showSelectionDialog(deviceNames, devicesAddresses);
    }

    private void showSelectionDialog(ArrayList deviceNames, ArrayList devicesAddresses) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.select_dialog_singlechoice, deviceNames.toArray(new String[devicesAddresses.size()]));

        alertDialog.setSingleChoiceItems(adapter, -1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        int position = ((AlertDialog) dialog)
                                .getListView()
                                .getCheckedItemPosition();
                        String deviceAddress = (String) devicesAddresses.get(position);
                        try {
                            startConnection(deviceAddress);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
        alertDialog.setTitle("Select a Bluetooth Module");
        alertDialog.show();
    }

    private void startConnection(String deviceAddress) throws IOException {
        if (deviceAddress == null || "".equals(deviceAddress)) {
            Log.e("Bluetooth: ", "No Bluetooth device has been selected.");
        } else {
            final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            btModule = btAdapter.getRemoteDevice(deviceAddress);

            Log.i("Bluetooth: ", "Stopping Bluetooth discovery.");
            btAdapter.cancelDiscovery();

            try {
                socket = btModule.createRfcommSocketToServiceRecord(MY_UUID);
                socket.connect();
                Log.i("Bluetooth: ", "Connected");
                // Change Bluetooth Status
                bluetoothStatus.setTitle("Connected");
            } catch (Exception e1) {
                Log.e("Bluetooth: ", "Error connecting socket, falling back to alternative method");

                Class<?> clazz = socket.getRemoteDevice().getClass();
                Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};

                try {
                    Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                    Object[] params = new Object[]{Integer.valueOf(1)};
                    socketFallback = (BluetoothSocket) m.invoke(socket.getRemoteDevice(), params);
                    socketFallback.connect();
                    socket = socketFallback;
                    Log.i("Bluetooth: ", "Connected");
                    // Change Bluetooth Status
                    bluetoothStatus.setTitle("Connected");
                } catch (Exception e2) {
                    Log.e("Bluetooth: ", "Error connecting fallback socket");
                    // Change Bluetooth Status
                    bluetoothStatus.setTitle("Disconnected");
                }
            }
        }
    }

    private void write(String data) throws IOException {
        socket.getOutputStream().write(data.getBytes());
    }
}