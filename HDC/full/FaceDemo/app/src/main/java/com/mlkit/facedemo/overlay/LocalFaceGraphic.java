/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.mlkit.facedemo.overlay;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.Log;

import com.huawei.hms.mlsdk.common.MLPosition;
import com.huawei.hms.mlsdk.face.MLFace;
import com.huawei.hms.mlsdk.face.MLFaceShape;
import com.mlkit.facedemo.util.CommonUtils;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.content.ContentValues.TAG;

public class LocalFaceGraphic extends BaseGraphic {


    private final Paint facePaint;
    private final Paint faceFeaturePaintText;
    private final Paint faceFeaturePaint;


    private volatile MLFace face;
    private Context mContext;
    private final GraphicOverlay overlay;

    List<MLFace> list = new ArrayList<>();

    public LocalFaceGraphic(GraphicOverlay overlay, MLFace face, Context context) {
        super(overlay);

        this.mContext = context;
        this.face = face;
        this.overlay = overlay;
        list.add(face);

        float lineWidth = CommonUtils.dp2px(this.mContext, 1);
        this.facePaint = new Paint();
        this.facePaint.setColor(Color.parseColor("#ffcc66"));
        this.facePaint.setStyle(Paint.Style.STROKE);
        this.facePaint.setStrokeWidth(lineWidth);

        this.faceFeaturePaintText = new Paint();
        this.faceFeaturePaintText.setColor(Color.WHITE);
        this.faceFeaturePaintText.setTextSize(CommonUtils.dp2px(this.mContext, 11));
        this.faceFeaturePaintText.setTypeface(Typeface.DEFAULT);

        this.faceFeaturePaint = new Paint();
        this.faceFeaturePaint.setColor(this.faceFeaturePaintText.getColor());
        this.faceFeaturePaint.setStyle(Paint.Style.STROKE);
        this.faceFeaturePaint.setStrokeWidth(CommonUtils.dp2px(this.mContext, 2));
    }

