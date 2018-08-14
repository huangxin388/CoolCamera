package com.example.coolcamera;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.example.bean.FolderBean;
import com.example.utils.ImageLoader;

import java.util.List;

/**
 * Created by 竹轩听雨 on 2018/3/30.
 */

public class ListImagePopupWindow extends PopupWindow {

    private int mWidth;
    private int mHeight;
    private View mContentView;
    private ListView mListView;
    private List<FolderBean> mDatas;

    public interface OnDirSelectedListener {
        void onSelected(FolderBean folderBean);
    }

    public OnDirSelectedListener mListener;

    public void setOnDirSelectedListener(OnDirSelectedListener mListener) {
        this.mListener = mListener;
    }

    public ListImagePopupWindow(Context context, List<FolderBean> datas) {
        calWidthAndHeight(context);

        mContentView = LayoutInflater.from(context).inflate(R.layout.popup_window_layout,null);
        mDatas = datas;

        setContentView(mContentView);
        setWidth(mWidth);
        setHeight(mHeight);

        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());

        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        initViews(context);
        initEvents();
    }

    private void initViews(Context context) {
        mListView = mContentView.findViewById(R.id.pop_window_list_view);
        mListView.setAdapter(new ListDirAdapter(context,mDatas));
    }

    private void initEvents() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(mListener != null) {
                    mListener.onSelected(mDatas.get(i));
                }
            }
        });



    }

    private void calWidthAndHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(outMetrics);

        mWidth = outMetrics.widthPixels;
        mHeight = (int)(outMetrics.heightPixels * 0.7f);
    }

    private class ListDirAdapter extends ArrayAdapter<FolderBean> {

        private LayoutInflater mInflater;
        private List<FolderBean> mDatas;

        public ListDirAdapter(@NonNull Context context, List<FolderBean> objects) {
            super(context, 0,objects);

            mInflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder = null;
            if(convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.pop_window_list_item,parent,false);
                holder.mImage = (ImageView) convertView.findViewById(R.id.pop_item_image);
                holder.mDirName = convertView.findViewById(R.id.id_dir_item_name);
                holder.mDirCount = convertView.findViewById(R.id.id_dir_item_count);
                convertView.setTag(holder);
            }
            holder = (ViewHolder) convertView.getTag();

            FolderBean bean = getItem(position);
            //重置
            holder.mImage.setImageResource(R.mipmap.load96);
            ImageLoader.getInstance(3, ImageLoader.Type.LIFO).loadImage(bean.getFirstImgPath(),holder.mImage);

            holder.mDirName.setText(bean.getName());
            holder.mDirCount.setText(bean.getCount() + "");
            return convertView;
        }


        private class ViewHolder {
            ImageView mImage;
            TextView mDirName;
            TextView mDirCount;
        }
    }


}
