package acoustically.pessenger;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import org.json.JSONObject;

import java.net.Socket;

public class ReceiveSmsService extends Service {
  public ReceiveSmsService() {
  }

  @Override
  public IBinder onBind(Intent intent) {
    // TODO: Return the communication channel to the service.
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void onCreate() {
    super.onCreate();
    setBroadcastReceiver();
  }
  private void setBroadcastReceiver() {
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");

    registerReceiver(new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if ("android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
          try {
            String jsonData = buildJson(getSmsMessage(intent), getPhoneNumber());
            sendSmsMessageToServer(jsonData);
          } catch (Exception e) {
            Log.e("error", "fail to build json");
          }
        }
      }
    }, intentFilter);
  }
  private void sendSmsMessageToServer(String xmlData) {
    try {
      ServerWriteThread serverWriteThread = new ServerWriteThread(SocketConnector.getSocket(), xmlData);
      serverWriteThread.start();
    } catch (Exception e) {
      Log.e("error", "cannot send data to server");
    }
  }
  private String buildJson(SmsMessage smsMessage, String phoneNumber) throws Exception{
    JSONObject json = new JSONObject();
    json.put("client", "android");
    json.put("myPhoneNumber", phoneNumber);
    json.put("smsSenderPhoneNumber", smsMessage.getOriginatingAddress());
    json.put("smsBody", smsMessage.getMessageBody());
    return json.toString();
  }
  private String getPhoneNumber() {
    TelephonyManager telephonyManager = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
    return telephonyManager.getLine1Number();
  }
  private SmsMessage getSmsMessage(Intent intent) {
    Bundle bundle = intent.getExtras();
    Object messages[] = (Object[])bundle.get("pdus");
    SmsMessage smsMessage[] = new SmsMessage[messages.length];
    for(int i = 0; i < messages.length; i++) {
      smsMessage[i] = SmsMessage.createFromPdu((byte[])messages[i]);
    }
    return smsMessage[0];
  }
}
