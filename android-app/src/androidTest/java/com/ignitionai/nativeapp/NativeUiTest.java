package com.ignitionai.nativeapp;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.widget.*;

public final class NativeUiTest extends Instrumentation {
    public void onCreate(Bundle args) { super.onCreate(args); start(); }
    public void onStart() {
        Bundle result=new Bundle(); MainActivity activity=null;
        try {
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            MainActivity app=activity;
            waitForIdleSync(); waitReady(app);
            runOnMainSync(()->((Button)app.findViewById(101)).performClick());
            waitReady(app);
            final String[] text={""};runOnMainSync(()->text[0]=((TextView)app.findViewById(102)).getText().toString());
            if(!text[0].contains("Records:") || !text[0].contains("SIMULATION"))throw new AssertionError("UI did not render simulated backend assessment: "+text[0]);
            if(!text[0].contains("Unavailable"))throw new AssertionError("Uncalibrated result is not explicit");
            result.putString("stream","PASS NativeUiTest: Activity -> simulator -> shared backend -> assessment widgets\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable t){result.putString("stream","FAIL NativeUiTest: "+t+"\n");finish(Activity.RESULT_CANCELED,result);}
        finally{if(activity!=null){MainActivity app=activity;runOnMainSync(app::finish);}}
    }
    private void waitReady(MainActivity app)throws Exception{
        long deadline=System.nanoTime()+30_000_000_000L;boolean[] enabled={false};
        do{Thread.sleep(100);runOnMainSync(()->enabled[0]=app.findViewById(101).isEnabled());}while(!enabled[0]&&System.nanoTime()<deadline);
        if(!enabled[0])throw new AssertionError("Timed out waiting for native action");
    }
}
