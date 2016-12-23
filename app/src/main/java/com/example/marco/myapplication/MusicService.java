package com.example.marco.myapplication;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;

import com.example.marco.myapplication.Lrc.LrcContent;
import com.example.marco.myapplication.Lrc.LrcProcess;
import com.example.marco.myapplication.tool.Mp3Info;
import com.example.marco.myapplication.tool.PlayerMSG;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 56820 on 2016/12/14.
 */
public class MusicService extends Service {


    private LrcProcess mLrcProcess; //歌词处理
    private List<LrcContent> lrcList = new ArrayList<LrcContent>(); //存放歌词列表对象
    private int index = 0;          //歌词检索值


    // mp3的绝对路径
    private String path;
    //当前播放位置
    private int position;
    //当前播放进度
    private int currentTime;

    private int msg;                //播放信息

    private boolean isPause;

    static List<Mp3Info> mp3Infos;    //存放Mp3Info对象的集合

    private int duration;            //播放长度

    int palyflag = 0;


    //服务要发送的一些Action
    public static final String UPDATE_ACTION = "com.example.marco.myapplication.action.UPDATE_ACTION";  //更新音乐播放曲目
    public static final String CTL_ACTION = "com.example.marco.myapplication.action.CTL_ACTION";        //控制播放模式
    public static final String MUSIC_CURRENT = "com.example.marco.myapplication.action.MUSIC_CURRENT";  //当前音乐播放时间更新
    public static final String MUSIC_DURATION = "com.example.marco.myapplication.action.MUSIC_DURATION";//播放音乐长度更新
    public static final String PLAY_STATUE = "com.example.marco.myapplication.action.PLAY_STATUE";      //更新播放状态

    //播放音乐的媒体类
    MediaPlayer mediaPlayer;
    //广播接收器，接收来自MusicActivity的广播
    private MusicReceiver musicReceiver;


    IBinder musicBinder = new MyBinder(); //远程对象基本接口

