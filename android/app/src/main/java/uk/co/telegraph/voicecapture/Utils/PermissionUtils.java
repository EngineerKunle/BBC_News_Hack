package uk.co.telegraph.voicecapture.Utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Created by ogunjimik on 31/01/2017.
 */

public class PermissionUtils {

    static int requestCode = 100;
    public static boolean isAudioRecordingAllowed(Context context){
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestRecordAudio(Activity context){
        ActivityCompat.requestPermissions(context,
                new String[]{Manifest.permission.RECORD_AUDIO},
                requestCode);
    }
}
