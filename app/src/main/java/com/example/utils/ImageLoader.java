package com.example.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;


/**
 * 图片加载类
 * Created by 竹轩听雨 on 2018/3/30.
 */

public class ImageLoader {

    private static ImageLoader mInstance;
    /**
     * 图片缓存的核心对象
     */
    private LruCache<String,Bitmap> mLruCache;
    /**
     * 线程池
     */
    private ExecutorService mThreadPool;
    private static final int DEFAULT_THREAD_COUNT = 1;
    /**
     * 队列调度方式
     */
    private Type mType = Type.LIFO;
    public enum Type {
        FIFO,LIFO,
    }
    /**
     * 任务队列
     */
    private LinkedList<Runnable> mTaskQueue;
    /**
     * 后台轮询线程
     */
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;
    /**
     * UI线程中的Handler
     */
    private Handler mUIHandler;

    private Semaphore mSemphorePoolThreadHandler = new Semaphore(0);
    private Semaphore mSemphoreThreadPool;

    private ImageLoader(int threadCount, Type type)
    {
        init(threadCount,type);
    }

    /**
     * 初始化操作
     * @param threadCount
     * @param type
     */
    private void init(int threadCount, Type type) {
        /**
         * 后台轮询线程
         */
        mPoolThread = new Thread(){
            @Override
            public void run() {
                Looper.prepare();
                mPoolThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        //线程池取出任务执行
                        mThreadPool.execute(getTask());

                        try {
                            mSemphoreThreadPool.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                //释放一个信号量a
                mSemphorePoolThreadHandler.release();
                Looper.loop();
            };
        };
        mPoolThread.start();

        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;

        mLruCache = new LruCache<String,Bitmap>(cacheMemory) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
        //创建线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList<Runnable>();
        mType = type;

        mSemphoreThreadPool = new Semaphore(threadCount);
    }

    /**
     * 从任务队列取出一个方法
     * @return
     */
    private Runnable getTask() {
        if(mType == Type.FIFO) {
            return mTaskQueue.removeFirst();
        } else if(mType == Type.LIFO) {
            return mTaskQueue.removeLast();
        }
        return null;
    }

    /**
     * 单利模式
     * @return
     */
    public static ImageLoader getInstance(int threadCount,Type type)
    {
        if(mInstance == null) {
            synchronized (ImageLoader.class) {
                if(mInstance == null) {
                    mInstance = new ImageLoader(threadCount,type);
                }
            }
        }
        return mInstance;
    }

    /**
     * 根据path为iamgeview设置图片
     * @param path
     * @param imageView
     */
    public void loadImage(final String path, final ImageView imageView) {
        imageView.setTag(path);
        if(mUIHandler == null) {
            mUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    //获取得到的图片，为iamgeview回调设置图片
                    ImageBeanHolder holder = (ImageBeanHolder) msg.obj;
                    Bitmap bm = holder.bitmap;
                    ImageView imageView = holder.imageView;
                    String path = holder.path;
                    //将path与getTag存储的路径进行比较
                    if(imageView.getTag().toString().equals(path)) {
                        imageView.setImageBitmap(bm);
                    }
                };
            };
        }
        
        Bitmap bm = getBitmapFromLruCache(path);

        if(bm != null) {
            refreashBitmap(path,imageView,bm);
        } else {
            addTasks(new Runnable() {
                @Override
                public void run() {
                    //加载图片
                    //图片的压缩
                    //1、获得图片需要显示的大小
                    ImageSize imageSize =  getImageViewSize(imageView);
                    //2、压缩图片
                    Bitmap bm = decodeSampleBitmapFromPath(path,imageSize.width,imageSize.height);
                    //3、把图片加入到缓存
                    addBitmapToLruCache(path,bm);

                    refreashBitmap(path,imageView,bm);

                    mSemphoreThreadPool.release();
                }
            });
        }
    }

    private void refreashBitmap(String path, ImageView imageView, Bitmap bm) {
        Message message = Message.obtain();
        ImageBeanHolder holder = new ImageBeanHolder();
        holder.bitmap = bm;
        holder.path = path;
        holder.imageView = imageView;
        message.obj = holder;
        mUIHandler.sendMessage(message);
    }


    /**
     *  把图片加入到缓存
     * @param path
     * @param bm
     */
    private void addBitmapToLruCache(String path, Bitmap bm) {
        if(getBitmapFromLruCache(path) == null) {
            if(bm != null) {
                mLruCache.put(path,bm);
            }
        }
    }

    /**
     * 根据图片需要显示的宽和高对图片进行压缩
     * @param path
     * @param width
     * @param height
     * @return
     */
    private Bitmap decodeSampleBitmapFromPath(String path, int width, int height) {
        //获取图片的宽和高并不把图片加载到内存中
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path,options);

        options.inSampleSize = caculateInSampleSize(options,width,height);

        //使用获取到的InSampleSize再次解析图片
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path,options);
        return bitmap;
    }

    /**
     * 根据需求的宽和高以及图片实际的宽和高计算SampleSize
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private int caculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;

        int inSampleSize = 1;

        if(width > reqWidth || height > reqHeight) {
            int widthRadio = Math.round(width*1.0f/reqWidth);
            int heightRadio = Math.round(height*1.0f/reqHeight);

            inSampleSize = Math.max(widthRadio,heightRadio);
        }
        return inSampleSize;
    }

    /**
     * 根据imageview获取适当的宽和高
     * @param imageView
     * @return
     */
    private ImageSize getImageViewSize(ImageView imageView) {
        ImageSize imageSize = new ImageSize();

        DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();

        ViewGroup.LayoutParams lp = imageView.getLayoutParams();
        int width = imageView.getWidth();
        if(width <= 0) {
            width = lp.width;//获取iamgeview在layout中声明的宽度
        }
        if(width <= 0) {
            //width = imageView.getMaxWidth();//检查最大值
            width = getImagViewFileValue(imageView,"mMaxWidth");
        }
        if(width <= 0) {
            width  = displayMetrics.widthPixels;
        }

        int height = imageView.getHeight();
        if(height <= 0) {
            height = lp.height;//获取iamgeview在layout中声明的宽度
        }
        if(height <= 0) {
            //height = imageView.getMaxHeight();//检查最大值
            getImagViewFileValue(imageView,"mMaxHeight");
        }
        if(height <= 0) {
            height  = displayMetrics.heightPixels;
        }

        imageSize.width = width;
        imageSize.height = height;
        return imageSize;
    }

    /**
     * 通过反射获取imageView的某个属性值
     * @param object
     * @param fileName
     * @return
     */
    private static int getImagViewFileValue(Object object, String fileName) {
        int value = 0;
        try {
            Field field = ImageView.class.getDeclaredField(fileName);
            field.setAccessible(true);
            int fieldValue = field.getInt(object);
            if(fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
                value = fieldValue;
            }
        } catch (Exception e) {

        }
        return value;
    }

    private synchronized void addTasks(Runnable runnable) {
        mTaskQueue.add(runnable);
        try {
            if(mPoolThreadHandler == null)
            mSemphorePoolThreadHandler.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mPoolThreadHandler.sendEmptyMessage(0x110);
    }

    /**
     * 根据path在缓存中获取bitmap
     * @param key
     * @return
     */
    private Bitmap getBitmapFromLruCache(String key) {
        return mLruCache.get(key);
    }

    private class ImageSize {
        int width;
        int height;
    }

    private class ImageBeanHolder {
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }
}
