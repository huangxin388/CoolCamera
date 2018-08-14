package com.example.fragment;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

import com.example.adapter.MyExpandableListAdapter;
import com.example.bean.ExpandableBean;
import com.example.constant.Constant;
import com.example.coolcamera.LoadImageActivity;
import com.example.coolcamera.R;
import com.example.utils.FucUtil;
import com.example.utils.ImageHelper;
import com.example.utils.JsonParser;
import com.example.utils.XmlParser;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.util.ContactManager;
import com.iflytek.cloud.util.ResourceUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import tyrantgit.explosionfield.ExplosionField;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

public class VariationFragment extends Fragment{
    private ExplosionField explosionField;

    private ImageView mImageView;
    private MyExpandableListAdapter adapter;
    private ExpandableListView expandableListView;
    private ArrayList<String> parentList;
    private ArrayList<ExpandableBean> childList;
    private ExpandableBean bean;
    private Map<String,ArrayList<ExpandableBean>> map;
    private Bitmap loadedBitmap;

    private EditText mHorizontalEt;
    private EditText mVerticalEt;
    private EditText mRotationEt;
    private Button mChooseImageBtn;
    private RadioButton mHorizontalBtn;
    private RadioButton mVerticalBtn;


    private float translationDistanceX = 0;
    private float translationDistanceY = 0;
    private float rotationAngles = 0;
    private float scaleX = 0;
    private float scaleY = 0;
    private float firstX = 0;
    private float firstY = 0;
    private float secondX = 255;
    private float secondY = 255;
    private int n;//用于记录n*n均值滤波法输入值
    private int T;//用于记录超限邻域平均法输入值
    private int na;//用记录于n*n中值滤波法输入值
    private int nc;//用于记录十字型中值滤波输入值
    private int nm;//用于记录n*n最大值滤波输入值


    //一下为语音功能用到的

    private static String TAG = "Variation";
    // 语音识别对象
    private SpeechRecognizer mAsr;
    private Toast mToast;
    // 缓存
    private SharedPreferences mSharedPreferences;
    // 本地语法文件
    private String mLocalGrammar = null;
    // 本地词典
    private String mLocalLexicon = null;
    // 云端语法文件
    private String mCloudGrammar = null;
    // 本地语法构建路径
    private String grmPath = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/msc/test";
    // 返回结果格式，支持：xml,json
    private String mResultType = "json";

    private  final String KEY_GRAMMAR_ABNF_ID = "grammar_abnf_id";
    private  final String GRAMMAR_TYPE_ABNF = "abnf";
    private  final String GRAMMAR_TYPE_BNF = "bnf";

    private String mEngineType = "cloud";
    @SuppressLint("ShowToast")
    private String mContent;// 语法、词典临时变量
    private int ret = 0;// 函数调用返回值






    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.variation_layout,null);
        mImageView = view.findViewById(R.id.iv_variation_wrapper);
        expandableListView = view.findViewById(R.id.exlv_variation);
        mChooseImageBtn = view.findViewById(R.id.btn_variatoin_choose_iamge);
        initData();
        adapter = new MyExpandableListAdapter(parentList,map,VariationFragment.this.getContext());
        expandableListView.setAdapter(adapter);
        loadedBitmap = BitmapFactory.decodeResource(getResources(),R.mipmap.computer);
        mImageView.setImageBitmap(loadedBitmap);

        //语音

        requestPermissions();

        // 初始化识别对象
        mAsr = SpeechRecognizer.createRecognizer(getContext(), mInitListener);

        // 初始化语法、命令词
        mLocalLexicon = "张海羊\n刘婧\n王锋\n";
        mLocalGrammar = FucUtil.readFile(getContext(),"call.bnf", "utf-8");
        mCloudGrammar = FucUtil.readFile(getContext(),"grammar_sample.abnf","utf-8");

        mSharedPreferences = getActivity().getSharedPreferences(getActivity().getPackageName(),	MODE_PRIVATE);
        mToast = Toast.makeText(getContext(),"",Toast.LENGTH_SHORT);
        view.findViewById(R.id.isr_recognize).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(getContext(),"上传预设关键词/语法文件",Toast.LENGTH_SHORT).show();
                // 本地-构建语法文件，生成语法id

                //((EditText)findViewById(R.id.isr_text)).setText(mLocalGrammar);
                mEngineType =  SpeechConstant.TYPE_LOCAL;

                mContent = new String(mLocalGrammar);
                mAsr.setParameter(SpeechConstant.PARAMS, null);
                // 设置文本编码格式
                mAsr.setParameter(SpeechConstant.TEXT_ENCODING,"utf-8");
                // 设置引擎类型
                mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
                // 设置语法构建路径
                mAsr.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
                //使用8k音频的时候请解开注释
