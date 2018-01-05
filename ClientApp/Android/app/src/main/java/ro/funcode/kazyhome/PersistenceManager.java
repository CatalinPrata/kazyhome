package ro.funcode.kazyhome;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by catalinprata on 18/11/2017.
 */
public class PersistenceManager {

    private static final String ACTIVE_IP = "active_ip";
    private SharedPreferences appStateSharedPreferences;

    public PersistenceManager(Context context) {
        appStateSharedPreferences = context.getSharedPreferences("app_state", Context.MODE_PRIVATE);
    }

    public void saveIP(String ip) {
        appStateSharedPreferences.edit().putString(ACTIVE_IP, ip).apply();
    }

    public String getIP(String defaultIp) {
        return appStateSharedPreferences.getString(ACTIVE_IP, defaultIp);
    }
}
