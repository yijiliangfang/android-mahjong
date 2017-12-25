package com.main.mahjong.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.URL;

/**
 * Created by admin on 2017/11/27.
 */

public class GetNetImageTask {
    ImageView imageView;
    Bitmap bitmap=null;
    String strurl="";

    public GetNetImageTask(ImageView imageView,String strurl) {

        this.imageView = imageView;
        this.strurl=strurl;
    }

    protected Bitmap doInBackground() {

        Runnable networkImg = new Runnable() {
            @Override
            public void run() {
                try {
                    URL conn = new URL(strurl);
                    InputStream in = conn.openConnection().getInputStream();
                    bitmap = BitmapFactory.decodeStream(in);
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(networkImg).start();
        while(bitmap == null)
            continue;

        return bitmap;
    }

    public void execute() {

        imageView.setImageBitmap(doInBackground());
    }
}
