package com.google.zxing.client.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import com.cipherlab.barcode.GeneralString;
import com.cipherlab.barcode.ReaderManager;
import com.cipherlab.barcode.decoder.Enable_State;
import com.cipherlab.barcode.decoderparams.ReaderOutputConfiguration;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

public class CipherCaptureActivity extends CaptureActivity {

    // TODO: Support more formats
    private enum CipherBarcodeFormat {
        CODE_39("[]A0", BarcodeFormat.CODE_39),
        ITF("[]I0", BarcodeFormat.ITF),
        EAN_13("[]E0", BarcodeFormat.EAN_13);

        private final String mPrefix;

        private final BarcodeFormat mFormat;

        CipherBarcodeFormat(String prefix, BarcodeFormat format) {
            mPrefix = prefix;
            mFormat = format;
        }

        BarcodeFormat getFormat() {
            return mFormat;
        }

        String getPrefix() {
            return mPrefix;
        }
    }

    private ReaderManager mReaderManager;

    private Enable_State mOldKeyboardEnabledState;

    private final BroadcastReceiver mCipherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(GeneralString.Intent_READERSERVICE_CONNECTED)
                    && mReaderManager != null) {
                ReaderOutputConfiguration config = new ReaderOutputConfiguration();
                mReaderManager.Get_ReaderOutputConfiguration(config);
                mOldKeyboardEnabledState = config.enableKeyboardEmulation;
                config.enableKeyboardEmulation = Enable_State.FALSE;
                mReaderManager.Set_ReaderOutputConfiguration(config);
            } else if (intent.getAction().equals(GeneralString.Intent_PASS_TO_APP)) {
                String data = intent.getStringExtra(GeneralString.BcReaderData);
                Result result = parseResult(data);
                if (result != null) {
                    beepManager.playBeepSoundAndVibrate();
                    handleDecode(result, null, 1.0f);
                }
            }
        }
    };

    private Result parseResult(String data) {
        if (data == null) {
            return null;
        }

        for (CipherBarcodeFormat format : CipherBarcodeFormat.values()) {
            String prefix = format.getPrefix();
            if (data.startsWith(prefix)) {
                return new Result(data.replace(prefix, ""), null, null, format.getFormat());
            }
        }

        // Couldn't find applicable format
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(GeneralString.Intent_READERSERVICE_CONNECTED);
        filter.addAction(GeneralString.Intent_PASS_TO_APP);
        registerReceiver(mCipherReceiver, filter);

        mReaderManager = ReaderManager.InitInstance(this);
        if (mReaderManager != null) {
            Toast.makeText(this, R.string.cipherlabs_scanner_connected, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            unregisterReceiver(mCipherReceiver);
        } catch (Exception e) {
            // Receiver not registered
        }

        if (mReaderManager != null) {
            if (mOldKeyboardEnabledState != null) {
                ReaderOutputConfiguration config = new ReaderOutputConfiguration();
                mReaderManager.Get_ReaderOutputConfiguration(config);
                config.enableKeyboardEmulation = mOldKeyboardEnabledState;
                mReaderManager.Set_ReaderOutputConfiguration(config);
            }
            mReaderManager.Release();
        }

        mOldKeyboardEnabledState = null;
        mReaderManager = null;
    }
}
