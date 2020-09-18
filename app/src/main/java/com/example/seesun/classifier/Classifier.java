package com.example.seesun.classifier;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.telecom.RemoteConference;

import java.util.List;

// interface에 Recognition class를 정의 & recognizeImage() 함수 정의
public interface Classifier {

    List<Recognition> recognizeImage(Bitmap bitmap);

    public class Recognition{
        private final String id;    // 클래스 인덱스 ?
        private final String title; // 객체 이름
        private final Float confidence; // 정확도
        private final RectF location;   // 위치
        private int detectedClass;


        public Recognition(final String id, final String title, final Float confidence, final RectF location){
            // RectF : float 값을 저장하여 사각형을 그린다.
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.location = location;
        };

        public Recognition(String id, String title, Float confidence, RectF location, int detectedClass) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.location = location;
            this.detectedClass = detectedClass;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Float getConfidence() {
            return confidence;
        }

        public RectF getLocation() {
            return location;
        }

        public int getDetectedClass() {
            return detectedClass;
        }

        public void setDetectedClass(int detectedClass) {
            this.detectedClass = detectedClass;
        }

        @Override
        public String toString() {
            String resultString = "";
            if (id != null) {
                resultString += "[" + id + "] ";
            }

            if (title != null) {
                resultString += title + " ";
            }

            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f);
            }

            if (location != null) {
                resultString += location + " ";
            }

            return resultString.trim();
        }
    }

}
