package com.example.marco.myapplication;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.marco.myapplication.Lrc.LrcContent;
import com.example.marco.myapplication.Lrc.LrcProcess;
import com.example.marco.myapplication.Lrc.LrcView;
import com.example.marco.myapplication.tool.Mp3Info;
import com.example.marco.myapplication.tool.PlayerMSG;
import com.example.marco.myapplication.utils.HttpDownloader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by 56820 on 2016/12/14.
 */
public class MusicActivity extends Activity implements View.OnClickListener{


    private LrcProcess mLrcProcess; //歌词处理
    private List<LrcContent> lrcList = new ArrayList<LrcContent>(); //存放歌词列表对象
    private int index = 0;          //歌词检索值
    static public LrcView lrcView;
    //mp3需要播放的数据
    static List<Mp3Info> mp3Infos;


    //播放按钮
    private ImageView MusicPlay;
    //下一首
    private ImageView MusicNext;
    //上一首
    private ImageView MusicPrevious;
    //播放方式选择
    private ImageView MusicMOde;
    //播放菜单
    private ImageView MusicMenu;
    //显示总时间
    private TextView MusicTime;
    //显示当前时间
    private TextView MusicCurrentTime;
    //显示歌曲名
    private TextView MusicTitle;
    //显示歌曲艺术家
    private TextView MusicArtist;
    //进度条
    SeekBar seekBar;
    //广播
    MusicPlayerReceiver musicPlayerReceiver;
    //下载按钮
    private ImageView Download;


    private final int isorderplay = 0;//顺序播放
    private final int islistloop = 1;//表示列表循环
    private final int isrepeatone = 2;//单曲循环
    private final int israndomplay = 3;//随机

    public static final String UPDATE_ACTION = "com.example.marco.myapplication.action.UPDATE_ACTION";  //更新动作
    public static final String CTL_ACTION = "com.example.marco.myapplication.action.CTL_ACTION";        //控制动作
    public static final String MUSIC_CURRENT = "com.example.marco.myapplication.action.MUSIC_CURRENT";  //音乐当前时间改变动作
    public static final String MUSIC_DURATION = "com.example.marco.myapplication.action.MUSIC_DURATION";//音乐播放长度改变动作
    public static final String PLAY_STATUE = "com.example.marco.myapplication.action.PLAY_STATUE";     //更新播放状态


    //播放方式表识0表示顺序播放，1表示列表循环，2表示单曲循环，3表示随机，初始为顺序播放
    int playmodeflag = 0;
    //歌曲播放的位置,就能够获取位置
    int position;
    int currentTime;
    int duration;//总时间


    // 正在播放
    private boolean isPlaying;


    //控制后台线程退出
    boolean playStatus = true;

