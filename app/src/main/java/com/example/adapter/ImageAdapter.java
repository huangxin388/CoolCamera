package com.example.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;


import com.example.coolcamera.R;
import com.example.utils.ImageLoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by 竹轩听雨 on 2018/3/30.
 */

public class ImageAdapter extends BaseAdapter {

    private static Set<String> mSelectedImage = new HashSet<>();

    private String mDirPath;
    private List<String> mImagePath;
    private LayoutInflater mInflater;
    private Context mContext;

    public String returnPath;

    public ImageAdapter(Context context, List<String> mDatas, String dirPath) {
        this.mDirPath = dirPath;
        this.mImagePath = mDatas;
        mInflater = LayoutInflater.from(context);
        mContext = context;
    }

    @Override
    public int getCount() {
        return mImagePath.size();
    }

    @Override
    public Object getItem(int i) {
        return mImagePath.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        final ViewHolder holder;
        if (view == null) {
            view = mInflater.inflate(R.layout.item_gridview, null);
            holder = new ViewHolder();
            holder.mImage = view.findViewById(R.id.id_item_image);
            holder.mSelect = view.findViewById(R.id.id_item_select);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        //重置状态
        holder.mImage.setImageResource(R.mipmap.load96);
        holder.mSelect.setImageResource(R.mipmap.unselect);
        holder.mImage.setColorFilter(null);

        ImageLoader.getInstance(3, ImageLoader.Type.LIFO).loadImage(mDirPath + "/" + mImagePath.get(i), holder.mImage);
        final String filePath = mDirPath + "/" + mImagePath.get(i);
        holder.mImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(mSelectedImage.contains(filePath)) {
                    //图片已经被选择
                    mSelectedImage.remove(filePath);
                    holder.mImage.setColorFilter(null);
                    holder.mSelect.setImageResource(R.mipmap.unselect);
                    returnPath = "";
                } else {//图片尚未被选择
                    returnPath = filePath;
                    mSelectedImage.add(filePath);
                    holder.mImage.setColorFilter(Color.parseColor("#77000000"));
                    holder.mSelect.setImageResource(R.mipmap.selected);
                }
               // notifyDataSetChanged();
            }
        });

        if(mSelectedImage.contains(filePath)) {
            holder.mImage.setColorFilter(Color.parseColor("#77000000"));
            holder.mSelect.setImageResource(R.mipmap.selected);
        }

        return view;
    }

    private class ViewHolder {
        ImageView mImage;
        ImageButton mSelect;
    }
}
