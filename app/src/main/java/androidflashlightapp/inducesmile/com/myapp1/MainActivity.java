package androidflashlightapp.inducesmile.com.myapp1;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.SwitchCompat;
import android.util.Size;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.ikovac.timepickerwithseconds.MyTimePickerDialog;
import com.ikovac.timepickerwithseconds.TimePicker;
import com.michaelmuenzer.android.scrollablennumberpicker.ScrollableNumberPicker;
import com.michaelmuenzer.android.scrollablennumberpicker.ScrollableNumberPickerListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import yuku.ambilwarna.AmbilWarnaDialog;

public class MainActivity extends AppCompatActivity implements OnCheckedChangeListener {

    private CameraManager objCameraManager;
    private String mCameraId;
    private ImageButton isOnOFF;
    private Boolean isTorchOn;

    private AdView mAdView;
    private Menu menu;
    private Camera camera;
    private CameraDevice cameraDevice;
    private CameraCaptureSession session;
    private CaptureRequest.Builder builder;
    Camera.Parameters parameters;

    private Handler mTimerHandler = new Handler();
    private Handler mTimerHandlerForDiscoLightBlinker = new Handler();
    private Handler timerHandlerForDiscoLightColor = new Handler();
    private Handler timerHandlerForSos = new Handler();
    private Handler timerHandlerForMorseCode = new Handler();

    public static final int[] colorArray = {Color.RED, Color.GREEN, Color.YELLOW, Color.BLUE, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.DKGRAY, Color.BLACK, Color.LTGRAY};

    private SwitchCompat blinker;
    boolean blink;
    int blinkTimeValue = 100;
    private Runnable runnableCodeBlinker;

    private SwitchCompat timer;
    boolean time;
    private CountDownTimer countDownTimer;
    public static long timepickerDuration = 30 * 1000;
    private Runnable runnableCode1Timer;

    Button timerPicker;
    Button colorPicker;

    Button sos;
    private Runnable runnableCodeSos;
    boolean sosFlag = false;
    int sosblinktime = 100;

    Button morseCode;
    private Runnable runnableCodeMorseCode;
    boolean morseFlag = false;
    int morseblinktime = 100;
    String morseCodeText = "";
    boolean discoLightFlag = false;
    int discoLightValue = 50;
    int discoLightColorValue = 50;
    private Runnable runnableForDiscoLightColor;
    int discoLightColor = 0;
    private Button discoLight;

    Toast toast;

    RelativeLayout relativeLayout;
    int DefaultColor;

    NotificationCompat.Builder notification;
    private NotificationManager mNotificationManager;
    private static final int notification_id = 123456;

    public static final String PREFS_NAME = "FlashlightLiteFile";

