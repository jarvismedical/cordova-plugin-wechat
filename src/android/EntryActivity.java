package __PACKAGE_NAME__;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.modelbiz.ChooseCardFromWXCardPackage;

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
        CallbackContext ctx = Wechat.getInstance().getCallbackContext();

        if (ctx == null) {
            return ;
        }
        
        switch (resp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                switch (resp.getType()) {
                    case ConstantsAPI.COMMAND_SENDAUTH:
                        auth(resp);
                        break;
                    case ConstantsAPI.COMMAND_CHOOSE_CARD_FROM_EX_CARD_PACKAGE:
                    case ConstantsAPI.COMMAND_PAY_BY_WX:
                    default:
                        ctx.success();
                        break;
                }
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                ctx.error("ERR_USER_CANCEL");
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                ctx.error("ERR_AUTH_DENIED");
                break;
            case BaseResp.ErrCode.ERR_SENT_FAILED:
                ctx.error("ERR_SENT_FAILED");
                break;
            case BaseResp.ErrCode.ERR_UNSUPPORT:
                ctx.error("ERR_UNSUPPORT");
                break;
            case BaseResp.ErrCode.ERR_COMM:
                ctx.error("ERR_COMM");
                break;
            default:
                ctx.error("ERR_UNKONWN");
                break;
        }

        this.finish();
    }

    @Override
    public void onReq(BaseReq req) {
        finish();
    }

    private void auth(BaseResp resp) {
        SendAuth.Resp res = ((SendAuth.Resp) resp);

        Log.d(Wechat.TAG, res.toString());

        // get current callback context
        CallbackContext ctx = Wechat.getInstance().getCallbackContext();

        if (ctx == null) {
            return ;
        }

        JSONObject response = new JSONObject();
        try {
            response.put("code", res.code);
            response.put("state", res.state);
            response.put("country", res.country);
            response.put("lang", res.lang);
        } catch (JSONException e) {
            Log.e(Wechat.TAG, e.getMessage());
        }

        ctx.success(response);
    }

}

