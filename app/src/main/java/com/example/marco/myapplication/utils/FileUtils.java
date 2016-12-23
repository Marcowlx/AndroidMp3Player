package com.example.marco.myapplication.utils;

import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by 56820 on 2016/12/18.
 */
public class FileUtils {
    File file=null;
    FileOutputStream output;

    String SDPATH= Environment.getExternalStorageDirectory().getAbsolutePath()+"/";
    public File write2SDFromInput(String path, String fileName, InputStream inputStream) throws FileNotFoundException {
        SDPATH= Environment.getExternalStorageDirectory().getAbsolutePath()+"/";
        //在SD卡上创建目录
        File dir=new File(SDPATH+path);
        if(!dir.exists())
            dir.mkdir();
        //在SD卡上创建文件
        file=new File(SDPATH+path+"/"+fileName);
        System.out.println(file.getAbsolutePath());
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(file.exists() && file.canWrite()){

            try {
                output=new FileOutputStream(file);
                byte buffer []=new byte[4*1024];
                int temp;
                while ((temp=inputStream.read(buffer))!=-1){
                    output.write(buffer,0,temp);
                }
                output.flush();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return dir;
    }
    public boolean isFileExist(String path){
        File f=new File(path);
        if(f.exists()){
            return true;
        }
        else {
            return false;
        }
    }



}
