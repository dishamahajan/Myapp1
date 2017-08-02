package com.appmec.flashlightlite;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.SwitchCompat;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.michaelmuenzer.android.scrollablennumberpicker.ScrollableNumberPicker;
import com.michaelmuenzer.android.scrollablennumberpicker.ScrollableNumberPickerListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import yuku.ambilwarna.AmbilWarnaDialog;

public class MainActivity extends AppCompatActivity implements OnCheckedChangeListener {

    private CameraManager objCameraManager;
    private String mCameraId;
    private ImageButton isOnOFF;
    private Boolean isTorchOn;
    private AdView mAdView;
    private Camera camera;
    private CameraDevice cameraDevice;
    private CameraCaptureSession session;
    private CaptureRequest.Builder builder;
    Camera.Parameters parameters;
    PowerManager pm;
    Boolean screenOn;
    private Handler mTimerHandler = new Handler();
    private Handler mTimerHandler1 = new Handler();
    private Handler timerHandlerForDiscoLightColor = new Handler();
    public static final int[] colorArray = {Color.RED, Color.GREEN, Color.YELLOW, Color.BLUE, Color.CYAN, Color.GRAY};

    private SwitchCompat blinker;
    private SwitchCompat timer;
    boolean blink;
    boolean time;
    boolean discoLightFlag = false;
    private CountDownTimer countDownTimer;
    int blinkTimeValue = 100;
    int discoLightValue = 50;
    int discoLightColorValue = 50;
    int duration;
    Toast toast;
    public static long timepickerDuration = 30 * 1000;
    Button colorPicker;
    RelativeLayout relativeLayout;
    int DefaultColor;
    private Runnable runnableCode;
    private Runnable runnableCode1;
    private Runnable runnableForDiscoLightColor;
    int discoLightColor = 0;
    Button timerPicker;

    NotificationCompat.Builder notification;
    private NotificationManager mNotificationManager;
    private static final int notification_id = 123456;
    private RemoteViews remoteViews;
    //private TextView timerText;
    private Button discoLight;

    private String hmsTimeFormatter(long milliSeconds) {

        String hms = String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(milliSeconds),
                TimeUnit.MILLISECONDS.toMinutes(milliSeconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliSeconds)),
                TimeUnit.MILLISECONDS.toSeconds(milliSeconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliSeconds)));
        return hms;
    }

    private void stopCountDownTimer() {
        //timerText.setText("");
        timerPicker.setText("Set Time");
        countDownTimer.cancel();
    }

    private void startCountDownTimer() {

        countDownTimer = new CountDownTimer(timepickerDuration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                turnOnFlashLight();
                //timerText.setText(hmsTimeFormatter(millisUntilFinished));
                timerPicker.setText(hmsTimeFormatter(millisUntilFinished));
            }

            @Override
            public void onFinish() {
                if (camera != null) {
                    turnOffFlash();
                } else {
                    turnOffLight();
                }
                SwitchCompat switchCompat = (SwitchCompat) findViewById(R.id.timer);
                switchCompat.setChecked(false);
                timerPicker.setText("Set Time");
                //timerText.setText("");
                toast = Toast.makeText(MainActivity.this, "Timer : OFF", duration);
                toast.show();
            }
        }.start();
        countDownTimer.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        relativeLayout = (RelativeLayout) findViewById(R.id.main);
        colorPicker = (Button) findViewById(R.id.colorPicker);
        discoLight = (Button) findViewById(R.id.discoLight);
        DefaultColor = ContextCompat.getColor(MainActivity.this, R.color.white);

        colorPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OpenColorPickerDialog(false);
            }
        });

        isOnOFF = (ImageButton) findViewById(R.id.isOnOFF);
        timerPicker = (Button) findViewById(R.id.timePicker);
        //timerText = (TextView) findViewById(R.id.timerText);

        //for blinker
        blinker = (SwitchCompat) findViewById(R.id.blinker);
        blinker.setOnCheckedChangeListener(this);
        blinker.setChecked(false);

        //for timer
        timer = (SwitchCompat) findViewById(R.id.timer);
        timer.setOnCheckedChangeListener(this);
        timer.setChecked(false);

        //for notification
        notification = new NotificationCompat.Builder(this);
        notification.setAutoCancel(true);

        Boolean isFlashAvailable = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        if (!isFlashAvailable) {
            AlertDialog alert = new AlertDialog.Builder(MainActivity.this).create();
            alert.setTitle(getString(R.string.app_name));
            alert.setMessage(getString(R.string.msg_error));
            alert.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.lbl_ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            alert.show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionDialogBox();
            objCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                mCameraId = objCameraManager.getCameraIdList()[0];
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
            permissionDialogBox();
            objCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                CameraCharacteristics cameraCharacteristics = objCameraManager.getCameraCharacteristics("0");
                boolean flashAvailable = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (flashAvailable) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        objCameraManager.openCamera("0", new CameraDeviceStateCallback(), null);
                    }
                }
            } catch (Exception e) {

            }
        } else {
            getCamera();
        }
