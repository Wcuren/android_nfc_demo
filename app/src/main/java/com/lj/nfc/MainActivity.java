package com.lj.nfc;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements NfcView{

    private static final String TAG = MainActivity.class.getName();
    private static final String DEFAULT_COMMAND = "00A404000B464D53482E44432E415050";
    private static final String DEFAULT_COMMAND2 = "0084000008";

    @BindView(R.id.rev_data) EditText mRevDataEt;
    @BindView(R.id.send_command_et) EditText mSendCommandEt;
    @BindView(R.id.read_btn) Button mReadCardBtn;
    @BindView(R.id.send_command_btn) Button mSendCommandBtn;

    private NfcHandler mNfcHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate()! ");
        mNfcHandler = new NfcHandler(this);
        mNfcHandler.init(this);
        ButterKnife.bind(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent()! action is:" + intent.getAction());
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()! intent action is:" + getIntent().getAction());
        mNfcHandler.enableNfc(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNfcHandler.disableNfc(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNfcHandler.onDestroy();
    }

    @Override
    public void appendResponse(String response) {
        mRevDataEt.append(response);
    }

    @OnClick({R.id.send_command_btn, R.id.read_btn})
    void onBtnClick(View view) {
        switch (view.getId()) {
            case R.id.read_btn:
                mNfcHandler.readCardId(getIntent());
                break;
            case R.id.send_command_btn:
                mNfcHandler.sendCommand(this, mSendCommandEt.getText().toString());
                break;
            default:
                break;
        }
    }

}
