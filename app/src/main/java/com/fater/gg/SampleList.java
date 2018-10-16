package com.fater.gg;

import android.util.Log;

import java.util.LinkedList;
import java.util.List;

/**
 * Created on 2018-05-21.
 * 使用 List 实现 Sample 类型的二维向量
 * 虽然存入了所有像素，但只有未知像素参与了计算，可优化
 * 不需优化，因为虽然只有未知像素参与了计算，但是需要像素坐标来确定位置，如果不存入所有像素将无法判断位置
 */

public class SampleList
{
    static List<Sample> ls;
    static int width;           // 图像宽度
    static int height;          // 图像高度

    static
    {
        ls = new LinkedList<>();
        width = 0;
        height = 0;
    }

    // 初始化
    static void Init(int w, int h)
    {
        width = w;
        height = h;

        ls.clear();
        for(int i = 0; i < w*h; i++)
        {
            ls.add(new Sample());
        }
    }

    // 获取第 row 行，第 col 列的Sample值，下标从0开始
    static Sample GetValue(int col, int row)
    {
        if(row < height && col < width)
        {
            int num = row * width + col;
            return ls.get(num);
        }
        else
        {
            Log.d("错误", "坐标超出图像范围 in SampleList.GetValue()");
            return null;
        }

    }
}
