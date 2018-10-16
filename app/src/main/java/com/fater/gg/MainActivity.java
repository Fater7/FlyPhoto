package com.fater.gg;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.theartofdev.edmodo.cropper.CropImage;

public class MainActivity extends AppCompatActivity
{
    private static final int REQUEST_IMAGE_GET = 1;
    private static final int TAKE_PHOTO = 2;    //取照片

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //获取存储权限
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            //验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                    return;
                }
            }
        }
    }

    //从相册中选择照片
    public void openWithAlbum(View v)
    {
        //初始化图像操作类
        ImageProcess.Init();

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
            ImageProcess.imageUri = data.getData();

            // start cropping activity for pre-acquired image saved on the device
            CropImage.activity(ImageProcess.imageUri).start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK)
            {
                ImageProcess.imageUri = result.getUri();

                //打开操作页面
                Intent i = new Intent(this, ProcessActivity.class);
                startActivity(i);
            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE)
            {
                Exception error = result.getError();
            }
        }

        if (requestCode == TAKE_PHOTO)
        {
            //拍照取图
            Bundle bundle = data.getExtras();                   //获取data数据集合
            ImageProcess.fgImage = (Bitmap) bundle.get("data");        //获得data数据
            ImageProcess.iscamera = true;

            //打开操作页面
            Intent i = new Intent(this, ProcessActivity.class);
            startActivity(i);
        }
    }

    //从照相机拍照
    public void openWithCamera(View v)
    {
        Intent mIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(mIntent, TAKE_PHOTO);
    }

}
