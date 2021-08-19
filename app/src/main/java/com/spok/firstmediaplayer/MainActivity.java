package com.spok.firstmediaplayer;

import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.solver.LinearSystem;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private boolean isTouch = false;
    private MediaPlayer mediaPlayer;
    private SeekBar seekBar_player;
    private ArrayList<String> tracks, locations;
    private ArrayAdapter<String> adapter;
    private ListView music_list;
    private Button btn_play;
    private TextView trackName;
    private int currentPosition = -1;

    private void getMusicFiles() // Получаем аудиофайлы с устройства.
    {
        music_list = findViewById(R.id.listView_music);
        tracks = new ArrayList<>();
        locations = new ArrayList<>();

        Uri uriTrack = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI; // С Uri определяем информацию о файлах.
        Cursor cursor = getContentResolver().query(uriTrack, null, null, null, null);

        if (cursor != null && cursor.moveToFirst())
        {
            for (int i = 0; i < cursor.getColumnCount(); i++) // Заполняем ArrayList с местонахождением файлов и получаем название трека и исполнителя.
            {
                locations.add(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
                tracks.add(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)) + " - " + cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
                cursor.moveToNext();
            }
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tracks);
        music_list.setAdapter(adapter); // Заполняем адаптер и выводим список треков в ListView.
        music_list.setOnItemClickListener(new AdapterView.OnItemClickListener() { // Обработчик нажатия на элемент списка.
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (mediaPlayer != null){ if (mediaPlayer.isPlaying()) mediaPlayer.stop();}
                seekBar_player.setEnabled(true);
                currentPosition = position;
                PlayTrack(); // Проигрываем выбранный трек.
            }
        });
    }

    private void PlayTrack() // Проигрываем трек.
    {
        btn_play.setBackgroundResource(R.drawable.button_pause_style);
        trackName.setText(tracks.get(currentPosition));
        mediaPlayer = MediaPlayer.create(MainActivity.this, Uri.parse(locations.get(currentPosition))); // Устанавливаем трек в зависимости от currentPosition.
        mediaPlayer.start();
        seekBar_player.setMax(mediaPlayer.getDuration()); // Устанавливаем максимальное значение seekbar, в зависимости от продолжительности трека.
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                UpdateSeekbar(); // Запускаем обновление seekbar.
            }
        });
    }

    private void UpdateSeekbar() // Обновляем прогресс Seekbar-a каждую миллисекунду.
    {

        if (!isTouch) seekBar_player.setProgress(mediaPlayer.getCurrentPosition());
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                UpdateSeekbar();
            }
        }, 1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Проверка разрешения на чтение файлов в устройстве.
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE))
            {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
        else
        {
            getMusicFiles(); // Получаем файлы, если пользователь разрешил чтение файлов ранее.
        }


        trackName = findViewById(R.id.textView_TracksName);
        seekBar_player = findViewById(R.id.seekbar_player);
        seekBar_player.setEnabled(false);
        btn_play = findViewById(R.id.btn_play_and_pause);
        final Button btn_stop = findViewById(R.id.btn_stop);

        seekBar_player.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // Изменение состояния seekbar.
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { isTouch = true; }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                mediaPlayer.seekTo(seekBar.getProgress());
                if (seekBar.getProgress() == seekBar.getMax()) // Если пользователь дотянул значение seekbar-a до максимума.
                {
                    // Включаем следующий по списку, трек.
                    currentPosition++;
                    if (currentPosition >= tracks.size()) currentPosition = 0; // Если это был последний трек из списка,
                    // то заново проигрываем треки, начиная с начала.
                    mediaPlayer.stop();
                    PlayTrack();
                }
                isTouch = false;
            }
        });


       btn_play.setOnClickListener(new View.OnClickListener() { // Нажатие на кнопку Старт\Пауза.
           @Override
           public void onClick(View v) {
               if (mediaPlayer != null)
               {
                    if (!mediaPlayer.isPlaying()) // Если трек не проигрывается.
                    {
                        btn_play.setBackgroundResource(R.drawable.button_pause_style);
                        mediaPlayer.start();
                    }
                    else // Если трек проигрывается.
                    {
                        btn_play.setBackgroundResource(R.drawable.button_play_style);
                        mediaPlayer.pause();
                    }
               }
               else
               {
                   if (adapter != null) // Если пользователь дал разрешение на чтение файлов.
                   {
                        seekBar_player.setEnabled(true);
                        currentPosition = 0;
                        PlayTrack();
                   }
               }
           }
       });

       btn_stop.setOnClickListener(new View.OnClickListener() { // Обработка кнопки - Стоп.
           @Override
           public void onClick(View v) {
               if (mediaPlayer != null)
               {
                   // Делаем паузу, но проигрываем трек с начала.
                   btn_play.setBackgroundResource(R.drawable.button_play_style);
                   seekBar_player.setProgress(0);
                   mediaPlayer.pause();
                   mediaPlayer.seekTo(0);
               }
           }
       });


    }

    @Override
    public void onBackPressed() { // Системная кнопка - Назад
        // Выходим из приложения.
        if (mediaPlayer != null)
        {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
        }
        moveTaskToBack(true);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { // Проверка результата
        if (requestCode == 1)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // Если пользователь дал разрешение на чтение файлов.
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Теперь ты можешь слушать свои треки. :)", Toast.LENGTH_LONG).show();
                    getMusicFiles();
                }
            }
            else // Если пользователь не дал разрешение на чтение файлов.
            {
                String text = "Дай мне доступ к чтению твоих аудиотреков, чтобы ты мог их прослушивать";
                trackName.setText(text);
                Toast.makeText(this, text, Toast.LENGTH_LONG).show();
            }
        }
    }
}
