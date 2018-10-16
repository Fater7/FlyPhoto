package com.fater.gg;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created on 2018-05-15.
 */

public class Matting
{
    static Bitmap foreground;
    static Bitmap alpha;

    static
    {
        foreground = null;
        alpha = null;
    }


    static void expansionOfKnownRegions(Bitmap img, Bitmap trimap, int niter)
    {
        if((img.getWidth() == trimap.getWidth()) && (img.getHeight() == trimap.getHeight()))
        {
            for(int i = 0; i < niter; i++)
            {
                Log.d("减小未知区域范围", "第" + i + "遍");
                expansionOfKnownRegionsHelper(img, trimap, i + 1, niter - i);
            }

            //erodeFB(trimp, 2);
        }
    }

    //减小未知区域范围
    static private void expansionOfKnownRegionsHelper(Bitmap img, Bitmap trimap, int r, float c)
    {
        int w = img.getWidth();
        int h = img.getHeight();

        for(int x = 0; x < w; x++)
        {
            for(int y = 0; y < h; y++)
            {
                int p1 = Color.alpha(trimap.getPixel(x, y));
                if (p1 != 128)
                    continue;

                // 当为未知像素时
                for (int j = y-r; j <= y+r; ++j)
                    for (int i = x-r; i <= x+r; ++i)
                    {
                        // 如果附近颜色超出边界或仍未未知像素，跳过
                        if (i < 0 || i >= w || j < 0 || j >= h)
                            continue;

                        if (Color.alpha(trimap.getPixel(i, j)) != 0 && Color.alpha(trimap.getPixel(i, j)) != 255)
                            continue;

                        int p2 = img.getPixel(i, j);

                        // 计算两像素之间的几何距离
                        double pd = Math.pow((Math.pow((x - i), 2) + Math.pow((y - j), 2)), 0.5);
                        // 计算两像素在RGB空间中的距离
                        double cd = colorDist(p1, p2);

                        if (pd <= r && cd <= c)
                        {
                            if (Color.alpha(trimap.getPixel(i, j)) == 0)
                                trimap.setPixel(x, y, Color.argb(1, 0, 0, 0));
                            else if (Color.alpha(trimap.getPixel(i, j)) == 255)
                                trimap.setPixel(x, y, Color.argb(254, 0, 0, 0));
                        }
                    }
            }
        }

        //可多
        for (int x = 0; x < w; ++x)
            for (int y = 0; y < h; ++y)
            {
                if (Color.alpha(trimap.getPixel(x, y)) == 1)
                    trimap.setPixel(x, y, Color.argb(0, 0, 0, 0));
                else if (Color.alpha(trimap.getPixel(x, y)) == 254)
                    trimap.setPixel(x, y, Color.argb(255, 0, 0, 0));
            }
    }

    //抠图
    static void globalMatting(Bitmap img, Bitmap trimap)
    {
        if((img.getWidth() == trimap.getWidth()) && (img.getHeight() == trimap.getHeight()))
        {
            globalMattingHelper(img, trimap);
        }
    }

    //抠图
    static private void globalMattingHelper(Bitmap img, Bitmap trimap)
    {
        int w = trimap.getWidth();
        int h = trimap.getHeight();

        //查找三值图边界
        List<CPoint> foregroundBoundary = findBoundaryPixels(img, trimap, 255, 128);
        List<CPoint> backgroundBoundary = findBoundaryPixels(img, trimap, 0, 128);

        Log.d("抠图", "查找边界结束");
        int n = foregroundBoundary.size() + backgroundBoundary.size();

//        //又随机选n个已知像素插入边界数组
//        for (int i = 0; i < n; ++i)
//        {
//            int x = (int)(Math.random() * 1000 % w);
//            int y = (int)(Math.random() * 1000 % h);
//
//            if (Color.alpha(trimap.getPixel(x, y)) == 0)
//                backgroundBoundary.add(new CPoint(x, y, img.getPixel(x, y)));
//            else if (Color.alpha(trimap.getPixel(x, y)) == 255)
//                foregroundBoundary.add(new CPoint(x, y, img.getPixel(x, y)));
//        }

        Log.d("抠图", "排列边界");

        //根据强度排列边界数组
        Collections.sort(foregroundBoundary);
        Collections.sort(backgroundBoundary);

        Log.d("抠图", "排列边界结束");

        calculateAlphaPatchMatch(img, trimap, foregroundBoundary, backgroundBoundary);

        Log.d("抠图", "计算完毕");

        alpha = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        //foreground = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        //似乎是最后一步生成图像了
        for (int y = 0; y < h; ++y)
        {
            for (int x = 0; x < w; ++x)
            {
                switch (Color.alpha(trimap.getPixel(x, y)))
                {
                    case 0:
                        //alpha.setPixel(x, y, Color.argb(0, 0, 0, 0));
                        alpha.setPixel(x, y, img.getPixel(x, y));
                        //foreground.setPixel(x, y, Color.argb(0, 0, 0, 0));
                        break;
                    case 128:
                    {
                        Sample s = SampleList.GetValue(x, y);
                        //alpha.setPixel(x, y, Color.argb((int)(255 * s.alpha), 0, 0, 0));
                        if((255 * s.alpha) > 128)
                        {
                            alpha.setPixel(x, y, Color.argb(0, 0, 0, 0));
                        }
                        else
                        {
                            alpha.setPixel(x, y, img.getPixel(x, y));
                        }
                        //CPoint p = foregroundBoundary.get(s.fi);
                        //foreground.setPixel(x, y, img.getPixel(p.x, p.y));
                        break;
                    }
                    case 255:
                        //alpha.setPixel(x, y, Color.argb(255, 0, 0, 0));
                        alpha.setPixel(x, y, Color.argb(0, 0, 0, 0));
                        //foreground.setPixel(x, y, img.getPixel(x, y));
                        break;
                }
            }
        }

        Log.d("抠图", "图像生成完毕");

    }

