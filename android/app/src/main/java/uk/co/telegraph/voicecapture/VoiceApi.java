package uk.co.telegraph.voicecapture;

import android.util.Base64;
import android.util.Log;

import com.google.gson.GsonBuilder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

import retrofit2.http.Query;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

class Config {
    String encoding;    // enum(AudioEncoding),
    long sampleRate;
    String languageCode;

    Config() {
        this.encoding = "AMR_WB";
        this.sampleRate = 16000;
        this.languageCode = "EN";
    }
}

class AudioData {
    final Config config;
    final String audio;

    AudioData(File data) throws IOException {
        config = new Config();
        audio = Base64.encodeToString(readFile(data), Base64.DEFAULT);
    }

    private byte[] readFile(File file) throws IOException {
        int size = (int) file.length();
        byte[] bytes = new byte[size];

        BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
        buf.read(bytes, 0, bytes.length);
        buf.close();
        return bytes;
    }
}

class VoiceApi {

    private final static String apiKey = "AIzaSyAyQvf3giU4IT9LNZTzKaogZJ8A-4ClhHI";
    private final static String baseUrl = "https://speech.googleapis.com/";

    interface SpeechApi {
        @POST("v1beta1/speech:syncrecognize")
        Observable<String> processAudio(@Query("key") String key, @Body AudioData payload);
    }

    private SpeechApi speechApi = null;

    private PublishSubject<String> subject = PublishSubject.create();

    VoiceApi() {
        createCloudApi();
    }

    Observable<String> processSpeech(File file) throws IOException {
        speechApi.processAudio(apiKey, new AudioData(file))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(subject::onNext, this::onError);

        return subject;
    }

    private void onError(Throwable e) {
        Log.e("Wibble", e.getMessage());
    }

    private void createCloudApi() {

        final OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    final Request request = chain.request().newBuilder()
                            .addHeader("content-type", "application/json")
                            .build();

                    return chain.proceed(request);
                });

        if (BuildConfig.DEBUG) {
            final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            httpClientBuilder.addInterceptor(loggingInterceptor);
        }

        final GsonConverterFactory converterFactory = GsonConverterFactory.create(new GsonBuilder().create());

        OkHttpClient httpClient = httpClientBuilder.build();

        speechApi = buildSpeechApi(httpClient, baseUrl, converterFactory);
    }


    private SpeechApi buildSpeechApi(OkHttpClient httpClient, String baseUrl, Converter.Factory conv) {
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(conv)
                .build();

        return retrofit.create(SpeechApi.class);
    }
}