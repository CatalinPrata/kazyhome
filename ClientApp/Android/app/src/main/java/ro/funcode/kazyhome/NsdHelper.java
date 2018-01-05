 /*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ro.funcode.kazyhome;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class NsdHelper {

    private static final String SERVICE_TYPE = "_arduino._tcp.";
    private static final String TAG = "NsdHelper";
    private String serviceName = "kazy";
    private NsdManager nsdManager;
    private NsdManager.ResolveListener resolveListener;
    private NsdManager.DiscoveryListener discoveryListener;
    private MDNSListener listener;

    public NsdHelper(Context context, MDNSListener listener) {
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        this.listener = listener;
    }

    private void initializeNsd() {
        initializeResolveListener();
        initializeDiscoveryListener();
    }

    private void initializeDiscoveryListener() {
        discoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(final NsdServiceInfo service) {
                Log.d(TAG, "Service discovery success " + service);
                try {
                    nsdManager.resolveService(service, resolveListener);
                } catch (Exception e) {
                    Log.d(TAG, "resolve listener already in use...");
                    postErrorMessage("Could not find device, resolve listener already in use...");
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.d(TAG, "service lost" + service);
                postErrorMessage("Could not find device, service lost.");
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.d(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.d(TAG, "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
                postErrorMessage("Could not start discovery.");
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.d(TAG, "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
            }
        };
    }

    private void initializeResolveListener() {
        resolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "Resolve failed" + errorCode);
                postErrorMessage("Could not find device, error code: " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Resolve Succeeded. " + serviceInfo);

                if (serviceInfo.getServiceName().equals(serviceName) && listener != null) {
                    Log.d(TAG, "Same IP.");
                    new Handler(Looper.getMainLooper()).post(() -> listener.ipFound(serviceInfo.getHost().getHostAddress()));
                    // stop discovery after the first result
                    stopDiscovery();
                } else if (!serviceInfo.getServiceName().equals(serviceName) && listener != null) {
                    new Handler(Looper.getMainLooper()).post(() -> listener.error("Could not find device"));
                }
            }
        };
    }

    public void discoverServices() {

        if (discoveryListener != null) {
            stopDiscovery();
        }

        initializeNsd();

        try {
            nsdManager.discoverServices(
                    SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        } catch (Exception exception) {
            Log.d(TAG, exception.getLocalizedMessage());
            postErrorMessage("Could not find device");
        }
    }

    private void postErrorMessage(String error) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (listener != null) {
                listener.error(error);
            }
        });
    }

    public void stopDiscovery() {
        try {
            nsdManager.stopServiceDiscovery(discoveryListener);
        } catch (Exception exception){
            Log.d(TAG, exception.getLocalizedMessage());
        }
    }

    public interface MDNSListener {
        void ipFound(String ip);

        void error(String error);
    }
}
