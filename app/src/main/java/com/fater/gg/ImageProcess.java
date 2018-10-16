package com.fater.gg;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.util.SparseArray;
import android.util.SparseIntArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.Callable;

/**
 * Created on 2018-04-09.
 */

public class ImageProcess
{
    static boolean fgdone;
    static boolean quantdone;                   //判断是否已进行过量化
    static Bitmap fgImage;                      //前景原显著图像
    static Bitmap ffgImage;                     //扣出后的前景图
    static Bitmap tempImage;                    //前景图计算期间的临时图像
    static Uri imageUri;                        //存放图像路径
    static int balance;                         //显著阈值
    static Bitmap bgImage;                      //背景图像
    private static int pixelSum;                //像素总数
    static SparseArray<ColorInfo> colorSa;      //存放 高频像素值-颜色信息 对应表
    static SparseIntArray colorCh;              //存放 低频颜色-相似高频颜色 对应表
    static boolean iscamera;

    static
    {
        fgImage = null;
        ffgImage = null;
        tempImage = null;
        imageUri = null;
        balance = 0;
        bgImage = null;
        pixelSum = 0;
        colorSa = new SparseArray<>();
        colorCh = new SparseIntArray();
        fgdone = false;
        quantdone = false;
        iscamera = false;
    }

    //初始化
    static void Init()
    {
        colorSa.clear();
        colorCh.clear();
        fgdone = false;
        quantdone = false;
        pixelSum = 0;
        iscamera = false;
    }

