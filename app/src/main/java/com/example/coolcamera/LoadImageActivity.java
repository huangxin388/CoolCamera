package com.example.coolcamera;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.adapter.ImageAdapter;
import com.example.bean.FolderBean;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LoadImageActivity extends AppCompatActivity {

    private GridView mGridView;
    private ImageAdapter imageAdapter;
    private RelativeLayout mBottomLy;
    private ImageView mReturnView;
    private Button mCompleteBtn;
    private TextView mDirName;
    private TextView mDirCount;

    private List<String> mImages;
    private List<FolderBean> mFolderBerans = new ArrayList<>();
    private File mCurrentDir;
    private int mMaxCount;

    private ListImagePopupWindow listImagePopupWindow;

    private ProgressDialog mProgressDialog;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == 0x110) {
                mProgressDialog.dismiss();
                //绑定数据到view中
                data2View();
                
                initImagePopupWindow();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_image);

        if(ContextCompat.checkSelfPermission(LoadImageActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(LoadImageActivity.this,new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},1);
        } else {
            initView();
            initDatas();
            initEvents();
        }
    }

    private void initImagePopupWindow() {
        listImagePopupWindow = new ListImagePopupWindow(this,mFolderBerans);
        listImagePopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                lightOn();
            }
        });

        listImagePopupWindow.setOnDirSelectedListener(new ListImagePopupWindow.OnDirSelectedListener() {
            @Override
            public void onSelected(FolderBean folderBean) {
                mCurrentDir = new File(folderBean.getDir());

                mImages = Arrays.asList(mCurrentDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String s) {
                        if(s.endsWith(".jpg") || s.endsWith(".jpeg") || s.endsWith(".png"))
                            return true;
                        return false;
                    }
                }));

                imageAdapter = new ImageAdapter(LoadImageActivity.this,
                        mImages,mCurrentDir.getAbsolutePath());

                mGridView.setAdapter(imageAdapter);

                mDirCount.setText(mImages.size() + "");
                mDirName.setText(folderBean.getName());

                listImagePopupWindow.dismiss();
            }
        });
    }

    /**
     * 内容区域变亮
     */
    private void lightOn() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 1.0f;
        getWindow().setAttributes(lp);
    }

    private void data2View() {
        if(mCurrentDir == null) {
            Toast.makeText(LoadImageActivity.this,"未扫描到任何图片", Toast.LENGTH_SHORT).show();
            return;
        }
        mImages = Arrays.asList(mCurrentDir.list());
        imageAdapter = new ImageAdapter(this,mImages,mCurrentDir.getAbsolutePath());
        mGridView.setAdapter(imageAdapter);

        mDirCount.setText(mMaxCount + "");
        mDirName.setText(mCurrentDir.getName());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initView();
                    initDatas();
                    initEvents();
                } else {
                    Toast.makeText(LoadImageActivity.this,"You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    private void initEvents() {

        mBottomLy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //listImagePopupWindow.setAnimationStyle();
                listImagePopupWindow.showAsDropDown(mBottomLy,0,0);
                lightOff();
            }
        });

        mCompleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.putExtra("path",imageAdapter.returnPath);
                setResult(RESULT_OK,intent);
                finish();
            }
        });

        mReturnView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    /**
     * 内容区域变黑
     */
    private void lightOff() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.3f;
        getWindow().setAttributes(lp);
    }

    /**
     * 利用ContentProvider扫描手机中的图片
     */
    private void initDatas() {
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this,"当前存储卡不可用", Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressDialog = ProgressDialog.show(this,null,"正在加载...");
        new Thread() {
            @Override
            public void run() {
                Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = LoadImageActivity.this.getContentResolver();

                Cursor cursor = cr.query(mImageUri,
                        null,
                        MediaStore.Images.Media.MIME_TYPE + "= ? or " + MediaStore.Images.Media.MIME_TYPE + "= ?",
                        new String[]{"image/jpeg","image/png"},
                        MediaStore.Images.Media.DATE_MODIFIED);

                Set<String> mDirPaths = new HashSet<>();

                        while(cursor.moveToNext()) {
                            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                            File parentFile = new File(path).getParentFile();
                            if(parentFile == null)
                                continue;

                            String dirPath = parentFile.getAbsolutePath();

                            FolderBean folderBean = null;

                            if(mDirPaths.contains(dirPath)) {
                                continue;
                            } else {
                                mDirPaths.add(dirPath);
                                folderBean = new FolderBean();
                                folderBean.setDir(dirPath);
                                folderBean.setFirstImgPath(path);
                            }

                            if(parentFile.list() == null) {
                                continue;
                            }
                            int picSize = parentFile.list(new FilenameFilter() {
                                @Override
                                public boolean accept(File file, String s) {
                                    if(s.endsWith(".jpg") || s.endsWith(".jpeg") || s.endsWith(".png"))
                                        return true;
                                    return false;
                                }
                            }).length;

                            folderBean.setCount(picSize);
                            mFolderBerans.add(folderBean);

                            if(picSize > mMaxCount) {
                                mMaxCount = picSize;
                                mCurrentDir = parentFile;
                            }
                        }
                cursor.close();
                        //通知handler扫描图片完成
                handler.sendEmptyMessage(0x110);

            };
        }.start();
    }

    private void initView() {
        mGridView = findViewById(R.id.id_grid_view);
        mBottomLy = findViewById(R.id.id_bottom_ly);
        mDirName = findViewById(R.id.id_dir_name);
        mDirCount = findViewById(R.id.id_dir_count);
        mReturnView = findViewById(R.id.id_return);
        mCompleteBtn = findViewById(R.id.id_btn_complete);
    }




}
