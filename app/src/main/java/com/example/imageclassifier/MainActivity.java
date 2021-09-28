package com.example.imageclassifier;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
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
import android.widget.Toast;

import java.io.InputStream;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //갤러리 버튼을 클릭했을 때 처리
        Button galleryBtn = findViewById(R.id.idMainBtnGallery);
        galleryBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(
                        MainActivity.this, GalleryActivity.class);
                //다음 인텐트에 데이터 전달
                intent.putExtra("data", "전달하는 데이터");
                startActivity(intent);
            }
        });
    }
}


