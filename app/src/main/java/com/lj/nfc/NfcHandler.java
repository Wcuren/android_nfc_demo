package com.lj.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.widget.Toast;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class NfcHandler {

    private NfcAdapter mNfcAdapter;
    private IsoDep mIsoDep;
    private NfcView mView;
    public NfcHandler(NfcView view) {
        this.mView = view;
    }

    public void init(Context context) {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context);
    }

    private boolean checkNfc(Context context) {
        if (mNfcAdapter == null) {
            Toast.makeText(context, "未找到NFC设备！", Toast.LENGTH_SHORT).show();
            return false;
        } else if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(context, "请在设置中打开NFC开关！", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * 通过tag.getTechList()可以获取当前目标Tag支持的Tag Technology，这里默认支持IsoDep。
     * 通过IsoDep.get(tag)方式获取IsoDep的实例。然后通过函数connect()我们应用和IC卡之间建立联系，
     * 建立联系后我们可以往IC卡发送指令进行交互。
     * @param intent
     */
    private void connectNfc(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) ||
                NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                mIsoDep = IsoDep.get(tag);
                try {
                    mIsoDep.connect();  //这里建立我们应用和IC卡
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 这个函数可以获取IC卡的序列号
     * @param intent
     */
    public void readCardId(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null && mView != null) {
            byte[] ids = tag.getId();
            String uid = DataUtil.bytesToHexString(ids, ids.length);
            mView.appendResponse("\n uid is:" + uid);
        }
    }

    public void sendCommand(final Context context, final String command) {
        Observable<String> observable = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter){
                if (mIsoDep == null) {
                    emitter.onError(new Throwable("NFC设备未连接！"));
                    return;
                }
                try {
                    if (!mIsoDep.isConnected()) {
                        mIsoDep.connect();
                    }
                    byte[] sendData = DataUtil.hexStringToBytes(command);
                    if (sendData == null) {
                        emitter.onError(new Throwable("指令输入有误！"));
                        return;
                    }
                    byte[] responseData = mIsoDep.transceive(sendData);
                    emitter.onNext(DataUtil.bytesToHexString(responseData, responseData.length));
                } catch (IOException e) {
                    emitter.onError(new Throwable("NFC连接中断！"));
                }
            }
        });
        observable.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) {
                        if (s == null) {
                            Toast.makeText(context, "指令输入有误！", Toast.LENGTH_SHORT).show();
                        } else {
                            mView.appendResponse("\n apdu is:" + s);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        Toast.makeText(context, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void enableNfc(Activity activity) {
        if (checkNfc(activity)) {
            PendingIntent pendingIntent = PendingIntent.getActivity(activity,
                    0, new Intent(activity, activity.getClass()), 0);
            mNfcAdapter.enableForegroundDispatch(activity, pendingIntent, null, null);
            connectNfc(activity.getIntent());
        }
    }

    public void disableNfc(Activity activity) {
        if (mIsoDep != null && mIsoDep.isConnected()) {
            try{
                mIsoDep.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(activity);
        }
    }

    public void onDestroy() {
        mView = null;
    }
}
