package com.fater.gg;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;


/**
 * Created on 2018-05-18.
 *  自定义 ImageView
 */

public class MyImageView extends AppCompatImageView
{
    private Context mContext;
    private float startX,startY;
    private Matrix currentMatrix, savedMatrix;// Matrix对象
    private PointF startF= new PointF();
    private PointF midF;// 起点、中点对象

    // 初始的两个手指按下的触摸点的距离
    private float oldDis = 1f;
    private float saveRotate = 0F;// 保存了的角度值
    private static final int MODE_NONE = 0;// 默认的触摸模式
    private static final int MODE_DRAG = 1;// 拖拽模式
    private static final int MODE_ZOOM = 2;// 缩放模式
    private int mode = MODE_NONE;



    public MyImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;

        // 初始化
        init();
    }

    private void init()
    {
        /*
         * 实例化对象
         */
        currentMatrix = new Matrix();
        savedMatrix = new Matrix();
    }

    //点击事件操作图像
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch (event.getAction()& MotionEvent.ACTION_MASK)
        {
            case MotionEvent.ACTION_DOWN:// 单点接触屏幕时
                savedMatrix.set(currentMatrix);
                startF.set(event.getX(), event.getY());
                mode=MODE_DRAG;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:// 第二个手指按下事件
                oldDis = calDis(event);
                if (oldDis > 10F) {
                    savedMatrix.set(currentMatrix);
                    midF=calMidPoint(event);
                    mode = MODE_ZOOM;
                }
                saveRotate = calRotation(event);//计算初始的角度
                break;
            case MotionEvent.ACTION_MOVE:// 触摸点移动时

                /*
                 * 单点触控拖拽平移
                 */

                if (mode == MODE_DRAG) {
                    currentMatrix.set(savedMatrix);
                    float dx = event.getX() - startF.x;
                    float dy = event.getY() - startF.y;
                    currentMatrix.postTranslate(dx, dy);
                }
                /*
                 * 两点触控拖放
                 */
                else if(mode == MODE_ZOOM && event.getPointerCount() == 2){
                    float newDis = calDis(event);
                    float rotate = calRotation(event);
                    currentMatrix.set(savedMatrix);

                    //指尖移动距离大于10F缩放
                    if (newDis > 10F) {
                        float scale = newDis / oldDis;
                        currentMatrix.postScale(scale, scale, midF.x, midF.y);
                    }

                    System.out.println("degree"+rotate);
                    //当旋转的角度大于5F才进行旋转
                    if(Math.abs(rotate - saveRotate)>5F){
                        currentMatrix.postRotate(rotate - saveRotate, getMeasuredWidth() / 2, getMeasuredHeight() / 2);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:// 单点离开屏幕时
                mode=MODE_NONE;
                break;
            case MotionEvent.ACTION_POINTER_UP:// 第二个点离开屏幕时
                System.out.println(event.getActionIndex());
                savedMatrix.set(currentMatrix);
                if(event.getActionIndex()==0)
                    startF.set(event.getX(1), event.getY(1));
                else if(event.getActionIndex()==1)
                    startF.set(event.getX(0), event.getY(0));
                mode=MODE_DRAG;
                break;
        }

        setImageMatrix(currentMatrix);
        return true;
    }

    // 计算两个触摸点之间的距离
    private float calDis(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    // 计算两个触摸点的中点
    private PointF calMidPoint(MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        return new PointF(x / 2, y / 2);
    }

    //计算角度
    private float calRotation(MotionEvent event) {
        double deltaX = (event.getX(0) - event.getX(1));
        double deltaY = (event.getY(0) - event.getY(1));
        double radius = Math.atan2(deltaY, deltaX);
        return (float) Math.toDegrees(radius);
    }

}
