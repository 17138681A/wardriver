package com.example.wardriver;
import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ExampleDialog.ExampleDialogListener {

    //View component:
    private LocationManager locationManager;
    private LocationListener locationListener;
    private WifiManager mainWifi;
    private WifiReceiver receiverWifi;
    private ListAdapter adapter;
    private ListView lvWifiDetails;
    private List wifiList;
    private ImageButton load;
    private Button set, open, email, save, openFolder;
    private ImageView gpsAlert;
    private EditText interval;

    //Variables:
    private static final int PERMISSION_REQUEST_CODE = 1; //Code for permission of writing external storage
    private long time = 0; //Refresh time
    private Calendar calendar; //System time
    private String strDate; //String of system time
    private SimpleDateFormat sdf; //Simple date format
    private SimpleDateFormat dff; //Default File name Format
    private String sl; //String of location's info
    private String fileName = ""; //Temporary memory of last valid file name
    private String tempData; //Temporary memory of current data list, update after each rescan
    private Handler scanHandler = new Handler(); //Handle each rescan
    private Handler alertHandler = new Handler(); //Handle the animation of alert symbol
    private Runnable scanRunnable = new Runnable() {
        @Override
        public void run() { //Update data list according to refresh time
            configureDate();
            scanWifiList();
            scanHandler.postDelayed(this,time);
        }
    };
    private Runnable alertRunnable = new Runnable() {
        @Override
        public void run() {//Alert animation when gps function off
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                gpsAlert.setVisibility(View.INVISIBLE);
                alertHandler.removeCallbacks(this);
            }
            else {

                if (gpsAlert.getVisibility() == View.INVISIBLE)
                    gpsAlert.setVisibility(View.VISIBLE);
                else
                    gpsAlert.setVisibility(View.INVISIBLE);
                alertHandler.postDelayed(this, 500);

            }
        }
    };


    @Override
    protected void onCreate( Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        interval = findViewById(R.id.interval);
        set = findViewById(R.id.set);
        save = findViewById(R.id.save);
        open = findViewById(R.id.open);
        email = findViewById(R.id.email);
        openFolder = findViewById(R.id.openFolder);
        load = findViewById(R.id.load);
        lvWifiDetails = findViewById(R.id.lvWifiDetails);
        gpsAlert = findViewById(R.id.alert);

        //Initialize wifi manager
        mainWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        receiverWifi = new WifiReceiver();
        registerReceiver(receiverWifi, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        //Initialize location manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                sl = "\nLongitude :: " + location.getLongitude() + "\nLatitude :: " + location.getLatitude();

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                askUserToOpenGPS();
            }
        };

        //Registers view components:
        load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadLocation();
                showToast("Default location is loaded");
            }
        });
        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(interval.getText().length() == 0)
                    showToast("Empty input!");
                    //Toast.makeText(MainActivity.this, "Empty input!", Toast.LENGTH_LONG).show();
                else if(interval.getText().length() > 3)
                    showToast("Value is too large! Modification denied");
                    //Toast.makeText(MainActivity.this, "Value is too large! Modification denied", Toast.LENGTH_LONG).show();
                else if (Integer.parseInt(interval.getText().toString()) < 5) {
                    showToast("Value is too small! Modification denied");
                    //Toast.makeText(MainActivity.this, "Value is too small! Modification denied", Toast.LENGTH_LONG).show();
                }else {
                    time = 1000 * Integer.parseInt(interval.getText().toString());
                    showToast("Refresh time is updated to " + time/1000 + " seconds");
                    //Toast.makeText(MainActivity.this, "Refresh time is updated to " + time/1000 + " seconds", Toast.LENGTH_LONG).show();
                }

            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(adapter.locationIsScanning()){
                    showToast("Please wait. Location is still scanning");
                    //Toast.makeText(MainActivity.this, "Please wait. Location is still scanning", Toast.LENGTH_LONG).show();
                }else {
                    stopScan();
                    openDialog("Save as");
                }
            }
        });

        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopScan();
                openDialog("Open");
            }
        });

        email.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopScan();
                openDialog("Send");
            }
        });

        openFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFolder();
                showToast("Open storage location");
                //Toast.makeText(MainActivity.this, "Check location folder", Toast.LENGTH_LONG).show();
            }
        });

        if(!isStoragePermissionGranted())
            requestPermission();
        time = 1000;
        startScan();
        time = 5000;


    }

    public void startScan(){
        scanHandler.postDelayed(scanRunnable, time); }

    public void stopScan(){
        scanHandler.removeCallbacks(scanRunnable);
    }

    //Scan location
    private void configure_location(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}
                        , 10);
            }
            return;
        }
        locationManager.requestLocationUpdates("gps", time, 0, locationListener);
    }

    class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
        }
    }

    //Set date for new data list
    public void configureDate(){
        calendar = Calendar.getInstance();
        sdf = new SimpleDateFormat("dd MMMM YYYY hh:mma");
        strDate = sdf.format(calendar.getTime());
    }

    //Request input of file name
    public void openDialog(String title){
        ExampleDialog exampleDialog = new ExampleDialog();
        exampleDialog.dialogTitle = title;
        exampleDialog.setCancelable(false);
        exampleDialog.show(getSupportFragmentManager(), "example dialog");
    }

    private void loadLocation(){
        sl = "\nLongitude :: default longitude\nLatitude :: default latitude";
        setAdapter();
    }

    //Set list adapter
    private void setAdapter() {
        adapter = new ListAdapter(getApplicationContext(), wifiList, sl, strDate);
        adapter.tempData = "";
        lvWifiDetails.setAdapter(adapter);
    }

    //Start wifi scanning
    private void scanWifiList() {
        mainWifi.startScan();
        wifiList = mainWifi.getScanResults();
        setAdapter();

    }

    //Request gps service
    public void askUserToOpenGPS() {
        AlertDialog.Builder mAlertDialog = new AlertDialog.Builder(this);
        // Setting Dialog Title
        mAlertDialog.setTitle("Location not available, Open GPS?")
                .setMessage("Activate GPS to use location services?")
                .setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        alertHandler.removeCallbacks(alertRunnable);
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        showToast("GPS inactivated. Please turn on your GPS!");
                        //Toast.makeText(MainActivity.this, "GPS inactivated. Please turn on your GPS!", Toast.LENGTH_LONG).show();
                        alertHandler.removeCallbacks(alertRunnable);
                        alertHandler.postDelayed(alertRunnable,0);
                        dialog.cancel();
                    }
                }).show();
    }

    //Remove wifi and location listeners after activity pause
    @Override
    protected void onPause() {
        locationManager.removeUpdates(locationListener);
        unregisterReceiver(receiverWifi);
        super.onPause();
    }

    //Revive wifi and location listeners after activity resume
    @Override
    protected void onResume(){
        gpsAlert.setVisibility(View.INVISIBLE);
        configure_location();
        registerReceiver(receiverWifi, new IntentFilter(
            WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    //Check whether storage permission is granted
    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("Yes","Permission is granted");
                return true;
            } else {

                Log.v("No","Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //Permission is automatically granted on sdk<23 upon installation
            Log.v("Yes","Permission is granted");
            return true;
        }
    }

    //Request permission for writing external storage
    private void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showToast("Write External Storage permission allows us to do store data. Please allow this permission in App Settings.");
            //Toast.makeText(this, "Write External Storage permission allows us to do store data. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    //Check whether external storage is available
    private boolean isExternalStorageWritable(){
        if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
            Log.i("Yes","SD card is writable");
            return true;
        } else {
            Log.i("No", "Not writable");
            return false;
        }
    }

    ///Avoid duplicate default file name
    private boolean fileExist(int ver, String name){
        String newName = name + "v" + String.valueOf(ver) + ".txt";
        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "WarDriver_data", newName);
        return file.exists();
    }

    //Save file after receive a file name
    private void saveFile(String name, String text, boolean emptyFileName){
        if(isExternalStorageWritable()) {
            try {
                File folder = new File(Environment.getExternalStorageDirectory(), "WarDriver_data");
                Log.v("Path",Environment.getExternalStorageDirectory().toString());
                if(!folder.exists())
                    folder.mkdirs();

                    String newName;
                    if(emptyFileName) {
                        int fileVer = 1;
                        while (fileExist(fileVer, name))
                            fileVer++;
                        newName = name + "v" + String.valueOf(fileVer) + ".txt";
                    }else
                        newName = name;

                    String extension = ".txt";
                    if(!getFileExt(newName).equals(extension) ) {
                        showToast("File type " + getFileExt(newName) + " not match. Please save as " + extension + " format!");
                        //Toast.makeText(this, "File type " + getFileExt(newName) + " not match. Please save as " + extension + " format!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    fileName = newName;
                    File file = new File(Environment.getExternalStorageDirectory() + File.separator + "WarDriver_data", fileName);

                    if(file.exists()) {
                        showToast("File already existed!");
                        //Toast.makeText(this, "File already existed!", Toast.LENGTH_LONG).show();
                        return;
                    }

                FileOutputStream fos = new FileOutputStream(file);
                fos.write(text.getBytes());
                fos.close();
                if(emptyFileName)
                    showToast("Empty input!\nFile saved to " + file);
                    //Toast.makeText(this, "Empty input!\nFile saved to " + file, Toast.LENGTH_LONG).show();
                else
                    showToast("File saved as " + file);
                    //Toast.makeText(this, "File saved to " + file, Toast.LENGTH_LONG).show();

            } catch (StringIndexOutOfBoundsException e){
                showToast("Please saved as .txt format");
                //Toast.makeText(this, "Please saved as .txt format", Toast.LENGTH_LONG).show();
            }catch (Exception e) {
                e.printStackTrace();
                showToast("Error saving file!");
                //Toast.makeText(this, "Error saving file!", Toast.LENGTH_LONG).show();
            }

        }else
            showToast("External storage not found!");
            //Toast.makeText(this, "External storage not found!", Toast.LENGTH_LONG).show();
    }

    //Read file after receive a file name
    private String readFile(String name){
        String text = "";

        try{
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + "WarDriver_data", name);
                if(!file.exists()) {
                    showToast("File not found!");
                    //Toast.makeText(this, "File not found!", Toast.LENGTH_LONG).show();
                    return text;
                }
            FileInputStream fis = new FileInputStream(file);
            int size = fis.available();
            byte[] buffer = new byte[size];
            fis.read(buffer);
            fis.close();
            text = new String(buffer);
        } catch (Exception e){
            e.printStackTrace();
            showToast("Error reading file!");
            //Toast.makeText(this, "Error reading file!", Toast.LENGTH_SHORT).show();
        }

        return text;
    }

    //Send file after receive a file name
    private void sendFile(String name){

        try {
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + "WarDriver_data", name);
            if (!file.exists()) {
                showToast("File not found!");
                //Toast.makeText(this, "File not found!", Toast.LENGTH_LONG).show();
                return;
            }
            Uri path = FileProvider.getUriForFile(this, "wardriver.folder.provider", file);

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            // Set the type to 'email'
            emailIntent.setType("vnd.android.cursor.dir/email");
            String to[] = {"william.tsang24@gmail.com"};
            emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
            // The attachment
            emailIntent.putExtra(Intent.EXTRA_STREAM, path);
            // The mail subject
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "War driving's data");
            startActivity(Intent.createChooser(emailIntent, "Send to..."));
        }catch (Exception e){
            e.printStackTrace();
            showToast("Error sending file!");
            //Toast.makeText(this, "Error sending file!", Toast.LENGTH_SHORT).show();
        }
    }

    //Open save location and check what file existed manually
    private void openFolder()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        Uri uri = Uri.parse(Environment.getExternalStorageDirectory().getPath());
        Log.i("path","The path is " + uri.toString());
        intent.setDataAndType(uri, "*/*");//specify your type
        startActivity(Intent.createChooser(intent, "Open folder"));
    }

    //Get file name extension (eg. .txt)
    private String getFileExt(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."));
    }

    //Show data in new pop up screen
    private void popData(String data){
        Intent intent = new Intent(this,Pop.class);
        intent.putExtra("data",data);
        startActivity(intent);
    }

    public void showToast(String text){

        Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER,0,0);
        toast.show();

    }

    //Method that listen input from ExampleDialog
    @Override
    public void setFileName(String name, String title){


        String action = title;
        tempData = adapter.getTempData();


        switch(action){

            //When request file writing
            case "Save as":

                if(adapter.locationIsScanning()){
                    showToast("Please wait. Location is still scanning");
                    //Toast.makeText(this, "Please wait. Location is still scanning", Toast.LENGTH_LONG).show();
                    break;
                }else {

                    if (name.length() == 0) {
                        dff = new SimpleDateFormat("ddMMyyyy_");
                        fileName = dff.format(calendar.getTime());
                    } else
                        fileName = name;

                    saveFile(fileName, tempData, name.length() == 0);

                }

                break;
            //When request file reading
            case "Open":

                if(name.length( )== 0){
                    if(fileName != "") {
                        popData(readFile(fileName));
                        showToast("Empty input! Opening last saved file");
                        //Toast.makeText(this, "Empty input! Opening last saved file", Toast.LENGTH_LONG).show();
                    } else
                        showToast("Empty input!");
                        //Toast.makeText(this, "Empty input!", Toast.LENGTH_LONG).show();
                } else
                    if(readFile(name)!="")
                        popData(readFile(name));

                break;

            //When request file sending
            case "Send":

                if(name.length( )== 0) {
                    if (fileName != "") {
                        sendFile(fileName);
                        showToast("Empty input!\nSending last saved file: ");
                        //Toast.makeText(this, "Empty input!\nSending last saved file: " + fileName, Toast.LENGTH_LONG).show();
                    } else
                        showToast("Empty input!");
                        //Toast.makeText(this, "Empty input!", Toast.LENGTH_LONG).show();
                } else
                    sendFile(name);

                break;
        }
    }
}