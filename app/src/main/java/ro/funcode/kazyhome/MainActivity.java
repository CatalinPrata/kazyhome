package ro.funcode.kazyhome;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

public class MainActivity extends AppCompatActivity {

    public static final String BASIC_AUTH = "Basic c21hcnRfMTp5b3VfZ290X01FXzY5";
    @BindView(R.id.ipTxt)
    EditText ipEditText;
    @BindView(R.id.statusTxt)
    TextView statusTxt;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    PersistenceManager persistenceManager;
    OkHttpClient client;
    private boolean stopSearching = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        persistenceManager = new PersistenceManager(getApplicationContext());

        client = new OkHttpClient();
        client.setConnectTimeout(1400, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onResume() {
        super.onResume();

        ipEditText.setText(persistenceManager.getIP("192.168.0.23"));
    }

    @OnClick(R.id.led_on)
    void onLedOnClick() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                persistenceManager.saveIP(ipEditText.getText().toString());
                Request request = new Request.Builder()
                        .header("Authorization", BASIC_AUTH)
                        .url("http://" + ipEditText.getText() + "/?action=on")
                        .build();
                try {
                    client.newCall(request).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onGetStatusClick();
                    }
                });
            }
        }).start();

    }

    @OnClick(R.id.led_off)
    void onLedOffClick() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                persistenceManager.saveIP(ipEditText.getText().toString());
                Request request = new Request.Builder()
                        .header("Authorization", BASIC_AUTH)
                        .url("http://" + ipEditText.getText() + "/?action=off")
                        .build();
                try {
                    client.newCall(request).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onGetStatusClick();
                    }
                });
            }
        }).start();

    }

    @OnClick(R.id.statusBtn)
    void onGetStatusClick() {

        stopSearching = true;
        progressBar.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                persistenceManager.saveIP(ipEditText.getText().toString());
                String message = "";
                Request request = new Request.Builder()
                        .header("Authorization", BASIC_AUTH)
                        .url("http://" + ipEditText.getText() + "/kitchen_status")
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
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        statusTxt.setText(finalMessage);
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }).start();
    }

    @OnClick(R.id.checkNetworkBtn)
    void onCheckNetworkClick() {
        stopSearching = false;
        int previousIndex = 0;
        for (int index = 10; index < 256; index += 10) {
            checkAddress("192.168.0." + previousIndex, previousIndex, index);
            previousIndex = index;
        }
    }

    public void checkAddress(final String address, final int chunk, final int max) {
        if (stopSearching){
            return;
        }
        if (chunk >= max || chunk >= 254) {
            statusTxt.setText("Server not found!");
            progressBar.setVisibility(View.INVISIBLE);
            return;
        } else {
            statusTxt.setText("Finding kitchen led...");
            progressBar.setVisibility(View.VISIBLE);
        }

        Log.d("IP search", "searching IP: " + address + " chunk:" + chunk + " max:" + max);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Request request = new Request.Builder()
                        .header("Authorization", BASIC_AUTH)
                        .url("http://" + address + "/kitchen_status")
                        .build();
                try {
                    OkHttpClient okHttpClient = new OkHttpClient();
                    okHttpClient.setConnectTimeout(1500, TimeUnit.MILLISECONDS);
                    Response response = okHttpClient.newCall(request).execute();
                    if (response.code() == 200 && response.body().string().contains("pin_status")) {
                        stopSearching = true;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                persistenceManager.saveIP(ipEditText.getText().toString());
                                ipEditText.setText(address);
                                statusTxt.setText("Found the led!");
                                progressBar.setVisibility(View.INVISIBLE);
                            }
                        });

                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                checkAddress("192.168.0." + (chunk + 1), chunk + 1, max);
                            }
                        });
                    }
                } catch (IOException e) {
                    Log.d("KazyHome", "Could not find server at: " + address);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            checkAddress("192.168.0." + (chunk + 1), chunk + 1, max);
                        }
                    });
                }
            }
        }).start();
    }
}
