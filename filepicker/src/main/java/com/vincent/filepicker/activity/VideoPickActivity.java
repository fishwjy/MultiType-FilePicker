package com.vincent.filepicker.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.vincent.filepicker.Constant;
import com.vincent.filepicker.DividerGridItemDecoration;
import com.vincent.filepicker.R;
import com.vincent.filepicker.adapter.OnSelectStateListener;
import com.vincent.filepicker.adapter.VideoPickAdapter;
import com.vincent.filepicker.filter.FileFilter;
import com.vincent.filepicker.filter.callback.FilterResultCallback;
import com.vincent.filepicker.filter.entity.Directory;
import com.vincent.filepicker.filter.entity.VideoFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Vincent Woo
 * Date: 2016/10/21
 * Time: 14:02
 */

public class VideoPickActivity extends BaseActivity {
    public static final String THUMBNAIL_PATH = "FilePick";
    public static final String IS_NEED_CAMERA = "IsNeedCamera";
    public static final String IS_TAKEN_AUTO_SELECTED = "IsTakenAutoSelected";

    public static final int DEFAULT_MAX_NUMBER = 9;
    public static final int COLUMN_NUMBER = 3;
    private int mMaxNumber;
    private int mCurrentNumber = 0;
    private Toolbar mTbImagePick;
    private RecyclerView mRecyclerView;
    private VideoPickAdapter mAdapter;
    private boolean isNeedCamera;
    private boolean isTakenAutoSelected;
    private ArrayList<VideoFile> mSelectedList = new ArrayList<>();
    private ProgressBar mProgressBar;

    @Override
    void permissionGranted() {
        loadData();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.vw_activity_video_pick);

        mMaxNumber = getIntent().getIntExtra(Constant.MAX_NUMBER, DEFAULT_MAX_NUMBER);
        isNeedCamera = getIntent().getBooleanExtra(IS_NEED_CAMERA, false);
        isTakenAutoSelected = getIntent().getBooleanExtra(IS_TAKEN_AUTO_SELECTED, true);
        initView();

        super.onCreate(savedInstanceState);
    }

    private void initView() {
        mTbImagePick = (Toolbar) findViewById(R.id.tb_video_pick);
        mTbImagePick.setTitle(mCurrentNumber + "/" + mMaxNumber);
        setSupportActionBar(mTbImagePick);
        mTbImagePick.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.rv_video_pick);
        GridLayoutManager layoutManager = new GridLayoutManager(this, COLUMN_NUMBER);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.addItemDecoration(new DividerGridItemDecoration(this));

        mAdapter = new VideoPickAdapter(this, isNeedCamera, mMaxNumber);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnSelectStateListener(new OnSelectStateListener<VideoFile>() {
            @Override
            public void OnSelectStateChanged(boolean state, VideoFile file) {
                if (state) {
                    mSelectedList.add(file);
                    mCurrentNumber++;
                } else {
                    mSelectedList.remove(file);
                    mCurrentNumber--;
                }
                mTbImagePick.setTitle(mCurrentNumber + "/" + mMaxNumber);
            }
        });

        mProgressBar = (ProgressBar) findViewById(R.id.pb_video_pick);
        File folder = new File(getExternalCacheDir().getAbsolutePath() + File.separator + THUMBNAIL_PATH);
        if (!folder.exists()) {
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mProgressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constant.REQUEST_CODE_TAKE_VIDEO:
                if (resultCode == RESULT_OK) {
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    File file = new File(mAdapter.mVideoPath);
                    Uri contentUri = Uri.fromFile(file);
                    mediaScanIntent.setData(contentUri);
                    sendBroadcast(mediaScanIntent);

                    loadData();
                }
                break;
        }
    }

    private void loadData() {
        FileFilter.getVideos(this, new FilterResultCallback<VideoFile>() {
            @Override
            public void onResult(List<Directory<VideoFile>> directories) {
                mProgressBar.setVisibility(View.GONE);
                boolean tryToFindTaken = isTakenAutoSelected;

                // if auto-select taken file is enabled, make sure requirements are met
                if (tryToFindTaken && !TextUtils.isEmpty(mAdapter.mVideoPath)) {
                    File takenFile = new File(mAdapter.mVideoPath);
                    tryToFindTaken = !mAdapter.isUpToMax() && takenFile.exists(); // try to select taken file only if max isn't reached and the file exists
                }

                List<VideoFile> list = new ArrayList<>();
                for (Directory<VideoFile> directory : directories) {
                    list.addAll(directory.getFiles());

                    // auto-select taken file?
                    if (tryToFindTaken) {
                        tryToFindTaken = findAndAddTaken(directory.getFiles());   // if taken file was found, we're done
                    }
                }

                for (VideoFile file : mSelectedList) {
                    int index = list.indexOf(file);
                    if (index != -1) {
                        list.get(index).setSelected(true);
                    }
                }
                mAdapter.refresh(list);
            }
        });
    }

    private boolean findAndAddTaken(List<VideoFile> list) {
        for (VideoFile videoFile : list) {
            if (videoFile.getPath().equals(mAdapter.mVideoPath)) {
                mSelectedList.add(videoFile);
                mCurrentNumber++;
                mAdapter.setCurrentNumber(mCurrentNumber);
                mTbImagePick.setTitle(mCurrentNumber + "/" + mMaxNumber);

                return true;   // taken file was found and added
            }
        }
        return false;    // taken file wasn't found
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.vw_menu_image_pick, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_done) {
            Intent intent = new Intent();
            intent.putParcelableArrayListExtra(Constant.RESULT_PICK_VIDEO, mSelectedList);
            setResult(RESULT_OK, intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
