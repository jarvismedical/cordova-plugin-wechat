package __PACKAGE_NAME__;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;

import xu.li.cordova.wechat.Wechat;

/**
 * Created by xu.li<AthenaLightenedMyPath@gmail.com> on 9/1/15.
 */
public class EntryActivity extends Activity implements IWXAPIEventHandler {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Wechat.TAG, "Create wx entry activity.");

        Wechat wechat = Wechat.getInstance();
        IWXAPI wxapi = wechat.getWxAPI();
        wxapi.handleIntent(getIntent(), this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        Wechat wechat = Wechat.getInstance();
        IWXAPI wxapi = wechat.getWxAPI();
        wxapi.handleIntent(intent, this);

        Log.d(Wechat.TAG, "Got a new intent by wx entry activity.");
    }

    @Override
    public void onResp(BaseResp resp) {
        Log.d(Wechat.TAG, "Wx resp errorCode=" + resp.errCode + " errorStr=" + resp.errStr);
        Wechat.getInstance().getCallbackContext().success();
        this.finish();
    }

    @Override
    public void onReq(BaseReq req) {
        finish();
    }

}

