package com.example.bean;

/**
 * Created by 竹轩听雨 on 2018/3/28.
 */

public class ExpandableBean {
    private String childName;
    private int id;

    public ExpandableBean(String childName, int id) {
        this.childName = childName;
        this.id = id;
    }

    public String getChildName() {
        return childName;
    }

    public void setChildName(String childName) {
        this.childName = childName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
