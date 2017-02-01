package uk.co.telegraph.voicecapture;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import uk.co.telegraph.voicecapture.Utils.PermissionUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String TEST_TRUMP_TEXT = "When Mexico sends its people, they're not sending their best. They're not sending you. They're not sending you. They're sending people that have lots of problems, and they're bringing those problems with us. They're bringing drugs. They're bringing crime. They're rapists. And some, I assume, are good people.";

    private RemoteDatabase remoteDb;
    private VoiceApi       voiceApi;


    private FloatingActionButton fab;
    private MediaPlayer mp = null;
    private MediaRecorder mediaRecorder = null;

    private static String mFileName = null;

    private Button playButton;
    private Button stopButton;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        remoteDb = new RemoteDatabase();
        voiceApi = new VoiceApi();

        // Record to the external cache directory for visibility
        mFileName = getExternalCacheDir().getAbsolutePath();
        mFileName += "/audiorecord.amr";
        remoteDb = new RemoteDatabase();

        setUpViews();
    }

    @Override
    public void onStart() {
        super.onStart();

        remoteDb.onStart(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // PS: I'll put my test stubs here, K?
//            remoteDb.uploadText(System.currentTimeMillis(), TEST_TRUMP_TEXT);

            makeAndTestFileUpload();


            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        remoteDb.onStop();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }

        if (mp != null) {
            mp.release();
            mp = null;
        }
    }

    private void setUpViews(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        playButton = (Button) findViewById(R.id.button_play);
        stopButton = (Button) findViewById(R.id.button_stop);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        mTextView = (TextView) findViewById(R.id.recording_change);

        fab.setOnTouchListener(record);

        playButton.setOnClickListener(this::playRecording);
        stopButton.setOnClickListener(this::stopPlayback);

        requestFabButton();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (PermissionUtils.isAudioRecordingAllowed(this))
            fab.setVisibility(View.VISIBLE);
    }

    private void requestFabButton(){
        if (PermissionUtils.isAudioRecordingAllowed(this)) {
            Toast.makeText(this, "we can record", Toast.LENGTH_LONG).show();
        } else {
            PermissionUtils.requestRecordAudio(this);
            fab.setVisibility(View.INVISIBLE);
        }
    }


    View.OnTouchListener record = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:

                    startRecording(v);
                    mTextView.setText("Recording");
                    mTextView.setTextColor(Color.RED);
                    fab.setPressed(true);
                    return true;
                case MotionEvent.ACTION_UP:
                    stopRecording();
                    fab.setPressed(false);
                    mTextView.setTextColor(Color.BLACK);
                    mTextView.setText("You can record");
                    return true;
            }
            return false;
        }
    };



    private void startRecording(View view) {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
        mediaRecorder.setOutputFile(mFileName);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        mediaRecorder.start();
    }

    private void stopRecording() {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
    }

    private void playRecording(View view) {
        mp = new MediaPlayer();
        try {
            mp.setDataSource(mFileName);
            mp.prepare();
            mp.start();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
    }

    private void stopPlayback(View view){
        if (mp == null) {
            return;
        } else {
            mp.release();
            mp = null;
        }
    }

    private void makeAndTestFileUpload() {

        final String testFilename = getExternalCacheDir().getAbsolutePath() + "/test_up.amr";

//        File f = new File(testFilename);
        File f = new File(mFileName);

        try {
            FileWriter writer = new FileWriter(f);
            writer.append(TEST_TRUMP_TEXT);
            writer.flush();
            writer.close();

            voiceApi.processSpeech(f);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
