package xu.li.cordova.wechat;

import android.util.Log;

import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
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

    private static final String ERROR_WECHAT_NOT_INSTALLED = "未安装微信";
    private static final String ERROR_INVALID_PARAMETERS = "参数格式错误";
    private static final String ERROR_SEND_REQUEST_FAILED = "发送请求失败";

    private static final String KEY_ARG_SCENE = "scene";

    private static final int SCENE_SESSION = 0;
    private static final int SCENE_TIMELINE = 1;
    private static final int SCENE_FAVORITE = 2;

    private IWXAPI wxAPI;
    private CallbackContext callbackContext;

    private static Wechat instance;


    @Override
    protected void pluginInitialize() {
        super.pluginInitialize();

        String appId = preferences.getString(WXAPPID_PROPERTY_KEY, "");
        this.wxAPI = WXAPIFactory.createWXAPI(cordova.getActivity(), appId, true);
        this.wxAPI.registerApp(appId);
        instance = this;
        Log.d(TAG, "plugin initialized.");
    }

    public static Wechat getInstance() {
        return instance;
    }

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) {
        Log.d(TAG, String.format("%s is called. Callback ID: %s.", action, callbackContext.getCallbackId()));

        try {
            switch (action) {
                case "share":
                    return share(args, callbackContext);
                case "sendPaymentRequest":
                    return sendPaymentRequest(args, callbackContext);
                case "sendAuthRequest":
                    return sendAuthRequest(args, callbackContext);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            callbackContext.error(ERROR_INVALID_PARAMETERS);
            return false;
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

        if (wxAPI.sendReq(req)) {
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

    private boolean share(CordovaArgs args, final CallbackContext callbackContext) throws Exception {

        // check if installed
        if (!wxAPI.isWXAppInstalled()) {
            callbackContext.error(ERROR_WECHAT_NOT_INSTALLED);
            return true;
        }

        // check if # of arguments is correct
        final JSONObject params;
        try {
            params = args.getJSONObject(0);
        } catch (JSONException e) {
            callbackContext.error(ERROR_INVALID_PARAMETERS);
            return true;
        }

        final SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction();

        if (params.has(KEY_ARG_SCENE)) {
            switch (params.getInt(KEY_ARG_SCENE)) {
                case SCENE_FAVORITE:
                    req.scene = SendMessageToWX.Req.WXSceneFavorite;
                    break;
                case SCENE_TIMELINE:
                    req.scene = SendMessageToWX.Req.WXSceneTimeline;
                    break;
                case SCENE_SESSION:
                    req.scene = SendMessageToWX.Req.WXSceneSession;
                    break;
                default:
                    req.scene = SendMessageToWX.Req.WXSceneTimeline;
            }
        } else {
            req.scene = SendMessageToWX.Req.WXSceneTimeline;
        }


        req.message = WechatMessageBuilder.buildSharingMessage(params);
        if (wxAPI.sendReq(req)) {
            Log.i(TAG, "Message has been sent successfully.");
        } else {
            Log.i(TAG, "Message has been sent unsuccessfully.");

            // send error
            callbackContext.error(ERROR_SEND_REQUEST_FAILED);
        }


        // send no result
        sendNoResultPluginResult(callbackContext);

        return true;
    }

    private String buildTransaction() {
        return String.valueOf(System.currentTimeMillis());
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
