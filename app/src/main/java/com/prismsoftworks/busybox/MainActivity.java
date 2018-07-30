package com.prismsoftworks.busybox;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorListener {
    /**
     * Obligatory TAG string
     */
    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * SharedPreferences keys and filename. Explanation for data:
     * We are using a long string CSV-style and to store values with a pipe "|" seperating members.
     * This is being parsed when the ListView is shown
     */
    private static final String PREFS_NAME = "cntrPrefs";
    private static final String COUNTER_KEY = "cntrVal";
    private static final String PRESS_VALUE_KEY = "cntrValLabel";
    private static final String SHAKE_DATE_KEY = "cntrShake";

    /**
     * width constant. The number 70 seems to be the magic number for my test device (Nexus 5X)
     * */
    private static final int LABEL_WIDTH = 70;


    /**
     * Necessary UI and counter member variables
     */
    private int mCounterValue = 0;
    private TextView mLabel;
    private TextView mShakeLabel;
    private Button mIncrementButton;
    private Button mHistoryButton;
    private ListView mListView;
    private SharedPreferences prefs;

    /**
     * Sensor variables that need a wider scope
     */
    private SensorManager sensor;
    private long lastUpdate = -1;
    private float last_x = -1;
    private float last_y = -1;
    private float last_z = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
    }

    /**
     * Basic initialization. Calling on resume to "refresh" the app.
     */
    protected void init() {
        sensor = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor.registerListener(this, SensorManager.SENSOR_ACCELEROMETER, SensorManager.SENSOR_DELAY_GAME);
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mCounterValue = prefs.getInt(COUNTER_KEY, 0);
        mLabel = findViewById(R.id.counter_label);
        mIncrementButton = findViewById(R.id.btn_increment);
        mHistoryButton = findViewById(R.id.btn_history);
        mListView = findViewById(R.id.history_list);
        mShakeLabel = findViewById(R.id.lbl_tooltip);
        correctWidth();
        mLabel.setText("" + mCounterValue);

        mLabel.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mCounterValue = 0;
                prefs.edit().putInt(COUNTER_KEY, 0).commit();
                correctWidth();
                mLabel.setText("" + mCounterValue);
                return true;
            }
        });

        mIncrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCounterValue++;
                String pressedSuper = "" + prefs.getString(PRESS_VALUE_KEY, "");
                pressedSuper += System.currentTimeMillis() + "|" + mCounterValue + ",";
                prefs.edit().putInt(COUNTER_KEY, mCounterValue).commit();
                prefs.edit().putString(PRESS_VALUE_KEY, pressedSuper).commit();
                correctWidth();
                mLabel.setText("" + mCounterValue);
            }
        });

        mHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListView.getVisibility() == View.VISIBLE) {
                    mListView.setVisibility(View.INVISIBLE);
                    mShakeLabel.setVisibility(View.GONE);
                } else {
                    populateHistory();
                    mListView.setVisibility(View.VISIBLE);
                    mShakeLabel.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    /**
     * This populates the ListView when the history button is pressed when the listview is invisible
     */
    protected void populateHistory() {
        int pressLabel = R.string.press_label, labelValue = R.string.press_value_label,
                shakeLabel = R.string.shake_label;
        List<String> pressItems = new ArrayList<>();

        //set up the strings
        String[] pressVals = prefs.getString(PRESS_VALUE_KEY, "").split(",");
        String[] shakeVals = prefs.getString(SHAKE_DATE_KEY, "").split(",");
        for (String val : pressVals) {
            if(!val.equals("")) {
                String[] process = val.split("\\|");
                String dateStr = new SimpleDateFormat("MM/dd/yyyy 'at' HH:mm:ss z").format(new Date(Long.parseLong(process[0])));
                String pressStr = getResources().getString(pressLabel) + ": " + dateStr + " "
                        + getResources().getString(labelValue) + ": " + process[1];

                pressItems.add(pressStr);
            }
        }

        for(String val : shakeVals){
            if(!val.equals("")) {
                String dateStr = new SimpleDateFormat("MM/dd/yyyy 'at' HH:mm:ss z").format(new Date(Long.parseLong(val)));
                pressItems.add(getResources().getString(shakeLabel) + ": " + dateStr);
            }
        }

        ArrayAdapter<String> adapt = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                pressItems);

        mListView.setAdapter(adapt);
    }

    /**
     * Dynamically shrink the counter label based on length of characters. There is probably a much
     * easier way to achieve this.
     */
    protected void correctWidth() {
        String[] raw = (mCounterValue + "").split("");
        Log.i(TAG, "text size: " + mLabel.getTextSize());
        for (int i = 1; i < raw.length; i++) {
            //skipping empty string and first digit (element 0 )
            mLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, LABEL_WIDTH / (i + (i - 1)));
//            Log.i(TAG, "new text size: " + mLabel.getTextSize());

        }

        //this got too complicated
//        int strWidth;
//        do {
//            Rect bounds = new Rect();
//            Paint textPaint = mLabel.getPaint();
//            textPaint.getTextBounds(raw, 0, raw.length(), bounds);
//            strWidth = bounds.width();
//            Log.i(TAG, "str width: " + strWidth + " | " + mLabel.getTextSize());
//            if(strWidth > LABEL_WIDTH){
//                mLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, strWidth-1);
//            }
//
//            if(strWidth > 1000){
//                break;
//            }
//        } while(strWidth > LABEL_WIDTH && false);

    }

    /**
     * sensor detection for shake. Code imported from StackOverflow
     */
    @Override
    public void onSensorChanged(int sensor, float[] values) {
        float x, y, z;
        if (sensor == SensorManager.SENSOR_ACCELEROMETER) {
            long curTime = System.currentTimeMillis();

            // only allow one update every 100ms.
            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                x = values[SensorManager.DATA_X];
                y = values[SensorManager.DATA_Y];
                z = values[SensorManager.DATA_Z];

                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                if (speed > 800) {
                    Log.d("sensor", "shake detected w/ speed: " + speed);
                    String shakeSuper = prefs.getString(SHAKE_DATE_KEY, "");
                    shakeSuper += curTime + ",";
                    prefs.edit().putString(SHAKE_DATE_KEY, shakeSuper).commit();
                }

                last_x = x;
                last_y = y;
                last_z = z;
            }
        }
    }

    @Override
    public void onAccuracyChanged(int sensor, int accuracy) { }
}
