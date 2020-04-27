package il.ac.tau.adviplab.bluetoothtry;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.util.UUID;



@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final String HR_SENSOR_ADDRESS = "44:EA:D8:F9:BD:A6"; //MAC address of thermometer
    SharedPreferences sharedPreferences;
    BluetoothDevice myDevice;
    UUID HEART_RATE_SERVICE_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    UUID HEART_RATE_MEASUREMENT_CHAR_UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb");

    BluetoothAdapter bluetoothAdapter;
    BluetoothManager bluetoothManager;
    BluetoothGatt mBluetoothGatt;

    DataToSend dataToSend = new DataToSend();
    //boolean isThermometerFound = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        sharedPreferences = this.getSharedPreferences("HashMapBluetoothTry", Context.MODE_PRIVATE);
        Toast.makeText(getApplicationContext(), "started app", Toast.LENGTH_LONG).show();
        Log.i("started", "application started!");
        bluetoothManager= (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                Log.d("LeScan","doing LE scan");
                if(device.getAddress().equals(HR_SENSOR_ADDRESS)){
                    Log.i("LeScan","Found thermometer!");
                    myDevice = device;
                    findViewById(R.id.progress_loader).setVisibility(View.GONE);
                    findViewById(R.id.SearchingText).setVisibility(View.GONE);
                    findViewById(R.id.PleaseMakeSureTxt).setVisibility(View.GONE);

                    findViewById(R.id.ForeheadText).setVisibility(View.VISIBLE);
                    findViewById(R.id.ForeheadTempText).setVisibility(View.VISIBLE);

                    Toast toast = Toast.makeText(getApplicationContext(), "Found thermometer", Toast.LENGTH_LONG);
                    toast.show();
                    //isThermometerFound = true;
                    startBLEGat();
                }
            }
        };

        bluetoothAdapter.startLeScan(scanCallback);

        //need to use stoplescan somewhere


    }


    public void startBLEGat(){
        Log.i("found device","gatt starting");

        BluetoothGattCallback gattCallback = new BluetoothGattCallback(){
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState){
                Log.i("onConnectionStateChange","state changed to:" + newState);
                if (newState == BluetoothProfile.STATE_CONNECTED){
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            boolean ans = mBluetoothGatt.discoverServices();
                            Log.i("called in UI thread", "Discover Services started: " + ans);
                        }
                    });
                    Log.i("onConnectionStateChange", "gat discover services");
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status){

                Log.i("onServicesDiscovered","Services discovered");
                BluetoothGattCharacteristic characteristic = gatt.getService(HEART_RATE_SERVICE_UUID).getCharacteristic(HEART_RATE_MEASUREMENT_CHAR_UUID);

                BluetoothGattDescriptor descriptor1 = characteristic.getDescriptors().get(0);
                BluetoothGattDescriptor descriptor2 = characteristic.getDescriptors().get(1);

                descriptor1.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                descriptor2.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor1);
                gatt.writeDescriptor(descriptor2);
                gatt.setCharacteristicNotification(characteristic, true);



            }
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

                byte[] bytesArr = characteristic.getValue();
                int byte1int = bytesArr[1] & 0xFF;
                int byte2int = bytesArr[2] & 0xFF;
                String byte1Str = String.valueOf(byte1int);
                String byte2Str = String.valueOf(byte2int);
                byte1Str = byte1Str + "-" + byte2Str;

                String temperatureValue = sharedPreferences.getString(byte1Str, "none");

                if(temperatureValue.equals("none")){
                    buildHashMap();
                    temperatureValue = sharedPreferences.getString(byte1Str, "too low or too high");
                }
                if(dataToSend.forehead_temp.equals("")){
                    dataToSend.forehead_temp = temperatureValue;
                    final String finalTemperatureValue = temperatureValue;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView foreheadTempText = findViewById(R.id.ForeheadTempText);
                            foreheadTempText.setText(finalTemperatureValue);
                            findViewById(R.id.cmText).setVisibility(View.VISIBLE);
                            findViewById(R.id.cmTempText).setVisibility(View.VISIBLE);
                        }
                    });

                }
                else if(dataToSend.cm_temp.equals("")){
                    dataToSend.cm_temp = temperatureValue;
                    final String finalTemperatureValue = temperatureValue;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView cmTempText = findViewById(R.id.cmTempText);
                            cmTempText.setText(finalTemperatureValue);
                            findViewById(R.id.infectedText).setVisibility(View.VISIBLE);
                            findViewById(R.id.InfectedTempText).setVisibility(View.VISIBLE);
                        }
                    });
                }
                else if(dataToSend.infection_temp.equals("")){
                    dataToSend.infection_temp=temperatureValue;
                }
                Log.i("onCharacteristicChanged","measured temprature is: " + temperatureValue + " bytes are: " + byte1Str);

            }

        };

        mBluetoothGatt = myDevice.connectGatt(this, true, gattCallback); //true?
        Log.i("started gatt","gatt init");
    }

    void buildHashMap(){
        Log.i("buildHashMap","building hash table for first time!");
        sharedPreferences.edit().putString("54-11","28.7").apply();
        sharedPreferences.edit().putString("64-11","28.8").apply();
        sharedPreferences.edit().putString("74-11","28.9").apply();
        sharedPreferences.edit().putString("84-11","29").apply();
        sharedPreferences.edit().putString("94-11","29.1").apply();
        sharedPreferences.edit().putString("104-11","29.2").apply();
        sharedPreferences.edit().putString("114-11","29.3").apply();
        sharedPreferences.edit().putString("124-11","29.4").apply();
        sharedPreferences.edit().putString("134-11","29.5").apply();
        sharedPreferences.edit().putString("144-11","29.6").apply();
        sharedPreferences.edit().putString("154-11","29.7").apply();
        sharedPreferences.edit().putString("164-11","29.8").apply();
        sharedPreferences.edit().putString("174-11","29.9").apply();
        sharedPreferences.edit().putString("184-11","30").apply();
        sharedPreferences.edit().putString("194-11","30.1").apply();
        sharedPreferences.edit().putString("204-11","30.2").apply();
        sharedPreferences.edit().putString("214-11","30.3").apply();
        sharedPreferences.edit().putString("224-11","30.4").apply();
        sharedPreferences.edit().putString("234-11","30.5").apply();
        sharedPreferences.edit().putString("244-11","30.6").apply();
        sharedPreferences.edit().putString("254-11","30.7").apply();
        sharedPreferences.edit().putString("8-12","30.8").apply();
        sharedPreferences.edit().putString("18-12","30.9").apply();
        sharedPreferences.edit().putString("28-12","31").apply();
        sharedPreferences.edit().putString("38-12","31.1").apply();
        sharedPreferences.edit().putString("48-12","31.2").apply();
        sharedPreferences.edit().putString("58-12","31.3").apply();
        sharedPreferences.edit().putString("68-12","31.4").apply();
        sharedPreferences.edit().putString("78-12","31.5").apply();
        sharedPreferences.edit().putString("88-12","31.6").apply();
        sharedPreferences.edit().putString("98-12","31.7").apply();
        sharedPreferences.edit().putString("108-12","31.8").apply();
        sharedPreferences.edit().putString("118-12","31.9").apply();
        sharedPreferences.edit().putString("128-12","32").apply();
        sharedPreferences.edit().putString("138-12","32.1").apply();
        sharedPreferences.edit().putString("148-12","32.2").apply();
        sharedPreferences.edit().putString("158-12","32.3").apply();
        sharedPreferences.edit().putString("168-12","32.4").apply();
        sharedPreferences.edit().putString("178-12","32.5").apply();
        sharedPreferences.edit().putString("188-12","32.6").apply();
        sharedPreferences.edit().putString("198-12","32.7").apply();
        sharedPreferences.edit().putString("208-12","32.8").apply();
        sharedPreferences.edit().putString("218-12","32.9").apply();
        sharedPreferences.edit().putString("228-12","33").apply();
        sharedPreferences.edit().putString("238-12","33.1").apply();
        sharedPreferences.edit().putString("248-12","33.2").apply();
        sharedPreferences.edit().putString("2-13","33.3").apply();
        sharedPreferences.edit().putString("12-13","33.4").apply();
        sharedPreferences.edit().putString("22-13","33.5").apply();
        sharedPreferences.edit().putString("32-13","33.6").apply();
        sharedPreferences.edit().putString("42-13","33.7").apply();
        sharedPreferences.edit().putString("52-13","33.8").apply();
        sharedPreferences.edit().putString("62-13","33.9").apply();
        sharedPreferences.edit().putString("72-13","34").apply();
        sharedPreferences.edit().putString("82-13","34.1").apply();
        sharedPreferences.edit().putString("92-13","34.2").apply();
        sharedPreferences.edit().putString("102-13","34.3").apply();
        sharedPreferences.edit().putString("112-13","34.4").apply();
        sharedPreferences.edit().putString("122-13","34.5").apply();
        sharedPreferences.edit().putString("132-13","34.6").apply();
        sharedPreferences.edit().putString("142-13","34.7").apply();
        sharedPreferences.edit().putString("152-13","34.8").apply();
        sharedPreferences.edit().putString("162-13","34.9").apply();
        sharedPreferences.edit().putString("172-13","35").apply();
        sharedPreferences.edit().putString("182-13","35.1").apply();
        sharedPreferences.edit().putString("192-13","35.2").apply();
        sharedPreferences.edit().putString("202-13","35.3").apply();
        sharedPreferences.edit().putString("212-13","35.4").apply();
        sharedPreferences.edit().putString("222-13","35.5").apply();
        sharedPreferences.edit().putString("232-13","35.6").apply();
        sharedPreferences.edit().putString("242-13","35.7").apply();
        sharedPreferences.edit().putString("252-13","35.8").apply();
        sharedPreferences.edit().putString("6-14","35.9").apply();
        sharedPreferences.edit().putString("16-14","36").apply();
        sharedPreferences.edit().putString("26-14","36.1").apply();
        sharedPreferences.edit().putString("36-14","36.2").apply();
        sharedPreferences.edit().putString("46-14","36.3").apply();
        sharedPreferences.edit().putString("56-14","36.4").apply();
        sharedPreferences.edit().putString("66-14","36.5").apply();
        sharedPreferences.edit().putString("76-14","36.6").apply();
        sharedPreferences.edit().putString("86-14","36.7").apply();
        sharedPreferences.edit().putString("96-14","36.8").apply();
        sharedPreferences.edit().putString("106-14","36.9").apply();
        sharedPreferences.edit().putString("116-14","37").apply();
        sharedPreferences.edit().putString("126-14","37.1").apply();
        sharedPreferences.edit().putString("136-14","37.2").apply();
        sharedPreferences.edit().putString("146-14","37.3").apply();
        sharedPreferences.edit().putString("156-14","37.4").apply();
        sharedPreferences.edit().putString("166-14","37.5").apply();
        sharedPreferences.edit().putString("176-14","37.6").apply();
        sharedPreferences.edit().putString("186-14","37.7").apply();
        sharedPreferences.edit().putString("196-14","37.8").apply();
        sharedPreferences.edit().putString("206-14","37.9").apply();
        sharedPreferences.edit().putString("216-14","38").apply();
        sharedPreferences.edit().putString("226-14","38.1").apply();
        sharedPreferences.edit().putString("236-14","38.2").apply();
        sharedPreferences.edit().putString("246-14","38.3").apply();
        sharedPreferences.edit().putString("0-15","38.4").apply();
        sharedPreferences.edit().putString("10-15","38.5").apply();
        sharedPreferences.edit().putString("20-15","38.6").apply();
        sharedPreferences.edit().putString("30-15","38.7").apply();
        sharedPreferences.edit().putString("40-15","38.8").apply();
        sharedPreferences.edit().putString("50-15","38.9").apply();
        sharedPreferences.edit().putString("60-15","39").apply();
        sharedPreferences.edit().putString("70-15","39.1").apply();
        sharedPreferences.edit().putString("80-15","39.2").apply();
        sharedPreferences.edit().putString("90-15","39.3").apply();
        sharedPreferences.edit().putString("100-15","39.4").apply();
        sharedPreferences.edit().putString("110-15","39.5").apply();
        sharedPreferences.edit().putString("120-15","39.6").apply();
        sharedPreferences.edit().putString("130-15","39.7").apply();
        sharedPreferences.edit().putString("140-15","39.8").apply();
        sharedPreferences.edit().putString("150-15","39.9").apply();
        sharedPreferences.edit().putString("160-15","40").apply();
        sharedPreferences.edit().putString("170-15","40.1").apply();
        sharedPreferences.edit().putString("180-15","40.2").apply();
        sharedPreferences.edit().putString("190-15","40.3").apply();
        sharedPreferences.edit().putString("200-15","40.4").apply();
        sharedPreferences.edit().putString("210-15","40.5").apply();
        sharedPreferences.edit().putString("220-15","40.6").apply();
        sharedPreferences.edit().putString("230-15","40.7").apply();
        sharedPreferences.edit().putString("240-15","40.8").apply();
        sharedPreferences.edit().putString("250-15","40.9").apply();
        sharedPreferences.edit().putString("4-16","41").apply();
        sharedPreferences.edit().putString("14-16","41.1").apply();
        sharedPreferences.edit().putString("24-16","41.2").apply();
        sharedPreferences.edit().putString("34-16","41.3").apply();
        sharedPreferences.edit().putString("44-16","41.4").apply();
        sharedPreferences.edit().putString("54-16","41.5").apply();
        sharedPreferences.edit().putString("64-16","41.6").apply();
        sharedPreferences.edit().putString("74-16","41.7").apply();
        sharedPreferences.edit().putString("84-16","41.8").apply();
        sharedPreferences.edit().putString("94-16","41.9").apply();
        sharedPreferences.edit().putString("104-16","42").apply();
        sharedPreferences.edit().putString("114-16","42.1").apply();
        sharedPreferences.edit().putString("124-16","42.2").apply();
        sharedPreferences.edit().putString("134-16","42.3").apply();
        sharedPreferences.edit().putString("144-16","42.4").apply();
        sharedPreferences.edit().putString("154-16","42.5").apply();
        sharedPreferences.edit().putString("164-16","42.6").apply();
        sharedPreferences.edit().putString("174-16","42.7").apply();
        sharedPreferences.edit().putString("184-16","42.8").apply();
        sharedPreferences.edit().putString("194-16","42.9").apply();
        sharedPreferences.edit().putString("204-16","43").apply();
        sharedPreferences.edit().putString("214-16","43.1").apply();
        sharedPreferences.edit().putString("224-16","43.2").apply();
        sharedPreferences.edit().putString("234-16","43.3").apply();
        sharedPreferences.edit().putString("244-16","43.4").apply();
        sharedPreferences.edit().putString("254-16","43.5").apply();
        sharedPreferences.edit().putString("8-17","43.6").apply();
        sharedPreferences.edit().putString("18-17","43.7").apply();
        sharedPreferences.edit().putString("28-17","43.8").apply();
        sharedPreferences.edit().putString("38-17","43.9").apply();
        sharedPreferences.edit().putString("48-17","44").apply();
        sharedPreferences.edit().putString("58-17","44.1").apply();
        sharedPreferences.edit().putString("68-17","44.2").apply();
        sharedPreferences.edit().putString("78-17","44.3").apply();
        sharedPreferences.edit().putString("88-17","44.4").apply();
        sharedPreferences.edit().putString("98-17","44.5").apply();
        sharedPreferences.edit().putString("108-17","44.6").apply();
        sharedPreferences.edit().putString("118-17","44.7").apply();
        sharedPreferences.edit().putString("128-17","44.8").apply();
        sharedPreferences.edit().putString("138-17","44.9").apply();
        sharedPreferences.edit().putString("148-17","45").apply();
        sharedPreferences.edit().putString("158-17","45.1").apply();
        sharedPreferences.edit().putString("168-17","45.2").apply();
        sharedPreferences.edit().putString("178-17","45.3").apply();
        sharedPreferences.edit().putString("188-17","45.4").apply();
        sharedPreferences.edit().putString("198-17","45.5").apply();
        sharedPreferences.edit().putString("208-17","45.6").apply();
        sharedPreferences.edit().putString("218-17","45.7").apply();
        sharedPreferences.edit().putString("228-17","45.8").apply();
        sharedPreferences.edit().putString("238-17","45.9").apply();
        sharedPreferences.edit().putString("248-17","46").apply();
        sharedPreferences.edit().putString("2-18","46.1").apply();
        sharedPreferences.edit().putString("12-18","46.2").apply();
        sharedPreferences.edit().putString("22-18","46.3").apply();
        sharedPreferences.edit().putString("32-18","46.4").apply();
        sharedPreferences.edit().putString("42-18","46.5").apply();
        sharedPreferences.edit().putString("52-18","46.6").apply();
        sharedPreferences.edit().putString("62-18","46.7").apply();
        sharedPreferences.edit().putString("72-18","46.8").apply();
        sharedPreferences.edit().putString("82-18","46.9").apply();
        sharedPreferences.edit().putString("92-18","47").apply();
        sharedPreferences.edit().putString("102-18","47.1").apply();
        sharedPreferences.edit().putString("112-18","47.2").apply();
        sharedPreferences.edit().putString("122-18","47.3").apply();
        sharedPreferences.edit().putString("132-18","47.4").apply();
        sharedPreferences.edit().putString("142-18","47.5").apply();
        sharedPreferences.edit().putString("152-18","47.6").apply();
        sharedPreferences.edit().putString("162-18","47.7").apply();
        sharedPreferences.edit().putString("172-18","47.8").apply();
        sharedPreferences.edit().putString("182-18","47.9").apply();
        sharedPreferences.edit().putString("192-18","48").apply();
        sharedPreferences.edit().putString("202-18","48.1").apply();
        sharedPreferences.edit().putString("212-18","48.2").apply();
        sharedPreferences.edit().putString("222-18","48.3").apply();
        sharedPreferences.edit().putString("232-18","48.4").apply();
        sharedPreferences.edit().putString("242-18","48.5").apply();
        sharedPreferences.edit().putString("252-18","48.6").apply();


    }
}