    //转换毫秒数为时间模式，一般都是分钟数，音乐文件
    public static String formatDuring(long mss) {
        long minutes = (mss % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (mss % (1000 * 60)) / 1000;
        return String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        switch (v.getId()) {
            case R.id.musicplay:
                if (isPlaying) {
                    MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicpause, null));
                    intent.setClass(MusicActivity.this, MusicService.class);
                    intent.putExtra("MSG", PlayerMSG.MSG.PAUSE_MSG); //键名 键值
                    startService(intent);
                    isPlaying = false;

                } else {
                    MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicplay, null));
                    intent.setClass(MusicActivity.this, MusicService.class);
                    intent.putExtra("MSG", PlayerMSG.MSG.CONTINUE_MSG);
                    startService(intent);
                    isPlaying = true;
                }
                break;
            case R.id.musicplaymode:
                setPlayMOde();
                break;
            case R.id.musicnext:
                PlayNextMusic();
                break;
            case R.id.musicprevious:
                PlayPreviousMusic();
                break;
            case R.id.musicplaymenu:
                Intent intent1=new Intent(MusicActivity.this, MainActivity.class);
                startActivity(intent1);
                break;
            case R.id.download:
                System.out.println("Downloading");
                String lrcAddr="http://10.0.2.2:81/"+mp3Infos.get(position).getTitle()+".lrc";
                System.out.println("Address:http://10.0.2.2:81/"+mp3Infos.get(position).getTitle()+".lrc");
                Thread httpDownloader=new Thread(new HttpDownloader(lrcAddr,"Music",mp3Infos.get(position).getTitle()+".lrc"));
                System.out.println("Create Httpdownloader");
                httpDownloader.start();
                System.out.println("Finished");
            default:
                break;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.musicplay);

        //初始化控件
        InitView();

        //获得传过来的值
        Intent intent = getIntent();
        position = Integer.parseInt(intent.getStringExtra("position"));

        MusicArtist.setText(mp3Infos.get(position).getArtist());
        MusicTitle.setText(mp3Infos.get(position).getTitle());

        //注册广播
        musicPlayerReceiver = new MusicPlayerReceiver();
        IntentFilter filter = new IntentFilter(); //过滤intent
        filter.addAction(UPDATE_ACTION); //筛选
        filter.addAction(MUSIC_CURRENT);
        filter.addAction(MUSIC_DURATION);
        filter.addAction(PLAY_STATUE);

        registerReceiver(musicPlayerReceiver, filter); //广播接收器


        //设置响应事件
        MusicNext.setOnClickListener(this);
        MusicPrevious.setOnClickListener(this);
        MusicMenu.setOnClickListener(this);
        MusicMOde.setOnClickListener(this);
        MusicPlay.setOnClickListener(this);
        Download.setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        PlayMusic();
    }


    //初始化控件
    void InitView(){
        MusicPlay = (ImageView) findViewById(R.id.musicplay);
        MusicNext = (ImageView) findViewById(R.id.musicnext);
        MusicPrevious = (ImageView) findViewById(R.id.musicprevious);
        MusicMenu = (ImageView) findViewById(R.id.musicplaymenu);
        MusicMOde = (ImageView) findViewById(R.id.musicplaymode);
        MusicTime = (TextView) findViewById(R.id.playtime);
        MusicCurrentTime = (TextView) findViewById(R.id.playcurrenttime);
        MusicTitle = (TextView) findViewById(R.id.musictitle);
        MusicArtist = (TextView) findViewById(R.id.musicartist);
        seekBar = (SeekBar) findViewById(R.id.MusicProgress);
        lrcView= (LrcView) findViewById(R.id.lrcShowView);
        Download= (ImageView) findViewById(R.id.download);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    void setPlayMOde() {
        playmodeflag++;
        Intent intent = new Intent(CTL_ACTION); //注册intent传递消息
        intent.putExtra("control", playmodeflag); //键名 键值
        sendBroadcast(intent);
        switch (playmodeflag) {
            case isorderplay:
                MusicMOde.setImageDrawable(getResources().getDrawable(R.drawable.orderplay, null));
                break;
            case islistloop:
                MusicMOde.setImageDrawable(getResources().getDrawable(R.drawable.repeatplay, null));
                break;
            case isrepeatone:
                MusicMOde.setImageDrawable(getResources().getDrawable(R.drawable.repeatoneplay, null));
                break;
            case israndomplay:
                MusicMOde.setImageDrawable(getResources().getDrawable(R.drawable.randomplay, null));
                playmodeflag = -1;
                break;
            default:
                break;
        }
    }
    //进度条拖动事件
    SeekBar.OnSeekBarChangeListener seekBarChangeListener= new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        //停止拖动事件
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

            int progress = seekBar.getProgress();

            ChangeProgress(progress);

        }
    };

    public void ChangeProgress(int progress) {
        Intent intent = new Intent();
        intent.setClass(MusicActivity.this, MusicService.class);
        intent.putExtra("url", mp3Infos.get(position).getUrl());
        intent.putExtra("position", position);
        intent.putExtra("MSG", PlayerMSG.MSG.PROGRESS_CHANGE);
        intent.putExtra("progress", progress);
        startService(intent);
    }

    //播放音乐
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void PlayMusic() {
        isPlaying = true;

        MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicplay, null));
        // 开始播放的时候为顺序播放
        Intent intent = new Intent();
        intent.setClass(MusicActivity.this, MusicService.class);
        intent.putExtra("url", mp3Infos.get(position).getUrl());
        intent.putExtra("position", position);
        intent.putExtra("MSG", PlayerMSG.MSG.PLAY_MSG);
        MusicService.mp3Infos = mp3Infos;
        startService(intent);
    }

    //播放上一首音乐
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void PlayPreviousMusic() {
        position = position - 1;
        if (position < 0) {
            position = mp3Infos.size() - 1;
        }
        Mp3Info mp3Info = mp3Infos.get(position);
        MusicTitle.setText(mp3Info.getTitle());
        MusicArtist.setText(mp3Info.getArtist());
        Intent intent = new Intent();
        intent.setClass(MusicActivity.this, MusicService.class);
        intent.putExtra("url", mp3Info.getUrl());
        intent.putExtra("position", position);
        intent.putExtra("MSG", PlayerMSG.MSG.PRIVIOUS_MSG);
        startService(intent);
        isPlaying = true;

        MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicplay, null));
    }

    //播放下一首音乐
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void PlayNextMusic() {
        //判断是否是随机播放，因为随机播放设置后，playmodeflag变为-1了
        if (playmodeflag == -1) {
            Random random = new Random();
            position = random.nextInt(mp3Infos.size());
        } else
            position = position + 1;
        if (position >= mp3Infos.size())
            position = 0;

        Mp3Info mp3Info = mp3Infos.get(position);
        MusicTitle.setText(mp3Info.getTitle());
        MusicArtist.setText(mp3Info.getArtist());
        Intent intent = new Intent();
        intent.setClass(MusicActivity.this, MusicService.class);
        intent.putExtra("url", mp3Info.getUrl());
        intent.putExtra("position", position);
        intent.putExtra("MSG", PlayerMSG.MSG.NEXT_MSG);
        startService(intent);
        MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicplay, null));
        isPlaying = true;

    }


    public class MusicPlayerReceiver extends BroadcastReceiver {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //当前时间更新
            if (action.equals(MUSIC_CURRENT)) {
                currentTime = intent.getIntExtra("currentTime", -1); //键名 键值
                seekBar.setProgress(currentTime);
                MusicCurrentTime.setText(formatDuring(currentTime));


            } else if (action.equals(MUSIC_DURATION)) {
                //总时间更新
                duration = intent.getIntExtra("duration", -1);
                seekBar.setMax(duration);
                MusicTime.setText(formatDuring(duration));

            } else if (action.equals(UPDATE_ACTION)) {
                position = intent.getIntExtra("position", -1);
                String url = mp3Infos.get(position).getUrl();

                MusicTitle.setText(mp3Infos.get(position).getTitle());
                MusicArtist.setText(mp3Infos.get(position).getArtist());
                MusicTime.setText(formatDuring(mp3Infos.get(position).getDuration()));

            } else if (action.equals(PLAY_STATUE)) {
                boolean playstatue = intent.getBooleanExtra("playstatue", true);
                if (playstatue) {
                    MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicplay, null));
                    isPlaying = true;
                } else {
                    MusicPlay.setImageDrawable(getResources().getDrawable(R.drawable.musicpause, null));
                    isPlaying = false;
                }
            }

        }
    }






    public void onDestroy() {
        //销毁activity时，要记得销毁线程
        playStatus = false;
        super.onDestroy();
    }

}