    public List<String> sortHashMap(HashMap<String, Float> map) {
        Set<Map.Entry<String, Float>> entey = map.entrySet();
        List<Map.Entry<String, Float>> list = new ArrayList<Map.Entry<String, Float>>(entey);
        Collections.sort(list,comparator);
        List<String> emotions = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            emotions.add(list.get(i).getKey());
        }
        return emotions;
    }

    static Comparator comparator =new Comparator<Map.Entry<String, Float>>() {
        @Override
        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
            if (o2.getValue() - o1.getValue() >= 0) {
                return 1;
            } else {
                return -1;
            }
        }
    };

    @Override
    public void draw(Canvas canvas) {
        if (this.face == null) {
            return;
        }
        MLFaceShape faceShape = this.face.getFaceShape(MLFaceShape.TYPE_FACE);
        List<MLPosition> points = faceShape.getPoints();
        float verticalMin = Float.MAX_VALUE;
        float verticalMax = 0f;
        float horizontalMin = Float.MAX_VALUE;
        float horizontalMax = 0f;
        for (int i = 0; i < points.size(); i++) {
            MLPosition point = points.get(i);
            if (point == null) {
                continue;
            }
            if (point.getX() != null && point.getY() != null) {
                if (point.getX() > horizontalMax) horizontalMax = point.getX();
                if (point.getX() < horizontalMin) horizontalMin = point.getX();
                if (point.getY() > verticalMax) verticalMax = point.getY();
                if (point.getY() < verticalMin) verticalMin = point.getY();

            }
        }
        Rect rect = new Rect((int) this.translateX(horizontalMin), (int) this.translateY(verticalMin), (int) this.translateX(horizontalMax), (int) this.translateY(verticalMax));
        canvas.drawRect(rect, this.facePaint);

        float start = this.overlay.getWidth() / 3.0f;
        float x = start;
        float width = this.overlay.getWidth() / 3.0f;
        float y;
        if (this.isLandScape()) {
            y = (CommonUtils.dp2px(this.mContext, this.overlay.getHeight() / 8.0f)) < 130 ? 130 : (CommonUtils.dp2px(this.mContext, this.overlay.getHeight() / 8.0f));
        } else {
            y = (CommonUtils.dp2px(this.mContext, this.overlay.getHeight() / 16.0f)) < 340.0 ? 340 :(CommonUtils.dp2px(this.mContext, this.overlay.getHeight() / 16.0f));
            if (this.overlay.getHeight() > 2500) {
                y = CommonUtils.dp2px(this.mContext, this.overlay.getHeight() / 10.0f);
            }
        }
        Log.i(TAG, x + "," + y + "; height" + this.overlay.getHeight()+ ",width" + this.overlay.getWidth());

        // Show all features mode.
        if (list.size()>0) {
            List<MLFace> faceFeatures = new ArrayList<MLFace>();
            faceFeatures.add(this.list.get(0));
        this.paintFeatures(faceFeatures, canvas, x, y, width);
            for (int i = 1; i < this.list.size(); i++) {
                // Paint all point in face.
                List<MLPosition> getPoints = this.list.get(i).getAllPoints();
                if (getPoints == null){
                    return;
                }
                for (int j = 0; j < getPoints.size(); j++) {
                    MLPosition point = getPoints.get(j);
                    if (point == null) {
                        continue;
                    }
                    canvas.drawPoint(this.translateX(point.getX().floatValue()), this.translateY(point.getY().floatValue()), this.facePaint);
                }
            }
        }
        return;

    }

    private boolean isLandScape() {
        Configuration configuration = this.mContext.getResources().getConfiguration(); // Get the configuration information.
        int ori = configuration.orientation; // Get screen orientation.
        return ori == Configuration.ORIENTATION_LANDSCAPE;
    }


    private void paintFeatures(List<MLFace> faces, Canvas canvas, float x, float y, float width) {
        float start = x;
        float space = CommonUtils.dp2px(this.mContext, 12);
        for (MLFace face : faces) {
            HashMap<String, Float> emotions = new HashMap<>();
            emotions.put("Smiling", face.possibilityOfSmiling());
            emotions.put("Neutral", face.getEmotions().getNeutralProbability());
            emotions.put("Angry", face.getEmotions().getAngryProbability());
            emotions.put("Fear", face.getEmotions().getFearProbability());
            emotions.put("Sad", face.getEmotions().getSadProbability());
            emotions.put("Disgust", face.getEmotions().getDisgustProbability());
            emotions.put("Surprise", face.getEmotions().getSurpriseProbability());
            List<String> result = this.sortHashMap(emotions);
            DecimalFormat decimalFormat = new DecimalFormat("0.00");

            canvas.drawText("Glass Probability: " + decimalFormat.format(face.getFeatures().getSunGlassProbability()), x, y, this.faceFeaturePaintText);
            x = x + width;
            String sex = (face.getFeatures().getSexProbability() > 0.5f) ? "Female" : "Male";
            canvas.drawText("Gender: " + sex, x, y, this.faceFeaturePaintText);
            y = y - space;
            x = start;
            canvas.drawText("EulerAngleY: " + decimalFormat.format(face.getRotationAngleY()), x, y, this.faceFeaturePaintText);
            x = x + width;
            canvas.drawText("EulerAngleX: " + decimalFormat.format(face.getRotationAngleX()), x, y, this.faceFeaturePaintText);
            y = y - space;
            x = start;
            canvas.drawText("EulerAngleZ: " + decimalFormat.format(face.getRotationAngleZ()), x, y, this.faceFeaturePaintText);
            x = x + width;
            canvas.drawText("Emotion: " + result.get(0), x, y, this.faceFeaturePaintText);
            x = start;
            y = y - space;
            canvas.drawText("Hat Probability: " + decimalFormat.format(face.getFeatures().getHatProbability()), x, y, this.faceFeaturePaintText);
            x = x + width;
            canvas.drawText("Age: " + face.getFeatures().getAge(), x, y, this.faceFeaturePaintText);
            y = y - space;
            x = start;
            canvas.drawText("Moustache Probability: " + decimalFormat.format(face.getFeatures().getMoustacheProbability()), x, y, this.faceFeaturePaintText);
            y = y - space;
            canvas.drawText("Right eye open Probability: " + decimalFormat.format(face.opennessOfRightEye()), x, y, this.faceFeaturePaintText);
            y = y - space;
            canvas.drawText("Left eye open Probability: " + decimalFormat.format(face.opennessOfLeftEye()), x, y, this.faceFeaturePaintText);
            // Paint all points in face.
            List<MLPosition> points = face.getAllPoints();
            for (int i = 0; i < points.size(); i++) {
                MLPosition point = points.get(i);
                if (point == null) {
                    continue;
                }
                canvas.drawPoint(this.translateX(point.getX().floatValue()), this.translateY(point.getY().floatValue()), this.faceFeaturePaint);
            }
        }
    }
}