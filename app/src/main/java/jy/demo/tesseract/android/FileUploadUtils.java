package jy.demo.tesseract.android;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class FileUploadUtils {
    private ArrayList<JSONObject> detectedObjs;

    public ArrayList<JSONObject> send2Server(File file){
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file",file.getName(),RequestBody.create(MultipartBody.FORM,file))
                .build();
        Request request = new Request.Builder()
        .url("https://yesor.ngrok.io/file_upload")
        .post(requestBody).build();

        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                detectedObjs = new ArrayList<>();
                try {
                    String content = response.body().string();
                    Log.d("GET: ", content);
                    if(content == null){return;}
                    if(content.contains("<!doctype html5>")){return;}
                    JSONObject jsonObject = new JSONObject(content);
                    JSONArray jsonArray = jsonObject.getJSONArray("obj");
//                    JSONArray jsonArray = new JSONArray(response.body().string());
                    for (int i=0;i<jsonArray.length();i++){
                        JSONObject jsonObject2 = jsonArray.getJSONObject(i);
                        Log.d("detected info", String.valueOf(jsonObject2));
                        detectedObjs.add(jsonObject2);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        return detectedObjs;
    }
}
