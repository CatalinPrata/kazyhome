package ro.funcode.kazyhome;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements NsdHelper.MDNSListener {

    /**
     * Authorization token to be able to execute the API calls
     */
    public static final String BASIC_AUTH = "Basic c21hcnRfMTp5b3VfZ290X01FXzY5";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    @BindView(R.id.ipTxt)
    EditText ipEditText;
    @BindView(R.id.statusTxt)
    TextView statusTxt;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    NsdHelper nsdHelper;
    private PersistenceManager persistenceManager;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        persistenceManager = new PersistenceManager(getApplicationContext());

        client = new OkHttpClient();
        // set a bigger timeout because of the delay of the esp8266
        client.setConnectTimeout(8, TimeUnit.SECONDS);
    }

    @Override
    protected void onResume() {
        super.onResume();

        nsdHelper = new NsdHelper(this, this);
        ipEditText.setText(persistenceManager.getIP("Press find server!"));
    }

    @Override
    protected void onPause() {
        nsdHelper.stopDiscovery();

        super.onPause();
    }

    @OnClick(R.id.led_on)
    void onLedOnClick() {
        switchLed(true);
    }

    @OnClick(R.id.led_off)
    void onLedOffClick() {
        switchLed(false);
    }

    /**
     * As the method name suggests, it switches the led on or off by calling the appropriate API
     */
    private void switchLed(boolean state) {
        new Thread(() -> {
            persistenceManager.saveIP(ipEditText.getText().toString());

            String action = state ? "on" : "off";

            Request request = new Request.Builder()
                    .header(HEADER_AUTHORIZATION, BASIC_AUTH)
                    .url("http://" + ipEditText.getText() + "/?action=" + action)
                    .build();
            try {
                client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }

            runOnUiThread(this::onGetStatusClick);
        }).start();
    }

    @OnClick(R.id.statusBtn)
    void onGetStatusClick() {

        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            persistenceManager.saveIP(ipEditText.getText().toString());
            String message;
            Request request = new Request.Builder()
                    .header(HEADER_AUTHORIZATION, BASIC_AUTH)
                    .url("http://" + ipEditText.getText() + "/status")
                    .build();
            try {
                Response response = client.newCall(request).execute();
                if (response.code() == 200) {
                    message = "Status: " + response.body().string();
                } else {
                    message = "Response code: " + response.code();
                }
            } catch (IOException e) {
                e.printStackTrace();
                message = "Response code: " + e.getLocalizedMessage();
            }

            final String finalMessage = message;
            new Handler(Looper.getMainLooper()).post(() -> {
                statusTxt.setText(finalMessage);
                progressBar.setVisibility(View.INVISIBLE);
            });
        }).start();
    }

    @OnClick(R.id.checkNetworkBtn)
    void onCheckNetworkClick() {
        progressBar.setVisibility(View.VISIBLE);
        nsdHelper.discoverServices();
    }

    @Override
    public void ipFound(final String ip) {
        updateIp(ip);
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void error(String error) {
        statusTxt.setText(error);
    }

    private void updateIp(String ip) {
        persistenceManager.saveIP(ip);
        ipEditText.post(() -> ipEditText.setText(ip));
    }
}
