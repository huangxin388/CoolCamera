package com.example.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.coolcamera.R;

import at.technikum.mti.fancycoverflow.FancyCoverFlow;
import at.technikum.mti.fancycoverflow.FancyCoverFlowAdapter;

/**
 * Created by 竹轩听雨 on 2018/3/21.
 */

public class FancyCoverFlowSimpleAdapter extends FancyCoverFlowAdapter {

    private int[] images = {R.mipmap.jisaw, R.mipmap.libai, R.mipmap.pintu, R.mipmap.scen};


    @Override
    public int getCount() {
        return images.length;
    }

    @Override
    public Integer getItem(int i) {
        return images[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }
    @Override
    public View getCoverFlowItem(int position, View reusableView, ViewGroup parent) {
        ImageView imageView = null;

        if (reusableView != null) {
            imageView = (ImageView) reusableView;
        } else {
            imageView = new ImageView(parent.getContext());
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageView.setLayoutParams(new FancyCoverFlow.LayoutParams(300, 400));

        }

        imageView.setImageResource(this.getItem(position));
        return imageView;
    }
}
