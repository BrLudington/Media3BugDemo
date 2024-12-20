package com.example.exoplayerbugdemo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.source.ConcatenatingMediaSource2;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.ui.PlayerView;

import java.util.ArrayList;


public class MainActivity extends Activity {
    public final static int PHOTO_PICKER_REQUEST_CODE = 1001;
    private final static int PERMISSION_CODE = 1002;
    private final static String MEDIA_PATHS = "media_paths";
    private final static String PLAYBACK_POSITION = "playback_position";
    private ExoPlayer mPlayer;
    private ArrayList<MediaSource> mMediaSources;
    private ArrayList<String> mMediaPaths;
    private long mPlaybackPosition = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mMediaSources = new ArrayList<MediaSource>();
        mMediaPaths = new ArrayList<String>();

        if (savedInstanceState != null){
            if (savedInstanceState.containsKey(MEDIA_PATHS)) {
                mMediaPaths = savedInstanceState.getStringArrayList(MEDIA_PATHS);
                reloadVideos();
            }
            if (savedInstanceState.containsKey(PLAYBACK_POSITION)) {
                mPlaybackPosition = savedInstanceState.getLong(PLAYBACK_POSITION);
            }
        }

        findViewById(R.id.add_video_button).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT < 33) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_CODE);
            } else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_VIDEO}, PERMISSION_CODE);
            } else {
                onAddVideoButtonTapped();
            }
        });

    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(MEDIA_PATHS, mMediaPaths);

        outState.putLong(PLAYBACK_POSITION, mPlaybackPosition);

    }

    @OptIn(markerClass = UnstableApi.class)
    private void reloadVideos() {
        for (String path : mMediaPaths) {
            Uri video = Uri.parse(path);
            MediaItem mediaItem = MediaItem.fromUri(video);
            MediaSource mediaSource = new DefaultMediaSourceFactory(this).createMediaSource(mediaItem);
            mMediaSources.add(mediaSource);
        }
    }

    private void onAddVideoButtonTapped() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("video/*");
        i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PHOTO_PICKER_REQUEST_CODE);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void addVideoToTotal(Intent data) {
        mPlayer.stop();
        mPlayer.clearMediaItems();
        String videoPath = data.getDataString();

        Uri video = Uri.parse(videoPath);
        MediaItem mediaItem = MediaItem.fromUri(video);
        MediaSource mediaSource = new DefaultMediaSourceFactory(this).createMediaSource(mediaItem);
        mMediaSources.add(mediaSource);
        mMediaPaths.add(videoPath);

        ConcatenatingMediaSource2.Builder builder = new ConcatenatingMediaSource2.Builder();
        builder.useDefaultMediaSourceFactory(this);
        for (MediaSource source : mMediaSources) {
            builder.add(source, 1);
        }

        mPlayer.setMediaSource(builder.build());
        mPlayer.prepare();
        mPlayer.play();
    }

    @OptIn(markerClass = UnstableApi.class)
    private void initializePlayer() {
        PlayerView playerView = findViewById(R.id.reel_video_viewer);
        if (mPlayer == null) {
            mPlayer = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(mPlayer);
        }

        mPlayer.setSeekParameters(SeekParameters.EXACT);

        mPlayer.addListener(new Player.Listener() {
            private int mediaSourcesReady = 0;
            @Override
            public void onTimelineChanged(Timeline timeline, int reason) {
//                Log.e(Constants.sLogTag, "onTimelineChanged reason " + reason);
                if (reason == Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE) {
                    if (mPlayer.getDuration() != mMediaSources.size()) {
                        mediaSourcesReady++;
                    }

                    long timelineDuration = 0;

                    for (int i = 0; i < timeline.getWindowCount(); i++) {
                        Timeline.Window window = new Timeline.Window();
                        timeline.getWindow(i, window);
                        timelineDuration += window.getDurationMs();
                    }

                    Log.e(Constants.sLogTag, "timelineDuration " + timelineDuration + " media sources ready " + mediaSourcesReady);//+ " mReel.getDurationTime() " + mReel.getDurationTime());
                    if (mediaSourcesReady == mMediaSources.size()) {
                        Log.e(Constants.sLogTag, "all media sources ready. Seek back to position " + mPlaybackPosition);
                        mPlayer.seekTo(mPlaybackPosition);
                        mediaSourcesReady = 0;
                    }
                }
            }
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
            }

            @Override
            public void onIsLoadingChanged(boolean isLoading) {
            }

            // Add timestamps to the UI when the player is set up and timeStampLayout is inflated.
            @Override
            public void onPlaybackStateChanged(@Player.State int state) {
                if (state == Player.STATE_READY) {
                    // update positions of time stamps and maximum value of rotary scrubber
                }
                else if (state == Player.STATE_ENDED) {

                }
                else if (state == Player.STATE_BUFFERING) {
                    //Log.d("BUFFERING", "position: " + player.getCurrentPosition());
                }
            }

            @Override
            public void onSurfaceSizeChanged(int width, int height) {
                // ToDo: figure out what to do with the size of the draw view.
            }
        });

        if (!mMediaSources.isEmpty()) {
            ConcatenatingMediaSource2.Builder builder = new ConcatenatingMediaSource2.Builder();
            builder.useDefaultMediaSourceFactory(this);
            for (MediaSource source : mMediaSources) {
                builder.add(source, 1);
            }

            mPlayer.setMediaSource(builder.build());
        }

        mPlayer.prepare();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mPlayer == null) {
            initializePlayer();
            mPlayer.seekTo(mPlaybackPosition);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPlayer == null) {
            initializePlayer();
            mPlayer.seekTo(mPlaybackPosition);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPlayer != null) {
            mPlaybackPosition = mPlayer.getCurrentPosition();
            mPlayer.release();
            mPlayer = null;
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == PHOTO_PICKER_REQUEST_CODE) {
            getContentResolver().takePersistableUriPermission(data.getData(), Intent.FLAG_GRANT_READ_URI_PERMISSION);
            addVideoToTotal(data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if(requestCode == PERMISSION_CODE) {
                onAddVideoButtonTapped();
            }
        } else {
            if (requestCode == PERMISSION_CODE) {
                Toast.makeText(this, "Go give the app video permissions", Toast.LENGTH_LONG).show();
            }
        }
    }
}
