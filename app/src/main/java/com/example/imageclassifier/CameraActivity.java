package com.example.imageclassifier;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;


// 추론에 필요한 인스턴스 변수를 선언
public class CameraActivity extends AppCompatActivity {

    // Log 출력에 사용할 태그 이름
    static final String TAG = "CAMERA ACTIVITY";
    // 갤러리 이미지를 사용하고 응답을 받을 때 사용할 코드
    static final int GALLERY_REQUEST_CODE = 1;

    // 이미지를 안드로이드 10.0 미만 버전에서 사용하기 위한 변수
    private static final String KEY_SELECT_URI = "KEY_SELECT_URI";
    private Uri selectedImageUri;

    // 객체지향 측면에서 private을 붙여주면 좋음
    private ImageView cameraIV;
    private Button cameraBtn;

    Classifier cls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // 뷰 찾아오기
        cameraIV = findViewById(R.id.idMainIVCamera);
        cameraBtn = findViewById(R.id.idMainBtnCamera);

        cameraBtn.setOnClickListener(view -> {
            // 화면에 카메라 출력하는 코드 작성
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, 1);
        });

        // cls 생성
        cls = new Classifier(this);
        try {
            cls.init();
        } catch (IOException e) {
            Log.e(TAG, "초기화 실패");
        }

        // 이전에 저장된 번들이 있으면 읽어오기
        if(savedInstanceState != null) {
            Uri uri = savedInstanceState.getParcelable(KEY_SELECT_URI);
            if (uri != null) {
                selectedImageUri = uri;
            }
        }
    }

    // startActivityForResult를 호출해서 Activity를 출력한 후,
    // Activity가 사라지면 호출되는 메소드
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK && requestCode ==1){
            // Bitmap: 그래픽을 메모리에 저장하는 경우
            // 이 경우 이미지를 Bitmap이라고 부름
            Bitmap bitmap = null;
            try {
                if (Build.VERSION.SDK_INT >= 29) {
                    bitmap = (Bitmap)data.getExtras().get("data");
                } else {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                }
            } catch (Exception e) {
                Log.e(TAG, "이미지 가져오기 실패");
            }

            // 이미지 확인
            if (bitmap != null) {
                // 이미지 출력
                cameraIV.setImageBitmap(bitmap);

                // 이미지 추론
                Pair<String, Float> output = cls.classify(bitmap, 0);
                // 결과 해석
                String resultStr = String.format("class: %s prob: %.2f%%", output.first, output.second *100);
                // 출력
                cameraBtn.setText(resultStr);
            }
        }
    }

    // Activity의 상태 변화가 발생했을 때 호출되는 메소드
    // 현재 상태를 저장
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_SELECT_URI, selectedImageUri);
    }

    @Override
    // 정리할 때는 자신이 만든 것을 먼저 정리하고
    // 프레임워크의 정리 메소드를 호출하는 것이 올바른 순서 ㅇㅅㅇ)!
    public void onDestroy(){
        cls.finish();
        super.onDestroy();
    }

}