/*
        if (checkVersion() && !hasPermission("android.permission.CAMERA")) {
            permissionDialogBox();
        } else {
            if (camera != null) {
                turnOnFlash();
            } else {
                turnOnLight();
            }
            isTorchOn = false;
        }
*/
        isTorchOn = false;
        isOnOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (checkVersion() && !hasPermission("android.permission.CAMERA")) {
                        permissionDialogBox();
                    } else {
                        if (isTorchOn) {
                            mNotificationManager.cancel(notification_id);

                            turnOffFlashLight();
                        } else {
                            showNotification();
                            turnOnFlashLight();
                            isTorchOn = true;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        final ScrollableNumberPicker blinkerNumberPicker = (ScrollableNumberPicker) findViewById(R.id.number_picker_blinker);
        blinkerNumberPicker.setListener(new ScrollableNumberPickerListener() {
            @Override
            public void onNumberPicked(int value) {
                blinkTimeValue = value;
            }
        });
        //runnable for blinker
        runnableForBlinker();

        discoLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(discoLightFlag){
                    discoLight.setBackground(getResources().getDrawable(R.drawable.disco_off));
                    discoLightFlag=!discoLightFlag;
                }else{
                    discoLight.setBackground(getResources().getDrawable(R.drawable.disco_on));
                    discoLightFlag=!discoLightFlag;
                }
                relativeLayout.setBackground(getResources().getDrawable(R.drawable.background));
            }
        });
        //runnable for discolight
        runnableForDiscolight();

        timerPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OpenTimePicker();
            }
        });

        //Admob
        MobileAds.initialize(this, "ca-app-pub-7860341576927713~5587659182");
        mAdView = (AdView) findViewById(R.id.adViewAd);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("D8D6B049EDAB3CB2227DD36B3ED29F2D")
                .addTestDevice("25F149879ED72631F3CB460DEED0436A")
                .addTestDevice("B117F7C5611FB5503E6D2BD2CCA8C928")
                .build();
        mAdView.loadAd(adRequest);

    }

    private void runnableForDiscolight() {
        runnableCode1 = new Runnable() {
            @Override
            public void run() {
                if (discoLightFlag) {
                    if (camera != null) {
                        turnOnFlash();
                        try {
                            Thread.sleep(discoLightValue);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        turnOffFlash();
                    } else {
                        turnOnLight();
                        try {
                            Thread.sleep(discoLightValue);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        turnOffLight();
                    }
                    discoLightValue = discoLightValue + 25;
                    if (discoLightValue == 250) {
                        discoLightValue = 25;
                    }
                }
                mTimerHandler1.postDelayed(runnableCode1, discoLightValue);
            }
        };

        mTimerHandler1.post(runnableCode1);

        runnableForDiscoLightColor = new Runnable() {
            @Override
            public void run() {
                if (discoLightFlag) {
                    setRelativeBackground(discoLightColor++);
                    if (discoLightColor == colorArray.length - 1) {
                        discoLightColor = 0;
                    }
                }
                timerHandlerForDiscoLightColor.postDelayed(runnableForDiscoLightColor, discoLightColorValue);
            }
        };
        timerHandlerForDiscoLightColor.post(runnableForDiscoLightColor);
    }

    private void runnableForBlinker() {
        runnableCode = new Runnable() {
            @Override
            public void run() {
                if (blink) {
                    if (camera != null) {
                        turnOnFlash();
                        try {
                            Thread.sleep(blinkTimeValue);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        turnOffFlash();
                    } else {
                        turnOnLight();
                        try {
                            Thread.sleep(blinkTimeValue);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        turnOffLight();
                    }
                }
                mTimerHandler.postDelayed(runnableCode, blinkTimeValue);
            }
        };
        mTimerHandler.post(runnableCode);
    }

    private void turnOnFlashLight() {
        if (camera != null) {
            turnOnFlash();
        } else {
            turnOnLight();
        }
    }

    private void turnOffFlashLight() {
        if (camera != null) {
            turnOffFlash();
        } else {
            turnOffLight();
        }
        isTorchOn = false;
    }

    private void OpenTimePicker() {
        DialogFragment newFragment = new PickerDialogFragment();
        newFragment.setStyle(2,1);
        newFragment.show(getFragmentManager(), "dialog");
    }

    public void setRelativeBackground(int i) {
        final int color = colorArray[i];
        relativeLayout.setBackgroundColor(color);
    }

    private void showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notification.setSmallIcon(R.drawable.notification_icon);
        } else {
            notification.setSmallIcon(R.drawable.ic_launcher);
        }
        notification.setTicker("Flash is ON!");
        notification.setWhen(System.currentTimeMillis());
        notification.setContentTitle("Flashlight Lite");
        notification.setContentText("Flash is ON! Click to turn it OFF!");

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setContentIntent(pendingIntent);

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.notify(notification_id, notification.build());
    }

    private void OpenColorPickerDialog(boolean AlphaSupport) {

        AmbilWarnaDialog ambilWarnaDialog = new AmbilWarnaDialog(MainActivity.this, DefaultColor, AlphaSupport, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(AmbilWarnaDialog ambilWarnaDialog, int color) {
                DefaultColor = color;
                relativeLayout.setBackgroundColor(color);
             /*   Dialog dialog=new Dialog(MainActivity.this,android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                dialog.getWindow().setBackgroundDrawableResource(color);
                dialog.show();*/
            }

            @Override
            public void onCancel(AmbilWarnaDialog ambilWarnaDialog) {
                Toast.makeText(MainActivity.this, "Color Picker Closed", Toast.LENGTH_SHORT).show();
            }
        });
        ambilWarnaDialog.show();
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        final ScrollableNumberPicker blinkerNumberPicker = (ScrollableNumberPicker) findViewById(R.id.number_picker_blinker);
        blinkerNumberPicker.setListener(new ScrollableNumberPickerListener() {
            @Override
            public void onNumberPicked(int value) {
                blinkTimeValue = value;
            }
        });
        if (R.id.blinker == buttonView.getId()) {
            if (isChecked) {
                SwitchCompat switchCompat = (SwitchCompat) findViewById(R.id.timer);
                switchCompat.setChecked(false);
                blink = true;
                time = false;
                duration = Toast.LENGTH_SHORT;
                toast = Toast.makeText(MainActivity.this, "Blinker : ON", duration);
                toast.show();
            } else {
                blink = false;
                duration = Toast.LENGTH_SHORT;
                toast = Toast.makeText(MainActivity.this, "Blinker : OFF", duration);
                toast.show();
            }
        }
        if (R.id.timer == buttonView.getId()) {
            if (isChecked) {
                startCountDownTimer();
                SwitchCompat switchCompat = (SwitchCompat) findViewById(R.id.blinker);
                switchCompat.setChecked(false);
                //    time = true;
                blink = false;
                turnOnFlashLight();
                duration = Toast.LENGTH_SHORT;
                toast = Toast.makeText(MainActivity.this, "Timer : ON", duration);
                toast.show();
            } else {
                stopCountDownTimer();
                duration = Toast.LENGTH_SHORT;
                turnOffFlashLight();
                toast = Toast.makeText(MainActivity.this, "Timer : OFF", duration);
                toast.show();
            }
        }
    }

    class CameraDeviceStateCallback extends CameraDevice.StateCallback {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            try {
                builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);
                builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
                builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                List<Surface> list = new ArrayList<Surface>();
                SurfaceTexture mSurfaceTexture = new SurfaceTexture(1);
                Size size = getSmallestSize(cameraDevice.getId());
                mSurfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
                Surface mSurface = new Surface(mSurfaceTexture);
                list.add(mSurface);
                builder.addTarget(mSurface);
                camera.createCaptureSession(list, new CameraCaptureSessionStateCallback(), null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {

        }

        @Override
        public void onError(CameraDevice camera, int error) {

        }
    }

    class CameraCaptureSessionStateCallback extends CameraCaptureSession.StateCallback {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            MainActivity.this.session = session;
            try {
                MainActivity.this.session.setRepeatingRequest(builder.build(), null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }
    }

    private Size getSmallestSize(String cameraId) throws CameraAccessException {
        Size[] outputSizes = objCameraManager.getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                .getOutputSizes(SurfaceTexture.class);
        if (outputSizes == null || outputSizes.length == 0) {
            throw new IllegalStateException(
                    "Camera " + cameraId + "doesn't support any outputSize.");
        }
        Size chosen = outputSizes[0];
        for (Size s : outputSizes) {
            if (chosen.getWidth() >= s.getWidth() && chosen.getHeight() >= s.getHeight()) {
                chosen = s;
            }
        }
        return chosen;
    }

    public void turnOnLight() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                objCameraManager.setTorchMode(mCameraId, true);
                isOnOFF.setImageResource(R.drawable.on);
            } else {
                try {
                    builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                    session.setRepeatingRequest(builder.build(), null, null);
                    isOnOFF.setImageResource(R.drawable.on);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void turnOffLight() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                objCameraManager.setTorchMode(mCameraId, false);
                isOnOFF.setImageResource(R.drawable.off);
            } else {
                try {
                    builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                    session.setRepeatingRequest(builder.build(), null, null);
                    isOnOFF.setImageResource(R.drawable.off);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void turnOnFlash() {
        if (this.camera == null || this.parameters == null) {
            return;
        }
        this.parameters = camera.getParameters();
        this.parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        this.camera.setParameters(parameters);
        this.camera.startPreview();
        isOnOFF.setImageResource(R.drawable.on);
        isTorchOn = true;
    }

    private void turnOffFlash() {
        if (this.camera == null || this.parameters == null) {
            return;
        }
        this.parameters = this.camera.getParameters();
        this.parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        this.camera.setParameters(parameters);
        this.camera.stopPreview();
        isOnOFF.setImageResource(R.drawable.off);
        isTorchOn = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    private void permissionDialogBox() {
        String[] perms = {"android.permission.FLASHLIGHT", "android.permission.CAMERA"};
        int permsRequestCode = 200;
        requestPermissions(perms, permsRequestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 200:
                boolean flashLightAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean cameraAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                break;
        }
    }

    private boolean hasPermission(String permission) {
        if (checkVersion()) {
            return (checkSelfPermission(permission)) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean checkVersion() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
    }

    private void getCamera() {
        if (this.camera == null) {
            try {
                this.camera = Camera.open();
                this.parameters = this.camera.getParameters();
            } catch (Exception e) {

            }
        }
    }
}

