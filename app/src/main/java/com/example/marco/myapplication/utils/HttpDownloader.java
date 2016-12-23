package com.example.marco.myapplication.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by 56820 on 2016/12/18.
 */
public class HttpDownloader implements Runnable {

    private String urlStr;
    private String Path;
    private String FileName;

    public HttpDownloader(String urlStr,String path,String fileName) {

        this.urlStr = urlStr;
        this.Path = path;
        this.FileName = fileName;
    }


    @Override
    public void run() {

        FileUtils fileUtils = new FileUtils();
        InputStream inputStream = null;
        File resultFile = null;
        URL url = null;


        if (fileUtils.isFileExist(Path + FileName)) {
            System.out.println("文件已存在");
            return;
        } else {
            try {
                url = new URL(urlStr);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
                try {
                    HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
                    inputStream = urlConn.getInputStream();
                    resultFile = fileUtils.write2SDFromInput(Path, FileName, inputStream);
                    if (resultFile == null) {
                        System.out.println("下载失败！");
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }


