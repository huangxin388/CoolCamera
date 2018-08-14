package com.example.coolcamera;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.bean.FancyCoverFlowBean;
import com.example.fragment.CameraFragment;
import com.example.fragment.ColorChangeFragment;
import com.example.fragment.JisawFragment;
import com.example.fragment.VariationFragment;
//import com.example.fragment.CameraFragment;
//import com.example.fragment.ColorChangeFragment;
//import com.example.fragment.JisawFragment;
//import com.example.fragment.VariationFragment;

import java.util.ArrayList;

import at.technikum.mti.fancycoverflow.FancyCoverFlow;
import at.technikum.mti.fancycoverflow.FancyCoverFlowAdapter;

public class ContentActivity extends AppCompatActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }
        public static int CAMERA = 1;
    public static int VARIATION = 2;
    public static int JISAW = 3;
    public static int COLORCHANGE = 4;

    private ArrayList<FancyCoverFlowBean> list;
    private FancyCoverFlowBean bean;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);

        startView();

        FancyCoverFlow fancyCoverFlow = (FancyCoverFlow) findViewById(R.id.fancyCoverFlow);
        list = new ArrayList<>();
        bean = new FancyCoverFlowBean("万能相机", R.mipmap.camera_icon,CAMERA);
        list.add(bean);
        bean = new FancyCoverFlowBean("特效变换", R.mipmap.variation_icon,VARIATION);
        list.add(bean);
        bean = new FancyCoverFlowBean("拼图", R.mipmap.jisaw_icon,JISAW);
        list.add(bean);
        bean = new FancyCoverFlowBean("颜色处理", R.mipmap.color_change_icon,COLORCHANGE);
        list.add(bean);


        fancyCoverFlow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                FragmentManager manager = getFragmentManager();
                FragmentTransaction transaction = manager.beginTransaction();

                switch (list.get(i).getId()) {
                    case 1:
                        transaction.replace(R.id.content_layout,new CameraFragment());
                        break;
                    case 2:
                        transaction.replace(R.id.content_layout,new VariationFragment());
                        break;
                    case 3:
                        transaction.replace(R.id.content_layout,new JisawFragment());
                        break;
                    case 4:
                        transaction.replace(R.id.content_layout,new ColorChangeFragment());
                        break;
                    default:
                        transaction.replace(R.id.content_layout,new CameraFragment());
                        break;
                }
               // Toast.makeText(ContentActivity.this,"序号：" + i,Toast.LENGTH_SHORT).show();
                transaction.commit();
            }
        });

        fancyCoverFlow.setAdapter(new ViewGroupExampleAdapter(list));
    }

    private static class ViewGroupExampleAdapter extends FancyCoverFlowAdapter {

        // =============================================================================
        // Private members
        // =============================================================================

        private int[] images = {R.mipmap.camera_icon, R.mipmap.variation_icon, R.mipmap.jisaw_icon, R.mipmap.color_change_icon};
        private String[] name = {"万能相机","特效变换","拼图","颜色处理"};

        private ArrayList<FancyCoverFlowBean> listBean;

        public ViewGroupExampleAdapter(ArrayList<FancyCoverFlowBean> bean) {
            this.listBean = bean;
        }
// =============================================================================
        // Supertype overrides
        // =============================================================================

        @Override
        public int getCount() {
            return listBean.size();
        }

        @Override
        public FancyCoverFlowBean getItem(int i) {
            return listBean.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getCoverFlowItem(int i, View reuseableView, ViewGroup viewGroup) {
            CustomViewGroup customViewGroup = null;

            if (reuseableView != null) {
                customViewGroup = (CustomViewGroup) reuseableView;
            } else {
                customViewGroup = new CustomViewGroup(viewGroup.getContext());
                customViewGroup.setLayoutParams(new FancyCoverFlow.LayoutParams(300, 250));
                //customViewGroup.setBackgroundColor(Color.parseColor("#eeffeeee"));
            }

            customViewGroup.getImageView().setImageResource(listBean.get(i).getImageId());
            customViewGroup.getTextView().setText(listBean.get(i).getName());



            return customViewGroup;
        }

    }

    private static class CustomViewGroup extends LinearLayout {

        // =============================================================================
        // Child views
        // =============================================================================

        private TextView textView;

        private ImageView imageView;



        // =============================================================================
        // Constructor
        // =============================================================================

        private CustomViewGroup(Context context) {
            super(context);

            this.setOrientation(VERTICAL);

            this.textView = new TextView(context);
            this.imageView = new ImageView(context);

            LinearLayout.LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            this.textView.setLayoutParams(layoutParams);
            this.imageView.setLayoutParams(layoutParams);

            this.textView.setGravity(Gravity.CENTER);
            this.textView.setTextColor(Color.parseColor("#FFFD4965"));

            this.imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            this.imageView.setAdjustViewBounds(true);

            /*
            this.button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://davidschreiber.github.com/FancyCoverFlow"));
                    view.getContext().startActivity(i);
                }
            });
            */

            this.addView(this.imageView);
            this.addView(this.textView);
        }

        // =============================================================================
        // Getters
        // =============================================================================

        private TextView getTextView() {
            return textView;
        }

        private ImageView getImageView() {
            return imageView;
        }
    }

    private void startView() {

        int start = 0;
        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        start = getIntent().getIntExtra("start_view",0);

        switch (start)
        {
            case 2:
                transaction.add(R.id.content_layout,new VariationFragment());
                break;
            case 3:
                transaction.add(R.id.content_layout,new JisawFragment());
                break;
            case 4:
                transaction.add(R.id.content_layout,new ColorChangeFragment());
                break;
            case 1:
            default:
                transaction.add(R.id.content_layout,new CameraFragment());
        }

        transaction.commit();
    }

}
