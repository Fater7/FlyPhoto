package com.fater.gg;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ProcessActivity extends AppCompatActivity
{
    //ImageView的尺寸
    int imageViewWidth;
    int imageViewHeight;
    int bNum = 0;
    private ImageView bv;
    private MyImageView iv;
    float xMax, xMin, yMax, yMin, a, b;
    Bitmap trimap;

    private static final int REQUEST_IMAGE_GET = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process);

        iv = (MyImageView)findViewById(R.id.image);
        bv = findViewById(R.id.bgimg);
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);

        //记录ImageView的大小用于缩放
        imageViewWidth = bv.getWidth();
        imageViewHeight = bv.getHeight();

        if(!ImageProcess.fgdone)
        {
            if(ImageProcess.iscamera)
            {
                iv.setImageBitmap(ImageProcess.fgImage);
            }
            else
            {
                ImageProcess.fgImage = getBitmapFromUri(ImageProcess.imageUri);
                iv.setImageBitmap(ImageProcess.fgImage);
            }
        }
    }

    //调整图像大小
    private Bitmap getBitmapFromUri(Uri uri)
    {
        try
        {
            BitmapFactory.Options options = new BitmapFactory.Options();

            //只读取图像大小
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, options);
            int picWidth = options.outWidth;
            int picHeight = options.outHeight;

            //调整缩放比例
            options.inSampleSize = 1;
            if (picWidth > picHeight)
            {
                if (picWidth > imageViewWidth)
                    options.inSampleSize = picWidth / imageViewWidth;
            }
            else
            {
                if (picHeight > imageViewHeight)
                    options.inSampleSize = picHeight / imageViewHeight;
            }
            options.inJustDecodeBounds = false;

            //载入缩放后的图像
            return BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, options);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    //量化图像并计算显著性值,生成显著图
    public void modify1(View v)
    {
        if((ImageProcess.fgImage != null) && (!ImageProcess.quantdone))
        {
            ImageProcess.Init();

            ImageProcess.ReduceImageColor(ImageProcess.fgImage);

            String s = "高频色:" + Integer.toString(ImageProcess.colorSa.size()) +
                    " 低频色:" + Integer.toString(ImageProcess.colorCh.size());

            Log.d("量化完毕", s);

            //计算各像素的显著性值
            ImageProcess.SetSaliencyValue();

            //调试输出显著性值
            int num = 3;
            int part = ImageProcess.colorSa.size() / num;
            for(int i = 0; i <= part; i++)
            {
                String ss = "";
                for(int j = 0; (j < num) && ((num * i + j) < ImageProcess.colorSa.size()); j++)
                {
                    int index = num * i + j;
                    ColorInfo tempC = ImageProcess.colorSa.valueAt(index);
                    ss += ("    No." + Integer.toString(index) +
                            " argb: " + Integer.toString(tempC.pixelColor) +
                            " sv: " + Double.toString(tempC.saliencyValueS));
                }
                Log.d("显著性值", ss);
            }

            //设置显著阈值
            ImageProcess.balance = (int)Math.round(ImageProcess.colorSa.valueAt(1).saliencyValueS);
            ((TextView) findViewById(R.id.text)).setText(Integer.toString(ImageProcess.balance));

            ImageProcess.quantdone = true;
        }

        if((ImageProcess.colorSa.size() != 0) && (ImageProcess.fgImage != null))
        {
            trimap = ImageProcess.GetBinaryImage(ImageProcess.tempImage);
            ImageProcess.GetTrimap(trimap);
            iv.setImageBitmap(trimap);
        }
    }

    //降低显著阈值
    public void deBalance(View v)
    {
        ImageProcess.balance--;
        ((TextView) findViewById(R.id.text)).setText(Integer.toString(ImageProcess.balance));
    }

    //增加显著阈值
    public void inBalance(View v)
    {
        ImageProcess.balance++;
        ((TextView) findViewById(R.id.text)).setText(Integer.toString(ImageProcess.balance));
    }

    //添加背景图像
    public void addbg(View v)
    {
        //读取图像
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null)
        {
            startActivityForResult(intent, REQUEST_IMAGE_GET);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        //获取图片路径
        if (requestCode == REQUEST_IMAGE_GET && resultCode == RESULT_OK)
        {
            Uri bguri = data.getData();

            ImageProcess.bgImage = getBitmapFromUri(bguri);
            bv.setImageBitmap(ImageProcess.bgImage);
        }
    }

    public void matting(View v)
    {


//        Matting.expansionOfKnownRegions(img, trimap, 9);
//        iv.setImageBitmap(trimap);

        Log.d("aaaaa", "开始抠了");
        Matting.globalMatting(ImageProcess.fgImage, trimap);

        Log.d("aaaaa", "开始出图了");
        iv.setImageBitmap(Matting.alpha);
        ImageProcess.fgdone = true;
    }

    //得到灰度图
    public void modify2(View v)
    {
        if(Matting.alpha != null)
        {
            iv.setImageBitmap(ImageProcess.SetGrayscale(Matting.alpha));
        }
        if(ImageProcess.bgImage != null)
        {
            bv.setImageBitmap(ImageProcess.SetGrayscale(ImageProcess.bgImage));
        }
    }

    //泛黄滤镜
    public void modify3(View v)
    {
        if(Matting.alpha != null)
        {
            iv.setImageBitmap(ImageProcess.SetOlder(Matting.alpha));
        }
        if(ImageProcess.bgImage != null)
        {
            bv.setImageBitmap(ImageProcess.SetOlder(ImageProcess.bgImage));
        }
    }

    //保存图像
    public void savePic(View v)
    {
        //截取整个屏幕，然后去掉上下部只留下图片
        //获取window最底层的view
        View view=this.getWindow().getDecorView();
        view.buildDrawingCache();

        //状态栏高度
        Rect rect=new Rect();
        view.getWindowVisibleDisplayFrame(rect);
        int stateBarHeight=rect.top;
        Display display=this.getWindowManager().getDefaultDisplay();

        //获取屏幕宽高
        int widths=display.getWidth();
        int height=display.getHeight();

        //设置允许当前窗口保存缓存信息
        view.setDrawingCacheEnabled(true);

        //去掉状态栏高度
        Bitmap bitmap=Bitmap.createBitmap(view.getDrawingCache(),0,stateBarHeight,widths,height-stateBarHeight);
        view.destroyDrawingCache();

        //截取
        if(ImageProcess.bgImage.getWidth() > ImageProcess.bgImage.getHeight())
        {
            int th = imageViewWidth * ImageProcess.bgImage.getHeight() / ImageProcess.bgImage.getWidth();
            int cutHeight = (imageViewHeight - th) / 2;
            bitmap = Bitmap.createBitmap(bitmap, 0, cutHeight, imageViewWidth, th);
        }
        else
        {
            int tw = imageViewHeight * ImageProcess.bgImage.getWidth() / ImageProcess.bgImage.getHeight();
            int cutWeight = (imageViewWidth - tw) / 2;
            bitmap = Bitmap.createBitmap(bitmap, cutWeight, 0, tw, imageViewHeight);
        }

        saveBitmap(bitmap, this);
    }

    static void saveBitmap(Bitmap bmp, Context context)
    {
        // 首先保存图片
        File appDir = new File(Environment.getExternalStorageDirectory(), "flyPhoto");
        if (!appDir.exists())
        {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        Log.d("保存图像", file.toString());
        try
        {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        // 其次把文件插入到系统图库
        try
        {
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    file.getAbsolutePath(), fileName, null);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        // 最后通知图库更新
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
        Log.d("保存图像", "保存成功");
        Toast.makeText(context, "图片保存成功!",   Toast.LENGTH_SHORT).show();
    }
}
