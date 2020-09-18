package com.example.seesun.classifier;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.telecom.RemoteConference;

import com.example.seesun.env.Logger;
import com.example.seesun.env.Utils;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;


public class CustomClassifier implements Classifier {
//    private static final int INPUT_SIZE = 416;
    private static final int INPUT_SIZE = 512;
    private static final int BATCH_SIZE = 1;
    private static final int PIXEL_SIZE = 3;
    private static final int[] OUTPUT_WIDTH_FULL = new int[]{16128, 16128};
    private ByteBuffer imgData;
    private static final Logger LOGGER = new Logger();
    private static final int NUM_THREADS = 4;


    // Pre-allocated bufferes
    private boolean isModelQuantized;
    private int[] intValues;
    private Vector<String> labels = new Vector<String>();
    private Interpreter tfLite;  // interpreter interface for tensorlite models


    public static CustomClassifier create(final AssetManager assetManager,
                                          String modelFilename,
                                          final String labelFilename,
                                          final boolean isQuantized) throws IOException {
        final CustomClassifier d = new CustomClassifier();

        String actualFilename = labelFilename.split("file://android_asset/")[1];
        InputStream labelsInput = assetManager.open(actualFilename);
        BufferedReader br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine())!=null){
            LOGGER.w(line);     // warn 이면 메시지 전달
            d.labels.add(line);     // labels에 라벨 이름 저장
        }
        br.close();

        try{
            Interpreter.Options options = (new Interpreter.Options());
            options.setNumThreads(NUM_THREADS); // 4개의 thread 생성
            d.tfLite = new Interpreter(Utils.loadModelFile(assetManager, modelFilename), options);
        } catch (Exception e){
            throw new RuntimeException(e);
        }

        d.isModelQuantized = isQuantized;
        int numBytesPerChannel;
        if (isQuantized){
            numBytesPerChannel = 1;     // Quantized
        }   else {
            numBytesPerChannel = 4;
        }

        d.imgData = ByteBuffer.allocateDirect(1 * d.INPUT_SIZE * d.INPUT_SIZE * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        d.intValues = new int[d.INPUT_SIZE * d.INPUT_SIZE];

        return d;
    }

    @Override
    public ArrayList<Recognition> recognizeImage(Bitmap bitmap) {
        ByteBuffer byteBuffer = BitmapToByteBuffer(bitmap);

        ArrayList<Recognition> detections = getDetections(byteBuffer, bitmap);
        final ArrayList<Recognition> recognition = nms(detections);

        return recognition;
    }

    /**
    * Writes Image data into ByteBuffer
    * **/
    protected ByteBuffer BitmapToByteBuffer(Bitmap bitmap){ /** 시간 단축 시켜보기 **/
        // ByteBuffer.allocate()의 방식도 있는 듯(성능 차이)
        // ByteBuffer.allocateDirect() : heap이 아닌 외부 os에 할당
        // os를 이용하는 입출력에서 중간 개체를 거치지 않아 속도가 빠르나 할당 해제시 한단계를 더 거치기 때문에 속도 감소
        // 한번 할당 이후 오래동안 사용되는 부분에 사용하면 효율적
        ByteBuffer bf = ByteBuffer.allocateDirect(4*BATCH_SIZE * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE);
        bf.order(ByteOrder.nativeOrder());   // nativeOrder() : 현 시스템이 어떤 바이트 순서를 갖는 지
        // order() : 현재 버퍼가 어떤 바이트 순서를 갖는지 알아내 ByteOrder() 형태로 변환

        int[] imgSize = new int[INPUT_SIZE*INPUT_SIZE];

        // getPixels : 픽셀값을 저장할 메모리 객체를 파라미터로 넘긴다.
        bitmap.getPixels(imgSize, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixels = 0;
        for (int i=0; i<INPUT_SIZE; ++i){
            for (int j=0;j< INPUT_SIZE; ++i){
                final int val = imgSize[pixels++];
                bf.putFloat(((val>>16) & 0xFF) / 255.0f);
                bf.putFloat(((val>>8) & 0xFF) / 255.0f);
                bf.putFloat(((val) & 0xFF) / 255.0f);
            }
        }

        return bf;
    }

    private ArrayList<Recognition> getDetections(ByteBuffer byteBuffer, Bitmap bitmap){
        ArrayList<Recognition> directs = new ArrayList<Recognition>();
        Map<Integer, Object> output = new HashMap<>();
        output.put(0, new float[1][OUTPUT_WIDTH_FULL[0]][4]);
        output.put(1, new float[1][OUTPUT_WIDTH_FULL[1]][labels.size()]);
        Object[] inputArray = {byteBuffer};
        tfLite.runForMultipleInputsOutputs(inputArray, output);  // detect

        int gridwidth = OUTPUT_WIDTH_FULL[0];
        float[][][] bboxes = (float [][][]) output.get(0);      // bbox
        float[][][] out_score = (float[][][]) output.get(1);    // score

        for (int i=0; i<gridwidth; i++){
            float maxClass = 0;
            int detectedClass = -1;
            final float[] classes = new float[labels.size()];
            for (int c=0; c<labels.size(); ++c){
                if (classes[c] > maxClass){
                    detectedClass = c;
                    maxClass = classes[c];
                }
            }

        }

    }
}
