package uk.co.telegraph.voicecapture;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
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

import rx.Observable;
import rx.Subscription;
import uk.co.telegraph.voicecapture.Utils.PermissionUtils;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    private RemoteDatabase remoteDb;
    private VoiceApi       voiceApi;

    private FloatingActionButton fab;
    private MediaPlayer mp = null;
    private MediaRecorder mediaRecorder = null;

    private static String mFileName = null;

    private Toolbar  toolbar;
    private TextView textView;

    private Subscription subscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        remoteDb = new RemoteDatabase();
        voiceApi = new VoiceApi();

        // Record to the external cache directory for visibility
        mFileName = getExternalCacheDir().getAbsolutePath() + "/audiorecord.amr";
        remoteDb = new RemoteDatabase();

        setUpViews();
    }

    @Override
    public void onStart() {
        super.onStart();

        subscription = voiceApi.subscribe(this::onTextReceived, this::onError);
        remoteDb.onStart(this);
    }

    private void onTextReceived(String txt) {
        remoteDb.uploadText(System.currentTimeMillis(), txt);
        textView.setText(textView.getText() + "\n" + txt);
    }

    private void onError(Throwable t) {
        AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);
        dlgBuilder.setTitle("Oooooops")
                .setMessage("Well, that's not ideal\n" + t.getMessage())
//                .setPositiveButton("Hey-ho", v -> ())
                .create();
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
        switch (id) {
            case R.id.action_start_playback:    playRecording(); return true;
            case R.id.action_stop_playback:     stopPlayback();  return false;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(subscription != null) {
            subscription.unsubscribe();
        }

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
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        textView = (TextView) findViewById(R.id.recording_change);

        fab.setOnTouchListener(record);

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

                    startRecording();
                    toolbar.setBackgroundColor(getResources().getColor(R.color.red));
//                    mTextView.setText("Recording");
//                    mTextView.setTextColor(Color.RED);
                    fab.setPressed(true);
                    return true;
                case MotionEvent.ACTION_UP:
                    stopRecording();
                    toolbar.setBackgroundColor(getResources().getColor(R.color.blue));
                    fab.setPressed(false);
//                    mTextView.setTextColor(Color.BLACK);
//                    mTextView.setText("You can record");
                    return true;
            }
            return false;
        }
    };

    private void startRecording() {
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

        processAudio();
    }

    private void playRecording() {
        mp = new MediaPlayer();
        try {
            mp.setDataSource(mFileName);
            mp.prepare();
            mp.start();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
    }

    private void stopPlayback(){
        if (mp == null) {
            return;
        } else {
            mp.release();
            mp = null;
        }
    }

    private void processAudio() {
        File f = new File(mFileName);
        try {
            voiceApi.processSpeech(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
