package jy.demo.tesseract.android;
import java.util.ArrayList;
import java.util.Date;

public class CrossThread extends Thread {
    ArrayList<Long> DateArr;
    boolean cw_mode;
    boolean tf_mode;
    Long tf_mode_on;

    public CrossThread(){
        DateArr = new ArrayList<>(3);
        cw_mode = true;
        tf_mode = false;
    }

    public void findCrosswalk(Long time){
        int n = DateArr.size();
        if (n>0 && n<3) {
            if (time-DateArr.get(n-1)/1000.0>3) {    // 시간이 3초 이상 지나면
                DateArr.removeAll(DateArr); // 배열 전체 비우기
            }
            DateArr.add(time);
        }
    }

    public void run(){  // cw_mode가 작동 될 수 있는지 계속 감시
        while(cw_mode){
            System.out.println("cross walk mode");
            System.out.println("TL mode 되기 "+Integer.toString(3-DateArr.size()));
            if (DateArr.size()==3){
                DateArr.removeAll(DateArr);
                cw_mode = false;    // cw_mode off
                tf_mode = true;
                tf_mode_on = System.currentTimeMillis();
            }
        }
        while(tf_mode){ // 신호등 모드 시작
            System.out.println("traffic light mode");
            if (tf_mode_on-System.currentTimeMillis()>=15){ // 신호등 모드 15초 이상 경과하면
                cw_mode = true;
                tf_mode = false;
            }
        }
    }
}