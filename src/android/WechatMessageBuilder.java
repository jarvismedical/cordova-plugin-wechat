package xu.li.cordova.wechat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.webkit.URLUtil;

import com.tencent.mm.opensdk.modelmsg.WXAppExtendObject;
import com.tencent.mm.opensdk.modelmsg.WXEmojiObject;
import com.tencent.mm.opensdk.modelmsg.WXFileObject;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXMusicObject;
import com.tencent.mm.opensdk.modelmsg.WXTextObject;
import com.tencent.mm.opensdk.modelmsg.WXVideoObject;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static xu.li.cordova.wechat.Wechat.TAG;

class WechatMessageBuilder {

    private static final String KEY_ARG_MESSAGE = "message";
    private static final String KEY_ARG_TEXT = "text";
    private static final String KEY_ARG_MESSAGE_TITLE = "title";
    private static final String KEY_ARG_MESSAGE_DESCRIPTION = "description";
    private static final String KEY_ARG_MESSAGE_THUMB = "thumb";
    private static final String KEY_ARG_MESSAGE_MEDIA = "media";
    private static final String KEY_ARG_MESSAGE_MEDIA_TYPE = "type";
    private static final String KEY_ARG_MESSAGE_MEDIA_WEBPAGEURL = "webpageUrl";
    private static final String KEY_ARG_MESSAGE_MEDIA_IMAGE = "image";
    private static final String KEY_ARG_MESSAGE_MEDIA_MUSICURL = "musicUrl";
    private static final String KEY_ARG_MESSAGE_MEDIA_MUSICDATAURL = "musicDataUrl";
    private static final String KEY_ARG_MESSAGE_MEDIA_VIDEOURL = "videoUrl";
    private static final String KEY_ARG_MESSAGE_MEDIA_FILE = "file";
    private static final String KEY_ARG_MESSAGE_MEDIA_EMOTION = "emotion";
    private static final String KEY_ARG_MESSAGE_MEDIA_EXTINFO = "extInfo";
    private static final String KEY_ARG_MESSAGE_MEDIA_URL = "url";

    private static final int TYPE_WECHAT_SHARING_APP = 1;
    private static final int TYPE_WECHAT_SHARING_EMOTION = 2;
    private static final int TYPE_WECHAT_SHARING_FILE = 3;
    private static final int TYPE_WECHAT_SHARING_IMAGE = 4;
    private static final int TYPE_WECHAT_SHARING_MUSIC = 5;
    private static final int TYPE_WECHAT_SHARING_VIDEO = 6;
    private static final int TYPE_WECHAT_SHARING_WEBPAGE = 7;

    private static final int MAX_THUMBNAIL_SIZE = 320;

    private static final String EXTERNAL_STORAGE_IMAGE_PREFIX = "external://";

    static WXMediaMessage buildSharingMessage(JSONObject params) throws JSONException, IOException {
        Log.d(TAG, "Start building message.");

        // media parameters
        WXMediaMessage.IMediaObject mediaObject;
        WXMediaMessage wxMediaMessage = new WXMediaMessage();

        if (params.has(KEY_ARG_TEXT)) {
            WXTextObject textObject = new WXTextObject();
            textObject.text = params.getString(KEY_ARG_TEXT);
            mediaObject = textObject;
            wxMediaMessage.description = textObject.text;
        } else {
            JSONObject message = params.getJSONObject(KEY_ARG_MESSAGE);
            JSONObject media = message.getJSONObject(KEY_ARG_MESSAGE_MEDIA);

            wxMediaMessage.title = message.getString(KEY_ARG_MESSAGE_TITLE);
            wxMediaMessage.description = message.getString(KEY_ARG_MESSAGE_DESCRIPTION);

            // thumbnail
            Bitmap thumbnail = getThumbnail(message);
            if (thumbnail != null) {
                wxMediaMessage.setThumbImage(thumbnail);
                thumbnail.recycle();
            }

            // check types
            int type = media.has(KEY_ARG_MESSAGE_MEDIA_TYPE) ? media
                    .getInt(KEY_ARG_MESSAGE_MEDIA_TYPE) : TYPE_WECHAT_SHARING_WEBPAGE;

            switch (type) {
                case TYPE_WECHAT_SHARING_APP:
                    WXAppExtendObject appObject = new WXAppExtendObject();
                    appObject.extInfo = media.getString(KEY_ARG_MESSAGE_MEDIA_EXTINFO);
                    appObject.filePath = media.getString(KEY_ARG_MESSAGE_MEDIA_URL);
                    mediaObject = appObject;
                    break;

                case TYPE_WECHAT_SHARING_EMOTION:
                    WXEmojiObject emoObject = new WXEmojiObject();
                    InputStream emoji = getFileInputStream(media.getString(KEY_ARG_MESSAGE_MEDIA_EMOTION));
                    if (emoji != null) {
                        emoObject.emojiData = Util.readBytes(emoji);

                    }
                    mediaObject = emoObject;
                    break;

                case TYPE_WECHAT_SHARING_FILE:
                    WXFileObject fileObject = new WXFileObject();
                    fileObject.filePath = media.getString(KEY_ARG_MESSAGE_MEDIA_FILE);
                    mediaObject = fileObject;
                    break;

                case TYPE_WECHAT_SHARING_IMAGE:
                    Bitmap image = getBitmap(message.getJSONObject(KEY_ARG_MESSAGE_MEDIA), KEY_ARG_MESSAGE_MEDIA_IMAGE, 0);
                    mediaObject = new WXImageObject(image);
                    assert image != null;
                    image.recycle();
                    break;

                case TYPE_WECHAT_SHARING_MUSIC:
                    WXMusicObject musicObject = new WXMusicObject();
                    musicObject.musicUrl = media.getString(KEY_ARG_MESSAGE_MEDIA_MUSICURL);
                    musicObject.musicDataUrl = media.getString(KEY_ARG_MESSAGE_MEDIA_MUSICDATAURL);
                    mediaObject = musicObject;
                    break;

                case TYPE_WECHAT_SHARING_VIDEO:
                    WXVideoObject videoObject = new WXVideoObject();
                    videoObject.videoUrl = media.getString(KEY_ARG_MESSAGE_MEDIA_VIDEOURL);
                    mediaObject = videoObject;
                    break;

                case TYPE_WECHAT_SHARING_WEBPAGE:
                default:
                    mediaObject = new WXWebpageObject(media.getString(KEY_ARG_MESSAGE_MEDIA_WEBPAGEURL));
            }
        }

        wxMediaMessage.mediaObject = mediaObject;

        return wxMediaMessage;
    }

