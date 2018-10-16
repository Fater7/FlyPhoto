package com.fater.gg;

import android.graphics.Color;

/**
 * Created on 2018-05-16.
 */

public class CPoint implements Comparable<CPoint>
{
    int x;
    int y;
    int pixel;
    int[] bgr = new int[3];

    CPoint(int xt, int yt, int p)
    {
        x = xt;
        y = yt;
        pixel = p;
        bgr[0] = Color.blue(p);
        bgr[1] = Color.green(p);
        bgr[2] = Color.red(p);
    }

    @Override
    public int compareTo(CPoint c)
    {

            int t1 = bgr[0] + bgr[1] + bgr[2];
            int t2 = c.bgr[0] + c.bgr[1] + c.bgr[2];
            return (t2 - t1);

    }
}
