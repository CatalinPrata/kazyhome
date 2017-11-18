package ro.funcode.kazyhome;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by catalinprata on 18/11/2017.
 */
public class PersistenceManager {

    private SharedPreferences appStateSharedPreferences;

    public PersistenceManager(Context context) {
        appStateSharedPreferences = context.getSharedPreferences("app_state", Context.MODE_PRIVATE);
    }

    public void saveIP(String ip) {
        appStateSharedPreferences.edit().putString("active_ip", ip).apply();
    }

    public String getIP(String defaultIp) {
        return appStateSharedPreferences.getString("active_ip", defaultIp);
    }
}