    private static Bitmap getThumbnail(JSONObject message) throws IOException, JSONException {
        return getBitmap(message, KEY_ARG_MESSAGE_THUMB, MAX_THUMBNAIL_SIZE);
    }

    private static Bitmap getBitmap(JSONObject message, String key, int maxSize) throws JSONException, IOException {
        Bitmap bmp;
        String url = message.getString(key);

        if (!message.has(key)) {
            return null;
        }

        // get input stream
        InputStream inputStream = getFileInputStream(url);
        if (inputStream == null) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        bmp = BitmapFactory.decodeStream(inputStream, null, options);

        // scale
        if (maxSize > 0 && (options.outWidth > maxSize || options.outHeight > maxSize)) {

            Log.d(TAG, String.format("Bitmap was decoded, dimension: %d x %d, max allowed size: %d.",
                    options.outWidth, options.outHeight, maxSize));

            int width, height;

            if (options.outWidth > options.outHeight) {
                width = maxSize;
                height = width * options.outHeight / options.outWidth;
            } else {
                height = maxSize;
                width = height * options.outWidth / options.outHeight;
            }

            assert bmp != null;
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, width, height, true);
            bmp.recycle();

            int length = scaled.getRowBytes() * scaled.getHeight();

            if (length > (maxSize / 10) * 1024) {
                scaled = compressImage(scaled, (maxSize / 10));
            }

            bmp = scaled;
        }

        inputStream.close();


        return bmp;
    }

    private static InputStream getFileInputStream(String url) throws IOException {

        InputStream inputStream;

        if (URLUtil.isHttpUrl(url) || URLUtil.isHttpsUrl(url)) {
            try {
                inputStream = new URL(url).openStream();
            } catch (Exception e) {
                inputStream = null;
            }

            Log.d(TAG, String.format("File was downloaded and cached to %s.", url));
        } else if (url.startsWith("data:image")) {  // base64 image
            String imageDataBytes = url.substring(url.indexOf(",") + 1);
            byte imageBytes[] = Base64.decode(imageDataBytes.getBytes(), Base64.DEFAULT);
            inputStream = new ByteArrayInputStream(imageBytes);

            Log.d(TAG, "Image is in base64 format.");

        } else if (url.startsWith(EXTERNAL_STORAGE_IMAGE_PREFIX)) { // external path

            url = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + url.substring(EXTERNAL_STORAGE_IMAGE_PREFIX.length());
            inputStream = new FileInputStream(url);

            Log.d(TAG, String.format("File is located on external storage at %s.", url));
        } else {
            inputStream = new FileInputStream(url);
            Log.d(TAG, String.format("File is located at %s.", url));
        }

        return inputStream;
    }

    private static Bitmap compressImage(Bitmap image, Integer maxSize) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        int options = 90;

        while (baos.toByteArray().length / 1024 > maxSize) {
            baos.reset();
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);
            options -= 10;
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());

        return BitmapFactory.decodeStream(isBm, null, null);
    }
}
