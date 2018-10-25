package xu.li.cordova.wechat;

import android.util.Log;

import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

public class Wechat extends CordovaPlugin {

    public static final String TAG = "Cordova.Plugin.Wechat";

    private static final String WXAPPID_PROPERTY_KEY = "wechatappid";

    private static final String ERROR_INVALID_PARAMETERS = "参数格式错误";
    private static final String ERROR_SEND_REQUEST_FAILED = "发送请求失败";

    private IWXAPI wxAPI;
    private String appId;
    private CallbackContext callbackContext;

    private static Wechat instance;


    @Override
    protected void pluginInitialize() {
        super.pluginInitialize();

        this.appId = preferences.getString(WXAPPID_PROPERTY_KEY, "");
        this.wxAPI = WXAPIFactory.createWXAPI(cordova.getActivity(), this.appId, true);
        this.wxAPI.registerApp(appId);
        instance = this;
        Log.d(TAG, "plugin initialized.");
    }

    public static Wechat getInstance() {
        return instance;
    }

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, String.format("%s is called. Callback ID: %s.", action, callbackContext.getCallbackId()));

        if (action.equals("sendPaymentRequest")) {
            return sendPaymentRequest(args, callbackContext);
        } else if (action.equals("sendAuthRequest")) {
            return sendAuthRequest(args, callbackContext);
        } 

        return false;
    }

    private boolean sendPaymentRequest(CordovaArgs args, CallbackContext callbackContext) {

        // check if # of arguments is correct
        final JSONObject params;
        try {
            params = args.getJSONObject(0);
        } catch (JSONException e) {
            callbackContext.error(ERROR_INVALID_PARAMETERS);
            return true;
        }

        PayReq req = new PayReq();
        try {
            req.appId = params.getString("appid");
            req.partnerId = params.getString("partnerid");
            req.prepayId = params.getString("prepayid");
            req.nonceStr = params.getString("noncestr");
            req.timeStamp = params.getString("timestamp");
            req.sign = params.getString("sign");
            req.packageValue = params.getString("package");
            req.signType = "MD5";
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());

            callbackContext.error(ERROR_INVALID_PARAMETERS);
            return true;
        }

        if (wxAPI.sendReq(req)) {
            Log.i(TAG, "Payment request has been sent successfully.");
            // send no result
            sendNoResultPluginResult(callbackContext);
        } else {
            Log.i(TAG, "Payment request has been sent unsuccessfully.");

            // send error
            callbackContext.error(ERROR_SEND_REQUEST_FAILED);
        }


        return true;
    }

    private boolean sendAuthRequest(CordovaArgs args, CallbackContext callbackContext) {

        final SendAuth.Req req = new SendAuth.Req();
        try {
            req.scope = args.getString(0);
            req.state = args.getString(1);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());

            req.scope = "snsapi_userinfo";
            req.state = "wechat";
        }

        if (api.sendReq(req)) {
            Log.i(TAG, "Auth request has been sent successfully.");

            // send no result
            sendNoResultPluginResult(callbackContext);
        } else {
            Log.i(TAG, "Auth request has been sent unsuccessfully.");

            // send error
            callbackContext.error(ERROR_SEND_REQUEST_FAILED);
        }

        return true;
    }

    private String getAppId() {
        return appId;
    }

    public IWXAPI getWxAPI() {
        return wxAPI;
    }

    public CallbackContext getCallbackContext() {
        return callbackContext;
    }

    private void sendNoResultPluginResult(CallbackContext callbackContext) {
        this.callbackContext = callbackContext;

        // send no result and keep callback
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
    }
}