    public boolean soundFlag = false;
    MediaPlayer mp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        relativeLayout = (RelativeLayout) findViewById(R.id.main);
        mp = MediaPlayer.create(this, R.raw.soho);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.ic_action_name);
        getSupportActionBar().setLogo(R.drawable.ic_action_name);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        int color = settings.getInt("bg_Color", 0);
        if (color != 0) {
            /*Drawable a = getDrawable(color);*/
            relativeLayout.setBackgroundColor(color);
        }

        isOnOFF = (ImageButton) findViewById(R.id.isOnOFF);
        timerPicker = (Button) findViewById(R.id.timePicker);

        //for blinker
        blinker = (SwitchCompat) findViewById(R.id.blinker);
        blinker.setOnCheckedChangeListener(this);
        blinker.setChecked(false);
        final ScrollableNumberPicker blinkerNumberPicker = (ScrollableNumberPicker) findViewById(R.id.number_picker_blinker);

        //for timer
        timer = (SwitchCompat) findViewById(R.id.timer);
        timer.setOnCheckedChangeListener(this);
        timer.setChecked(false);

        //Color Picker
        colorPicker = (Button) findViewById(R.id.colorPicker);

        //Disco Light
        discoLight = (Button) findViewById(R.id.discoLight);

        sos = (Button) findViewById(R.id.sos);
        morseCode = (Button) findViewById(R.id.morseCode);
        DefaultColor = ContextCompat.getColor(MainActivity.this, R.color.white);

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

        //Turn On Torch Listener
        isTorchOn = false;
        isOnOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (checkVersion() && !hasPermission("android.permission.CAMERA")) {
                        permissionDialogBox();
                    } else if (blink) {
                        showToast("Blinker is ON! Switch Off Blinker to turn Flash ON!");
                    } else if (time) {
                        showToast("Timer is ON! Switch Off Timer to turn Flash ON!");
                    } else if (discoLightFlag) {
                        showToast("Disco is ON! Switch Off Disco Light to turn Flash ON!");
                    } else if (sosFlag) {
                        showToast("Sos is ON! Switch Off Sos to turn Flash ON!");
                    } else if (morseFlag) {
                        showToast("Morse code is ON! Switch Off morse to turn Flash ON!");
                    } else {
                        if(!soundFlag) {
                            if (mp.isPlaying()) {
                                mp.stop();
                                mp.release();
                                mp = MediaPlayer.create(MainActivity.this, R.raw.soho);
                            }
                            mp.start();
                        }
                        if (isTorchOn) {
                            mNotificationManager.cancel(notification_id);
                            turnOffFlashLight();
                            isOnOFF.setImageResource(R.drawable.off);
                        } else {
                            showNotification("turnOff");
                            turnOnFlashLight();
                            isOnOFF.setImageResource(R.drawable.on);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        //Listener for blinker
        blinkerNumberPicker.setListener(new ScrollableNumberPickerListener() {
            @Override
            public void onNumberPicked(int value) {
                blinkTimeValue = value;
            }
        });

        //Listener for Timer
        timerPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OpenTimePicker();

            }
        });

        //Listener for Disco
        discoLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (blink) {
                    showToast("Switch Off Blinker to turn Disco Light ON!");
                } else if (time) {
                    showToast("Switch Off Timer to turn Disco Light ON!");
                } else if (isTorchOn) {
                    showToast("Switch Off Flash to turn Disco Light ON!");
                } else if (sosFlag) {
                    showToast("Sos is ON! Switch Off Sos to turn Disco Light ON!");
                } else if (morseFlag) {
                    showToast("Morse code flash is ON! Switch Off morse to turn Disco Light ON!");
                }else {
                    if (discoLightFlag) {
                        discoLight.setBackground(getResources().getDrawable(R.drawable.disco_off));
                        discoLightFlag = !discoLightFlag;
                        showToast("Disco Light: OFF");
                        mNotificationManager.cancel(notification_id);
                    } else {
                        discoLight.setBackground(getResources().getDrawable(R.drawable.disco_on));
                        showNotification("discoison");
                        discoLightFlag = !discoLightFlag;
                        showToast("Disco Light: ON");
                    }
                    /*relativeLayout.setBackground(getResources().getDrawable(R.drawable.background));*/
                }
            }
        });

        //Listener for SOS
        sos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (blink) {
                    showToast("Switch Off Blinker to turn SOS ON!");
                } else if (time) {
                    showToast("Switch Off Timer to turn SOS ON!");
                } else if (isTorchOn) {
                    showToast("Switch Off Flash to turn SOS ON!");
                } else if (discoLightFlag) {
                    showToast("Disco light is ON! Switch Off Disco Light to turn SOS ON!");
                } else if (morseFlag) {
                    showToast("Morse code is ON! Switch Off morse to turn SOS ON!");
                } else if (sosFlag) {
                    sos.setBackground(getResources().getDrawable(R.drawable.sos_off));
                    sosFlag = !sosFlag;
                    showToast("SOS: OFF");
                } else {
                    sos.setBackground(getResources().getDrawable(R.drawable.sos_on));
                    sosFlag = !sosFlag;
                    showToast("SOS: ON");
                }

            }
        });

        //Listener for MorseCode
        morseCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (blink) {
                    showToast("Switch Off Blinker to turn Morse ON!");
                } else if (time) {
                    showToast("Switch Off Timer to turn Morse ON!");
                } else if (isTorchOn) {
                    showToast("Switch Off Flash to turn Morse ON!");
                } else if (sosFlag) {
                    showToast("Sos is ON! Switch Off Sos to turn Morse ON!");
                } else if (discoLightFlag) {
                    showToast("Disco light is ON! Switch Off Disco Light to turn Morse ON!");
                }else {
                    final Dialog dialog = new Dialog(MainActivity.this);
                    dialog.setContentView(R.layout.customdialog);
                    Window window = dialog.getWindow();
                    final TextView insertText = (TextView) dialog.findViewById(R.id.insertText);
                    final TextView insertText1 = (TextView) dialog.findViewById(R.id.insertText1);
                    Button transmit = (Button) dialog.findViewById(R.id.transmit);
                    Button morseButton = (Button) dialog.findViewById(R.id.submitButton);
                    transmit.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        insertText1.setText(MorseCode.alphaToMorse(insertText.getText().toString()));
                                                        morseCodeText = (MorseCode.alphaToMorse(insertText.getText().toString())).trim();
                                                    }
                                                }
                    );
                    morseButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!insertText1.getText().toString().trim().equals("")) {
                                morseCode.setBackground(getResources().getDrawable(R.drawable.morse_on));
                                morseFlag = !morseFlag;
                                showToast("Morse: ON");
                                dialog.dismiss();
                            }
                        }
                    });
                    if (morseFlag) {
                        morseCode.setBackground(getResources().getDrawable(R.drawable.morse_off));
                        morseFlag = !morseFlag;
                        morseCodeText = "";
                        i = 0;
                        showToast("Morse: OFF");
                    } else {
                        dialog.show();
                    }
                }
            }
        });

        //Listener for Colorpicker
        colorPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OpenColorPickerDialog(false);
            }
        });

        runnableForBlinker();
        runnableForTimerAndDiscolight();
        runnableForSos();
        runnableForMorse();

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null) {

            } else if (extras.getBoolean("turnOff")) {
                turnOffFlashLight();
            }
        }

        //Admob
        MobileAds.initialize(this, "ca-app-pub-7860341576927713~5587659182");
        mAdView = (AdView) findViewById(R.id.adViewAd);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("D8D6B049EDAB3CB2227DD36B3ED29F2D")
                .addTestDevice("25F149879ED72631F3CB460DEED0436A")
                .addTestDevice("B117F7C5611FB5503E6D2BD2CCA8C928")
                .addTestDevice("5FBF995F76CDF38832A294D8A2EE7DD0")
                .build();
        mAdView.loadAd(adRequest);

    }

    private void runnableForBlinker() {
        runnableCodeBlinker = new Runnable() {
            @Override
            public void run() {
                if (blink) {
                    turnOnFlashLight();
                    try {
                        Thread.sleep(blinkTimeValue);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    turnOffFlashLight();
                }
                mTimerHandler.postDelayed(runnableCodeBlinker, blinkTimeValue);
            }
        };
        mTimerHandler.post(runnableCodeBlinker);
    }

    int i = 0;

    private void runnableForSos() {
        final String[] sosCode = {".", ".", ".", "_", "_", "_", ".", ".", "."};

        runnableCodeSos = new Runnable() {
            @Override
            public void run() {
                if (sosFlag) {
                    if (i == sosCode.length) {
                        i = 0;
                    }
                    turnOnFlashLight();
                    try {
                        if (sosCode[i].equals("_")) {
                            Thread.sleep(550);
                            sosblinktime = 550;
                        } else {
                            Thread.sleep(100);
                            sosblinktime = 100;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    turnOffFlashLight();

                    i++;
                }
                timerHandlerForSos.postDelayed(runnableCodeSos, sosblinktime);
            }
        };
        timerHandlerForSos.post(runnableCodeSos);
    }

    private void runnableForMorse() {
        runnableCodeMorseCode = new Runnable() {
            @Override
            public void run() {
                char[] sosCode = morseCodeText.toCharArray();
                if (morseFlag) {
                    if (i == sosCode.length) {
                        i = 0;
                    }
                    turnOnFlashLight();
                    try {
                        if (sosCode[i] == '-') {
                            Thread.sleep(550);
                             morseblinktime = 550;
                        } else {
                            Thread.sleep(100);
                            morseblinktime = 100;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    turnOffFlashLight();

                    i++;
                }
                timerHandlerForMorseCode.postDelayed(runnableCodeMorseCode, morseblinktime);
            }
        };
        timerHandlerForMorseCode.post(runnableCodeMorseCode);
    }

    private void runnableForTimerAndDiscolight() {
        runnableCode1Timer = new Runnable() {
            @Override
            public void run() {
                if (discoLightFlag) {
                    turnOnFlashLight();
                    try {
                        Thread.sleep(discoLightValue);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    turnOffFlashLight();
                    discoLightValue = discoLightValue + 25;
                    if (discoLightValue == 250) {
                        discoLightValue = 25;
                    }
                }
                mTimerHandlerForDiscoLightBlinker.postDelayed(runnableCode1Timer, discoLightValue);
            }
        };

        mTimerHandlerForDiscoLightBlinker.post(runnableCode1Timer);

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

    private void turnOnFlashLight() {
        if (camera != null) {
            turnOnFlash();
        } else {
            turnOnLight();
        }
        isTorchOn = true;
    }

    private void turnOffFlashLight() {
        if (camera != null) {
            turnOffFlash();
        } else {
            turnOffLight();
        }
        isTorchOn = false;
    }

    @TargetApi(21)
    public void turnOnLight() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                objCameraManager.setTorchMode(mCameraId, true);
            } else {
                try {
                    builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                    session.setRepeatingRequest(builder.build(), null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @TargetApi(21)
    public void turnOffLight() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                objCameraManager.setTorchMode(mCameraId, false);
            } else {
                try {
                    builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                    session.setRepeatingRequest(builder.build(), null, null);
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
        isTorchOn = false;
    }

    //Timer
    private String hmsTimeFormatter(long milliSeconds) {

        String hms = String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(milliSeconds),
                TimeUnit.MILLISECONDS.toMinutes(milliSeconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliSeconds)),
                TimeUnit.MILLISECONDS.toSeconds(milliSeconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliSeconds)));
        return hms;
    }

    private void stopCountDownTimer() {
        timerPicker.setText("Set Time");
        time = false;
        countDownTimer.cancel();
    }

    private void startCountDownTimer() {
        countDownTimer = new CountDownTimer(timepickerDuration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (time) {
                    if (!isTorchOn) {
                        turnOnFlashLight();
                    }
                    timerPicker.setText(hmsTimeFormatter(millisUntilFinished));
                }
            }

            @Override
            public void onFinish() {
                turnOffFlashLight();
                SwitchCompat switchCompat = (SwitchCompat) findViewById(R.id.timer);
                switchCompat.setChecked(false);
                timerPicker.setText("Set Time");
                showToast("Timer : OFF");
                time = false;
            }
        }.start();
        countDownTimer.start();
    }

    private void OpenTimePicker() {
        /*DialogFragment newFragment = new PickerDialogFragment();
        newFragment.setStyle(2,1);
        newFragment.show(getFragmentManager(), "dialog");*/
        String format = String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(timepickerDuration),
                TimeUnit.MILLISECONDS.toMinutes(timepickerDuration) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timepickerDuration)), // The change is in this line
                TimeUnit.MILLISECONDS.toSeconds(timepickerDuration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timepickerDuration)));
        String formated[] = format.split(":");
        int hr = Integer.parseInt(formated[0]);
        int mn = Integer.parseInt(formated[1]);
        int sc = Integer.parseInt(formated[2]);
        Calendar now = Calendar.getInstance();
        MyTimePickerDialog mTimePicker = new MyTimePickerDialog(this, new MyTimePickerDialog.OnTimeSetListener() {

            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute, int seconds) {
                // TODO Auto-generated method stub
                timerPicker.setText(String.format("%02d", hourOfDay) +
                        ":" + String.format("%02d", minute) +
                        ":" + String.format("%02d", seconds));
                String a[];
                a = timerPicker.getText().toString().split(":");
                int hour = Integer.parseInt(a[0]);
                int min = Integer.parseInt(a[1]);
                int sec = Integer.parseInt(a[2]);

                timepickerDuration = (hour * 3600000) + (min * 60000) + (sec * 1000);
            }
        }, hr, mn, sc, true);

        mTimePicker.show();


    }

    private void OpenColorPickerDialog(boolean AlphaSupport) {

        AmbilWarnaDialog ambilWarnaDialog = new AmbilWarnaDialog(MainActivity.this, DefaultColor, AlphaSupport, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(AmbilWarnaDialog ambilWarnaDialog, int color) {
                DefaultColor = color;
                relativeLayout.setBackgroundColor(color);
            }

            @Override
            public void onCancel(AmbilWarnaDialog ambilWarnaDialog) {
                //Toast.makeText(MainActivity.this, "Color Picker Closed", Toast.LENGTH_SHORT).show();
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
                if (time) {
                    SwitchCompat switchCompat = (SwitchCompat) findViewById(R.id.timer);
                    switchCompat.setChecked(false);
                    blink = true;
                    time = false;
                    showToast("Blinker : ON");
                    showNotification("blinkerison");
                } else if (isTorchOn) {
                    showToast("Flash is On! Switch Off Flash to turn Blinker ON!");
                    blinker.setChecked(false);
                } else if (discoLightFlag) {
                    showToast("Disco is On! Switch Off Disco Light to turn Blinker ON!");
                    blinker.setChecked(false);
                } else if (sosFlag) {
                    showToast("SOS is On! Switch Off SOS to turn Blinker ON!");
                    blinker.setChecked(false);
                } else if (morseFlag) {
                    showToast("Morse is On! Switch Off Morse Light to turn Blinker ON!");
                    blinker.setChecked(false);
                } else {
                    SwitchCompat switchCompat = (SwitchCompat) findViewById(R.id.timer);
                    switchCompat.setChecked(false);
                    blink = true;
                    time = false;
                    showToast("Blinker : ON");
                    showNotification("blinkerison");
                }
            } else {
                blink = false;
                showToast("Blinker : OFF");
                mNotificationManager.cancel(notification_id);
            }
        }
        if (R.id.timer == buttonView.getId()) {
            if (isChecked) {
                if (timepickerDuration == 0) {
                    showToast("Set Time");
                    timer.setChecked(false);
                } else {
                    if (isTorchOn) {
                        showToast("Flash is On! Switch Off Flash to turn Timer ON!");
                        timer.setChecked(false);
                    } else if (discoLightFlag) {
                        showToast("Disco is On! Switch Off Disco Light to turn Timer ON!");
                        timer.setChecked(false);
                    } else if (sosFlag) {
                        showToast("SOS is On! Switch Off SOS to turn Timer ON!");
                        blinker.setChecked(false);
                    } else if (morseFlag) {
                        showToast("Morse is On! Switch Off Morse Light to turn Timer ON!");
                        blinker.setChecked(false);
                    } else {
                        time = true;
                        startCountDownTimer();
                        SwitchCompat switchCompat = (SwitchCompat) findViewById(R.id.blinker);
                        switchCompat.setChecked(false);
                        blink = false;
                        turnOnFlashLight();
                        showNotification("timerison");
                        showToast("Timer : ON");
                    }
                }
            } else {
                stopCountDownTimer();
                mNotificationManager.cancel(notification_id);
                turnOffFlashLight();
                showToast("Timer : OFF");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        soundFlag = settings.getBoolean("sound", false);
        if (soundFlag) {
            menu.getItem(0).setIcon(getResources().getDrawable(android.R.drawable.ic_lock_silent_mode));
        } else {
            menu.getItem(0).setIcon(getResources().getDrawable(android.R.drawable.ic_lock_silent_mode_off));
        }

        return true;
    }
/*
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        MenuItem settingsItem = menu.findItem(R.id.mute);
        // set your desired icon here based on a flag if you like
        settingsItem.setIcon(getResources().getDrawable(R.drawable.ic_launcher));

        return super.onPrepareOptionsMenu(menu);
    }
*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share:
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBodyText = "https://play.google.com/store/apps/details?id=androidflashlightapp.inducesmile.com.myapp1";
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBodyText);
                startActivity(Intent.createChooser(sharingIntent, "Shearing Option"));
                return true;
            case R.id.mute:
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("sound",!soundFlag);
                editor.commit();
                soundFlag = !soundFlag;
                if(soundFlag) {
                    item.setIcon(getResources().getDrawable(android.R.drawable.ic_lock_silent_mode));
                }else{
                    item.setIcon(getResources().getDrawable(android.R.drawable.ic_lock_silent_mode_off));
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showToast(String msg) {
        if (toast != null) {
            toast.cancel();
        }
        if(!soundFlag) {
            if (mp.isPlaying()) {
                mp.stop();
                mp.release();
                mp = MediaPlayer.create(MainActivity.this, R.raw.soho);
            }
            mp.start();
        }
        toast = Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 350);
        toast.show();
    }

    private void showNotification(final String from) {
        ServiceConnection mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                                           IBinder binder) {
                ((MyService.KillBinder) binder).service.startService(new Intent(
                        MainActivity.this, MyService.class));
                int int_condition = 0;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    notification.setSmallIcon(R.drawable.notification_icon);
                } else {
                    notification.setSmallIcon(R.drawable.ic_launcher);
                }
                notification.setTicker("Flash is ON!");
                notification.setWhen(System.currentTimeMillis());
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                if (from == "turnOff") {
                    notification.setContentTitle("Flash is ON!");
                    notification.setContentText("Tap to turn it OFF!");
                    intent.putExtra("turnOff", true);
                    PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    notification.setContentIntent(pendingIntent);
                    notification.setOngoing(true);
                } else if (from == "blinkerison") {
                    notification.setContentTitle("Blinker");
                    notification.setContentText("Blinker is ON!");
                    notification.setOngoing(true);
                    intent = null;
                    notification.setContentIntent(null);
                } else if (from == "timerison") {
                    notification.setContentTitle("Timer");
                    notification.setContentText("Timer is ON!");
                    notification.setOngoing(true);
                    intent = null;
                    notification.setContentIntent(null);
                } else if (from == "discoison") {
                    notification.setContentTitle("Disco Light");
                    notification.setContentText("Disco Light is ON!");
                    notification.setOngoing(true);
                    intent = null;
                    notification.setContentIntent(null);
                }
                mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                mNotificationManager.notify(MyService.NOTIFICATION_ID, notification.build());

            }

            public void onServiceDisconnected(ComponentName className) {
            }

        };
        bindService(new Intent(MainActivity.this,
                        MyService.class), mConnection,
                Context.BIND_AUTO_CREATE);
    }

    public void setRelativeBackground(int i) {
        final int color = colorArray[i];
        relativeLayout.setBackgroundColor(color);
    }

    @TargetApi(23)
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

    @TargetApi(23)
    private boolean hasPermission(String permission) {
        if (checkVersion()) {
            return (checkSelfPermission(permission)) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean checkVersion() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
    }

    @TargetApi(21)
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

    @TargetApi(21)
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

    @TargetApi(21)
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

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        int color = Color.WHITE;
        Drawable background = relativeLayout.getBackground();
        if (background instanceof ColorDrawable)
            color = ((ColorDrawable) background).getColor();

        editor.putInt("bg_Color", color);
        // Commit the edits!
        editor.commit();
        if (camera != null) {
            camera.release();
            camera = null;
        }
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

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        discoLightFlag = false;
                        blink = false;
                        time = false;
                        turnOffFlashLight();
                        if (camera != null) {
                            camera.release();
                            camera = null;
                        }
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(1);
                    }
                }).setNegativeButton("No", null).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


}