    //handler用来接收消息，来发送广播更新播放时间 接受子线程发送的数据， 并用此数据配合主线程更新UI
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (mediaPlayer != null) {
                        currentTime = mediaPlayer.getCurrentPosition();
                        Intent intent = new Intent();
                        intent.setAction(MUSIC_CURRENT);
                        intent.putExtra("currentTime", currentTime);
                        sendBroadcast(intent); // 给PlayerActivity发送广播
                        mHandler.sendEmptyMessageDelayed(1, 1000); //消息，延迟
                    }
                    break;
                default:
                    break;
            }
        }

        ;
    };

    class MyBinder extends Binder {
        public Service getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();

        /**
         * 设置音乐播放完成时的监听器
         */
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Intent intent = new Intent(PLAY_STATUE);
                //发送播放完毕的新号，更新播放状态
                intent.putExtra("playstatue", false);
                sendBroadcast(intent);
                if (palyflag == 2) {
                    Intent loopintent = new Intent(PLAY_STATUE);
                    //发送播放完毕的信号，更新播放状态
                    intent.putExtra("playstatue", true);
                    sendBroadcast(loopintent);
                    //单曲循环
                    mediaPlayer.start();
                } else if (palyflag == 1) {
                    // 列表循环
                    position++;
                    if (position > mp3Infos.size() - 1) {
                        //变为第一首的位置继续播放
                        position = 0;
                    }
                    Intent sendIntent = new Intent(UPDATE_ACTION);
                    sendIntent.putExtra("position", position);
                    // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                    sendBroadcast(sendIntent);
                    path = mp3Infos.get(position).getUrl();
                    play(0);
                } else if (palyflag == 0) { // 顺序播放
                    position++;    //下一首位置
                    if (position <= mp3Infos.size() - 1) {
                        Intent sendIntent = new Intent(UPDATE_ACTION);
                        sendIntent.putExtra("position", position);
                        // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                        sendBroadcast(sendIntent);
                        path = mp3Infos.get(position).getUrl();
                        play(0);
                    }
                } else if (palyflag == 3) {    //随机播放
                    position = getRandomIndex(mp3Infos.size() - 1);
                    Intent sendIntent = new Intent(UPDATE_ACTION);
                    sendIntent.putExtra("position", position);
                    // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                    sendBroadcast(sendIntent);
                    path = mp3Infos.get(position).getUrl();
                    play(0);
                } else {


                    mediaPlayer.seekTo(0); //设置位置为0毫秒
                    position = 0;
                    Intent sendIntent = new Intent(UPDATE_ACTION);
                    sendIntent.putExtra("position", position);
                    // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                    sendBroadcast(sendIntent);
                }


            }
        });

        musicReceiver = new MusicReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(CTL_ACTION);
        //     filter.addAction(SHOW_LRC);
        registerReceiver(musicReceiver, filter);


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        path = intent.getStringExtra("url");        //歌曲路径
        position = intent.getIntExtra("position", -1);    //当前播放歌曲的在mp3Infos的位置
        msg = intent.getIntExtra("MSG", 0);            //播放信息
        if (msg == PlayerMSG.MSG.PLAY_MSG) {    //直接播放音乐
            play(0);
        } else if (msg == PlayerMSG.MSG.PAUSE_MSG) {    //暂停
            pause();
        } else if (msg == PlayerMSG.MSG.STOP_MSG) {        //停止
            stop();
        } else if (msg == PlayerMSG.MSG.CONTINUE_MSG) {    //继续播放
            resume();
        } else if (msg == PlayerMSG.MSG.PRIVIOUS_MSG) {    //上一首
            previous();
        } else if (msg == PlayerMSG.MSG.NEXT_MSG) {        //下一首
            next();
        } else if (msg == PlayerMSG.MSG.PROGRESS_CHANGE) {    //进度更新
            currentTime = intent.getIntExtra("progress", -1);
            play(currentTime);
        } else if (msg == PlayerMSG.MSG.PLAYING_MSG) {
            mHandler.sendEmptyMessage(1);
        }
        return super.onStartCommand(intent, flags, startID);
    }

    private void play(int currentTime) {
        try {

            mediaPlayer.reset();// 把各项参数恢复到初始状态
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare(); // 进行缓冲
            mediaPlayer.setOnPreparedListener(new PreparedListener(currentTime));// 注册一个监听器
            //更新播放状态
            Intent intent = new Intent(PLAY_STATUE);
            // 发送播放完毕的信号，更新播放状态
            intent.putExtra("playstatue", true);
            sendBroadcast(intent);
            mHandler.sendEmptyMessage(1);
            initLrc();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //继续播放
    private void resume() {
        if (isPause) {
            mediaPlayer.start();
            isPause = false;
        }
    }

    /**
     * 上一首
     */
    private void previous() {
        Intent sendIntent = new Intent(UPDATE_ACTION);
        sendIntent.putExtra("position", position);
        // 发送广播，将被Activity组件中的BroadcastReceiver接收到
        sendBroadcast(sendIntent);
        play(0);
    }

    /**
     * 下一首
     */
    private void next() {
        Intent sendIntent = new Intent(UPDATE_ACTION);
        sendIntent.putExtra("position", position);
        // 发送广播，将被Activity组件中的BroadcastReceiver接收到
        sendBroadcast(sendIntent);
        play(0);
    }

    private void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPause = true;
        }
    }

    private void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            try {
                mediaPlayer.prepare(); // 在调用stop后如果需要再次通过start进行播放,需要之前调用prepare函数
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 实现一个OnPrepareLister接口,当音乐准备好的时候开始播放
     */
    private final class PreparedListener implements MediaPlayer.OnPreparedListener {

        private int currentTime;

        public PreparedListener(int currentTime) {
            this.currentTime = currentTime;
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            mediaPlayer.start(); // 开始播放
            if (currentTime > 0) { // 如果音乐不是从头播放
                mediaPlayer.seekTo(currentTime);
            }
            Intent intent = new Intent();
            intent.setAction(MUSIC_DURATION);
            duration = mediaPlayer.getDuration();
            intent.putExtra("duration", duration);    //通过Intent来传递歌曲的总长度
            sendBroadcast(intent);
        }
    }

    protected int getRandomIndex(int end) {
            int index = (int) (Math.random() * end);
            return index;
    }


    public class MusicReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int control = intent.getIntExtra("control", -1);
            switch (control) {
                case 0:
                    palyflag = 0; // 顺序播放
                    break;
                case 1:
                    palyflag = 1;    //列表循环
                    break;
                case 2:
                default:
                    palyflag = 2;    //单曲循环
                    break;
                case 3:
                    palyflag = 3;  //随机
                    break;
            }
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return musicBinder;
    }
    /*
    * 初始化歌词配置
    */


    public void initLrc(){
        mLrcProcess = new LrcProcess();
        //读取歌词文件
        mLrcProcess.readLRC(mp3Infos.get(position).getUrl());
        //传回处理后的歌词文件
        lrcList = mLrcProcess.getLrcList();
        MusicActivity.lrcView.setmLrcList(lrcList);
        mHandler.post(mRunnable); //更新UI
    }
    Runnable mRunnable = new Runnable() {  //创建线程

        @Override
        public void run() {
            MusicActivity.lrcView.setIndex(lrcIndex());
            MusicActivity.lrcView.invalidate();
            mHandler.postDelayed(mRunnable, 100);
        }
    };


    /**
     * 根据时间获取歌词显示的索引值
     * @return
     */
    public int lrcIndex() {
        if(mediaPlayer.isPlaying()) {
            currentTime = mediaPlayer.getCurrentPosition();
            duration = mediaPlayer.getDuration();
        }
        if(currentTime < duration) {
            for (int i = 0; i < lrcList.size(); i++) {
                if (i < lrcList.size() - 1) {
                    if (currentTime < lrcList.get(i).getLrcTime() && i == 0) {
                        index = i;
                    }
                    if (currentTime > lrcList.get(i).getLrcTime()
                            && currentTime < lrcList.get(i + 1).getLrcTime()) {
                        index = i;
                    }
                }
                if (i == lrcList.size() - 1
                        && currentTime > lrcList.get(i).getLrcTime()) {
                    index = i;
                }
            }
        }
        return index;
    }



    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