    //图像灰度化
    static Bitmap SetGrayscale(final Bitmap bitmap)
    {
        final int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        final Bitmap bm = Bitmap.createBitmap(width, height, bitmap.getConfig());

        final int heightCount = height / More.threadCount;

        int part;

        //多线程异步处理图像
        for(part = 1; part <= More.threadCount; part++)
        {
            final int partS = part;

            More.completionService.submit(
                    new Callable<Integer>()
                    {
                        @Override
                        public Integer call() throws Exception
                        {
                            for(int row = heightCount * (partS - 1); row < heightCount * partS; row++)
                            {
                                for(int col = 0; col < width; col++)
                                {
                                    int pixel = bitmap.getPixel(col, row);// ARGB
                                    int alpha = Color.alpha(pixel); // same as (pixel >>> 24)
                                    if(alpha != 0)
                                    {
                                        int red = Color.red(pixel); // same as (pixel >> 16) &0xff
                                        int green = Color.green(pixel); // same as (pixel >> 8) &0xff
                                        int blue = Color.blue(pixel); // same as (pixel & 0xff)
                                        int gray = (Math.max(blue, Math.max(red, green)) +
                                                Math.min(blue, Math.min(red, green))) / 2;
                                        bm.setPixel(col, row, Color.argb(alpha, gray, gray, gray));
                                    }

                                }
                            }
                            return null;
                        }
                    }
            );
        }

        //判断线程是否执行完毕
        try
        {
            for(part = 1; part <= More.threadCount; part++)
            {
                More.completionService.take();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return bm;
    }

    //图像泛黄
    static Bitmap SetOlder(final Bitmap bitmap)
    {
        final int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        final Bitmap bm = Bitmap.createBitmap(width, height, bitmap.getConfig());

        final int heightCount = height / More.threadCount;

        int part;

        //多线程异步处理图像
        for(part = 1; part <= More.threadCount; part++)
        {
            final int partS = part;

            More.completionService.submit(
                    new Callable<Integer>()
                    {
                        @Override
                        public Integer call() throws Exception
                        {
                            for(int row = heightCount * (partS - 1); row < heightCount * partS; row++)
                            {
                                for(int col = 0; col < width; col++)
                                {
                                    int pixel = bitmap.getPixel(col, row);// ARGB
                                    int alpha = Color.alpha(pixel); // same as (pixel >>> 24)
                                    if(alpha != 0)
                                    {
                                        int red = Color.red(pixel); // same as (pixel >> 16) &0xff
                                        int green = Color.green(pixel); // same as (pixel >> 8) &0xff
                                        int blue = Color.blue(pixel); // same as (pixel & 0xff)

                                        red += 50;
                                        green += 50;
                                        if(red > 255)
                                            red = 255;
                                        if(green > 255)
                                            green = 255;
                                        bm.setPixel(col, row, Color.argb(alpha, red, green, blue));
                                    }
                                }
                            }
                            return null;
                        }
                    }
            );
        }

        //判断线程是否执行完毕
        try
        {
            for(part = 1; part <= More.threadCount; part++)
            {
                More.completionService.take();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return bm;
    }

    //减少图像颜色数量
    static void ReduceImageColor(final Bitmap bitmap)
    {
        final int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        final Bitmap bm = Bitmap.createBitmap(width, height, bitmap.getConfig());
        pixelSum = width * height;

        final int heightCount = height / More.threadCount;
        int part;

        //多线程异步量化图像颜色
        for(part = 1; part <= More.threadCount; part++)
        {
            final int partS = part;

            More.completionService.submit(
                    new Callable<Integer>()
                    {
                        @Override
                        public Integer call() throws Exception
                        {
                            for(int row = heightCount * (partS - 1); row < heightCount * partS; row++)
                            {
                                for(int col = 0; col < width; col++)
                                {
                                    int pixel = bitmap.getPixel(col, row);// ARGB
                                    bm.setPixel(col, row, QuantizeRGB(pixel));
                                }
                            }
                            return null;
                        }
                    }
            );
        }

        try
        {
            for(part = 1; part <= More.threadCount; part++)
            {
                More.completionService.take();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        //将颜色存入colorSa
        for(int i = 0; i < height; i++)
        {
            for(int j = 0; j < width; j++)
            {
                int argb = bm.getPixel(j, i);
                ColorInfo ci = colorSa.get(argb);
                if(ci != null)
                {
                    ci.AddCount(1);
                }
                else
                {
                    colorSa.put(argb, new ColorInfo(argb, 1));
                }
            }
        }

        //找出低于5%的像素值
        LinkedList<ColorInfo> lc = new LinkedList<>();      //依照频率的像素信息表
        ArrayList<Integer> lfColor = new ArrayList<>();     //低频颜色表
        int len = colorSa.size();                           //原本颜色总数
        for(int i = 0; i < len; i++)
        {
            lc.add(colorSa.valueAt(i));
        }
        Collections.sort(lc);           //将像素信息由频率从高到低排列

        int sum = 0;        //当前像素总和
        int index;          //低频像素在lc中的起始下标
        for(index = 0; index < len; )
        {
            ColorInfo ci = lc.get(index);
            sum += ci.count;
            index++;
            if(sum > (pixelSum * 0.95))
            {
                break;
            }
        }

        //寻找每个低频颜色对应的高频颜色,
        for(int i = index; i < len; i++)
        {
            int lfc = lc.get(i).pixelColor;
            int simColor = lfc;                     //记录相似高频色
            double minLen = Integer.MAX_VALUE;      //记录最短距离
            for(int j = 0; j < index; j++)
            {
                int hfc = lc.get(j).pixelColor;
                double l = GetColorLen(lfc, hfc);
                if(l < minLen)
                {
                    minLen = l;
                    simColor = hfc;
                }
            }
            colorCh.put(lfc, simColor);             //在对应表中记录下对应关系
            ColorInfo lfC = colorSa.get(lfc);
            ColorInfo hfC = colorSa.get(simColor);
            hfC.AddCount(lfC.count);                //将低频色的数量加在对应高频色上
            colorSa.remove(lfc);                    //移除该低频色
        }

        tempImage = bm;
    }

    //计算图像中每一个像素的显著值
    static void SetSaliencyValue()
    {
        int hfcNum = colorSa.size();
        double[][] colorDistances;   //二维数组存放高频色之间的距离

        //计算colorSa中高频色相互之间的距离，可多线程
        colorDistances = new double[hfcNum][hfcNum];
        for(int i = 0; i < hfcNum; i++)
        {
            colorDistances[i][i] = 0;

            for(int j = i + 1; j < hfcNum; j++)
            {
                colorDistances[i][j] = GetColorLen(colorSa.keyAt(i), colorSa.keyAt(j));
                colorDistances[j][i] = colorDistances[i][j];
            }
        }

        //计算每一个颜色的显著性值，可多线程
        for(int i = 0; i < hfcNum; i++)
        {
            ColorInfo tempC = colorSa.valueAt(i);
            tempC.saliencyValue = 0;

            for(int j = 0; j < hfcNum; j++)
            {
                tempC.saliencyValue += (colorDistances[i][j] * colorSa.valueAt(j).count);
            }

            tempC.saliencyValue /= pixelSum;
        }

        //平滑每一个颜色的显著性值，可多线程
        int m = hfcNum / 4;
        int[] closeColor = new int[m];
        for(int i = 0; i < m; i++)
        {
            closeColor[i] = 0;
        }

        for(int i = 0; i < hfcNum; i++)
        {
            //取 m = n/4 个最近颜色的下标
            int room = m;
            for(int j = 0; j < hfcNum; j++)
            {
                if(j != i)
                {
                    if(room > 0)
                    {
                        int index = m - room;
                        closeColor[index] = j;
                        for(int k = index; (k > 0) && (colorDistances[i][closeColor[k]] < colorDistances[i][closeColor[k-1]]); k--)
                        {
                            int temp = closeColor[k-1];
                            closeColor[k-1] = closeColor[k];
                            closeColor[k] = temp;
                        }
                        room--;
                    }
                    else
                    {
                        if(colorDistances[i][j] < colorDistances[i][closeColor[m-1]])
                        {
                            closeColor[m-1] = j;
                            for(int k = m - 1; (k > 0) && (colorDistances[i][closeColor[k]] < colorDistances[i][closeColor[k-1]]); k--)
                            {
                                int temp = closeColor[k-1];
                                closeColor[k-1] = closeColor[k];
                                closeColor[k] = temp;
                            }
                        }
                    }
                }
            }

            //计算T，即m个最近颜色距离之和
            int T = 0;
            for(int l = 0; l < m; l++)
            {
                T += colorDistances[i][closeColor[l]];
            }

            //计算平滑显著性值
            for(int l = 0; l < m; l++)
            {
                colorSa.valueAt(i).saliencyValueS += (
                                (T - colorDistances[i][closeColor[l]])
                                * colorSa.valueAt(closeColor[l]).saliencyValue);
            }

            colorSa.valueAt(i).saliencyValueS /= (T * (m -1));
        }
    }

    //将图像置为二值图
    static Bitmap GetBinaryImage(final Bitmap bitmap)
    {
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        final int heightCount = height / More.threadCount;
        final Bitmap bt = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int part;

        for(part = 1; part <= More.threadCount; part++)
        {
            final int partS = part;
            More.completionService.submit(
                    new Callable<Integer>()
                    {
                        @Override
                        public Integer call() throws Exception
                        {
                            for(int row = heightCount * (partS - 1); (row < heightCount * partS) && (row < height); row++)
                            {
                                for(int col = 0; col < width; col++)
                                {
                                    int pixel = bitmap.getPixel(col, row);
                                    int p = colorCh.get(pixel);
                                    if(p != 0)
                                    {
                                        //如果查到
                                        double d = colorSa.get(p).saliencyValueS;
                                        if(d < balance)
                                        {
                                            bt.setPixel(col, row, Color.argb(255, 0, 0, 0));
                                        }
                                        else
                                        {
                                            bt.setPixel(col, row, Color.argb(0, 0, 0, 0));
                                        }
                                    }
                                    else
                                    {
                                        double d = colorSa.get(pixel).saliencyValueS;
                                        if(d < balance)
                                        {
                                            bt.setPixel(col, row, Color.argb(255, 0, 0, 0));
                                        }
                                        else
                                        {
                                            bt.setPixel(col, row, Color.argb(0, 0, 0, 0));
                                        }
                                    }
                                }
                            }
                            return null;
                        }
                    }
            );
        }

        try
        {
            for(part = 1; part <= More.threadCount; part++)
            {
                More.completionService.take();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return bt;
    }

    //将图像置为三值图
    static void GetTrimap(final Bitmap bitmap)
    {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        for(int y = 1; y < h - 1; y++)
        {
            for(int x = 1; x < w - 1; x++)
            {
                if(Color.alpha(bitmap.getPixel(x, y)) == 255)
                {
                    if (Color.alpha(bitmap.getPixel(x, y-1)) == 0 ||
                        Color.alpha(bitmap.getPixel(x, y+1)) == 0 ||
                        Color.alpha(bitmap.getPixel(x-1, y)) == 0 ||
                        Color.alpha(bitmap.getPixel(x+1, y)) == 0)
                    {
                        //检测到边界像素时，将其与周围八个像素置为未知像素
                        for(int j = y - 1; j <= y + 1; j++)
                        {
                            for(int i = x - 1; i <= x + 1; i++)
                            {
                                bitmap.setPixel(i, j, Color.argb(128, 0, 0, 0));
                            }
                        }
                    }
                }
            }
        }
    }

    //计算两个ARGB颜色在L*a*b*空间中的距离
    private static double GetColorLen(int lfc, int hfc)
    {
        double[] lowC = colorSa.get(lfc).labColor;
        double[] HighC = colorSa.get(hfc).labColor;

        double tL = lowC[0] - HighC[0];
        double ta = lowC[1] - HighC[1];
        double tb = lowC[2] - HighC[2];
        double temp = Math.pow(tL, 2) + Math.pow(ta, 2) + Math.pow(tb, 2);

        return Math.pow(temp, 0.5);
    }

    //量化RGB值
    private static int QuantizeRGB(int pixel)
    {
        int[] RGB = new int[3];
        RGB[0] = Color.red(pixel);      // same as (pixel >> 16) &0xff
        RGB[1] = Color.green(pixel);    // same as (pixel >> 8) &0xff
        RGB[2] = Color.blue(pixel);     // same as (pixel & 0xff)
        int alpha = Color.alpha(pixel); // same as (pixel >>> 24)

        for(int i = 0; i < 3; i++)
        {
            RGB[i] = RGB[i] / 21 * 21 + 12;

            if(RGB[i] > 243)
            {
                RGB[i] = 243;
            }
        }

        int argb = Color.argb(alpha, RGB[0], RGB[1], RGB[2]);

        return argb;
    }

    //将RGB颜色转化为L*a*b*空间中的值
    static double[] RGBToLab(int R, int G, int B)
    {
        double r, g, b, X, Y, Z, xr, yr, zr;

        // D65/2°
        double Xr = 95.047;
        double Yr = 100.0;
        double Zr = 108.883;


        // --------- RGB to XYZ ---------//

        r = R/255.0;
        g = G/255.0;
        b = B/255.0;

        if (r > 0.04045)
            r = Math.pow((r+0.055)/1.055,2.4);
        else
            r = r/12.92;

        if (g > 0.04045)
            g = Math.pow((g+0.055)/1.055,2.4);
        else
            g = g/12.92;

        if (b > 0.04045)
            b = Math.pow((b+0.055)/1.055,2.4);
        else
            b = b/12.92 ;

        r*=100;
        g*=100;
        b*=100;

        X =  0.4124*r + 0.3576*g + 0.1805*b;
        Y =  0.2126*r + 0.7152*g + 0.0722*b;
        Z =  0.0193*r + 0.1192*g + 0.9505*b;


        // --------- XYZ to Lab --------- //

        xr = X/Xr;
        yr = Y/Yr;
        zr = Z/Zr;

        if ( xr > 0.008856 )
            xr =  (float) Math.pow(xr, 1/3.);
        else
            xr = (float) ((7.787 * xr) + 16 / 116.0);

        if ( yr > 0.008856 )
            yr =  (float) Math.pow(yr, 1/3.);
        else
            yr = (float) ((7.787 * yr) + 16 / 116.0);

        if ( zr > 0.008856 )
            zr =  (float) Math.pow(zr, 1/3.);
        else
            zr = (float) ((7.787 * zr) + 16 / 116.0);


        double[] lab = new double[3];

        lab[0] = (116*yr)-16;
        lab[1] = 500*(xr-yr);
        lab[2] = 200*(yr-zr);

        return lab;
    }
}
