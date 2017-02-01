package uk.co.telegraph.voicecapture;

import android.app.Activity;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

class RemoteDatabase {

    private static final String LOG_TAG = "VC_DB_UPLOADER";
    private final FirebaseAuth firebaseAuth;

    RemoteDatabase() {
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    void onStart(Activity activity) {
        firebaseAuth.addAuthStateListener(authListener);
        signIn(activity);
    }

    // Call me from the activities onDestroy()
    void onStop() {
        firebaseAuth.removeAuthStateListener(authListener);
    }

    private void signIn(final Activity activity) {
        firebaseAuth.signInAnonymously()
                .addOnCompleteListener(activity, task -> {
                    Log.d(LOG_TAG, "signInAnonymously:onComplete:" + task.isSuccessful());

                    // If sign in fails, display a message to the user. If sign in succeeds
                    // the auth state listener will be notified and logic to handle the
                    // signed in user can be handled in the listener.
                    if (!task.isSuccessful()) {
                        Log.w(LOG_TAG, "signInAnonymously", task.getException());
                    }
                });
    }

    void uploadText(long timestamp, String text) {
        Observable<TextBlock> o = doTextUpload(new TextBlock(timestamp, text));
        o.subscribe();
    }

    private final static String TEXT_DATABASE_NAME = "VoiceCaptures";

    private static class TextBlock {
        final long timestamp;
        final String text;

        TextBlock(long timestamp, String text) {
            this.timestamp = timestamp;
            this.text = text;
        }
    }

    private static Observable<TextBlock> doTextUpload(final TextBlock text) {
        Observable<TextBlock> o = Observable.create(new Observable.OnSubscribe<TextBlock>() {
            @Override
            public void call(final Subscriber<? super TextBlock> subscriber) {
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference theDb = database.getReference(TEXT_DATABASE_NAME);
                DatabaseReference entryRef = theDb.push();

                entryRef.setValue(text, (e, ref) -> {
                    if (null == e) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(text);
                            subscriber.onCompleted();
                        }
                    } else {
                        subscriber.onError(e.toException());
                    }
                });
            }
        });
        o.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        return o;
    }


    private final FirebaseAuth.AuthStateListener authListener = firebaseAuth1 -> {
        FirebaseUser user = firebaseAuth1.getCurrentUser();
        if (user != null) {
            // User is signed in
            Log.d(LOG_TAG, "onAuthStateChanged:signed_in:" + user.getUid());
        } else {
            // User is signed out
            Log.d(LOG_TAG, "onAuthStateChanged:signed_out");
        }
        // ...
    };
}
