package com.example.marco.myapplication.tool;

/**
 * Created by 56820 on 2016/12/14.
 */
public class Mp3Info {
    private String  name;
    private long ID;
    private String title;//音乐标题
    private  String artist;//艺术家
    private long duration;//时长
    private long size;  //文件大小
    private String url; //文件路径

    public String getName()
    {
        return this.name;
    }

    public void  Name(String name)
    {
        this.name =name;
    }

    public String getTitle()
    {
        return this.title;
    }

    public void setTitle(String title)
    {
        this.title =title;
    }

    public void setArtist(String artist)
    {
        this.artist     =artist;
    }
    public String getArtist(){
        return this.artist;
    }

    public String getUrl(){
        return this.url;
    }
    public void setUrl(String url)
    {
        this.url =url;
    }

    public void setID(long id)
    {
        this.ID =id;
    }
    public long getID(){
        return this.ID;
    }

    public long getDuration(){
        return this.duration;
    }

    public long getSize()
    {
        return this.size;
    }
    public  void setDuration(long duration){
        this.duration   =duration;
    }
    public void setSize(long size){
        this.size       = size;
    }
}
