package jy.demo.tesseract.android;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

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


public class MainActivity extends AppCompatActivity {
    //카메라
    private File file; //캡처한 이미지
    private static CameraPreview surfaceView;
    private SurfaceHolder holder;
    private static Camera mCamera;
    private int RESULT_PERMISSIONS = 100;
    public static MainActivity getInstance;
    private Camera.PictureCallback myPictureCallback_JPG;
    private Camera.PictureCallback myPictureCallback_RAW;
    private Camera.ShutterCallback myShutterCallback;

    //테서랙트
    private TessBaseAPI tessBaseAPI;//tesseract 관련 클래스 객체
    private FileUploadUtils fu;
    private ArrayList<JSONObject> detectedObjs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fu = new FileUploadUtils();

        // 카메라 프리뷰를  전체화면으로 보여주기 위해 셋팅한다.
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 안드로이드 6.0 이상 버전에서는 CAMERA 권한 허가를 요청한다.
        requestPermissionCamera();

        myShutterCallback = new Camera.ShutterCallback(){
            @Override
            public void onShutter() {}};
        myPictureCallback_RAW = new Camera.PictureCallback(){
            @Override
            public void onPictureTaken(byte[] arg0, Camera arg1) {}};
        myPictureCallback_JPG = new Camera.PictureCallback(){
            @Override
            public void onPictureTaken(byte[] arg0, Camera arg1) {
                if (arg0 != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(arg0 , 0, arg0 .length);
                    if(bitmap!=null){
                        file = new File(Environment.getExternalStorageDirectory()+"/dirr");
                        if(!file.isDirectory()){
                            file.mkdir();
                        }
                        file = new File(Environment.getExternalStorageDirectory()+"/dirr","CAPTURE.jpg");
                        try
                        {
                            FileOutputStream fileOutputStream=new FileOutputStream(file);
                            bitmap.compress(Bitmap.CompressFormat.JPEG,100, fileOutputStream);

                            fileOutputStream.flush();
                            fileOutputStream.close();

//                            Toast.makeText(MainActivity.this, // 저장 테스트
//                                    "Image saved: " + file.toString(),
//                                    Toast.LENGTH_LONG).show();
                        }
                        catch(IOException e){
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
                        Thread.sleep(500);
                        dataProcessing(fu.send2Server(file));//response 호출될 때만
                    } catch (InterruptedException | JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void dataProcessing(ArrayList<JSONObject> detectedObjs) throws JSONException {
        Log.d("<dataprocessing 함수 호출>", "json 데이터 처리");
        if(detectedObjs == null){
            return;
        }
        for(int i=0;i<detectedObjs.size();i++){
            String object = detectedObjs.get(i).get("object").toString();
            float accuracy = Float.parseFloat(detectedObjs.get(i).get("accuracy").toString());
            float[] location = new float[4];
            String tmp = detectedObjs.get(i).get("location").toString();
            tmp = tmp.substring(1,tmp.length()-1);
//            System.out.println("tmp:"+tmp);
            for(int j = 0; j < 4 ; j++){
                location[j] = Float.parseFloat(tmp.substring(1,tmp.length()-1).split(",")[j]);
            }

            Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath()); // 비트맵 생성
            OpenCVLoader.initDebug(); // 이 코드를 선언해주지않으면 컴파일 에러 발생
            Mat img =new Mat();
            Utils.bitmapToMat(myBitmap ,img); // 비트맵을 Mat으로 변환
            //Draw rectangle
            //Parameters: mat object for drawing, point coordinates (x1,y1,x2,y2) and color BGR
            Imgproc.rectangle(img, new Point(location[0],location[1]), new Point(location[3],location[4]), new Scalar(255, 0, 0, 255), 3);

            Bitmap resultBitmap = Bitmap.createBitmap(img.cols(),  img.rows(),Bitmap.Config.ARGB_8888);;
            Utils.matToBitmap(img, resultBitmap); // Mat을 비트맵으로 변환

            ImageView imageView = (ImageView)findViewById(R.id.image_result);
            imageView.setImageBitmap(resultBitmap); // 이미지 뷰에 비트맵 출력
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

        if (RESULT_PERMISSIONS == requestCode) {

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
}