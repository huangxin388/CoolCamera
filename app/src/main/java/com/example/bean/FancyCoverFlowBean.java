package com.example.bean;

/**
 * Created by 竹轩听雨 on 2018/3/25.
 */

public class FancyCoverFlowBean {
    private String name;
    private int imageId;
    private int id;//用来区分不同的部分，处理不同的点击事件

    public FancyCoverFlowBean(String name, int imageId, int id) {
        this.name = name;
        this.imageId = imageId;
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImageId(int imageId) {
        this.imageId = imageId;
    }

    public void setId(int id ) {
        this.id = id;
    }

    public String getName() {

        return name;
    }

    public int getImageId() {
        return imageId;
    }

    public int getId() {
        return id ;
    }
}
