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

        //
//    public void uploadBitmap(final Bitmap bitmap, final Context context) {
//
//        VolleyMultipartRequest volleyMultipartRequest = new VolleyMultipartRequest(Request.Method.POST, "https://yesor.ngrok.io/file_upload",
//                new Response.Listener<NetworkResponse>() {
//                    @Override
//                    public void onResponse(NetworkResponse response) {
//                        try {
//                            JSONObject obj = new JSONObject(new String(response.data));
//                            Toast.makeText(context, obj.getString("message"), Toast.LENGTH_SHORT).show();
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                },
//                new Response.ErrorListener() {
//                    @Override
//                    public void onErrorResponse(VolleyError error) {
//                        Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
//                        Log.e("GotError",""+error.getMessage());
//                    }
//                }) {
//
//
//            @Override
//            protected Map<String, DataPart> getByteData() {
//                Map<String, DataPart> params = new HashMap<>();
//                long imagename = System.currentTimeMillis();
//                params.put("image", new DataPart(imagename + ".png", getFileDataFromDrawable(bitmap)));
//                return params;
//            }
//
//            public byte[] getFileDataFromDrawable(Bitmap bitmap) {
//                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//                bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
//                return byteArrayOutputStream.toByteArray();
//            }
//        };
//
//
//
//        //adding the request to volley
//        Volley.newRequestQueue(context).add(volleyMultipartRequest);
//    }
        return detectedObjs;
    }
}