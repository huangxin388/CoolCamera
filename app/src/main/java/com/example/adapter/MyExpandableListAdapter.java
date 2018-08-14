package com.example.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.example.bean.ExpandableBean;
import com.example.coolcamera.R;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by 竹轩听雨 on 2018/3/28.
 */

public class MyExpandableListAdapter extends BaseExpandableListAdapter {

    private ArrayList<String> parentList;
    private Map<String,ArrayList<ExpandableBean>> map;
    private Context mContext;

    public MyExpandableListAdapter(ArrayList<String> parentList, Map<String, ArrayList<ExpandableBean>> map, Context mContext) {
        this.parentList = parentList;
        this.map = map;
        this.mContext = mContext;
    }

    @Override
    public int getGroupCount() {
        return parentList.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return map.get(parentList.get(i)).size();
    }

    @Override
    public Object getGroup(int i) {
        return map.get(parentList.get(i));
    }

    @Override
    public Object getChild(int i, int i1) {
        return map.get(parentList.get(i)).get(i1);
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i1) {
        return i1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {
        ParentHolder holder = null;
        if(view == null) {
            holder = new ParentHolder();
            view = LayoutInflater.from(mContext).inflate(R.layout.parent_item_layout,null);
            holder.parentName = view.findViewById(R.id.parent_name);
            view.setTag(holder);
        }
        holder = (ParentHolder) view.getTag();
        holder.parentName.setText(parentList.get(i));
        return view;
    }

    @Override
    public View getChildView(int i, int i1, boolean b, View view, ViewGroup viewGroup) {
        ChildHolder holder = null;
        if(view == null) {
            holder = new ChildHolder();
            view = LayoutInflater.from(mContext).inflate(R.layout.child_item_layout,null);
            holder.childName = view.findViewById(R.id.child_name);
            view.setTag(holder);
        }
        holder = (ChildHolder) view.getTag();
        holder.childName.setText(map.get(parentList.get(i)).get(i1).getChildName());
        return view;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }

    class ParentHolder {
        TextView parentName;
    }

    class  ChildHolder {
        TextView childName;
    }
}
