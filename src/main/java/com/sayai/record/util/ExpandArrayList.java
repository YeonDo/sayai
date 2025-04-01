package com.sayai.record.util;

import java.util.ArrayList;

public class ExpandArrayList<T> extends ArrayList<T> {

    @Override
    public T set(int index, T element){
        ensure(index+1);
        return super.set(index,element);
    }

    @Override
    public void add(int index, T element){
        super.add(index, element);
    }

    private void ensure(int size){
        while(this.size() < size)
            this.add(null);
    }
}
