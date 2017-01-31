package uk.co.telegraph.voicecapture;

import com.google.gson.GsonBuilder;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import rx.Observable;
import rx.subjects.PublishSubject;

public class VoiceApi {

    private final static String baseUrl = "https://www.googleapis.com/";

    private final static String apiKey = "AIzaSyBSl2CYu6EE9d6X7XY93QcK1juuKqz_9h0";
    private final static String cloudBucket = "newshackday-eb793.appspot.com";
    
    interface SpeechApi {

    }

    interface CloudApi {
        @POST("/upload/{path}")
        Observable<String> uploadData(@Path("path") String path, @Part("uploadType") String uploadType, @Part("name") String name);

        @DELETE("/storage/{path}")
        Observable<Void> deleteData(@Path("path") String path, @Part("name") String name);
    }

    private SpeechApi speechApi = null;
    private CloudApi  cloudApi = null;

    private PublishSubject<String> subject = PublishSubject.create();

    public VoiceApi() {
        authenticate();
        createCloudApi();
    }

    private void authenticate() {

    }

    public Observable<String> getObservable() {
        return subject;
    }

    public void processSpeech(File file) {

    }

    private void createCloudApi(String baseUrl, String tokenType, String authToken) {

        final OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    final Request request = chain.request().newBuilder()
                            .addHeader("Accept", "application/json")
                            .addHeader("Authorization", tokenType + " " + authToken)
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
        cloudApi = buildCloudApi((httpClient, baseUrl, converterFactory);
    }


    private SpeechApi buildSpeechApi(OkHttpClient httpClient, String baseUrl, GsonConverterFactory conv) {
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(conv)
                .build();

        return retrofit.create(SpeechApi.class);
    }

    private CloudApi buildCloudApi(OkHttpClient httpClient, String baseUrl, GsonConverterFactory conv) {
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(conv)
                .build();

        return retrofit.create(CloudApi.class);
    }
}