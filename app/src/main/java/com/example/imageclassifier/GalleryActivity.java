package com.example.imageclassifier;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.util.Locale;

public class GalleryActivity extends AppCompatActivity {
    //Log 출력에 사용할 태그 이름
    static final String TAG = "GALLERY ACTIVITY";
    //갤러리 이미지를 사용하고 응답을 받을 때 사용할 코드
    static final int GALLERY_REQUEST_CODE = 1;

    //뷰 관련 변수
    ImageView imageView;
    TextView textView;
    Button selectBtn;

    //추론 모델 관련 변수
    Classifier cls;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        //이전 Activity에서 전달한 데이터 읽어오기
        String data = getIntent().getStringExtra("data");
        Log.e("데이터", data);
        //Toast.makeText(this, data, Toast.LENGTH_LONG).show();

        //뷰 찾아오기
        imageView = findViewById(R.id.idGalleryIV);
        textView = findViewById(R.id.idGalleryTV);
        selectBtn = findViewById(R.id.idGalleryBtn);
        //버튼을 누르면 동작할 코드
        selectBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                //갤러리 화면 출력
                Intent intent =
                        new Intent(Intent.ACTION_GET_CONTENT)
                                .setType("image/*");
                //응답을 받을 수 있도록 Activity를 출력
                //액티비티가 화면에 출력되고
                //액티비티에서 데이터를 선택하면
                //onActivityForResult(int requestCode, int resultCode, Intent data)
                //가 호출됩니다.
                //data 에 선택한 데이터가 넘어옵니다.
                startActivityForResult(intent, GALLERY_REQUEST_CODE);
            }
        });

        //추론을 위한 클래스의 인스턴스 생성
        cls = new Classifier(this);
        try{
            cls.init();
        }catch(Exception e){
            Log.e(TAG, "초기화 실패");
        }

    }

    //startActivityForResult로 Activity를 출력한 후
    //출력된 Activity 가 사라지면 호출되는 메소드
    @Override
    //Overriding 의 목적
    //기능 구현(추상 메소드) 이나 기능 확장(추상 메소드가 아닌 경우)
    public void onActivityResult(
            int requestCode, int resultCode, Intent data){
        //상위 클래스의 메소드 호출
        //메모리를 정리하는 메소드인 경우는 super 의 호출이 가장 마지막에
        //와야 합니다.
        super.onActivityResult(requestCode, resultCode, data);
        //사용자 정의 코드

        //갤러리를 호출해서 응답이 온 경우
        if(resultCode == Activity.RESULT_OK &&
                requestCode == GALLERY_REQUEST_CODE){
            //선택한 이미지가 없으면 return
            if(data == null){
                return;
            }
            //선택된 이미지의 Uri 가져오기
            Uri selectedImage = data.getData();
            Bitmap bitmap = null;

            try{
                //버전 별로 작업 - 29(10.0) 이상인 경우
                //갤러리에서 선택한 이미지를 Bitmap으로 읽어오기
                if(Build.VERSION.SDK_INT >= 29){
                    Uri fileUri = data.getData();
                    ContentResolver resolver = getContentResolver();
                    InputStream inputStream = resolver.openInputStream(fileUri);
                    bitmap = BitmapFactory.decodeStream(inputStream);
                }
                //8.0 버전 이하에서 gallery에서 선택한 이미지 가져오기
                else{
                    bitmap = MediaStore.Images.Media.getBitmap(
                            getContentResolver(), selectedImage);
                }
            }catch(Exception e){
                Log.e(TAG, "이미지 가져오기 실패");
            }

            //읽어온 이미지가 있다면
            if(bitmap != null){
                //이미지를 가지고 추론
                Pair<String, Float> output = cls.classify(bitmap, 0);
                //추론 결과를 텍스트 뷰에 출력
                String resultStr = String.format(Locale.ENGLISH, "분류:%s    확률:%.2f%%", output.first, output.second * 100);
                textView.setText(resultStr);

                //읽어온 이미지를 이미지 뷰에 출력
                imageView.setImageBitmap(bitmap);

            }
        }
    }
}