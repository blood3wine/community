package com.nowcoder.community.entity;

//用来封装分页相关的信息
public class Page {
    //让服务器接收页面传入的信息

    //当前页码
    private int current=1;  //
    //显示数据的上限
    private int limit=10;  //

    //返回给页面

    //数据总数 [用于计算总页数
    private int rows;  //
    //查询路径[用于复用分页链接
    private String path;

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        if(current>=1) {
            this.current = current;
        }
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        if(limit>=1&&limit<=100) {
            this.limit = limit;
        }
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        if(rows>=0) {
            this.rows = rows;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    //获取当前页的起始行
    public int getOffset(){
        //(current-1)*limit
        return (current-1)*limit;
    }

    //用来获取总的页数 返回给页面上的
    public int getTotal(){
        //rows/limit+1
        if(rows%limit==0){
            return rows/limit;
        }
        else{
            return rows/limit+1;
        }
    }

    //从第几页到第几页的页码
    public int getFrom(){
        int from=current-2;
        return from<1?1:from;

    }
    public int getTo(){
        int to=current+2;
        return to>getTotal()?getTotal():to;

    }
}