    //检测三值图边界序列
    static private List<CPoint> findBoundaryPixels(final Bitmap img, final Bitmap trimap, final int a, final int b)
    {
        final List<CPoint> result = Collections.synchronizedList(new ArrayList<CPoint>());
        result.clear();

        final int w = trimap.getWidth();
        final int h = trimap.getHeight();
        final int heightCount = h / More.threadCount;

        int part;
        //可多
        for(part = 0; part < More.threadCount; part++)
        {
            final int partS = part;

            More.completionService.submit(
                    new Callable<Integer>()
                    {
                        @Override
                        public Integer call() throws Exception
                        {
                            for(int y = heightCount * partS; y < heightCount * (partS + 1); y++)
                            {
                                for(int x = 1; x < w - 1; x++)
                                {
                                    if((y != 0) && (y != h))
                                    {
                                        if (Color.alpha(trimap.getPixel(x, y)) == a)
                                        {
                                            if (Color.alpha(trimap.getPixel(x, y-1)) == b ||
                                                    Color.alpha(trimap.getPixel(x, y+1)) == b ||
                                                    Color.alpha(trimap.getPixel(x-1, y)) == b ||
                                                    Color.alpha(trimap.getPixel(x+1, y)) == b)
                                            {
                                                result.add(new CPoint(x, y, img.getPixel(x, y)));
                                                Log.d("边界", x + ", " + y + ", " + img.getPixel(x, y));
                                            }
                                        }
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

//        for (int x = 1; x < w - 1; ++x)
//            for (int y = 1; y < h - 1; ++y)
//            {
//                if (Color.alpha(trimap.getPixel(x, y)) == a)
//                {
//                    if (Color.alpha(trimap.getPixel(x, y-1)) == b ||
//                            Color.alpha(trimap.getPixel(x, y+1)) == b ||
//                            Color.alpha(trimap.getPixel(x-1, y)) == b ||
//                            Color.alpha(trimap.getPixel(x+1, y)) == b)
//                    {
//                        result.add(new CPoint(x, y, img.getPixel(x, y)));
//                        Log.d("边界", x + ", " + y);
//                    }
//                }
//            }

        return result;
    }

    //寻找最优样本对
    static private void calculateAlphaPatchMatch(final Bitmap img, final Bitmap trimap, final List<CPoint> fl, final List<CPoint> bl)
    {
        final int w = trimap.getWidth();
        final int h = trimap.getHeight();

        SampleList.Init(w, h);                  // 初始化 sample 链表
        final List<CPoint> lc = Collections.synchronizedList(new ArrayList<CPoint>());   // 包含所有未知像素的链表
        lc.clear();

        Log.d("抠图", "计算距离每一未知像素最近边界点");

        for(int y =0; y < h; y++)
        {
            for(int x = 0; x < w; x++)
            {
                if(Color.alpha(trimap.getPixel(x, y)) == 128)
                {
                    CPoint p = new CPoint(x, y, img.getPixel(x, y));
                    lc.add(p);

                    Sample s = SampleList.GetValue(x, y);

                    s.fi = (int)(Math.random() * 1000 % fl.size());
                    s.bj = (int)(Math.random() * 1000 % bl.size());
                    s.df = nearestDistance(fl, p);		// 边界中距离p最近的一点的距离
                    s.db = nearestDistance(bl, p);
                    s.cost = Float.MAX_VALUE;
                }
            }
        }

        // 迭代计算
        for (int iter = 0; iter < 2; iter++)
        {
            Log.d("抠图计算", "第" + Integer.toString(iter + 1) + "遍");
            // 打乱顺序
            Collections.shuffle(lc);

            for(int i = 0; i < lc.size(); i++)
            {
                CPoint p = lc.get(i);
                Sample s = SampleList.GetValue(p.x, p.y);

                for(int x2 = p.x - 1; x2 <= p.x + 1; x2++)
                {
                    for(int y2 = p.y - 1; y2 <= p.y + 1; y2++)
                    {
                        if ((x2 >= 0) && (x2 < w) && (y2 >= 0) && (y2 < h))
                        {
                            if(Color.alpha(trimap.getPixel(x2, y2)) == 128)
                            {
                                Sample s2 = SampleList.GetValue(x2, y2);

                                CPoint fp = fl.get(s2.fi);
                                CPoint bp = bl.get(s2.bj);

                                float alpha = calculateAlpha(fp, bp, p);
                                double cost = colorCost(fp, bp, p, alpha) + distCost(p, fp, s.df) + distCost(p, bp, s.db);

                                if (cost < s.cost)
                                {
                                    s.fi = s2.fi;
                                    s.bj = s2.bj;
                                    s.cost = cost;
                                    s.alpha = alpha;
                                }
                            }
                        }
                    }
                }
            }

            // random walk
            int w2 = Math.max(fl.size(), bl.size());

            for(int i = 0; i < lc.size(); i++)
            {
                CPoint p = lc.get(i);
                Sample s = SampleList.GetValue(p.x, p.y);

                for (int k = 0; ; k++)
                {
                    double r = w2 * Math.pow(0.5f, k);

                    if (r < 1)
                        break;

                    int di = (int)(r * (Math.random() * 1000 / (1000 + 1.f)));
                    int dj = (int)(r * (Math.random() * 1000 / (1000 + 1.f)));

                    int fi = s.fi + di;
                    int bj = s.bj + dj;

                    if (fi < 0 || fi >= fl.size() || bj < 0 || bj >= bl.size())
                        continue;

                    CPoint fp = fl.get(fi);
                    CPoint bp = bl.get(bj);

                    float alpha = calculateAlpha(fp, bp, p);
                    double cost = colorCost(fp, bp, p, alpha) + distCost(p, fp, s.df) + distCost(p, bp, s.db);

                    if (cost < s.cost)
                    {
                        s.fi = fi;
                        s.bj = bj;
                        s.cost = cost;
                        s.alpha = alpha;
                    }
                }
            }
        }
    }

    //计算边界链表 l 中距离 p 最近的一点的距离
    static private double nearestDistance(List<CPoint> l, CPoint p)
    {
        double minDist = Double.MAX_VALUE;

        for(int i = 0; i < l.size(); i++)
        {
            double tempDist = SpaceDist(p, l.get(i));
            if(tempDist < minDist)
            {
                minDist = tempDist;
            }
        }

        return minDist;
    }

    //计算两个argb值在RGB空间中的距离
    static private double colorDist(int p1, int p2)
    {
        int r = Color.red(p1) - Color.red(p2);
        int g = Color.green(p1) - Color.green(p2);
        int b = Color.blue(p1) - Color.blue(p2);

        double temp = Math.pow(r, 2) + Math.pow(g, 2) + Math.pow(b, 2);

        return Math.pow(temp, 0.5);
    }

    //计算两个像素的空间几何距离
    static private double SpaceDist(CPoint p1, CPoint p2)
    {
        int xt = p1.x - p2.x;
        int yt = p1.y - p2.y;

        double temp = Math.pow(xt, 2) + Math.pow(yt, 2);

        return Math.pow(temp, 0.5);
    }

    //计算α值
    static private float calculateAlpha(CPoint F, CPoint B, CPoint I)
    {
        float result = 0;
        float div = 1e-6f;
        for (int c = 0; c < 3; ++c)
        {
            float f = F.bgr[c];
            float b = B.bgr[c];
            float i = I.bgr[c];

            result += (i - b) * (f - b);
            div += (f - b) * (f - b);
        }

        return Math.min(Math.max(result / div, 0.f), 1.f);
    }

    //计算色彩成本
    static private double colorCost(CPoint F, CPoint B, CPoint I, float alpha)
    {
        double result = 0;
        for (int c = 0; c < 3; ++c)
        {
            float f = F.bgr[c];
            float b = B.bgr[c];
            float i = I.bgr[c];

            result += Math.pow((i - (alpha * f + (1 - alpha) * b)), 2);
        }

        return Math.pow(result, 0.5);
    }

    //计算空间成本
    static private double distCost(CPoint p1, CPoint p2, double minDist)
    {
        return SpaceDist(p1, p2) / minDist;
    }
}
