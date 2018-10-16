package com.fater.gg;

import android.graphics.Color;

/**
 * Created on 2018-04-10.
 * This class use to store color information of pixels in the photo.
 */

public class ColorInfo implements Comparable<ColorInfo>
{
    int pixelColor;         //argb值
    double[] labColor;      //L*a*b*值
    int count;              //the count of this color in the photo
    double saliencyValue;   //the saliency value of this color
    double saliencyValueS;  //平滑操作后的显著性值

    ColorInfo(int argb, int i)
    {
        pixelColor = argb;
        count = i;
        labColor = new double[3];
        labColor = ImageProcess.RGBToLab(Color.red(pixelColor), Color.green(pixelColor), Color.blue(pixelColor));
        saliencyValue = 0;
        saliencyValueS = 0;
    }

    void AddCount(int i)
    {
        count += i;
    }

    void SetLow()
    {
        count = 0;
    }

    public int compareTo(ColorInfo o)
    {
        if(count > o.count)
            return -1;
        else if(count == o.count)
            return 0;
        else
            return 1;
    }
}
