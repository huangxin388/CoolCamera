package com.example.fragment;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.coolcamera.R;

/**
 * Created by 竹轩听雨 on 2018/3/16.
 */

public class JisawFragment extends Fragment {

    private boolean isAnimRun = false;//判断动画是否在进行，动画进行时不接受动作
    private GridLayout GameLayout;//布局
    private ImageView[][] Game_arr = new ImageView[3][5];//存储方块的数组
    private ImageView nullImageView;//空方块
    private GestureDetector mDetector;//手势控制
    private boolean isGameStart = false;//判断游戏是否开始，防止在打乱过程中出现游戏结束的意外

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.jisaw_layout,null);

        GameLayout = view.findViewById(R.id.game_screen);
        Bitmap bigBm = BitmapFactory.decodeResource(getResources(), R.mipmap.smalllibai);//获取整个大图

        int picWidth = bigBm.getWidth()/5;//将大图切好后每个小图的高和宽
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        int WandH = dm.widthPixels/5;

        for(int i = 0;i < Game_arr.length;i++)
        {
            for(int j = 0;j < Game_arr[0].length;j++)
            {
                Bitmap bm = Bitmap.createBitmap(bigBm,j*picWidth,i*picWidth,picWidth,picWidth);//获取剪切后的小图
                Game_arr[i][j] = new ImageView(getActivity());//初始化数组
                Game_arr[i][j].setImageBitmap(bm);//将每个小图放入数组中
                Game_arr[i][j].setLayoutParams(new ConstraintLayout.LayoutParams(WandH,WandH));
                Game_arr[i][j].setPadding(2,2,2,2);//设置小图间的间距
                Game_arr[i][j].setTag(new GameData(i,j,bm));
                Game_arr[i][j].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean flag = isByNull((ImageView) v);
                        // Toast.makeText(MainActivity.this,"位置关系" + flag,Toast.LENGTH_SHORT).show();
                        if(flag) {
                            changeDataByImageView((ImageView) v);
                        }
                    }
                });
            }
        }

        for(int i = 0;i < Game_arr.length;i++)
        {
            for(int j = 0;j < Game_arr[0].length;j++)
            {
                GameLayout.addView(Game_arr[i][j]);
            }
        }

        setNullImageVeiw(Game_arr[2][4]);

        randomMove();
        isGameStart = true;//游戏开始
        return view;
    }

    public void randomMove() {
        for(int i = 0;i < 100;i++) {
            int type = (int)(Math.random()*4) + 1;
            changeByDir(type,false);
        }
    }

    public void setNullImageVeiw(ImageView mImageView) {//设置空位置
        mImageView.setImageBitmap(null);
        nullImageView = mImageView;

    }

    public void changeByDir(int type) {
        changeByDir(type,true);
    }

    public void changeByDir(int type,boolean hasAnim) {
        GameData mNullData = (GameData) nullImageView.getTag();
        int new_x = mNullData.x;
        int new_y = mNullData.y;
        if(type == 1) {//空方块向下移动
            new_x++;
        } else if(type == 2) {
            new_x--;
        } else if(type == 3) {
            new_y++;
        } else if(type == 4) {
            new_y--;
        }

        if(new_x >= 0 && new_x < Game_arr.length && new_y >= 0 && new_y < Game_arr[0].length) {
            //如果位置合理
            changeDataByImageView(Game_arr[new_x][new_y],hasAnim);
        } else {
            //什么也不做
        }
    }

    public int TellDirByGes(float start_x,float start_y,float end_x,float end_y) {
        boolean leftOrRight = (Math.abs(end_x-start_x) > Math.abs(end_y-start_y)) ? true : false;
        if(leftOrRight) {//左右滑
            boolean isLeft = (start_x - end_x) > 0 ? true : false;
            if(isLeft) {
                return 3;
            }else {
                return 4;
            }
        }else {//上下滑动
            boolean isTop = start_y - end_y > 0 ? true : false;
            if(isTop) {
                return 1;
            }else {
                return 2;
            }
        }
    }

    public boolean isByNull(ImageView mImageView) {
        GameData mNullData = (GameData) nullImageView.getTag();
        GameData mData = (GameData) mImageView.getTag();
        if(mNullData.x == mData.x+1 && mData.y == mNullData.y) {//所点击方块在空方块的上边
            return true;
        }else if(mNullData.x == mData.x-1 && mData.y == mNullData.y) {//下边
            return true;
        }else if(mNullData.y == mData.y+1 && mData.x == mNullData.x) {//左边
            return true;
        }else if(mNullData.y == mData.y-1 && mData.x == mNullData.x) {//右边
            return true;
        }
        return false;

    }

    public void changeDataByImageView(final ImageView mImageView) {
        changeDataByImageView(mImageView,true);
    }

    public void changeDataByImageView(final ImageView mImageView, boolean hasAnim) {//设置动画移动位置
        if(isAnimRun) {
            return;
        }
        if(!hasAnim) {
            GameData mGameData = (GameData)mImageView.getTag();
            nullImageView.setImageBitmap(mGameData.bm);
            GameData mNullData = (GameData)nullImageView.getTag();
            mNullData.p_x = mGameData.p_x;
            mNullData.p_y = mGameData.p_y;
            mNullData.bm = mGameData.bm;
            setNullImageVeiw(mImageView);
            if(isGameStart) {
                isGameOver();
            }
            return;
        }
        TranslateAnimation translateAnimation = null;
        if(mImageView.getX() > nullImageView.getX()) {
            //向上移
            translateAnimation = new TranslateAnimation(0.1f,-mImageView.getWidth(),0.1f,0.1f);
        }else if(mImageView.getX() < nullImageView.getX()) {
            //向下移
            translateAnimation = new TranslateAnimation(0.1f,mImageView.getWidth(),0.1f,0.1f);
        }else if(mImageView.getY() < nullImageView.getY()) {
            //向右移
            translateAnimation = new TranslateAnimation(0.1f,0.1f,0.1f,mImageView.getWidth());
        }else if(mImageView.getY() > nullImageView.getY()) {
            //向左移
            translateAnimation = new TranslateAnimation(0.1f,0.1f,0.1f,-mImageView.getWidth());
        }

        translateAnimation.setDuration(70);//设置动画时长
        translateAnimation.setFillAfter(true);//设置动画结束后是否停留
        translateAnimation.setAnimationListener(new Animation.AnimationListener() {//设置动画监听
            @Override
            public void onAnimationStart(Animation animation) {
                isAnimRun = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isAnimRun = false;
                mImageView.clearAnimation();
                GameData mGameData = (GameData)mImageView.getTag();
                nullImageView.setImageBitmap(mGameData.bm);
                GameData mNullData = (GameData)nullImageView.getTag();
                mNullData.p_x = mGameData.p_x;
                mNullData.p_y = mGameData.p_y;
                mNullData.bm = mGameData.bm;
                setNullImageVeiw(mImageView);
                if(isGameStart) {
                    isGameOver();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        mImageView.startAnimation(translateAnimation);//开始动画
    }

    public void isGameOver() {
        boolean isGameOer = true;
        for(int i = 0;i < Game_arr.length;i++) {
            for(int j = 0;j < Game_arr[0].length;j++) {
                if(Game_arr[i][j] == nullImageView) {
                    continue;
                }
                GameData mGameData = (GameData) Game_arr[i][j].getTag();
                if(!mGameData.isTrue()) {
                    isGameOer = false;
                    break;
                }
            }
        }
        if(isGameOer) {
            Toast.makeText(getActivity(),"GameOver", Toast.LENGTH_SHORT).show();
        }
    }


    class GameData
    {
        public int x = 0;
        public int y = 0;
        public int p_x = 0;
        public int p_y = 0;
        public Bitmap bm;

        public GameData(int x, int y, Bitmap bm) {
            this.x = x;
            this.y = y;
            this.p_x = x;
            this.p_y = y;
            this.bm = bm;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public Bitmap getBm() {
            return bm;
        }

        public boolean isTrue() {
            if(x == p_x && y == p_y) {
                return true;
            } else {
                return false;
            }
        }
    }
}