//					mAsr.setParameter(SpeechConstant.SAMPLE_RATE, "8000");
                // 设置资源路径
                mAsr.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
                ret = mAsr.buildGrammar(GRAMMAR_TYPE_BNF, mContent, grammarListener);
                if(ret != ErrorCode.SUCCESS){
                    Toast.makeText(getContext(),"语法构建失败,错误码：" + ret,Toast.LENGTH_SHORT).show();
                }

                // ((EditText)findViewById(R.id.isr_text)).setText(null);// 清空显示内容
                // 设置参数
                if (!setParam()) {
                    Toast.makeText(getContext(),"请先构建语法。",Toast.LENGTH_SHORT).show();
                    return;
                };

                ret = mAsr.startListening(mRecognizerListener);
                if (ret != ErrorCode.SUCCESS) {
                    Toast.makeText(getContext(),"识别失败,错误码: " + ret,Toast.LENGTH_SHORT).show();
                }

            }
        });



        initEvent();

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                if(resultCode == RESULT_OK) {
                    String loadImagePath = data.getStringExtra("path");
                    if("".equals(loadImagePath)) {
                        loadedBitmap = BitmapFactory.decodeResource(getResources(),R.mipmap.computer);
                    } else {
                        //ImageLoader.getInstance(3, ImageLoader.Type.LIFO).loadImage(loadImagePath, mImageView);
                        /*下面这种方式可以加载原图，但是尺寸大了之后影响几何变换，下一个实验再用这种方法*/
                        loadedBitmap = BitmapFactory.decodeFile(loadImagePath);
                    }
                    mImageView.setImageBitmap(loadedBitmap);
                }
        }
    }

    private void initEvent() {
        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i1, long l) {
                //mImageView.setDrawingCacheEnabled(true);
                //loadedBitmap = ((BitmapDrawable)((ImageView)mImageView).getDrawable()).getBitmap();
                //mImageView.setDrawingCacheEnabled(false);
                switch (map.get(parentList.get(i)).get(i1).getId()) {
                    case 11://平移
                        translation(mImageView);
                        break;
                    case 12://旋转
                        rotation(mImageView);
                        break;
                    case 13://缩放
                        scale(mImageView);
                        break;
                    case 14://镜像
                        mirror(mImageView);
                        break;
                    case 15://平移
                        transposition(mImageView);
                        break;
                    case 17://固定阈值法
                        singleThreshold(loadedBitmap);
                        break;
                    case 18://双固定阈值法
                        doubleThreshold(loadedBitmap);
                        break;
                    case 20://非0即1二值化
                        mImageView.setImageBitmap(zeroAndOne(loadedBitmap));
                        break;
                    case 21://反色变换
                        mImageView.setImageBitmap(contraty(loadedBitmap));
                        break;
                    case 22://窗口灰度变换
                        window(loadedBitmap);
                        break;
                    case 23://分段线性变换
                        partLinearity(loadedBitmap);
                        break;
                    case 24://灰度分布均衡化
                        //功能暂未实现
                        break;
                    case 25://灰度分布均衡化
                        //功能暂未实现
                        break;
                    case 26://灰度对数变换
                        mImageView.setImageBitmap(logarithm(loadedBitmap));
                        break;
                    case 27://灰度指数变换
                        exponetial(loadedBitmap);
                        break;
                    case 28://灰度幂次变换
                        power(loadedBitmap);
                        break;
                    case 29://二值图像的黑白点噪声滤波
                        twoGrayWhiteAndBlack(loadedBitmap);
                        break;
                    case 30://消除孤立黑像素点
                        isolatedBlack(loadedBitmap,4);
                        break;
                    case 31://3*3均值滤波
                        average33(loadedBitmap);
                        break;
                    case 32://n*n均值滤波
                        averagenn(loadedBitmap);
                        break;
                    case 33://超限邻域平均法
                        overLimitAverage(loadedBitmap);
                        break;
                    case 34:///n*n中值滤波
                        middlenn(loadedBitmap);
                        break;
                    case 35://十字型中值滤波
                        cross(loadedBitmap);
                        break;
                    case 36://n*n最大值滤波
                        maxnn(loadedBitmap);
                        break;
                    case 37://随机噪声
                        random(loadedBitmap);
                        break;
                    case 38://椒盐噪声
                        spicedSalt(loadedBitmap);
                        break;
                    case 39://纵向微分
                        verticalDifferential(loadedBitmap);
                        break;
                    case 40://横向微分
                        horizontalDifferential(loadedBitmap);
                        break;
                    case 41://双向一次
                        doubleDifferential(loadedBitmap);
                        break;
                    case 42://门限锐化
                        thresholdSharpen(loadedBitmap);
                        break;
                    case 43://固定锐化
                        fixedSharpen(loadedBitmap);
                        break;
                    case 44://二值锐化
                        gray2BinarySharpen(loadedBitmap);
                        break;
                    case 45://Robert
                        robert(loadedBitmap);
                        break;
                    case 46://Sobel
                        sobel(loadedBitmap);
                        break;
                    case 47://PreWitt检测
                        preWitt(loadedBitmap);
                        break;
                    case 48://Krisch检测
                        krisch(loadedBitmap);
                        break;
                    case 49://Guasslaplacian检测
                        guasslaplacian(loadedBitmap);
                        break;
                    case 50://显示标号
                        robert(loadedBitmap);
                        break;
                    case 51://面积计算
                        robert(loadedBitmap);
                        break;
                    case 52://周长计算
                        robert(loadedBitmap);
                        break;
                    case 53://小区域消除
                        robert(loadedBitmap);
                        break;
                    case 54://迭代阀值
                        robert(loadedBitmap);
                        break;
                    case 55://峰谷阈值分割
                        robert(loadedBitmap);
                        break;
                    case 56://半阈值分割
                        robert(loadedBitmap);
                        break;
                    case 57://轮廓提取
                        outlineExtract(loadedBitmap);
                        break;
                    case 58://边界跟踪
                        outlineFollow(loadedBitmap);
                        break;
                    case 59://种子填充
                        robert(loadedBitmap);
                        break;
                    case 60://区域生长
                        test(loadedBitmap);
                        break;
                    case 61://水平投影
                        horizontalShadow(loadedBitmap);
                        break;
                    case 62://垂直投影
                        verticalShadow(loadedBitmap);
                        break;
                    case 63://垂直腐蚀
                        verticalCorrosion(loadedBitmap);
                        break;
                    case 64://水平腐蚀
                        horizontalCorrosion(loadedBitmap);
                        break;
                    case 66://t垂直膨胀
                        verticalExpand(loadedBitmap);
                        break;
                    case 67://水平膨胀
                        horizontalExpand(loadedBitmap);
                        break;
                    default:
                        break;
                }
                return true;
            }
        });

        mChooseImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), LoadImageActivity.class);
                startActivityForResult(intent,1);
            }
        });

        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                explosionField = ExplosionField.attach2Window(getActivity());
                explosionField.explode(mImageView);
            }
        });
    }

    /**
     * 水平膨胀
     * @param bm
     */
    private void horizontalExpand(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color,gray,temp;
        int r, g, b, a;

        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);
            a = Color.alpha(color);

            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

            if (gray > 127) {
                gray = 255;
            } else {
                gray = 0;
            }

            oldPx[i] = Color.argb(a,gray,gray,gray);
            newPx[i] = Color.rgb(255,255,255);
        }


        for (int j = 0; j < height;j++) {
            for(int i = 1;i < width -1;i++) {
                a = Color.alpha(oldPx[width*j+i]);
                //newPx[width*j+i] = Color.argb(a,255,255,255);

                for(int n = 0;n < 3;n++) {
                    if(Color.red(oldPx[j*width+i + (n-1)]) < 128) {
                        newPx[j*width+i] = Color.argb(a,0,0,0);
                    }
                }
            }
        }

        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    /**
     * 垂直膨胀
     * @param bm
     */
    private void verticalExpand(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color,gray,temp;
        int r, g, b, a;

        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);
            a = Color.alpha(color);

            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

            if (gray > 127) {
                gray = 255;
            } else {
                gray = 0;
            }

            oldPx[i] = Color.argb(a,gray,gray,gray);
            newPx[i] = Color.rgb(255,255,255);
        }


        for (int j = 1; j < height - 1;j++) {
            for(int i = 0;i < width;i++) {
                a = Color.alpha(oldPx[width*j+i]);
                //newPx[width*j+i] = Color.argb(a,0,0,0);

                for(int n = 0;n < 3;n++) {
                    if(Color.red(oldPx[(j + n-1)*width+i]) < 128) {
                        newPx[j*width+i] = Color.argb(a,0,0,0);
                    }
                }
            }
        }

        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    /**
     * 水平腐蚀
     * @param bm
     */
    private void horizontalCorrosion(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color,gray,temp;
        int r, g, b, a;

        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);
            a = Color.alpha(color);

            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

            if (gray > 127) {
                gray = 255;
            } else {
                gray = 0;
            }

            oldPx[i] = Color.argb(a,gray,gray,gray);
            newPx[i] = Color.rgb(255,255,255);
        }


        for (int j = 0; j < height;j++) {
            for(int i = 1;i < width -1;i++) {
                a = Color.alpha(oldPx[width*j+i]);
                newPx[width*j+i] = Color.argb(a,0,0,0);

                for(int n = 0;n < 3;n++) {
                    if(Color.red(oldPx[j*width+i + (n-1)]) > 128) {
                        newPx[j*width+i] = Color.argb(a,255,255,255);
                    }
                }
            }
        }

        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    /**
     * 垂直腐蚀
     * @param bm
     */
    private void verticalCorrosion(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color,gray,temp;
        int r, g, b, a;

        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);
            a = Color.alpha(color);

            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

            if (gray > 127) {
                gray = 255;
            } else {
                gray = 0;
            }

            oldPx[i] = Color.argb(a,gray,gray,gray);
            newPx[i] = Color.rgb(255,255,255);
        }


        for (int j = 1; j < height - 1;j++) {
            for(int i = 0;i < width;i++) {
                a = Color.alpha(oldPx[width*j+i]);
                newPx[width*j+i] = Color.argb(a,0,0,0);

                for(int n = 0;n < 3;n++) {
                    if(Color.red(oldPx[(j + n-1)*width+i]) > 128) {
                        newPx[j*width+i] = Color.argb(a,255,255,255);
                    }
                }
            }
        }

        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);

    }

    /**
     * 边界跟踪
     * @param bm
     */
    private void outlineFollow(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color,gray,temp;
        int r, g, b, a;
        boolean findStartPoint;//是否找到起始点及回到起始点
        boolean findPoint;//是否扫描到一个边界点
        int pixel;//像素值
        Point startPoint,currentPoint;//其实边界点与当前边界点
        startPoint = new Point();
        currentPoint = new Point();
        int[][] Goto = {{-1,-1},{0,-1},{1,-1},{1,0},{1,1},{0,1},{-1,1},{-1,0}};
        int startDirect;

        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);

            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

            if (gray > 127) {
                gray = 255;
            } else {
                gray = 0;
            }

            oldPx[i] = Color.rgb(gray,gray,gray);
            newPx[i] = Color.rgb(255,255,255);
        }

        findStartPoint = false;

        for (int j = 0; j < height && !findStartPoint; j++) {
            for(int i = 0;i < width && !findStartPoint;i++) {
                if(Color.red(oldPx[j*width+i]) == 0) {
                    findStartPoint = true;
                    startPoint.x = i;
                    startPoint.y = j;
                    newPx[j*width+i] = Color.rgb(0,0,0);
                }
            }
        }
        startDirect = 0;
        findStartPoint = false;
        currentPoint.x = startPoint.x;
        currentPoint.y = startPoint.y;
        while(!findStartPoint) {
            temp = (currentPoint.y + Goto[startDirect][1]) * width + (currentPoint.x + Goto[startDirect][0]);
            gray = Color.red(oldPx[temp]);
            if(gray == 0) {
                findPoint = true;
                currentPoint.y = currentPoint.y + Goto[startDirect][1];
                currentPoint.x = currentPoint.x + Goto[startDirect][0];
                if(currentPoint.y == startPoint.y && currentPoint.x == startPoint.x) {
                    findStartPoint = true;
                }
                temp = width * currentPoint.y + currentPoint.x;
                newPx[temp] = Color.rgb(0,0,0);
                startDirect--;
                if(startDirect == -1) {
                    startDirect = 7;
                    startDirect--;
                    if(startDirect == -1) {
                        startDirect = 7;
                    }
                }
            } else {
                startDirect++;
                if(startDirect == 8) {
                    startDirect = 0;
                }
            }
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    /**
     * 固定锐化
     * @param bm
     */
    private void fixedSharpen(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color,gray,temp;
        int r, g, b, a;
        int pixel[] = new int[4];
        int n1,n2,n3,n4,n5,n6,n7,n8;

        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);

            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

            if (gray > 255) {
                gray = 255;
            }

            oldPx[i] = Color.rgb(gray,gray,gray);
            newPx[i] = Color.rgb(255,255,255);
        }

        for (int j = 1; j < height - 1; j++) {
            for(int i = 1;i < width - 1;i++) {
                temp = (int)Math.sqrt((Color.red(oldPx[j*width+i])-Color.red(oldPx[j*width+i-1]))*(Color.red(oldPx[j*width+i])-Color.red(oldPx[j*width+i-1]))
                        +(Color.red(oldPx[(j)*width+i])-Color.red(oldPx[(j-1)*width+i]))*(Color.red(oldPx[j*width+i])-Color.red(oldPx[(j-1)*width+i])));
                if(temp > 30) {
                    newPx[j*width+i] = Color.rgb(255,255,255);
                } else {
                    newPx[j*width+i] = oldPx[j*width+i];
                }
            }
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    /**
     * 二值锐化
     * @param bm
     */
    private void gray2BinarySharpen(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color,gray,temp;
        int r, g, b, a;
        int pixel[] = new int[4];
        int n1,n2,n3,n4,n5,n6,n7,n8;

        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);

            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

            if (gray > 255) {
                gray = 255;
            }

            oldPx[i] = Color.rgb(gray,gray,gray);
            newPx[i] = Color.rgb(255,255,255);
        }

        for (int j = 1; j < height - 1; j++) {
            for(int i = 1;i < width - 1;i++) {
                temp = (int)Math.sqrt((Color.red(oldPx[j*width+i])-Color.red(oldPx[j*width+i-1]))*(Color.red(oldPx[j*width+i])-Color.red(oldPx[j*width+i-1]))
                        +(Color.red(oldPx[(j)*width+i])-Color.red(oldPx[(j-1)*width+i]))*(Color.red(oldPx[j*width+i])-Color.red(oldPx[(j-1)*width+i])));
                if(temp > 30) {
                    newPx[j*width+i] = Color.rgb(255,255,255);
                } else {
                    newPx[j*width+i] = Color.rgb(0,0,0);
                }
            }
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    /**
     * 门限锐化
     * @param bm
     */
    private void thresholdSharpen(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color,gray,temp;
        int r, g, b, a;
        int pixel[] = new int[4];
        int n1,n2,n3,n4,n5,n6,n7,n8;

        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);

            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

            if (gray > 255) {
                gray = 255;
            }
            oldPx[i] = Color.rgb(gray,gray,gray);
            newPx[i] = Color.rgb(255,255,255);
        }

        for (int j = 1; j < height - 1; j++) {
            for(int i = 1;i < width - 1;i++) {
                temp = (int)Math.sqrt((Color.red(oldPx[j*width+i])-Color.red(oldPx[j*width+i-1]))*(Color.red(oldPx[j*width+i])-Color.red(oldPx[j*width+i-1]))
                        +(Color.red(oldPx[(j)*width+i])-Color.red(oldPx[(j-1)*width+i]))*(Color.red(oldPx[j*width+i])-Color.red(oldPx[(j-1)*width+i])));
                if(temp >= 30) {
                    if(temp + 100 > 255) {
                        newPx[j*width+i] = Color.rgb(255,255,255);
                    } else {
                        newPx[j*width+i] = Color.rgb(temp+100,temp+100,temp+100);
                    }
                } else {
                    newPx[j*width+i] = oldPx[j*width+i];
                }
            }
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    /**
     * 轮廓提取
     * @param bm
     */
    private void outlineExtract(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color,gray;
        int r, g, b, a;
        int pixel[] = new int[4];
        int n1,n2,n3,n4,n5,n6,n7,n8;

        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);

            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

            if (gray > 128) {
                gray = 255;
            } else {
                gray = 0;
            }

            oldPx[i] = Color.rgb(gray,gray,gray);
            newPx[i] = Color.rgb(255,255,255);
        }

        for (int j = 1; j < height - 1; j++) {
            for(int i = 1;i < width - 1;i++) {
                if(Color.red(oldPx[j*width+i]) == 0) {
                    newPx[j*width+i] = Color.rgb(0,0,0);
                    n1 = Color.red(oldPx[(j+1)*width+i-1]);
                    n2 = Color.red(oldPx[(j+1)*width+i]);
                    n3 = Color.red(oldPx[(j+1)*width+i+1]);
                    n4 = Color.red(oldPx[(j)*width+i-1]);
                    n5 = Color.red(oldPx[(j)*width+i+1]);
                    n6 = Color.red(oldPx[(j-1)*width+i-1]);
                    n7 = Color.red(oldPx[(j-1)*width+i]);
                    n8 = Color.red(oldPx[(j-1)*width+i+1]);
                    if(n1+n2+n3+n4+n5+n6+n7+n8 == 0) {
                        newPx[j*width+i] = Color.rgb(255,255,255);
                    }
                }

            }
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    /**
     * Guasslaplacian
     * @param bm
     */
    private void guasslaplacian(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);
        int color,gray,r,g,b,a;
        int i,j;
        int tempH;//模板高度
        int tempW;//模板宽度
        float tempC;//模板系数
        int tempMY;//模板中心元素y坐标
        int tempMX;//模板中心元素x坐标
        float[] Template = new float[25];
        int[] temp1 = new int[width * height];
        int[] temp2 = new int[width * height];
        bm.getPixels(temp1, 0, width, 0, 0, width, height);
        bm.getPixels(temp2, 0, width, 0, 0, width, height);

        for (i = 0; i < width * height; i++) {
            color = temp1[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);

            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

            if (gray > 255) {
                gray = 255;
            }
            if(gray < 0) {
                gray = 0;
            }

            temp1[i] = gray;
            temp2[i] = gray;
        }

        tempW = 5;
        tempH = 5;
        tempC = 0.25f;
        tempMY = 4;
        tempMX = 4;

        Template[0] = -2.0f;
        Template[1] = -4.0f;
        Template[2] = -4.0f;
        Template[3] = -4.0f;
        Template[4] = -2.0f;
        Template[5] = -4.0f;
        Template[6] = 0.0f;
        Template[7] = 8.0f;
        Template[8] = 0.0f;
        Template[9] = -4.0f;
        Template[10] = -4.0f;
        Template[11] = -8.0f;
        Template[12] = 24.0f;
        Template[13] = 8.0f;
        Template[14] = -4.0f;
        Template[15] = -4.0f;
        Template[16] = 0.0f;
        Template[17] = 8.0f;
        Template[18] = 0.0f;
        Template[19] = -4.0f;
        Template[20] = -2.0f;
        Template[21] = -4.0f;
        Template[22] = -4.0f;
        Template[23] = -4.0f;
        Template[24] = -2.0f;

        temp1 = Template(temp1,width,height,tempH,tempW,tempMX,tempMY,Template,tempC);

        for(j = 0;j < height;j++) {
            for(i = 0;i < width;i++) {
                temp1[j*width+i] = Color.rgb(temp1[j*width+i],temp1[j*width+i],temp1[j*width+i]);
            }
        }
        bmp.setPixels(temp1, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    /**
     * drisch检测
     * @param bm
     */
    private void krisch(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);
        int color,gray,r,g,b,a;
        int i,j;
        int tempH;//模板高度
        int tempW;//模板宽度
        float tempC;//模板系数
        int tempMY;//模板中心元素y坐标
        int tempMX;//模板中心元素x坐标
        float[] Template = new float[9];
        int[] temp1 = new int[width * height];
        int[] temp2 = new int[width * height];
        bm.getPixels(temp1, 0, width, 0, 0, width, height);
        bm.getPixels(temp2, 0, width, 0, 0, width, height);

        for (i = 0; i < width * height; i++) {
            color = temp1[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);

            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

            if (gray > 255) {
                gray = 255;
            }
            if(gray < 0) {
                gray = 0;
            }

            temp1[i] = gray;
            temp2[i] = gray;
        }

        tempW = 3;
        tempH = 3;
        tempC = 0.5f;
        tempMY = 1;
        tempMX = 1;

        Template[0] = 5.0f;
        Template[1] = 5.0f;
        Template[2] = 5.0f;
        Template[3] = -3.0f;
        Template[4] = 0.0f;
        Template[5] = -3.0f;
        Template[6] = -3.0f;
        Template[7] = -3.0f;
        Template[8] = -3.0f;

        temp1 = Template(temp1,width,height,tempH,tempW,tempMX,tempMY,Template,tempC);

        Template[0] = -3.0f;
        Template[1] = 5.0f;
        Template[2] = 5.0f;
        Template[3] = -3.0f;
        Template[4] = 0.0f;
        Template[5] = 5.0f;
        Template[6] = -3.0f;
        Template[7] = -3.0f;
        Template[8] = -3.0f;

        temp2 = Template(temp2,width,height,tempH,tempW,tempMX,tempMY,Template,tempC);

        for(j = 0;j < height;j++) {
            for(i = 0;i < width;i++) {
                if(Color.red(temp2[j*width+i]) > Color.red(temp1[j*width+i])) {
                    temp1[j*width+i] = temp2[j*width+i];
                }
                //temp1[j*width+i] = Color.rgb(temp1[j*width+i],temp1[j*width+i],temp1[j*width+i]);
            }
        }

        bm.getPixels(temp2, 0, width, 0, 0, width, height);

        Template[0] = -3.0f;
        Template[1] = -3.0f;
        Template[2] = 5.0f;
        Template[3] = -3.0f;
        Template[4] = 0.0f;
        Template[5] = 5.0f;
        Template[6] = -3.0f;
        Template[7] = -3.0f;
        Template[8] = 5.0f;

        temp2 = Template(temp2,width,height,tempH,tempW,tempMX,tempMY,Template,tempC);

        for(j = 0;j < height;j++) {
            for(i = 0;i < width;i++) {
                if(Color.red(temp2[j*width+i]) > Color.red(temp1[j*width+i])) {
                    temp1[j*width+i] = temp2[j*width+i];
                }
                //temp1[j*width+i] = Color.rgb(temp1[j*width+i],temp1[j*width+i],temp1[j*width+i]);
            }
        }

        bm.getPixels(temp2, 0, width, 0, 0, width, height);

        Template[0] = -3.0f;
        Template[1] = -3.0f;
        Template[2] = -3.0f;
        Template[3] = -3.0f;
        Template[4] = 0.0f;
        Template[5] = 5.0f;
        Template[6] = -3.0f;
        Template[7] = 5.0f;
        Template[8] = 5.0f;

        temp2 = Template(temp2,width,height,tempH,tempW,tempMX,tempMY,Template,tempC);

        for(j = 0;j < height;j++) {
            for(i = 0;i < width;i++) {
                if(Color.red(temp2[j*width+i]) > Color.red(temp1[j*width+i])) {
                    temp1[j*width+i] = temp2[j*width+i];
                }
                //temp1[j*width+i] = Color.rgb(temp1[j*width+i],temp1[j*width+i],temp1[j*width+i]);
            }
        }

        bm.getPixels(temp2, 0, width, 0, 0, width, height);

        Template[0] = -3.0f;
        Template[1] = -3.0f;
        Template[2] = -3.0f;
        Template[3] = -3.0f;
        Template[4] = 0.0f;
        Template[5] = -3.0f;
        Template[6] = 5.0f;
        Template[7] = 5.0f;
        Template[8] = 5.0f;

        temp2 = Template(temp2,width,height,tempH,tempW,tempMX,tempMY,Template,tempC);

        for(j = 0;j < height;j++) {
            for(i = 0;i < width;i++) {
                if(Color.red(temp2[j*width+i]) > Color.red(temp1[j*width+i])) {
                    temp1[j*width+i] = temp2[j*width+i];
                }
                //temp1[j*width+i] = Color.rgb(temp1[j*width+i],temp1[j*width+i],temp1[j*width+i]);
            }
        }

        bm.getPixels(temp2, 0, width, 0, 0, width, height);

        Template[0] = -3.0f;
        Template[1] = -3.0f;
        Template[2] = -3.0f;
        Template[3] = 5.0f;
        Template[4] = 0.0f;
        Template[5] = -3.0f;
        Template[6] = 5.0f;
        Template[7] = 5.0f;
        Template[8] = -3.0f;

        temp2 = Template(temp2,width,height,tempH,tempW,tempMX,tempMY,Template,tempC);

        for(j = 0;j < height;j++) {
            for(i = 0;i < width;i++) {
                if(Color.red(temp2[j*width+i]) > Color.red(temp1[j*width+i])) {
                    temp1[j*width+i] = temp2[j*width+i];
                }
                //temp1[j*width+i] = Color.rgb(temp1[j*width+i],temp1[j*width+i],temp1[j*width+i]);
            }
        }

        bm.getPixels(temp2, 0, width, 0, 0, width, height);

        Template[0] = 5.0f;
        Template[1] = -3.0f;
        Template[2] = -3.0f;
        Template[3] = 5.0f;
        Template[4] = 0.0f;
        Template[5] = -3.0f;
        Template[6] = 5.0f;
        Template[7] = -3.0f;
        Template[8] = -3.0f;

        temp2 = Template(temp2,width,height,tempH,tempW,tempMX,tempMY,Template,tempC);

        for(j = 0;j < height;j++) {
            for(i = 0;i < width;i++) {
                if(Color.red(temp2[j*width+i]) > Color.red(temp1[j*width+i])) {
                    temp1[j*width+i] = temp2[j*width+i];
                }
                //temp1[j*width+i] = Color.rgb(temp1[j*width+i],temp1[j*width+i],temp1[j*width+i]);
            }
        }

        bm.getPixels(temp2, 0, width, 0, 0, width, height);

        Template[0] = 5.0f;
        Template[1] = 5.0f;
        Template[2] = -3.0f;
        Template[3] = 5.0f;
        Template[4] = 0.0f;
        Template[5] = -3.0f;
        Template[6] = -3.0f;
        Template[7] = -3.0f;
        Template[8] = -3.0f;

        temp2 = Template(temp2,width,height,tempH,tempW,tempMX,tempMY,Template,tempC);

        for(j = 0;j < height;j++) {
            for(i = 0;i < width;i++) {
                if(Color.red(temp2[j*width+i]) > Color.red(temp1[j*width+i])) {
                    temp1[j*width+i] = temp2[j*width+i];
                }
                temp1[j*width+i] = Color.rgb(temp1[j*width+i],temp1[j*width+i],temp1[j*width+i]);
            }
        }

        bmp.setPixels(temp1, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    /**
     * PreWitt检测
     * @param bm
     */
    private void preWitt(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);
        int color,gray,r,g,b,a;
        int i,j;
        int tempH;//模板高度
        int tempW;//模板宽度
        float tempC;//模板系数
        int tempMY;//模板中心元素y坐标
        int tempMX;//模板中心元素x坐标
        float[] Template = new float[9];
        int[] temp1 = new int[width * height];
        int[] temp2 = new int[width * height];
        bm.getPixels(temp1, 0, width, 0, 0, width, height);
        bm.getPixels(temp2, 0, width, 0, 0, width, height);

        for (i = 0; i < width * height; i++) {
            color = temp1[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);

            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

            if (gray > 255) {
                gray = 255;
            }
            if(gray < 0) {
                gray = 0;
            }

            temp1[i] = gray;
            temp2[i] = gray;
        }

        tempW = 3;
        tempH = 3;
        tempC = 1.0f;
        tempMY = 1;
        tempMX = 1;

        Template[0] = -1.0f;
        Template[1] = -1.0f;
        Template[2] = -1.0f;
        Template[3] = 0.0f;
        Template[4] = 0.0f;
        Template[5] = 0.0f;
        Template[6] = 1.0f;
        Template[7] = 1.0f;
        Template[8] = 1.0f;

        temp1 = Template(temp1,width,height,tempH,tempW,tempMX,tempMY,Template,tempC);

        Template[0] = 1.0f;
        Template[1] = 0.0f;
        Template[2] = -1.0f;
        Template[3] = 1.0f;
        Template[4] = 0.0f;
        Template[5] = -1.0f;
        Template[6] = 1.0f;
        Template[7] = 0.0f;
        Template[8] = -1.0f;

        temp2 = Template(temp2,width,height,tempH,tempW,tempMX,tempMY,Template,tempC);

        for(j = 0;j < height;j++) {
            for(i = 0;i < width;i++) {
                if(Color.red(temp2[j*width+i]) > Color.red(temp1[j*width+i])) {
                    temp1[j*width+i] = temp2[j*width+i];
                }
                temp1[j*width+i] = Color.rgb(temp1[j*width+i],temp1[j*width+i],temp1[j*width+i]);
            }
        }
        bmp.setPixels(temp1, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    private void test(Bitmap bm) {
        Log.d("xxx","我运行了test函数");
        int width = bm.getWidth();
        int height = bm.getHeight();

        for(int m = 0;m < height;m++) {
            for(int n = 0;n < width;n++) {
                Log.d("xyz","i = " + n);
            }
        }


    }

    /**
     * 水平投影
     * @param bm
     */
    private void horizontalShadow(Bitmap bm) {
        Bitmap grayBitmap = twoGray(bm);
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color,gray,average;
        int r, g, b, a = 0,i,m;

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        int[] temp=new int[width * height];
        grayBitmap.getPixels(oldPx, 0, width, 0, 0, width, height);
        int lBlackNum;
        int LineBytes=(((width*8)+31)/32*4);
        for (i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);
            a = Color.alpha(color);

            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

            if(gray > 100) {
                gray = 255;
            } else {
                gray = 0;
            }
            oldPx[i] = Color.argb(a,gray,gray,gray);
        }

        for(i = 0;i < width*height;i++) {
            newPx[i] = Color.rgb(255,255,255);
        }

        for (int n = 0; n < height; n++) {
            lBlackNum=0;
            for (m = 0; m < width; m++) {
                if(Color.red(oldPx[n*width+m])==0)
                    lBlackNum++;
            }
            for(m=0;m<lBlackNum;m++){
                color = newPx[n*width+m];
                r = Color.red(color);
                r = 0;
                newPx[n*width+m]=Color.rgb(r,r,r);
            }
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    /**
     * 垂直投影
     * @param bm
     */
    private void verticalShadow(Bitmap bm) {
        Bitmap grayBitmap = twoGray(bm);
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color,gray,average;
        int r, g, b, a = 0,i,j,m;

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        int[] temp=new int[width * height];
        grayBitmap.getPixels(oldPx, 0, width, 0, 0, width, height);

        int lBlackNum;
        int LineBytes=(((width*8)+31)/32*4);
        for (i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);
            a = Color.alpha(color);

            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

            if(gray > 100) {
                gray = 255;
            } else {
                gray = 0;
            }
            oldPx[i] = Color.argb(a,gray,gray,gray);
        }
        for(i = 0;i < width*height;i++) {
            newPx[i] = Color.rgb(255,255,255);
        }
        for (i = 0; i < width; i++) {
            lBlackNum=0;
            for (j = 0; j < height; j++) {
                if(Color.red(oldPx[j*width+i])==0)
                    lBlackNum++;
            }
            for(j=0;j<lBlackNum;j++){
                color = newPx[j*width+i];
                r = Color.red(color);
                r = 0;
                newPx[j*width+i]=Color.rgb(r,r,r);
            }
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }


    /**
     * Sobel
     * @param bm
     */
    private void sobel(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);
        int color,gray,r,g,b,a;
        int i,j;
        int tempH;//模板高度
        int tempW;//模板宽度
        float tempC;//模板系数
        int tempMY;//模板中心元素y坐标
        int tempMX;//模板中心元素x坐标
        float[] Template = new float[9];
        int[] temp1 = new int[width * height];
        int[] temp2 = new int[width * height];
        bm.getPixels(temp1, 0, width, 0, 0, width, height);
        bm.getPixels(temp2, 0, width, 0, 0, width, height);

        for (i = 0; i < width * height; i++) {
            color = temp1[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);

            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

            if (gray > 255) {
                gray = 255;
            }
            if(gray < 0) {
                gray = 0;
            }

            temp1[i] = gray;
            temp2[i] = gray;
        }

        tempW = 3;
        tempH = 3;
        tempC = 1.0f;
        tempMY = 1;
        tempMX = 1;

        Template[0] = -1.0f;
        Template[1] = -2.0f;
        Template[2] = -1.0f;
        Template[3] = 0.0f;
        Template[4] = 0.0f;
        Template[5] = 0.0f;
        Template[6] = 1.0f;
        Template[7] = 2.0f;
        Template[8] = 1.0f;

        temp1 = Template(temp1,width,height,tempH,tempW,tempMX,tempMY,Template,tempC);

        Template[0] = -1.0f;
        Template[1] = 0.0f;
        Template[2] = 1.0f;
        Template[3] = -2.0f;
        Template[4] = 0.0f;
        Template[5] = 2.0f;
        Template[6] = -1.0f;
        Template[7] = 0.0f;
        Template[8] = 1.0f;

        temp2 = Template(temp2,width,height,tempH,tempW,tempMX,tempMY,Template,tempC);

        for(j = 0;j < height;j++) {
            for(i = 0;i < width;i++) {
                if(Color.red(temp2[j*width+i]) > Color.red(temp1[j*width+i])) {
                    temp1[j*width+i] = temp2[j*width+i];
                }
                temp1[j*width+i] = Color.rgb(temp1[j*width+i],temp1[j*width+i],temp1[j*width+i]);
            }
        }
        bmp.setPixels(temp1, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    private int[] Template(int[] temp1,int width,int height,int tempH,int tempW,int tempMX,int tempMY,float[] fpArray,float fCoef) {
        int i,j,k,l;
        int[] temp = new int[width*height];
        System.arraycopy(temp1,0,temp,0,temp1.length);
        float fResult;
        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);
        for(j = tempMY;j < height - tempH + tempMY + 1;j++) {
            for(i = tempMX;i < width - tempW + tempMX + 1;i++) {
                fResult = 0;
                for(k = 0;k < tempH;k++) {
                    for(l = 0;l < tempW;l++) {
                        fResult = fResult + temp1[(j-tempMY+k)*width+(i-tempMX+l)]*fpArray[k*tempW+l];
                    }
                }
                fResult *= fCoef;
                fResult = (float)Math.abs(fResult);
                if(fResult > 255) {
                    temp[j*width+i] = 255;
                } else {
                    temp[j*width+i] = (int)(fResult + 0.5);

                }
            }
        }
        return temp;
    }

    /**
     * robert
     * @param bm
     */
    private void robert(Bitmap bm) {
        int width = bm.getWidth();//原图像宽度
        int height = bm.getHeight();//
        int color,gray;
        int r, g, b, a;
        int pixel[] = new int[4];

        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);

            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

            if (gray > 255) {
                gray = 255;
            }

            oldPx[i] = Color.rgb(gray,gray,gray);
            newPx[i] = Color.rgb(255,255,255);
        }

        for (int j = 0; j < height - 1; j++) {
            for(int i = 0;i < width - 1;i++) {

                pixel[0] = Color.red(oldPx[j*width+i]);
                pixel[1] = Color.red(oldPx[j*width+i+1]);
                pixel[2] = Color.red(oldPx[(j+1)*width+i]);
                pixel[3] = Color.red(oldPx[(j+1)*width+i+1]);

                gray = (int)Math.sqrt((pixel[0]-pixel[3])*(pixel[0]-pixel[3])+(pixel[1]-pixel[2])*(pixel[1]-pixel[2]));
                newPx[j*width+i] = Color.rgb(gray,gray,gray);
            }
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    /**
     * 横向微分
     * @param bm
     */
    private void horizontalDifferential(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color,gray;
        int r, g, b, a;

        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);
            a = Color.alpha(color);

            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);
            //Log.d("xyz","gray" + gray);

            if (gray > 100) {
                gray = 255;
            }

            oldPx[i] = Color.argb(a,gray,gray,gray);
        }

        for (int i = 1; i < height - 1; i++) {
            for(int j = 1;j < width - 1;j++) {
                color = oldPx[i*width+j];
                r = Color.red(color);
                a = Color.alpha(color);
                gray = Math.abs(Color.red(oldPx[i*width+j]) - Color.red(oldPx[(i-1)*width+j]));
                newPx[i*width+j] = Color.argb(a,gray,gray,gray);
            }
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    /**
     * 双向一次微分
     * @param bm
     */
    private void doubleDifferential(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color,gray;
        int r, g, b, a;

        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);
            a = Color.alpha(color);

            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);
            //Log.d("xyz","gray" + gray);

            if (gray > 100) {
                gray = 255;
            }

            oldPx[i] = Color.argb(a,gray,gray,gray);
        }

        for (int i = 1; i < height - 1; i++) {
            for(int j = 1;j < width - 1;j++) {
                color = oldPx[i*width+j];
                r = Color.red(color);
                a = Color.alpha(color);
                gray = (int)Math.sqrt((Color.red(oldPx[i*width+j])-Color.red(oldPx[i*width+j-1]))*(Color.red(oldPx[i*width+j])-Color.red(oldPx[i*width+j-1]))
                        +(Color.red(oldPx[(i)*width+j])-Color.red(oldPx[(i-1)*width+j]))*(Color.red(oldPx[i*width+j])-Color.red(oldPx[(i-1)*width+j-1])));

                newPx[i*width+j] = Color.argb(a,gray,gray,gray);
            }
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    /**
     * 纵向微分
     * @param bm
     */
    private void verticalDifferential(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color,gray;
        int r, g, b, a;

        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);
            a = Color.alpha(color);

            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);
            //Log.d("xyz","gray" + gray);

            if (gray > 100) {
                gray = 255;
            }

            oldPx[i] = Color.argb(a,gray,gray,gray);
        }

        for (int i = 1; i < height - 1; i++) {
            for(int j = 1;j < width - 1;j++) {
                color = oldPx[i*width+j];
                r = Color.red(color);
                a = Color.alpha(color);
                gray = Math.abs(Color.red(oldPx[i*width+j]) - Color.red(oldPx[i*width+j-1]));
                newPx[i*width+j] = Color.argb(a,gray,gray,gray);
            }
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    /**
     * 产生椒盐噪声
     * @param bm
     */
    private void spicedSalt(Bitmap bm) {
        Toast.makeText(getContext(),"暂时没写",Toast.LENGTH_SHORT).show();
    }

    /**
     * 随机噪声
     * @param bm
     */
    private void random(Bitmap bm) {
        Toast.makeText(getContext(),"暂时没写",Toast.LENGTH_SHORT).show();
    }



    /**
     * n*n最大值滤波
     * @param bm
     */
    private void maxnn(final Bitmap bm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("n*n最大值滤波");
        builder.setMessage("请输入n值（大于3的奇数）");
        View view1 = LayoutInflater.from(getContext()).inflate(R.layout.threshold,null);
        final EditText mSingleThresholdEt = view1.findViewById(R.id.digit_dialog);
        builder.setView(view1);

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int k) {

                Bitmap grayBitmap = twoGray(bm);
                int width = bm.getWidth();
                int height = bm.getHeight();
                int color,gray,average;
                int r, g, b, a = 0;
                int yy,xx,n2 = 0,nn,chg = 0,m,medi,madom,madomax;
                int[] mado = new int[1000];

                Bitmap bmp = Bitmap.createBitmap(width, height
                        , Bitmap.Config.ARGB_8888);



                int[] oldPx = new int[width * height];
                int[] newPx = new int[width * height];
                grayBitmap.getPixels(oldPx, 0, width, 0, 0, width, height);

                String str = mSingleThresholdEt.getText().toString();
                if("".equals(str)) {
                    nm = 0;
                } else {
                    nm = Integer.valueOf(str);
                }

                if(nm < 3 || nm % 2 != 1) {
                    Toast.makeText(getActivity(),"请输入一个大于等于3的奇数",Toast.LENGTH_SHORT).show();
                    return;
                }

                if(nm >= 3 && nm % 2 == 1) {
                    n2 = (nm - 1) / 2;
                }

                nn = nm*nm;

                for(int j = n2;j < height - n2;j++) {
                    for(int i = n2;i < width - n2;i++) {
                        m = 0;
                        color = oldPx[j*width+i];
                        a = Color.alpha(color);
                        for(yy = j - n2;yy <= j + n2;yy++) {
                            for (xx = i - n2; xx <= i + n2; xx++) {
                                mado[m] = Color.red(oldPx[yy * width + xx]);
                                m++;
                            }
                        }

                        madomax = mado[0];
                        for(m = 1;m < nn;m++) {
                            if(madomax<mado[m])
                                madomax = mado[m];
                        }

                        newPx[j*width+i] = Color.argb(a,madomax,madomax,madomax);
                    }
                }
                bmp.setPixels(newPx, 0, width, 0, 0, width, height);
                mImageView.setImageBitmap(bmp);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * 十字型中值滤波
     * @param bm
     */
    private void cross(final Bitmap bm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("十字型中值滤波");
        builder.setMessage("请输入n值（大于3的奇数）");
        View view1 = LayoutInflater.from(getContext()).inflate(R.layout.threshold,null);
        final EditText mSingleThresholdEt = view1.findViewById(R.id.digit_dialog);
        builder.setView(view1);

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int k) {

                Bitmap grayBitmap = twoGray(bm);
                int width = bm.getWidth();
                int height = bm.getHeight();
                int color,gray,average;
                int r, g, b, a = 0;
                int yy,xx,n2 = 0,nn,chuo,chg = 0,m,medi,madom;
                int[] mado = new int[1000];

                Bitmap bmp = Bitmap.createBitmap(width, height
                        , Bitmap.Config.ARGB_8888);



                int[] oldPx = new int[width * height];
                int[] newPx = new int[width * height];
                grayBitmap.getPixels(oldPx, 0, width, 0, 0, width, height);

                String str = mSingleThresholdEt.getText().toString();
                if("".equals(str)) {
                    nc = 0;
                } else {
                    nc = Integer.valueOf(str);
                }

                if(nc < 3 || nc % 2 != 1) {
                    Toast.makeText(getActivity(),"请输入一个大于等于3的奇数",Toast.LENGTH_SHORT).show();
                    return;
                }

                if(nc >= 3 && nc % 2 == 1) {
                    n2 = (nc - 1) / 2;
                }

                nn = nc + nc + 1;
                chuo = (nn - 1) / 2;

                for(int j = n2;j < height - n2;j++) {
                    for(int i = n2;i < width - n2;i++) {
                        m = 0;
                        color = oldPx[j*width+i];
                        a = Color.alpha(color);
                        for(yy = j - n2;yy <= j + n2;yy++) {
                            mado[m] = Color.red(oldPx[yy*width+i]);
                            m++;
                        }

                        for(xx = i - n2; xx <= i + n2; xx++) {
                            if(xx == i)
                                continue;
                            mado[m] = Color.red(oldPx[j*width+xx]);
                            m++;
                        }

                        //把mado[m]中的值按下降顺序用冒泡排序
                        do {
                            chg = 0;
                            for(m = 0;m < nn - 1;m++) {
                                if(mado[m] < mado[m+1]) {
                                    madom = mado[m];
                                    mado[m] = mado[m+1];
                                    mado[m+1] = madom;
                                    chg = 1;
                                }
                            }
                        }while(chg == 1);
                        //求中值medi
                        medi = mado[chuo];
                        //把中值代入显示图像中
                        //Log.d("xyz","medi + " + medi);
                        newPx[j*width+i] = Color.argb(a,medi,medi,medi);
                    }
                }
                bmp.setPixels(newPx, 0, width, 0, 0, width, height);
                mImageView.setImageBitmap(bmp);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * n*n中值滤波
     * @param bm
     */
    private void middlenn(final Bitmap bm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("n*n中值滤波");
        builder.setMessage("请输入n值（大于3的奇数）");
        View view1 = LayoutInflater.from(getContext()).inflate(R.layout.threshold,null);
        final EditText mSingleThresholdEt = view1.findViewById(R.id.digit_dialog);
        builder.setView(view1);

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int k) {

                Bitmap grayBitmap = twoGray(bm);
                int width = bm.getWidth();
                int height = bm.getHeight();
                int color,gray,average;
                int r, g, b, a = 0;
                int yy,xx,n2 = 0,nn,chuo,chg = 0,m,medi,madom;
                int[] mado = new int[1000];

                Bitmap bmp = Bitmap.createBitmap(width, height
                        , Bitmap.Config.ARGB_8888);



                int[] oldPx = new int[width * height];
                int[] newPx = new int[width * height];
                grayBitmap.getPixels(oldPx, 0, width, 0, 0, width, height);

                String str = mSingleThresholdEt.getText().toString();
                if("".equals(str)) {
                    na = 0;
                } else {
                    na = Integer.valueOf(str);
                }

                if(na < 3 || na % 2 != 1) {
                    Toast.makeText(getActivity(),"请输入一个大于等于3的奇数",Toast.LENGTH_SHORT).show();
                    return;
                }

                if(na >= 3 && na % 2 == 1) {
                    n2 = (na - 1) / 2;
                }

                nn = na * na;
                chuo = (nn - 1) / 2;

                for(int j = n2;j < height - n2;j++) {
                    for(int i = n2;i < width - n2;i++) {
                        m = 0;
                        color = oldPx[j*width+i];
                        a = Color.alpha(color);
                        for(yy = j - n2;yy <= j + n2;yy++) {
                            for (xx = i - n2; xx <= i + n2; xx++) {
                                mado[m] = Color.red(oldPx[yy * width + xx]);
                                m++;
                            }
                        }
                        //把mado[m]中的值按下降顺序用冒泡排序
                        do {
                            chg = 0;
                            for(m = 0;m < nn - 1;m++) {
                                if(mado[m] < mado[m+1]) {
                                    madom = mado[m];
                                    mado[m] = mado[m+1];
                                    mado[m+1] = madom;
                                    chg = 1;
                                }
                            }
                        }while(chg == 1);
                        //求中值medi
                        medi = mado[chuo];
                        //把中值代入显示图像中
                        //Log.d("xyz","medi + " + medi);
                        newPx[j*width+i] = Color.argb(a,medi,medi,medi);
                    }
                }
                bmp.setPixels(newPx, 0, width, 0, 0, width, height);
                mImageView.setImageBitmap(bmp);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * 超限邻域平均法
     * @param bm
     */
    private void overLimitAverage(final Bitmap bm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("超限邻域平均法");
        builder.setMessage("请输入阈值");
        View view1 = LayoutInflater.from(getContext()).inflate(R.layout.threshold,null);
        final EditText mSingleThresholdEt = view1.findViewById(R.id.digit_dialog);
        builder.setView(view1);

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int k) {

                Bitmap grayBitmap = twoGray(bm);
                int width = bm.getWidth();
                int height = bm.getHeight();
                int color,gray,average;
                int r, g, b, a = 0;

                Bitmap bmp = Bitmap.createBitmap(width, height
                        , Bitmap.Config.ARGB_8888);



                int[] oldPx = new int[width * height];
                int[] newPx = new int[width * height];
                grayBitmap.getPixels(oldPx, 0, width, 0, 0, width, height);

                String str = mSingleThresholdEt.getText().toString();
                if("".equals(str)) {
                    T = 0;
                } else {
                    T = Integer.valueOf(str);
                }

                for (int i = 1; i < height - 1; i++) {
                    for(int j = 1;j < width - 1;j++) {
                        color = oldPx[i*width+j];
                        r = Color.red(color);
                        a = Color.alpha(color);
                        average = 0;
                        average = (int)((Color.red(oldPx[(i-1)*width+j-1]) + Color.red(oldPx[(i-1)*width+j]) + Color.red(oldPx[(i-1)*width+j+1])
                                + Color.red(oldPx[(i)*width+j-1]) + Color.red(oldPx[(i)*width+j+1]) + Color.red(oldPx[(i+1)*width+j-1])
                                + Color.red(oldPx[(i+1)*width+j]) + Color.red(oldPx[(i+1)*width+j+1])) / 8);

                        if(Math.abs(r - average) > T) {
                            r = average;
                        }
                        newPx[i*width+j] = Color.argb(a,r,r,r);
                    }
                }
                bmp.setPixels(newPx, 0, width, 0, 0, width, height);
                mImageView.setImageBitmap(bmp);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * n*n均值滤波法
     * @param bm
     *
     */
    private void averagenn(final Bitmap bm) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("n*n均值滤波法");
        builder.setMessage("请输入n(奇数)");
        View view1 = LayoutInflater.from(getContext()).inflate(R.layout.threshold,null);
        final EditText mSingleThresholdEt = view1.findViewById(R.id.digit_dialog);
        builder.setView(view1);

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                Bitmap grayBitmap = twoGray(bm);
                int width = bm.getWidth();
                int height = bm.getHeight();
                int color,gray,average;
                int r, g, b, a = 0;
                int xx,yy,n2 = 0,sum;

                Bitmap bmp = Bitmap.createBitmap(width, height
                        , Bitmap.Config.ARGB_8888);



                int[] oldPx = new int[width * height];
                int[] newPx = new int[width * height];
                grayBitmap.getPixels(oldPx, 0, width, 0, 0, width, height);

                String str = mSingleThresholdEt.getText().toString();
                if("".equals(str)) {
                    n = 0;
                } else {
                    n = Integer.valueOf(str);
                }

                if(n < 3 || n % 2 != 1) {
                    return;
                }

                if(n >= 3 && n % 2 == 1) {
                    n2 = (n - 1) / 2;
                }


                for (int j = n2;j < height - n2;j++) {
                    for(int k = n2;k < width - n2;k++) {
                        color = oldPx[j*width+k];
                        a = Color.alpha(color);
                        sum = 0;
                        for(yy = j - n2;yy <= j + n2;yy++) {
                            for(xx = k - n2;xx <= k + n2;xx++) {
                                sum += Color.red(oldPx[yy * width + xx]);
                            }
                        }
                        r = (int)((float)sum/(n*n) + 0.5f);
                        //Log.d("xyz","sum = " + sum + "    i = " + j*width+k);
                        newPx[j*width+k] = Color.argb(a,r,r,r);
                    }
                }
                bmp.setPixels(newPx, 0, width, 0, 0, width, height);
                mImageView.setImageBitmap(bmp);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * 3*3均值滤波法
     * @param bm
     */
    private void average33(Bitmap bm) {
        Bitmap grayBitmap = twoGray(bm);
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color,gray,average;
        int r, g, b, a = 0;

        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);



        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        grayBitmap.getPixels(oldPx, 0, width, 0, 0, width, height);

        for (int i = 1; i < height - 1; i++) {
            for(int j = 1;j < width - 1;j++) {
                color = oldPx[i*width+j];
                a = Color.alpha(color);
                average = 0;
                average = (int)((Color.red(oldPx[(i-1)*width+j-1]) + Color.red(oldPx[(i-1)*width+j]) + Color.red(oldPx[(i-1)*width+j+1])
                        + Color.red(oldPx[(i)*width+j-1]) + Color.red(oldPx[(i)*width+j+1]) + Color.red(oldPx[(i+1)*width+j-1])
                        + Color.red(oldPx[(i+1)*width+j]) + Color.red(oldPx[(i+1)*width+j+1])) / 8);
                newPx[i*width+j] = Color.argb(a,average,average,average);
            }
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    /**
     * 消除孤立黑像素点
     * @param bm
     * @param count
     */
    private void isolatedBlack(Bitmap bm, int count) {
        Bitmap grayBitmap = twoGray(bm);
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color,gray,average;
        int r, g, b, a = 0;

        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);



        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        grayBitmap.getPixels(oldPx, 0, width, 0, 0, width, height);

        if(count == 4) {
            for(int i = 1;i < height - 1;i++) {
                for (int j = 1;j < width;j++) {
                    color = oldPx[i*width+j];
                    r = Color.red(color);//已经进行了二值化，因此r,g,b分量都等于灰度值
                    a = Color.alpha(color);
                    if(r == 255)
                        continue;
                    if(Color.red(oldPx[(i-1)*width+j]) + Color.red(oldPx[(i)*width+j-1]) + Color.red(oldPx[(i)*width+j+1])
                            + Color.red(oldPx[(i+1)*width+j]) == 255 * 4) {
                        r = 255;
                    }
                    newPx[i*width+j] = Color.argb(a,r,r,r);
                }
            }
        }

        if(count == 8) {
            for(int i = 1;i < height - 1;i++) {
                for (int j = 1;j < width;j++) {
                    color = oldPx[i*width+j];
                    r = Color.red(color);//已经进行了二值化，因此r,g,b分量都等于灰度值
                    a = Color.alpha(color);
                    if(r == 255)
                        continue;
                    if((Color.red(oldPx[(i-1)*width+j-1]) + Color.red(oldPx[(i-1)*width+j]) + Color.red(oldPx[(i-1)*width+j+1])
                            + Color.red(oldPx[(i)*width+j-1]) + Color.red(oldPx[(i)*width+j+1]) + Color.red(oldPx[(i+1)*width+j-1])
                            + Color.red(oldPx[(i+1)*width+j]) + Color.red(oldPx[(i+1)*width+j+1])) == 255 * 8) {
                        r = 255;
                    }
                    newPx[i*width+j] = Color.argb(a,r,r,r);
                }
            }
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);

    }

    /**
     * 二值图像的黑白点噪声滤波
     * @param bm
     */
    private void twoGrayWhiteAndBlack(Bitmap bm) {
        Bitmap grayBitmap = twoGray(bm);
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color,gray,average;
        int r, g, b, a = 0;

        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);



        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        grayBitmap.getPixels(oldPx, 0, width, 0, 0, width, height);

        for (int i = 1; i < height - 1; i++) {
            for(int j = 1;j < width - 1;j++) {
                color = oldPx[i*width+j];
                r = Color.red(color);//已经进行了二值化，因此r,g,b分量都等于灰度值
                a = Color.alpha(color);
                average = 0;
                average = (int)((Color.red(oldPx[(i-1)*width+j-1]) + Color.red(oldPx[(i-1)*width+j]) + Color.red(oldPx[(i-1)*width+j+1])
                        + Color.red(oldPx[(i)*width+j-1]) + Color.red(oldPx[(i)*width+j+1]) + Color.red(oldPx[(i+1)*width+j-1])
                        + Color.red(oldPx[(i+1)*width+j]) + Color.red(oldPx[(i+1)*width+j+1])) / 8);
                //Log.d("xyz","average = " + average + "    i = " + i*width+j);
                if(Math.abs(r - average) > 127.5) {
                    r = average;
                }
                newPx[i*width+j] = Color.argb(a,r,r,r);
            }
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    /**
     * 灰度处理
     * @param bm
     * @return
     */
    private Bitmap twoGray(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color;
        int r, g, b, a;

        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);
            a = Color.alpha(color);

            int gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

            if(gray > 100) {
                gray = 255;
            } else {
                gray = 0;
            }

            newPx[i] = Color.argb(a,gray,gray,gray);
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);

        return bmp;
    }

    /**
     * 幂次变换
     * @param bm
     */
    private void power(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color;
        int r, g, b, a;

        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);
            a = Color.alpha(color);

            int gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);
            gray = (int)(1.0f * Math.pow(gray/255.0f,1.7f) * 255 + 20);
            if(gray < 0) {
                gray = 0;
            } else if(gray > 255) {
                gray = 255;
            }

            newPx[i] = Color.argb(a,gray,gray,gray);
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    /**
     * 指数变换
     * @param bm
     */
    private void exponetial(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int color;
        int r, g, b, a;

        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];
        int[] newPx = new int[width * height];
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {
            color = oldPx[i];
            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);
            a = Color.alpha(color);

            int gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);
            gray = (int)(Math.pow(1.5f,0.065f * (gray - 0)) - 1.0f);
            if(gray < 0) {
                gray = 0;
            } else if(gray > 255) {
                gray = 255;
            }

            newPx[i] = Color.argb(a,gray,gray,gray);
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        mImageView.setImageBitmap(bmp);
    }

    /**
     * 对数变换
     * @param bm
     */
    private Bitmap logarithm(Bitmap bm) {
        int width = bm.getWidth();//原图像宽度
        int height = bm.getHeight();//原图高度
        int color;//用来存储某个像素点的颜色值
        int gray;//用来存储计算得到的灰度值
        int r, g, b, a;//红，绿，蓝，透明度
        //创建空白图像，宽度等于原图宽度，高度等于原图高度，用ARGB_8888渲染，这个不用了解，这样写就行了
        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];//用来存储原图每个像素点的颜色信息
        int[] newPx = new int[width * height];//用来处理处理之后的每个像素点的颜色信息
        /**
         * 第一个参数oldPix[]:用来接收（存储）bm这个图像中像素点颜色信息的数组//The array to receive the bitmap’s colors
         * 第二个参数offset:oldPix[]数组中第一个接收颜色信息的下标值// The first index to write into pixels[]
         * 第三个参数width:在行之间跳过像素的条目数，必须大于等于图像每行的像素数//The number of entries in pixels[] to skip between rows (must be >= bitmap’s width). Can be negative.
         * 第四个参数x:从图像bm中读取的第一个像素的横坐标 The x coordinate of the first pixel to read from the bitmap
         * 第五个参数y:从图像bm中读取的第一个像素的纵坐标The y coordinate of the first pixel to read from the bitmap
         * 第六个参数width:每行需要读取的像素个数The number of pixels to read from each row
         * 第七个参数height:需要读取的行总数The number of rows to read
         */
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {//循环处理图像中每个像素点的颜色值
            color = oldPx[i];//取得某个点的像素值
            r = Color.red(color);//取得此像素点的r(红色)分量
            g = Color.green(color);//取得此像素点的g(绿色)分量
            b = Color.blue(color);//取得此像素点的b(蓝色分量)
            a = Color.alpha(color);//取得此像素点的a通道值
            //此公式将r,g,b运算获得灰度值，经验公式不需要理解
            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

            gray = (int)(Math.log((float)gray + 1.0f)/0.025f + 10.0f);
            //溢出处理
            if(gray < 0) {
                gray = 0;
            } else if(gray > 255) {
                gray = 255;
            }

            newPx[i] = Color.argb(a,gray,gray,gray);
        }
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);//将处理后的透明度（没变），r,g,b分量重新合成颜色值并将其存储在数组中
        return bmp;//返回处理后的图像
    }

    /**
     * 分段线性
     * @param bm
     */
    private void partLinearity(final Bitmap bm) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());//创建AlertDialog.Builder
        builder.setTitle("分段线性变换");//对话框标题
        builder.setMessage("灰度拉伸参数");//对话框内容
        View view1 = LayoutInflater.from(getContext()).inflate(R.layout.dialog_part_linearity_layout,null);//载入自定义布局
        final EditText mFirstXEt = view1.findViewById(R.id.first_x_dialog);//自定义布局中的EditText，用于接收用户输入的第一个点横坐标
        final EditText mFirstYEt = view1.findViewById(R.id.first_y_dialog);//自定义布局中的EditText，用于接收用户输入的第一个点纵坐标
        final EditText mSecondtXEt = view1.findViewById(R.id.second_x_dialog);//自定义布局中的EditText，用于接收用户输入的第二个点横坐标
        final EditText mSecondYEt = view1.findViewById(R.id.second_y_dialog);//自定义布局中的EditText，用于接收用户输入的第二个点纵坐标
        builder.setView(view1);//将布局设置到对话框中


        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {//对话框的确定按钮点击事件
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                int width = bm.getWidth();//原图像宽度
                int height = bm.getHeight();//原图高度
                int color;//用来存储某个像素点的颜色值
                int r, g, b, a;//红，绿，蓝，透明度
                //创建空白图像，宽度等于原图宽度，高度等于原图高度，用ARGB_8888渲染，这个不用了解，这样写就行了
                Bitmap bmp = Bitmap.createBitmap(width, height
                        , Bitmap.Config.ARGB_8888);

                int[] oldPx = new int[width * height];//用来存储原图每个像素点的颜色信息
                int[] newPx = new int[width * height];//用来处理处理之后的每个像素点的颜色信息
                /**
                 * 第一个参数oldPix[]:用来接收（存储）bm这个图像中像素点颜色信息的数组//The array to receive the bitmap’s colors
                 * 第二个参数offset:oldPix[]数组中第一个接收颜色信息的下标值// The first index to write into pixels[]
                 * 第三个参数width:在行之间跳过像素的条目数，必须大于等于图像每行的像素数//The number of entries in pixels[] to skip between rows (must be >= bitmap’s width). Can be negative.
                 * 第四个参数x:从图像bm中读取的第一个像素的横坐标 The x coordinate of the first pixel to read from the bitmap
                 * 第五个参数y:从图像bm中读取的第一个像素的纵坐标The y coordinate of the first pixel to read from the bitmap
                 * 第六个参数width:每行需要读取的像素个数The number of pixels to read from each row
                 * 第七个参数height:需要读取的行总数The number of rows to read
                 */
                bm.getPixels(oldPx, 0, width, 0, 0, width, height);

                String str1 = mFirstXEt.getText().toString();//获取用户第一个点横坐标输入的内容
                String str2 = mFirstYEt.getText().toString();//获取用户第一个点纵坐标输入的内容
                String str3 = mSecondtXEt.getText().toString();//获取用户第二个点横坐标输入的内容
                String str4 = mSecondYEt.getText().toString();//获取用户第二个点纵坐标输入的内容
                if("".equals(str1)) {//如果用户输入的第一个点横坐标为空
                    firstX = 1;//将用户输入的第一个横坐标置为1
                } else {//否则
                    firstX = Integer.valueOf(str1);//将用户输入的第一个点横坐标转换为整数
                }

                if("".equals(str2)) {//如果用户输入的第一个点纵坐标为空
                    firstY = 1;//将用户输入的第一个横坐标置为1
                } else {//否则
                    firstY = Integer.valueOf(str2);//将用户输入的第一个点纵坐标转换为整数
                }

                if("".equals(str3)) {//如果用户输入的第二个点横坐标为空
                    secondX = 255;//将用户输入的第二个横坐标置为255
                } else {//否则
                    secondX = Integer.valueOf(str1);//将用户输入的第二个点横坐标转换为整数
                }

                if("".equals(str4)) {//如果用户输入的第二个点纵坐标为空
                    firstY = 255;//将用户输入的第二个纵坐标置为255
                } else {//否则
                    firstY = Integer.valueOf(str4);//将用户输入的第二个点纵坐标转换为整数
                }

                for (int j = 0; j < width * height; j++) {//循环处理图像中每个像素点的颜色值
                    color = oldPx[j];//取得某个点的像素值
                    r = Color.red(color);//取得此像素点的r(红色)分量
                    g = Color.green(color);//取得此像素点的g(绿色)分量
                    b = Color.blue(color);//取得此像素点的b(蓝色分量)
                    a = Color.alpha(color);//取得此像素点的a通道值
                    //此公式将r,g,b运算获得灰度值，经验公式不需要理解
                    int gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

                    //防止出现分母为0
                    if(firstX == 0) {
                        firstX = 1;
                    }
                    if(firstX == secondX) {
                        secondX = secondX + 1;
                    }
                    if(secondX == 255) {
                        secondX = 254;
                    }

                    if(gray > 0 && gray < firstX) {
                        gray = (int)(gray * firstY / firstX);
                    } else if(gray > firstX && gray < secondX) {
                        gray = (int)((secondY - firstY)*(gray - firstX) / (secondX -firstX) + firstY);
                    } else {
                        gray = (int)((255 - secondY) * (gray - secondX) / (255 - secondX) + secondY);
                    }
                    newPx[j] = Color.argb(a,gray,gray,gray);//将处理后的透明度（没变），r,g,b分量重新合成颜色值并将其存储在数组中
                }
                /**
                 * 第一个参数newPix[]:需要赋给新图像的颜色数组//The colors to write the bitmap
                 * 第二个参数offset:newPix[]数组中第一个需要设置给图像颜色的下标值//The index of the first color to read from pixels[]
                 * 第三个参数width:在行之间跳过像素的条目数//The number of colors in pixels[] to skip between rows.
                 * Normally this value will be the same as the width of the bitmap,but it can be larger(or negative).
                 * 第四个参数x:从图像bm中读取的第一个像素的横坐标//The x coordinate of the first pixels to write to in the bitmap.
                 * 第五个参数y:从图像bm中读取的第一个像素的纵坐标//The y coordinate of the first pixels to write to in the bitmap.
                 * 第六个参数width:每行需要读取的像素个数The number of colors to copy from pixels[] per row.
                 * 第七个参数height:需要读取的行总数//The number of rows to write to the bitmap.
                 */
                bmp.setPixels(newPx, 0, width, 0, 0, width, height);
                mImageView.setImageBitmap(bmp);//将处理后的新图赋给ImageView
            }
        });


        AlertDialog dialog = builder.create();//创建AlertDialog对话框
        dialog.show();//显示AlertDialog对话框
    }

    /**
     * 窗口变换
     * @param bm
     */
    private void window(final Bitmap bm) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());//创建AlertDialog.Builder
        builder.setTitle("窗口灰度变换");//对话框标题
        builder.setMessage("请输入上下限值");//对话框内容
        View view1 = LayoutInflater.from(getContext()).inflate(R.layout.double_threshold_layout,null);//载入自定义布局
        final EditText mLowThresholdEt = view1.findViewById(R.id.low_digit_dialog);//自定义布局中的EditText，用于接收用户输入的下限值
        final EditText mHighThresholdEt = view1.findViewById(R.id.heigh_digit_dialog);//自定义布局中的EditText，用于接收用户输入的上限值
        builder.setView(view1);//将布局设置到对话框中


        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {//对话框的确定按钮点击事件
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                int width = bm.getWidth();//原图像宽度
                int height = bm.getHeight();//原图高度
                int color;//用来存储某个像素点的颜色值
                int r, g, b, a;//红，绿，蓝，透明度
                int gray;//用来存储计算得到的灰度值
                int lowDigit = 0;//用于存储用户在对话框中输入的下限值
                int highDigit = 255;//用于存储用户在对话框中输入的上限值
                //创建空白图像，宽度等于原图宽度，高度等于原图高度，用ARGB_8888渲染，这个不用了解，这样写就行了
                Bitmap bmp = Bitmap.createBitmap(width, height
                        , Bitmap.Config.ARGB_8888);

                int[] oldPx = new int[width * height];//用来存储原图每个像素点的颜色信息
                int[] newPx = new int[width * height];//用来处理处理之后的每个像素点的颜色信息
                /**
                 * 第一个参数oldPix[]:用来接收（存储）bm这个图像中像素点颜色信息的数组//The array to receive the bitmap’s colors
                 * 第二个参数offset:oldPix[]数组中第一个接收颜色信息的下标值// The first index to write into pixels[]
                 * 第三个参数width:在行之间跳过像素的条目数，必须大于等于图像每行的像素数//The number of entries in pixels[] to skip between rows (must be >= bitmap’s width). Can be negative.
                 * 第四个参数x:从图像bm中读取的第一个像素的横坐标 The x coordinate of the first pixel to read from the bitmap
                 * 第五个参数y:从图像bm中读取的第一个像素的纵坐标The y coordinate of the first pixel to read from the bitmap
                 * 第六个参数width:每行需要读取的像素个数The number of pixels to read from each row
                 * 第七个参数height:需要读取的行总数The number of rows to read
                 */
                bm.getPixels(oldPx, 0, width, 0, 0, width, height);


                String str1 = mLowThresholdEt.getText().toString();//获取用户下限值输入的内容
                String str2 = mHighThresholdEt.getText().toString();//获取用户上限值输入的内容
                if("".equals(str1)) {//如果用户输入的内容为空
                    lowDigit = 0;//将用户输入的下限值置为0
                } else {//否则
                    lowDigit = Integer.valueOf(str1);//将用户输入的下限值转换为整数
                }

                if("".equals(str2)) {//如果用户输入的内容为空
                    highDigit = 255;//将用户输入的上限值置为255
                } else {//否则
                    highDigit = Integer.valueOf(str2);//将用户输入的下限值转换为整数
                }


                for (int j = 0; j < width * height; j++) {//循环处理图像中每个像素点的颜色值
                    color = oldPx[j];//取得某个点的像素值
                    r = Color.red(color);//取得此像素点的r(红色)分量
                    g = Color.green(color);//取得此像素点的g(绿色)分量
                    b = Color.blue(color);//取得此像素点的b(蓝色分量)
                    a = Color.alpha(color);//取得此像素点的a通道值
                    //此公式将r,g,b运算获得灰度值，经验公式不需要理解
                    gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

                    if(gray < lowDigit) {//如果某点灰度值小于下限值
                        gray = 0;//将此点灰度值置为0
                    } else if(gray > highDigit){//如果某点灰度值大于上限值
                        gray = 255;//将此点灰度值置为255
                    }
                    //如果某点灰度值位于上限值与下限值之间则不改变
                    newPx[j] = Color.argb(a,gray,gray,gray);//将处理后的透明度（没变），r,g,b分量重新合成颜色值并将其存储在数组中
                }
                /**
                 * 第一个参数newPix[]:需要赋给新图像的颜色数组//The colors to write the bitmap
                 * 第二个参数offset:newPix[]数组中第一个需要设置给图像颜色的下标值//The index of the first color to read from pixels[]
                 * 第三个参数width:在行之间跳过像素的条目数//The number of colors in pixels[] to skip between rows.
                 * Normally this value will be the same as the width of the bitmap,but it can be larger(or negative).
                 * 第四个参数x:从图像bm中读取的第一个像素的横坐标//The x coordinate of the first pixels to write to in the bitmap.
                 * 第五个参数y:从图像bm中读取的第一个像素的纵坐标//The y coordinate of the first pixels to write to in the bitmap.
                 * 第六个参数width:每行需要读取的像素个数The number of colors to copy from pixels[] per row.
                 * 第七个参数height:需要读取的行总数//The number of rows to write to the bitmap.
                 */
                bmp.setPixels(newPx, 0, width, 0, 0, width, height);
                mImageView.setImageBitmap(bmp);//将处理后的新图赋给ImageView
            }
        });


        AlertDialog dialog = builder.create();//创建AlertDialog对话框
        dialog.show();//显示AlertDialog对话框
    }

    /**
     * 反色变换
     * @param bm
     */
    private Bitmap contraty(Bitmap bm) {
        int width = bm.getWidth();//原图像宽度
        int height = bm.getHeight();//原图像高度
        int color;//用来存储某个像素点的颜色值
        int gray;//用来存储计算得到的灰度值
        int r, g, b, a;//红，绿，蓝，透明度
        //创建空白图像，宽度等于原图宽度，高度等于原图高度，用ARGB_8888渲染，这个不用了解，这样写就行了
        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];//用来存储原图每个像素点的颜色信息
        int[] newPx = new int[width * height];//用来处理处理之后的每个像素点的颜色信息
        /**
         * 第一个参数oldPix[]:用来接收（存储）bm这个图像中像素点颜色信息的数组//The array to receive the bitmap’s colors
         * 第二个参数offset:oldPix[]数组中第一个接收颜色信息的下标值// The first index to write into pixels[]
         * 第三个参数width:在行之间跳过像素的条目数，必须大于等于图像每行的像素数//The number of entries in pixels[] to skip between rows (must be >= bitmap’s width). Can be negative.
         * 第四个参数x:从图像bm中读取的第一个像素的横坐标 The x coordinate of the first pixel to read from the bitmap
         * 第五个参数y:从图像bm中读取的第一个像素的纵坐标The y coordinate of the first pixel to read from the bitmap
         * 第六个参数width:每行需要读取的像素个数The number of pixels to read from each row
         * 第七个参数height:需要读取的行总数The number of rows to read
         */
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {//循环处理图像中每个像素点的颜色值
            color = oldPx[i];//取得某个点的像素值
            r = Color.red(color);//取得此像素点的r(红色)分量
            g = Color.green(color);//取得此像素点的g(绿色)分量
            b = Color.blue(color);//取得此像素点的b(蓝色分量)
            a = Color.alpha(color);//取得此像素点的a通道值
            //此公式将r,g,b运算获得灰度值，经验公式不需要理解
            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

            gray = 255 -gray;//将该点灰度值取反，

            newPx[i] = Color.argb(a,gray,gray,gray);//将处理后的透明度（没变），r,g,b分量重新合成颜色值并将其存储在数组中
        }
        /**
         * 第一个参数newPix[]:需要赋给新图像的颜色数组//The colors to write the bitmap
         * 第二个参数offset:newPix[]数组中第一个需要设置给图像颜色的下标值//The index of the first color to read from pixels[]
         * 第三个参数width:在行之间跳过像素的条目数//The number of colors in pixels[] to skip between rows.
         * Normally this value will be the same as the width of the bitmap,but it can be larger(or negative).
         * 第四个参数x:从图像bm中读取的第一个像素的横坐标//The x coordinate of the first pixels to write to in the bitmap.
         * 第五个参数y:从图像bm中读取的第一个像素的纵坐标//The y coordinate of the first pixels to write to in the bitmap.
         * 第六个参数width:每行需要读取的像素个数The number of colors to copy from pixels[] per row.
         * 第七个参数height:需要读取的行总数//The number of rows to write to the bitmap.
         */
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);
        return bmp;
    }

    /**
     * 双固定阈值法
     * @param bm
     */
    private void doubleThreshold(final Bitmap bm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());//创建AlertDialog.Builder
        builder.setTitle("双固定阈值法(0-255-0)");//对话框标题
        builder.setMessage("请输入阈值");//对话框内容
        View view1 = LayoutInflater.from(getContext()).inflate(R.layout.double_threshold_layout,null);//载入自定义布局
        final EditText mLowThresholdEt = view1.findViewById(R.id.low_digit_dialog);//自定义布局中的EditText，用于接收用户输入的阈值下限
        final EditText mHighThresholdEt = view1.findViewById(R.id.heigh_digit_dialog);//自定义布局中的EditText，用于接收用户输入的阈值上限
        builder.setView(view1);//将布局设置到对话框中


        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {//对话框的确定按钮点击事件
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                int width = bm.getWidth();//原图像宽度
                int height = bm.getHeight();//原图高度
                int color;//用来存储某个像素点的颜色值
                int r, g, b, a;//红，绿，蓝，透明度
                int gray;//用来存储计算得到的灰度值
                int lowDigit = 0;//用于存储用户在对话框中输入的下限值
                int highDigit = 0;//用于存储用户在对话框中输入的上限值

                //创建空白图像，宽度等于原图宽度，高度等于原图高度，用ARGB_8888渲染，这个不用了解，这样写就行了
                Bitmap bmp = Bitmap.createBitmap(width, height
                        , Bitmap.Config.ARGB_8888);

                int[] oldPx = new int[width * height];//用来存储原图每个像素点的颜色信息
                int[] newPx = new int[width * height];//用来处理处理之后的每个像素点的颜色信息
                /**
                 * 第一个参数oldPix[]:用来接收（存储）bm这个图像中像素点颜色信息的数组
                 * 第二个参数offset:oldPix[]数组中第一个接收颜色信息的下标值
                 * 第三个参数width:在行之间跳过像素的条目数，必须大于等于图像每行的像素数
                 * 第四个参数x:从图像bm中读取的第一个像素的横坐标
                 * 第五个参数y:从图像bm中读取的第一个像素的纵坐标
                 * 第六个参数width:每行需要读取的像素个数
                 * 第七个参数height:需要读取的行总数
                 */
                bm.getPixels(oldPx, 0, width, 0, 0, width, height);

                String str1 = mLowThresholdEt.getText().toString();//获取用户阈值下限输入的内容
                String str2 = mHighThresholdEt.getText().toString();//获取用户阈值上限输入的内容
                if("".equals(str1)) {//如果用户输入的内容为空
                    lowDigit = 0;//将用户输入的阈值下限置为0
                } else {//否则
                    lowDigit = Integer.valueOf(str1);//将用户输入的阈值下限转换为整数
                }

                if("".equals(str2)) {
                    highDigit = 255;//将用户输入的阈值上限置为255
                } else {//否则
                    highDigit = Integer.valueOf(str2);//将用户输入的阈值上限转换为整数
                }

                for (int j = 0; j < width * height; j++) {//循环处理图像中每个像素点的颜色值
                    color = oldPx[j];//取得某个点的像素值
                    r = Color.red(color);//取得此像素点的r(红色)分量
                    g = Color.green(color);//取得此像素点的g(绿色)分量
                    b = Color.blue(color);//取得此像素点的b(蓝色分量)
                    a = Color.alpha(color);//取得此像素点的a通道值
                    //此公式将r,g,b运算获得灰度值，经验公式不需要理解
                    gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

                    if(gray < lowDigit || gray > highDigit) {//如果某点像素的灰度值小于阈值下限或大于阈值上限
                        gray = 0;//将该点灰度值置为0（黑色）
                    } else {//如果某点像素的灰度值大于等于阈值下限或小于等于阈值上限
                        gray = 255;//将该点灰度值置为255（白色）
                    }
                    newPx[j] = Color.argb(a,gray,gray,gray);//将处理后的透明度（没变），r,g,b分量重新合成颜色值并将其存储在数组中
                }
                /**
                 * 第一个参数newPix[]:需要赋给新图像的颜色数组//The colors to write the bitmap
                 * 第二个参数offset:newPix[]数组中第一个需要设置给图像颜色的下标值//The index of the first color to read from pixels[]
                 * 第三个参数width:在行之间跳过像素的条目数//The number of colors in pixels[] to skip between rows.
                 * Normally this value will be the same as the width of the bitmap,but it can be larger(or negative).
                 * 第四个参数x:从图像bm中读取的第一个像素的横坐标//The x coordinate of the first pixels to write to in the bitmap.
                 * 第五个参数y:从图像bm中读取的第一个像素的纵坐标//The y coordinate of the first pixels to write to in the bitmap.
                 * 第六个参数width:每行需要读取的像素个数The number of colors to copy from pixels[] per row.
                 * 第七个参数height:需要读取的行总数//The number of rows to write to the bitmap.
                 */
                bmp.setPixels(newPx, 0, width, 0, 0, width, height);//将处理后的像素信息赋给新图
                mImageView.setImageBitmap(bmp);//将处理后的新图赋给ImageView
            }
        });


        AlertDialog dialog = builder.create();//创建AlertDialog对话框
        dialog.show();//显示AlertDialog对话框
    }

    /**
     * 固定阈值法
     * @param bm
     */
    private void singleThreshold(final Bitmap bm) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());//创建AlertDialog.Builder
        builder.setTitle("固定阈值法");//对话框标题
        builder.setMessage("请输入阈值");//对话框内容
        View view1 = LayoutInflater.from(getContext()).inflate(R.layout.threshold,null);//载入自定义布局
        final EditText mSingleThresholdEt = view1.findViewById(R.id.digit_dialog);//自定义布局中的EditText，用于接收用户输入
        builder.setView(view1);//将布局设置到对话框中


        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {//对话框的确定按钮点击事件
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                int width = bm.getWidth();//原图像宽度
                int height = bm.getHeight();//原图高度
                int color;//用来存储某个像素点的颜色值
                int r, g, b, a;//红，绿，蓝，透明度
                int digit = 0;//用于存储用户在对话框中输入的阈值
                //创建空白图像，宽度等于原图宽度，高度等于原图高度，用ARGB_8888渲染，这个不用了解，这样写就行了
                Bitmap bmp = Bitmap.createBitmap(width, height
                        , Bitmap.Config.ARGB_8888);

                int[] oldPx = new int[width * height];//用来存储原图每个像素点的颜色信息
                int[] newPx = new int[width * height];//用来处理处理之后的每个像素点的颜色信息
                /**
                 * 第一个参数oldPix[]:用来接收（存储）bm这个图像中像素点颜色信息的数组//The array to receive the bitmap’s colors
                 * 第二个参数offset:oldPix[]数组中第一个接收颜色信息的下标值// The first index to write into pixels[]
                 * 第三个参数width:在行之间跳过像素的条目数，必须大于等于图像每行的像素数//The number of entries in pixels[] to skip between rows (must be >= bitmap’s width). Can be negative.
                 * 第四个参数x:从图像bm中读取的第一个像素的横坐标 The x coordinate of the first pixel to read from the bitmap
                 * 第五个参数y:从图像bm中读取的第一个像素的纵坐标The y coordinate of the first pixel to read from the bitmap
                 * 第六个参数width:每行需要读取的像素个数The number of pixels to read from each row
                 * 第七个参数height:需要读取的行总数The number of rows to read
                 */
                bm.getPixels(oldPx, 0, width, 0, 0, width, height);//获取原图中的像素信息

                String str = mSingleThresholdEt.getText().toString();//获取用户输入的内容

                if("".equals(str)) {//如果用户输入的内容为空
                    digit = 0;//将阈值置为0
                } else {//否则
                    digit = Integer.valueOf(str);//将用户输入的阈值转换为整数
                }

                for (int j = 0; j < width * height; j++) {//循环处理图像中每个像素点的颜色值
                    color = oldPx[j];//取得某个点的像素值
                    r = Color.red(color);//取得此像素点的r(红色)分量
                    g = Color.green(color);//取得此像素点的g(绿色)分量
                    b = Color.blue(color);//取得此像素点的b(蓝色分量)
                    a = Color.alpha(color);//取得此像素点的a通道值

                    //此公式将r,g,b运算获得灰度值，经验公式不需要理解
                    int gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);

                    if(gray < digit) {//如果某点像素灰度小于给定阈值
                        gray = 0;//将该点像素的灰度值置为0（黑色）
                    } else {//如果某点像素灰度大于或等于给定阈值
                        gray = 255;//将该点像素的灰度值置为1（白色）
                    }
                    newPx[j] = Color.argb(a,gray,gray,gray);//将处理后的透明度（没变），r,g,b分量重新合成颜色值并将其存储在数组中
                }
                /**
                 * 第一个参数newPix[]:需要赋给新图像的颜色数组//The colors to write the bitmap
                 * 第二个参数offset:newPix[]数组中第一个需要设置给图像颜色的下标值//The index of the first color to read from pixels[]
                 * 第三个参数width:在行之间跳过像素的条目数//The number of colors in pixels[] to skip between rows.
                 * Normally this value will be the same as the width of the bitmap,but it can be larger(or negative).
                 * 第四个参数x:从图像bm中读取的第一个像素的横坐标//The x coordinate of the first pixels to write to in the bitmap.
                 * 第五个参数y:从图像bm中读取的第一个像素的纵坐标//The y coordinate of the first pixels to write to in the bitmap.
                 * 第六个参数width:每行需要读取的像素个数The number of colors to copy from pixels[] per row.
                 * 第七个参数height:需要读取的行总数//The number of rows to write to the bitmap.
                 */
                bmp.setPixels(newPx, 0, width, 0, 0, width, height);//将处理后的像素信息赋给新图
                mImageView.setImageBitmap(bmp);//将处理后的新图赋给ImageView
            }
        });


        AlertDialog dialog = builder.create();//创建AlertDialog对话框
        dialog.show();//显示AlertDialog对话框
    }

    /**
     * 非0即1法
     * @param bm
     */
    private Bitmap zeroAndOne(Bitmap bm) {
        int width = bm.getWidth();//原图像宽度
        int height = bm.getHeight();//原图像高度
        int color;//用来存储某个像素点的颜色值
        int r, g, b, a;//红，绿，蓝，透明度
        int gray;//用来存储计算得到的灰度值
        //创建空白图像，宽度等于原图宽度，高度等于原图高度，用ARGB_8888渲染，这个不用了解，这样写就行了
        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];//用来存储原图每个像素点的颜色信息
        int[] newPx = new int[width * height];//用来处理处理之后的每个像素点的颜色信息
        /**
         * 第一个参数oldPix[]:用来接收（存储）bm这个图像中像素点颜色信息的数组//The array to receive the bitmap’s colors
         * 第二个参数offset:oldPix[]数组中第一个接收颜色信息的下标值// The first index to write into pixels[]
         * 第三个参数width:在行之间跳过像素的条目数，必须大于等于图像每行的像素数//The number of entries in pixels[] to skip between rows (must be >= bitmap’s width). Can be negative.
         * 第四个参数x:从图像bm中读取的第一个像素的横坐标 The x coordinate of the first pixel to read from the bitmap
         * 第五个参数y:从图像bm中读取的第一个像素的纵坐标The y coordinate of the first pixel to read from the bitmap
         * 第六个参数width:每行需要读取的像素个数The number of pixels to read from each row
         * 第七个参数height:需要读取的行总数The number of rows to read
         */
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);//获取原图中的像素信息

        for (int i = 0; i < width * height; i++) {//循环处理图像中每个像素点的颜色值
            color = oldPx[i];//取得某个点的像素值
            r = Color.red(color);//取得此像素点的r(红色)分量
            g = Color.green(color);//取得此像素点的g(绿色)分量
            b = Color.blue(color);//取得此像素点的b(蓝色分量)
            a = Color.alpha(color);//取得此像素点的a通道值

            //此公式将r,g,b运算获得灰度值，经验公式不需要理解
            gray = (int)((float)r*0.3+(float)g*0.59+(float)b*0.11);
            //下面前两个if用来做溢出处理，防止灰度公式得到到灰度超出范围（0-255）
            if(gray > 255) {
                gray = 255;
            }

            if(gray < 0) {
                gray = 0;
            }

            if (gray != 0) {//如果某像素的灰度值不是0(黑色)就将其置为255（白色）
                gray = 255;
            }

            newPx[i] = Color.argb(a,gray,gray,gray);//将处理后的透明度（没变），r,g,b分量重新合成颜色值并将其存储在数组中
        }
        /**
         * 第一个参数newPix[]:需要赋给新图像的颜色数组//The colors to write the bitmap
         * 第二个参数offset:newPix[]数组中第一个需要设置给图像颜色的下标值//The index of the first color to read from pixels[]
         * 第三个参数width:在行之间跳过像素的条目数//The number of colors in pixels[] to skip between rows.
         * Normally this value will be the same as the width of the bitmap,but it can be larger(or negative).
         * 第四个参数x:从图像bm中读取的第一个像素的横坐标//The x coordinate of the first pixels to write to in the bitmap.
         * 第五个参数y:从图像bm中读取的第一个像素的纵坐标//The y coordinate of the first pixels to write to in the bitmap.
         * 第六个参数width:每行需要读取的像素个数The number of colors to copy from pixels[] per row.
         * 第七个参数height:需要读取的行总数//The number of rows to write to the bitmap.
         */
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);//将处理后的像素信息赋给新图
        return bmp;//返回处理后的图像
    }

    /**
     * 转置
     * @param mImageView
     */
    private void transposition(ImageView mImageView) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(mImageView,"scaleX",1f,-1f);
        ObjectAnimator animator1 = ObjectAnimator.ofFloat(mImageView,"scaleY",1f,-1f);
        AnimatorSet set = new AnimatorSet();
        set.setDuration(3000);
        set.playTogether(animator,animator1);
        set.start();
    }

    /**
     * 镜像
     * @param mImageView
     */
    private void mirror(final ImageView mImageView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("镜像");
        builder.setMessage("请选择镜像方向");
        View view1 = LayoutInflater.from(getContext()).inflate(R.layout.dialog_mirror_layout,null);
        mHorizontalBtn = view1.findViewById(R.id.horizontal_radio_btn_dialog);
        mVerticalBtn = view1.findViewById(R.id.vertical_radio_btn_dialog);

        builder.setView(view1);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                if( mHorizontalBtn.isChecked()) {
                    Animator animator = ObjectAnimator.ofFloat(mImageView,"scaleX",1f,-1f);
                    animator.setDuration(3000);
                    animator.start();
                } else if(mVerticalBtn.isChecked()) {
                    Animator animator = ObjectAnimator.ofFloat(mImageView,"scaleY",1f,-1f);
                    animator.setDuration(3000);
                    animator.start();
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }



    /**
     * 缩放
     * @param mImageView
     */
    private void scale(final ImageView mImageView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("缩放");
        builder.setMessage("请输入缩放比例");
        View view1 = LayoutInflater.from(getContext()).inflate(R.layout.dialog_layout,null);
        mHorizontalEt = view1.findViewById(R.id.horizontal_edit_text_dialog);
        mVerticalEt = view1.findViewById(R.id.vertical_edit_text_dialog);

        builder.setView(view1);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String strX = mHorizontalEt.getText().toString();
                String strY = mVerticalEt.getText().toString();
                if("".equals(strX)) {
                    scaleX = 0;
                } else {
                    scaleX = Float.valueOf(strX);
                }
                if("".equals(strY)) {
                    scaleY = 0;
                } else {
                    scaleY = Float.valueOf(strY);
                }
                ObjectAnimator animator = ObjectAnimator.ofFloat(mImageView,"scaleX",1f,scaleX);
                ObjectAnimator animator1 = ObjectAnimator.ofFloat(mImageView,"scaleY",1f,scaleY);
                AnimatorSet set = new AnimatorSet();
                set.setDuration(3000);
                set.playTogether(animator,animator1);
                set.start();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * 旋转
     * @param mImageView
     */
    private void rotation(final ImageView mImageView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("旋转");
        builder.setMessage("请输入旋转角度");
        View view1 = LayoutInflater.from(getContext()).inflate(R.layout.dialog_rotation_layout,null);
        mRotationEt = view1.findViewById(R.id.rotation_edit_text_dialog);

        builder.setView(view1);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String str = mRotationEt.getText().toString();
                if("".equals(str)) {
                    rotationAngles = 0;
                } else {
                    rotationAngles = Float.valueOf(str);
                }
                ObjectAnimator animator = ObjectAnimator.ofFloat(mImageView,"rotation",1f,rotationAngles);
                animator.setDuration(3000);
                animator.start();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * 平移
     * @param view
     */
    private void translation(final View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("平移");
        builder.setMessage("请输入平移距离（像素）");
        View view1 = LayoutInflater.from(getContext()).inflate(R.layout.dialog_layout,null);
        mHorizontalEt = view1.findViewById(R.id.horizontal_edit_text_dialog);
        mVerticalEt = view1.findViewById(R.id.vertical_edit_text_dialog);

        builder.setView(view1);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String strX = mHorizontalEt.getText().toString();
                String strY = mVerticalEt.getText().toString();
                if("".equals(strX)) {
                    translationDistanceX = 0;
                } else {
                    translationDistanceX = Float.valueOf(strX);
                }
                if("".equals(strY)) {
                    translationDistanceY = 0;
                } else {
                    translationDistanceY = Float.valueOf(strY);
                }
                ObjectAnimator animator = ObjectAnimator.ofFloat(mImageView,"translationX",1f,translationDistanceX);
                ObjectAnimator animator1 = ObjectAnimator.ofFloat(mImageView,"translationY",1f,translationDistanceY);
                AnimatorSet set = new AnimatorSet();
                set.setDuration(3000);
                set.playTogether(animator,animator1);
                set.start();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void initData() {
        parentList = new ArrayList<>();
        map = new HashMap<>();

        parentList.add("几何变换");
        parentList.add("二值化");
        parentList.add("灰度变换");
        //parentList.add("锐化处理");
        parentList.add("噪声消除法");
        parentList.add("邻域平均法");
        parentList.add("中值滤波");
        parentList.add("产生噪声");
        parentList.add("微分运算");
        parentList.add("边缘检测");
        parentList.add("梯度锐化");
        parentList.add("计算");
        parentList.add("图像分割");
        parentList.add("边缘提取");
        parentList.add("粗化细化与中轴变换");
        parentList.add("腐蚀");
        parentList.add("膨胀");
        parentList.add("开启与闭合");


        childList = new ArrayList<>();
        bean = new ExpandableBean("平移", Constant.TRANSLATION);
        childList.add(bean);
        bean = new ExpandableBean("旋转",Constant.ROTATE);
        childList.add(bean);
        bean = new ExpandableBean("缩放",Constant.ALPH);
        childList.add(bean);
        bean = new ExpandableBean("镜像",Constant.MIRROR);
        childList.add(bean);
        bean = new ExpandableBean("转置",Constant.TRANSPOSITION);
        childList.add(bean);
        map.put("几何变换",childList);

        childList = new ArrayList<>();
        bean = new ExpandableBean("非0即1法",Constant.ZEROANDONE);
        childList.add(bean);
        bean = new ExpandableBean("固定阈值法",Constant.SINGLETHRESHOLD);
        childList.add(bean);
        bean = new ExpandableBean("双固定阈值法",Constant.DOUBLETHRESHOLD);
        childList.add(bean);
        map.put("二值化",childList);

        childList = new ArrayList<>();
        bean = new ExpandableBean("反色变换处理",Constant.CONTRARY);
        childList.add(bean);
        bean = new ExpandableBean("窗口变换处理",Constant.WINDOW);
        childList.add(bean);
        bean = new ExpandableBean("分段线性变换",Constant.PARTLINEARITY);
        childList.add(bean);
        bean = new ExpandableBean("灰度分布均衡化",Constant.BALANCE);
        childList.add(bean);
        bean = new ExpandableBean("灰度匹配变换",Constant.MATCH);
        childList.add(bean);
        bean = new ExpandableBean("灰度对数变换",Constant.LOGARITHM);
        childList.add(bean);
        bean = new ExpandableBean("灰度指数变换",Constant.EXPONETIAL);
        childList.add(bean);
        bean = new ExpandableBean("灰度幂次变换",Constant.POWER);
        childList.add(bean);
        map.put("灰度变换",childList);

        childList = new ArrayList<>();
        bean = new ExpandableBean("黑白点噪声滤波",Constant.TWOGRAYWHITEANDBLACKPOINT);
        childList.add(bean);
        bean = new ExpandableBean("消除孤立黑像素点",Constant.ISOLATEDBALCK);
        childList.add(bean);
        map.put("噪声消除法",childList);

        childList = new ArrayList<>();
        bean = new ExpandableBean("3*3均值滤波器",Constant.AVERAGE33);
        childList.add(bean);
        bean = new ExpandableBean("超限邻域平均法",Constant.OVERLIMITAVERAGE);
        childList.add(bean);
        bean = new ExpandableBean("n*n均值滤波器",Constant.AVERAGENN);
        childList.add(bean);
        map.put("邻域平均法",childList);

        childList = new ArrayList<>();
        bean = new ExpandableBean("n*n中值滤波器",Constant.MIDDLENN);
        childList.add(bean);
        bean = new ExpandableBean("十字型中值滤波",Constant.CROSS);
        childList.add(bean);
        bean = new ExpandableBean("n*n最大值滤波器",Constant.MAXNN);
        childList.add(bean);
        map.put("中值滤波",childList);

        childList = new ArrayList<>();
        bean = new ExpandableBean("随机噪声",Constant.RANDOM);
        childList.add(bean);
        bean = new ExpandableBean("椒盐噪声",Constant.SPICEDSALT);
        childList.add(bean);
        map.put("产生噪声",childList);

        childList = new ArrayList<>();
        bean = new ExpandableBean("纵向微分",Constant.VERTICALDIFFERENTIAL);
        childList.add(bean);
        bean = new ExpandableBean("横向微分",Constant.HORIZONTALDIFFERENTIAL);
        childList.add(bean);
        bean = new ExpandableBean("双方向一次微分",Constant.DOUBLEDIFFERENTIAL);
        childList.add(bean);
        map.put("微分运算",childList);

        childList = new ArrayList<>();
        bean = new ExpandableBean("门限锐化",Constant.THRESHOLDSHARPEN);
        childList.add(bean);
        bean = new ExpandableBean("固定锐化",Constant.FIXEDSHARPEN);
        childList.add(bean);
        bean = new ExpandableBean("二值锐化",Constant.GREY2BINARYSHAARPEN);
        childList.add(bean);
        map.put("梯度锐化",childList);

        childList = new ArrayList<>();
        bean = new ExpandableBean("Robert检测",Constant.ROBERT);
        childList.add(bean);
        bean = new ExpandableBean("Sobel检测",Constant.SOBEL);
        childList.add(bean);
        bean = new ExpandableBean("PreWitt检测",Constant.PREWITT);
        childList.add(bean);
        bean = new ExpandableBean("Krisch检测",Constant.KRISCH);
        childList.add(bean);
        bean = new ExpandableBean("Guasslaplacian检测",Constant.GUASSLAPLACION);
        childList.add(bean);
        map.put("边缘检测",childList);

        childList = new ArrayList<>();
        bean = new ExpandableBean("显示标号",Constant.TRANSLATION);
        childList.add(bean);
        bean = new ExpandableBean("面积计算",Constant.ROTATE);
        childList.add(bean);
        bean = new ExpandableBean("周长计算",Constant.ALPH);
        childList.add(bean);
        bean = new ExpandableBean("小区域消除",Constant.ALPH);
        childList.add(bean);
        map.put("计算",childList);

        childList = new ArrayList<>();
        bean = new ExpandableBean("迭代阀值",Constant.TRANSLATION);
        childList.add(bean);
        bean = new ExpandableBean("峰谷阈值分割",Constant.ROTATE);
        childList.add(bean);
        bean = new ExpandableBean("半阈值分割",Constant.ALPH);
        childList.add(bean);
        map.put("图像分割",childList);

        childList = new ArrayList<>();
        bean = new ExpandableBean("轮廓提取",Constant.OUTLINEEXTRACT);
        childList.add(bean);
        bean = new ExpandableBean("边界跟踪",Constant.OUTLINEFOLLOW);
        childList.add(bean);
        bean = new ExpandableBean("种子填充",Constant.ALPH);
        childList.add(bean);
        bean = new ExpandableBean("区域生长",Constant.ALPH);
        childList.add(bean);
        bean = new ExpandableBean("水平投影",Constant.HORIZONTALSHADOW);
        childList.add(bean);
        bean = new ExpandableBean("垂直投影",Constant.VERTICALSHADOW);
        childList.add(bean);
        map.put("边缘提取",childList);

        childList = new ArrayList<>();
        bean = new ExpandableBean("粗化处理",Constant.WIDEDISPOSE);
        childList.add(bean);
        bean = new ExpandableBean("细化处理",Constant.THINISPOSE);
        childList.add(bean);
        bean = new ExpandableBean("中轴变换",Constant.AXISVARY);
        childList.add(bean);
        map.put("粗化细化与中轴变换",childList);

        childList = new ArrayList<>();
        bean = new ExpandableBean("垂直腐蚀",Constant.VERTICALCORROSION);
        childList.add(bean);
        bean = new ExpandableBean("水平腐蚀",Constant.HORIZONTALCORROSION);
        childList.add(bean);
        bean = new ExpandableBean("全方位腐蚀",Constant.ALPH);
        childList.add(bean);
        map.put("腐蚀",childList);

        childList = new ArrayList<>();
        bean = new ExpandableBean("垂直膨胀",Constant.VERTICALEXPAND);
        childList.add(bean);
        bean = new ExpandableBean("水平膨胀",Constant.HORIZONTALEXPAND);
        childList.add(bean);
        bean = new ExpandableBean("全方位膨胀",Constant.ALLEXPAND);
        childList.add(bean);
        map.put("膨胀",childList);

        childList = new ArrayList<>();
        bean = new ExpandableBean("开运算",Constant.TRANSLATION);
        childList.add(bean);
        bean = new ExpandableBean("闭运算",Constant.ROTATE);
        childList.add(bean);
        bean = new ExpandableBean("全方位膨胀",Constant.ALPH);
        childList.add(bean);
        map.put("开启与闭合",childList);



        childList = new ArrayList<>();
        bean = new ExpandableBean("平移",Constant.TRANSLATION);
        childList.add(bean);
        bean = new ExpandableBean("旋转",Constant.ROTATE);
        childList.add(bean);
        bean = new ExpandableBean("缩放",Constant.ALPH);
        childList.add(bean);
        map.put("分组",childList);

        childList = new ArrayList<>();
        bean = new ExpandableBean("平移",Constant.TRANSLATION);
        childList.add(bean);
        bean = new ExpandableBean("旋转",Constant.ROTATE);
        childList.add(bean);
        bean = new ExpandableBean("缩放",Constant.ALPH);
        childList.add(bean);
        map.put("分组",childList);

        childList = new ArrayList<>();
        bean = new ExpandableBean("平移",Constant.TRANSLATION);
        childList.add(bean);
        bean = new ExpandableBean("旋转",Constant.ROTATE);
        childList.add(bean);
        bean = new ExpandableBean("缩放",Constant.ALPH);
        childList.add(bean);
        map.put("分组",childList);
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败,错误码："+code);
            }
        }
    };

    /**
     * 构建语法监听器。
     */
    private GrammarListener grammarListener = new GrammarListener() {
        @Override
        public void onBuildFinish(String grammarId, SpeechError error) {
            if(error == null){
                if (mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    if(!TextUtils.isEmpty(grammarId))
                        editor.putString(KEY_GRAMMAR_ABNF_ID, grammarId);
                    editor.commit();
                }
                showTip("语法构建成功：" + grammarId);
            }else{
                showTip("语法构建失败,错误码：" + error.getErrorCode());
            }
        }
    };

    /**
     * 识别监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            showTip("当前正在说话，音量大小：" + volume);
            Log.d(TAG, "返回音频数据："+data.length);
        }

        @Override
        public void onResult(final RecognizerResult result, boolean isLast) {
            if (null != result && !TextUtils.isEmpty(result.getResultString())) {
                Log.d(TAG, "recognizer result：" + result.getResultString());
                String text = "";
                if (mResultType.equals("json")) {
                    text = JsonParser.parseGrammarResult(result.getResultString(), mEngineType);
                } else if (mResultType.equals("xml")) {
                    text = XmlParser.parseNluResult(result.getResultString());
                }
                if (text.contains("水平膨胀")) {
                    horizontalExpand(loadedBitmap);
                    Toast.makeText(getContext(),"水平膨胀",Toast.LENGTH_SHORT).show();
                } else if(text.contains("垂直膨胀")) {
                    verticalExpand(loadedBitmap);
                } else if(text.contains("水平腐蚀")) {
                    horizontalCorrosion(loadedBitmap);
                } else if(text.contains("垂直腐蚀")) {
                    verticalCorrosion(loadedBitmap);
                }

            } else {
                Log.d(TAG, "recognizer result : null");
            }
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            //showTip("结束说话");
        }

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            showTip("开始说话");
        }

        @Override
        public void onError(SpeechError error) {
            showTip("onError Code："	+ error.getErrorCode());
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }

    };



    private void showTip(final String str) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToast.setText(str);
                mToast.show();
            }
        });
    }

    /**
     * 参数设置
     * @param
     * @return
     */
    public boolean setParam(){
        boolean result = false;
        // 清空参数
        mAsr.setParameter(SpeechConstant.PARAMS, null);
        // 设置识别引擎
        mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        if("cloud".equalsIgnoreCase(mEngineType))
        {
            String grammarId = mSharedPreferences.getString(KEY_GRAMMAR_ABNF_ID, null);
            if(TextUtils.isEmpty(grammarId))
            {
                result =  false;
            }else {
                // 设置返回结果格式
                mAsr.setParameter(SpeechConstant.RESULT_TYPE, mResultType);
                // 设置云端识别使用的语法id
                mAsr.setParameter(SpeechConstant.CLOUD_GRAMMAR, grammarId);
                result =  true;
            }
        }
        else
        {
            // 设置本地识别资源
            mAsr.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
            // 设置语法构建路径
            mAsr.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
            // 设置返回结果格式
            mAsr.setParameter(SpeechConstant.RESULT_TYPE, mResultType);
            // 设置本地识别使用语法id
            mAsr.setParameter(SpeechConstant.LOCAL_GRAMMAR, "call");
            // 设置识别的门限值
            mAsr.setParameter(SpeechConstant.MIXED_THRESHOLD, "30");
            // 使用8k音频的时候请解开注释
//			mAsr.setParameter(SpeechConstant.SAMPLE_RATE, "8000");
            result = true;
        }

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mAsr.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mAsr.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/asr.wav");
        return result;
    }

    //获取识别资源路径
    private String getResourcePath(){
        StringBuffer tempBuffer = new StringBuffer();
        //识别通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(getContext(), ResourceUtil.RESOURCE_TYPE.assets, "asr/common.jet"));
        //识别8k资源-使用8k的时候请解开注释
//		tempBuffer.append(";");
//		tempBuffer.append(ResourceUtil.generateResourcePath(this, RESOURCE_TYPE.assets, "asr/common_8k.jet"));
        return tempBuffer.toString();
    }

    private void requestPermissions() {
        int permission = ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),new String[] {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.LOCATION_HARDWARE,
                    //Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO,
                    //Manifest.permission.READ_CONTACTS
            },0x0010);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 0x0010:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getContext(),"权限申请成功",Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(),"你拒绝了权限",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }
}
