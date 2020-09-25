package jy.demo.tesseract.android;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import android.view.ViewGroup.LayoutParams;


public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private File file; //캡처한 이미지
    private static CameraPreview surfaceView;
    private SurfaceHolder holder;
    private static Camera mCamera;
    private int RESULT_PERMISSIONS = 100;
    public static MainActivity getInstance;
    private Camera.PictureCallback myPictureCallback_JPG;
    private Camera.PictureCallback myPictureCallback_RAW;
    private Camera.ShutterCallback myShutterCallback;
    private FileUploadUtils fu; //파일전송/수신 클래스
    private ArrayList<JSONObject> detectedObjs;//json data 배열

    private TessBaseAPI tessBaseAPI;//tesseract 관련 클래스 객체

    private TextToSpeech tts;

    AudioManager aManager;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        aManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        fu = new FileUploadUtils();
        tts = new TextToSpeech(this, this);

        // 카메라 프리뷰를  전체화면으로 보여주기 위해 셋팅한다.
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 안드로이드 6.0 이상 버전에서는 CAMERA 권한 허가를 요청한다.
        requestPermissionCamera();

        // 갤러리 저장하는 권한을 준다.
        if(Build.VERSION.SDK_INT>22){
            requestPermissions(new String[] {WRITE_EXTERNAL_STORAGE}, 1);
        }


        myShutterCallback = new Camera.ShutterCallback(){
            @Override
            public void onShutter() {
            }};
        myPictureCallback_RAW = new Camera.PictureCallback(){
            @Override
            public void onPictureTaken(byte[] arg0, Camera arg1) {
            }};
        myPictureCallback_JPG = new Camera.PictureCallback(){
            @Override
            public void onPictureTaken(byte[] arg0, Camera arg1) {
                if (arg0 != null) {
                    bitmap = BitmapFactory.decodeByteArray(arg0 , 0, arg0.length);
                    if(bitmap!=null){
                        file = new File(Environment.getExternalStorageDirectory()+"/dirr");
                        if(!file.isDirectory()){
                            file.mkdir();
                        }
                        file = new File(Environment.getExternalStorageDirectory()+"/dirr","CAPTURE.jpg");
                        try
                        {
                            FileOutputStream fileOutputStream=new FileOutputStream(file);
                            bitmap = Bitmap.createScaledBitmap(bitmap, 640, 480, true );
                            bitmap = GetRotatedBitmap(bitmap, 90);

                            bitmap.compress(Bitmap.CompressFormat.JPEG,50, fileOutputStream);


                            fileOutputStream.flush();
                            fileOutputStream.close();

                            Toast.makeText(MainActivity.this, // 저장 테스트
                                    "Image saved: " + file.toString(),
                                    Toast.LENGTH_LONG).show();
                        } catch(Exception e){
                            e.printStackTrace();
                        }
                        catch(Exception exception)
                        {
                            exception.printStackTrace();
                        }

                    }
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(1000);//1초 간격으로 사진 촬영
                        mCamera.takePicture(myShutterCallback,
                                myPictureCallback_RAW, myPictureCallback_JPG);

                        while(file==null){}

//                        fu.uploadBitmap(bitmap, MainActivity.this);
                        detectedObjs = fu.send2Server(file);

                        runOnUiThread(new Runnable() {//UI 변경
                            @Override
                            public void run() {
                                try {
                                    dataProcessing(detectedObjs);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        LinearLayout layout = findViewById(R.id.background);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean b, Camera camera) {

                    }
                });
            }
        });
    }

    public synchronized static Bitmap GetRotatedBitmap(Bitmap bitmap, int degrees)
    {
        if ( degrees != 0 && bitmap != null )
        {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2 );
            try
            {
                Bitmap b2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                if (bitmap != b2)
                {
                    bitmap.recycle();
                    bitmap = b2;
                }
            }
            catch (OutOfMemoryError ex)
            {
                // We have no memory to rotate. Return the original bitmap.
            }
        }

        return bitmap;
    }


    public void dataProcessing(ArrayList<JSONObject> detectedObjs) throws JSONException {
        Log.d("<dataprocessing 함수 호출>", "json 데이터 처리");
        ImageView imageView = (ImageView)findViewById(R.id.image_result);
        if(detectedObjs == null){
            return;
        }
        for(int i=0;i<detectedObjs.size();i++){
            String object = detectedObjs.get(i).get("object").toString();
            float accuracy = Float.parseFloat(detectedObjs.get(i).get("accuracy").toString());
            float[] location = new float[4];
            String tmp = detectedObjs.get(i).get("location").toString();
            for(int j = 0; j < 4 ; j++){
                location[j] = Float.parseFloat(tmp.substring(1,tmp.length()-1).split(",")[j]);
            }

            //이미지 뷰에 띄우기
            Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath()); // 비트맵 생성
            OpenCVLoader.initDebug(); // 이 코드를 선언해주지않으면 컴파일 에러 발생
            Mat img =new Mat();
            Utils.bitmapToMat(myBitmap ,img); // 비트맵을 Mat으로 변환

            //Draw rectangle
            //Parameters: mat object for drawing, point coordinates (x1,y1,x2,y2) and color BGR
            System.out.println("좌표:"+location[0]+"/"+location[1]+"/"+location[2]+"/"+location[3]);
            Imgproc.rectangle(img, new Point(location[0],location[1]), new Point(location[2],location[3]), new Scalar(255, 0, 0, 255), 3);

            Bitmap resultBitmap = Bitmap.createBitmap(img.cols(), img.rows(),Bitmap.Config.ARGB_8888);;
            Utils.matToBitmap(img, resultBitmap); // Mat을 비트맵으로 변환

            imageView.setImageBitmap(resultBitmap); // 이미지 뷰에 비트맵 출력

            //tts 음성 안내
            float MINACC = 50;
            if(accuracy >= MINACC){
                CharSequence obj = object;
                speakOut(obj);
            }
        }
    }

    public static Camera getCamera(){
        return mCamera;
    }
    private void setInit(){
        getInstance = this;
        // 카메라 객체를 R.layout.activity_main의 레이아웃에 선언한 SurfaceView에서 먼저 정의해야 함으로 setContentView 보다 먼저 정의한다.
        mCamera = Camera.open();
        setContentView(R.layout.activity_main);
        // SurfaceView를 상속받은 레이아웃을 정의한다.
        surfaceView = (CameraPreview) findViewById(R.id.preview);
        // SurfaceView 정의 - holder와 Callback을 정의한다.
        holder = surfaceView.getHolder();
        holder.addCallback(surfaceView);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public boolean requestPermissionCamera(){
        int sdkVersion = Build.VERSION.SDK_INT;
        if(sdkVersion >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CAMERA},
                        RESULT_PERMISSIONS);
            }else {
                setInit();
            }
        }else{  // version 6 이하일때
            setInit();
            return true;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        System.out.println("****************** on Request Permission"+String.valueOf(requestCode)+" / ");
//        switch (requestCode) {
//            case 1: {
//                if (!(grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
////                    Toast.makeText(MainActivity.this , Toast.LENGTH_SHORT).show();
//                    System.out.println("Permission denied to access your location.");
//                }
//            }
//            case 0:
//
//        }

        if (RESULT_PERMISSIONS == requestCode) {

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                System.out.println();
                // 권한 허가시
                setInit();
            } else {
                // 권한 거부시
            }
            return;
        }
    }

    static {
        System.loadLibrary("opencv_java4");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void speakOut(CharSequence text){
        tts.setPitch((float) 0.6);
//        tts.setSpeechRate((float)0.1);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH,null,"id1");
    }
    @Override
    public void onDestroy() {
        if (tts != null)  {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS)  {
            int result = tts.setLanguage(Locale.ENGLISH);
//            int result = tts.setLanguage(Locale.KOREA);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }
}