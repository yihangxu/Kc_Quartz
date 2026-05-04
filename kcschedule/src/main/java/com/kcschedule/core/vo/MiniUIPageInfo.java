package com.kcschedule.core.vo;

import java.util.List;

public class MiniUIPageInfo<T> {
    private List<T> data;
    private Integer total;

    public List<T> getData() { return data; }
    public void setData(List<T> data) { this.data = data; }

    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
}
