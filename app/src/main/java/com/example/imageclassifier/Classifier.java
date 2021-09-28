package com.example.imageclassifier;

import static org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod.NEAREST_NEIGHBOR;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Pair;
import android.widget.ListView;

import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.model.Model;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Classifier {

    // 모델 파일의 이름 설정
    private static final String MODEL_NAME = "mobilenet_imagenet_model.tflite";
    // assets에 있는 자원을 사용하기 위한 변수
    Context context;

    // 레이블 파일 이름과 목록 저장할 변수
    private static final String LABEL_FILE = "labels.txt";
    private List<String> labels;

    // 입력 이미지를 위한 변수
    // 자연어 처리에서 padding 설정과 비슷
    int modelInputWidth, modelInputHeight, modelInputChannel;
    TensorImage inputImage;
    TensorBuffer outputBuffer; // 출력을 위한 변수
    
    // init 메소드에서 파일의 내용을 읽어서 List에 저장하는 코드 추가
    
    // 생성자
    public Classifier(Context context) { this.context = context;}

    // 추론 모델 변수
    private Model model;

    // 사용자 정의 초기화 메소드
    public void init() throws IOException {
        // 모델 생성
        model = Model.createModel(context, MODEL_NAME);
        // 입력구조와 출력 구조를 만들어주는 사용자 정의 메소드 호출
        initModelShape();
        // 레이블 파일을 읽어서 labels에 저장
        labels = FileUtil.loadLabels(context, LABEL_FILE);
        // 파일을 만들 때 첫번째 줄을 삭제하지 않았으면 수행
        //labels.remove(0);
    }

    // 입력 구조와 출력 구조를 만들어주는 사용자 정의 메소드
    private void initModelShape() {
        // 입력 데이터의 shape를 가져와서 변수에 저장
        Tensor inputTensor = model.getInputTensor(0);
        int[] shape = inputTensor.shape();
        modelInputChannel = shape[0];
        modelInputWidth = shape[1];
        modelInputHeight = shape[2];

        // 입력 텐서 생성
        inputImage = new TensorImage(inputTensor.dataType());

/*        The issue is here that Interpreter does not consume TensorBuffer, but ByteBuffer and primitive arrays. See the Java doc for Interpreter.run.
          What you can do is to get the ByteBuffer from TensorBuffer through TensorBuffer.getBuffer(), and then pass the ByteBuffer to tflite.run(). Hope it helps.
 */

        // init()에서 입출력 텐서를 생성해주는 메소드 호출
        // 출력 버퍼 생성
        Tensor outputTensor = model.getOutputTensor(0);
        outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType());

    }

    // 안드로이드의 이미지를 분류 모델에서 사용할 수 있도록 변환해주는 메소드
    private Bitmap convertBitmapToARGB8888(Bitmap bitmap){
        return bitmap.copy(Bitmap.Config.ARGB_8888, true);
    }

    // 이미지를 읽어서 전처리 한 후 리턴해주는 딥러닝에 사용할 이미지를 리턴해주는 메소드
    private TensorImage loadImage(final Bitmap bitmap){

        if(bitmap.getConfig() != Bitmap.Config.ARGB_8888){
            inputImage.load(convertBitmapToARGB8888(bitmap));
        } else {
            inputImage.load(bitmap);
        }

        // 전처리 수행
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(modelInputWidth,
                                  modelInputHeight,
                                  NEAREST_NEIGHBOR)) // ResizeOp.ResizeMethod.NEAREST_NEIGHBOR
                .add(new NormalizeOp(0.0f, 255.0f))
                .build();
        // 전처리를 수행한 후 리턴
        return imageProcessor.process(inputImage);
    }

    // 출력 처리를 위한 파일 처리
    // ImageNet 데이터를 가지고 학습한 모델은 0~999까지 1,000 가    지 객체로 분류해서 인덱스와 확률 반환.
    // 사용자가 원하는 것은 인덱스가 아니고 인덱스에 매핑되는 label.
    // 인덱스와 매핑되는 레이블을 알아야 한다.

    // method for 추론 결과 해석
    // 확률이 가장 높은 레이블 이름과 확률을 Pair로 리턴하는 method
    private Pair<String, Float> argMax(Map<String, Float> map) {
        String maxKey = "";
        // 확률이 0~1 사이이므로 최대값을 구하기 위한 임시변수는
        // 0보다 작은 값에서 출발하면 됨.
        // 최소값을 구하는 문제라며 1보다 큰 값 아무거나 가능
        // 배열의 경우는 첫번째 데이터를 삽입하는 것이 효율적.
        float maxVal = -1;

        // enumerate 쓰는 거랑 같다
        for(Map.Entry<String, Float> entry: map.entrySet()){
            // 순회할 때마다 값을 가져와서 maxVal과 비교해서
            // maxVal 보다 크면 그 때의 key와 value 저장
            float f = entry.getValue();
            if (f > maxVal) {
                maxKey = entry.getKey();
                maxVal = f;
            }
        }

        // key와 value를 하나로 묶어서 리턴
        return new Pair<>(maxKey, maxVal);
    }

    // 추론을 위한 메소드
    // 스마트폰에서 굉장히 중요
    // 스마트폰에서 이미지를 사용할 때 기억해야 할 것은 -
    // 기기의 방향 문제.
    public Pair<String, Float> classify(
            Bitmap image, int sensorOrientation) {
        // 전처리된 이미지를 가져옴
        inputImage = loadImage(image);

        // 모델에 입력 가능한 형태로 변환
        Object[] inputs = new Object[]{inputImage.getBuffer()};
        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(0, outputBuffer.getBuffer().rewind());

        // 추론
        model.run(inputs, outputs);

        // 결과를 해석
        Map<String, Float> output = new TensorLabel(labels, outputBuffer).getMapWithFloatValue();

        return argMax(output);
    }

    // 메모리 정리를 위한 메소드
    public void finish(){
        if(model != null) {
            model.close();
        }
    }



}
