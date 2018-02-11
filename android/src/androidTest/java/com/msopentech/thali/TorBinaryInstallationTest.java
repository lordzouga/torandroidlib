package com.msopentech.thali;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyContext;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.torproject.android.binary.TorServiceConstants;
import org.torproject.android.binary.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
public class TorBinaryInstallationTest implements TorServiceConstants{
    Context mContext;
    String TAG = "TorBinaryTests";

    @Before
    public void init(){
        mContext = InstrumentationRegistry.getContext();
    }

    @Test
    public void doTestInstall() throws IOException, InterruptedException {
        String dirName = "tortest";

        AndroidOnionProxyContext proxyContext = new AndroidOnionProxyContext(mContext, dirName);

        proxyContext.installFiles();
        File dirFile = mContext.getDir(dirName, Context.MODE_PRIVATE);

        assertTrue(proxyContext.getGeoIpFile().exists());
        assertTrue(proxyContext.getGeoIpv6File().exists());
        assertTrue(proxyContext.getTorExecutableFile().exists());
        assertTrue(proxyContext.getTorrcFile().exists());

        Log.i(TAG, "Installation was successful");
    }

    @Test
    public void doTestConfigChange() throws IOException, InterruptedException {
        String dirName = "tortestconfigchange";
        AndroidOnionProxyContext proxyContext = new AndroidOnionProxyContext(mContext, dirName);

        proxyContext.installFiles();

        File torrcFile = proxyContext.getTorrcFile();
        String torrcFileContents = Utils.readString(new FileInputStream(torrcFile));

        String[] torrcLines = torrcFileContents.split("\n");

        boolean socksPortFound = false;

        for (String line: torrcLines){
            if (line.startsWith("SOCKSPort")){
                socksPortFound = true;

                String[] sockSportWords = line.split(" ");
                assertTrue(sockSportWords[1].equals("9050"));
            }
        }

        if (!socksPortFound) throw new RuntimeException("Invalid config file");

    }
}
