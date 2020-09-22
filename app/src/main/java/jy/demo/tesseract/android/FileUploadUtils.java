package jy.demo.tesseract.android;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class FileUploadUtils {
    public static void send2Server(File file){
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file",file.getName(),RequestBody.create(MultipartBody.FORM,file))
                .build();
        Request request = new Request.Builder()
        .url("http://fa5d348f7e0f.ngrok.io/file_upload")
        .post(requestBody).build();

        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("TEST :: ", response.body().toString());
//                try {
//                    JSONObject jsonObject = new JSONObject(response.body().toString());
//                    Log.d("obj0: ",jsonObject.getString("obj0"));
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
            }
        });
    }
}
