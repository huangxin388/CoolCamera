package com.example.fragment;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.example.coolcamera.R;
import com.example.utils.ImageHelper;
import com.example.utils.ImageLoader;

/**
 * Created by 竹轩听雨 on 2018/3/18.
 */

public class ColorChangeFragment extends Fragment implements SeekBar.OnSeekBarChangeListener{

    public static int MAX_VALUE = 255;
    public static int MID_VALUE = 127;

    private SeekBar hueSeekBar;
    private SeekBar saturationSeekBar;
    private SeekBar lumSeekBar;
    private ImageView mImageView;

    private Bitmap bitmap;

    private float mHue;
    private float mSaturation;
    private float mLum;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.colorchange_layout,null);
        hueSeekBar = view.findViewById(R.id.seek_bar_hue);
        saturationSeekBar = view.findViewById(R.id.seek_bar_saturation);
        lumSeekBar = view.findViewById(R.id.seek_bar_lum);
        mImageView = view.findViewById(R.id.image_view_color_photo);

        initSeekBar();
        bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.scen);

        mImageView.setImageBitmap(bitmap);
        return view;
    }

    public void initSeekBar()
    {

        hueSeekBar.setMax(MAX_VALUE);
        saturationSeekBar.setMax(MAX_VALUE);
        lumSeekBar.setMax(MAX_VALUE);

        hueSeekBar.setProgress(MID_VALUE);
        saturationSeekBar.setProgress(MID_VALUE);
        lumSeekBar.setProgress(MID_VALUE);

        hueSeekBar.setOnSeekBarChangeListener(this);
        saturationSeekBar.setOnSeekBarChangeListener(this);
        lumSeekBar.setOnSeekBarChangeListener(this);

        mHue = 0;
        mSaturation = 1;
        mLum = 1;


    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        switch (seekBar.getId())
        {
            case R.id.seek_bar_hue:
                mHue = (i - MID_VALUE) * 1.0F / MID_VALUE * 180;
                break;
            case R.id.seek_bar_saturation:
                mSaturation = i * 1.0F / MID_VALUE;
                break;
            case R.id.seek_bar_lum:
                mLum = i * 1.0F / MID_VALUE;
                break;
        }
        Bitmap bm = ImageHelper.ColorChange(bitmap,mHue,mSaturation,mLum);
        mImageView.setImageBitmap(bm);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
