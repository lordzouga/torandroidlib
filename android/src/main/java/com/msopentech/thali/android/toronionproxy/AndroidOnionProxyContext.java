/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED,
INCLUDING WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
MERCHANTABLITY OR NON-INFRINGEMENT.

See the Apache 2 License for the specific language governing permissions and limitations under the License.
*/

package com.msopentech.thali.android.toronionproxy;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.msopentech.thali.toronionproxy.OnionProxyContext;
import com.msopentech.thali.toronionproxy.WriteObserver;

import org.torproject.android.binary.TorResourceInstaller;
import org.torproject.android.binary.TorServiceConstants;
import org.torproject.android.binary.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static android.content.Context.MODE_PRIVATE;

public class AndroidOnionProxyContext extends OnionProxyContext implements TorServiceConstants {
    private final Context context;
    private TorResourceInstaller torResourceInstaller;
    private String TAG = "AndroidOnionProxyContext";

    public AndroidOnionProxyContext(Context context, String workingSubDirectoryName) {
        super(context.getDir(workingSubDirectoryName, MODE_PRIVATE));
        File subDirectory = context.getDir(workingSubDirectoryName, MODE_PRIVATE);

        torResourceInstaller = new TorResourceInstaller(context, subDirectory);
        this.context = context;
    }

    @Override
    public WriteObserver generateWriteObserver(File file) {
        return new AndroidWriteObserver(file);
    }

    @Override
    protected InputStream getAssetOrResourceByName(String fileName) throws IOException {
        return context.getResources().getAssets().open(fileName);
    }

    @Override
    public String getProcessId() {
        return String.valueOf(android.os.Process.myPid());
    }

    @Override
    public void installFiles() throws IOException, InterruptedException {
        Log.i(TAG, "Installing tor-android-binary");

        Thread.sleep(1000,0);

        try {
            torResourceInstaller.installResources();
        } catch (TimeoutException e) {
            Log.i(TAG, "Installing Tor Resources failed");
            return;
        }

        setSocksPortInConfig();
    }


    /* This function replaces the value of "SOCKSPort" in the torrc file to 9050 port.
    *
    * This was added just so that TorOnionProxySmokeTest can pass successfully*/
    public void setSocksPortInConfig() throws IOException {
        File torConfig = getTorrcFile();
        if(!torConfig.exists()){ // This shouldn't happen because it should have been installed by installResources
            throw new IOException("Config File not found.");
        }

        String torConfigString = Utils.readString(new FileInputStream(torConfig));
        StringBuilder newConfigBuilder = new StringBuilder("");
        String[] lines = torConfigString.split("\n");

        for (String line : lines){
            if (line.startsWith("SOCKSPort")){
                /* do it like this to avoid conflict in future versions if the default value gets
                * set to a specific port */
                line = line.replace("0", "auto");
            }

            newConfigBuilder.append(line);
            newConfigBuilder.append("\n");
        }

        if (!torConfig.delete()){
            Log.w(TAG, "Not sure if Tor is available for Local connections");
            return;
        }

        Utils.saveTextFile(torConfig.getAbsolutePath(), newConfigBuilder.toString());
    }
}
