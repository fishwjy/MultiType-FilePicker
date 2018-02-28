package com.vincent.filepicker.activity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vincent.filepicker.Constant;
import com.vincent.filepicker.DividerListItemDecoration;
import com.vincent.filepicker.R;
import com.vincent.filepicker.ToastUtil;
import com.vincent.filepicker.Util;
import com.vincent.filepicker.adapter.AudioPickAdapter;
import com.vincent.filepicker.adapter.FolderListAdapter;
import com.vincent.filepicker.adapter.OnSelectStateListener;
import com.vincent.filepicker.filter.FileFilter;
import com.vincent.filepicker.filter.callback.FilterResultCallback;
import com.vincent.filepicker.filter.entity.AudioFile;
import com.vincent.filepicker.filter.entity.Directory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Vincent Woo
 * Date: 2016/10/21
 * Time: 17:31
 */

public class AudioPickActivity extends BaseActivity {
    public static final String IS_NEED_RECORDER = "IsNeedRecorder";
    public static final String IS_TAKEN_AUTO_SELECTED = "IsTakenAutoSelected";

    public static final int DEFAULT_MAX_NUMBER = 9;
    private int mMaxNumber;
    private int mCurrentNumber = 0;
    private RecyclerView mRecyclerView;
    private AudioPickAdapter mAdapter;
    private boolean isNeedRecorder;
    private boolean isTakenAutoSelected;
    private ArrayList<AudioFile> mSelectedList = new ArrayList<>();
    private List<Directory<AudioFile>> mAll;
    private String mAudioPath;

    private TextView tv_count;
    private TextView tv_folder;
    private LinearLayout ll_folder;
    private RelativeLayout rl_done;
    private RelativeLayout tb_pick;
    private RelativeLayout rl_rec_aud;

    @Override
    void permissionGranted() {
        loadData();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vw_activity_audio_pick);

        mMaxNumber = getIntent().getIntExtra(Constant.MAX_NUMBER, DEFAULT_MAX_NUMBER);
        isNeedRecorder = getIntent().getBooleanExtra(IS_NEED_RECORDER, false);
        isTakenAutoSelected = getIntent().getBooleanExtra(IS_TAKEN_AUTO_SELECTED, true);
        initView();
    }

    private void initView() {
        tv_count = (TextView) findViewById(R.id.tv_count);
        tv_count.setText(mCurrentNumber + "/" + mMaxNumber);

        mRecyclerView = (RecyclerView) findViewById(R.id.rv_audio_pick);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.addItemDecoration(new DividerListItemDecoration(this,
                LinearLayoutManager.VERTICAL, R.drawable.vw_divider_rv_file));
        mAdapter = new AudioPickAdapter(this, mMaxNumber);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnSelectStateListener(new OnSelectStateListener<AudioFile>() {
            @Override
            public void OnSelectStateChanged(boolean state, AudioFile file) {
                if (state) {
                    mSelectedList.add(file);
                    mCurrentNumber++;
                } else {
                    mSelectedList.remove(file);
                    mCurrentNumber--;
                }
                tv_count.setText(mCurrentNumber + "/" + mMaxNumber);
            }
        });

        rl_done = (RelativeLayout) findViewById(R.id.rl_done);
        rl_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putParcelableArrayListExtra(Constant.RESULT_PICK_AUDIO, mSelectedList);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        tb_pick = (RelativeLayout) findViewById(R.id.tb_pick);
        ll_folder = (LinearLayout) findViewById(R.id.ll_folder);
        if (isNeedFolderList) {
            ll_folder.setVisibility(View.VISIBLE);
            ll_folder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFolderHelper.toggle(tb_pick);
                }
            });
            tv_folder = (TextView) findViewById(R.id.tv_folder);
            tv_folder.setText(getResources().getString(R.string.vw_all));

            mFolderHelper.setFolderListListener(new FolderListAdapter.FolderListListener() {
                @Override
                public void onFolderListClick(Directory directory) {
                    mFolderHelper.toggle(tb_pick);
                    tv_folder.setText(directory.getName());

                    if (TextUtils.isEmpty(directory.getPath())) { //All
                        refreshData(mAll);
                    } else {
                        for (Directory<AudioFile> dir : mAll) {
                            if (dir.getPath().equals(directory.getPath())) {
                                List<Directory<AudioFile>> list = new ArrayList<>();
                                list.add(dir);
                                refreshData(list);
                                break;
                            }
                        }
                    }
                }
            });
        }

        if (isNeedRecorder) {
            rl_rec_aud = (RelativeLayout) findViewById(R.id.rl_rec_aud);
            rl_rec_aud.setVisibility(View.VISIBLE);
            rl_rec_aud.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
                    if (Util.detectIntent(AudioPickActivity.this, intent)) {
                        startActivityForResult(intent, Constant.REQUEST_CODE_TAKE_AUDIO);
                    } else {
                        ToastUtil.getInstance(AudioPickActivity.this).showToast(getString(R.string.vw_no_audio_app));
                    }
                }
            });
        }
    }

    private void loadData() {
        FileFilter.getAudios(this, new FilterResultCallback<AudioFile>() {
            @Override
            public void onResult(List<Directory<AudioFile>> directories) {
                // Refresh folder list
                if (isNeedFolderList) {
                    ArrayList<Directory> list = new ArrayList<>();
                    Directory all = new Directory();
                    all.setName(getResources().getString(R.string.vw_all));
                    list.add(all);
                    list.addAll(directories);
                    mFolderHelper.fillData(list);
                }

                mAll = directories;
                refreshData(directories);
            }
        });
    }

    private void refreshData(List<Directory<AudioFile>> directories) {
        boolean tryToFindTaken = isTakenAutoSelected;

        // if auto-select taken file is enabled, make sure requirements are met
        if (tryToFindTaken && !TextUtils.isEmpty(mAudioPath)) {
            File takenFile = new File(mAudioPath);
            tryToFindTaken = !mAdapter.isUpToMax() && takenFile.exists(); // try to select taken file only if max isn't reached and the file exists
        }

        List<AudioFile> list = new ArrayList<>();
        for (Directory<AudioFile> directory : directories) {
            list.addAll(directory.getFiles());

            // auto-select taken file?
            if (tryToFindTaken) {
                tryToFindTaken = findAndAddTaken(directory.getFiles());   // if taken file was found, we're done
            }
        }

        for (AudioFile file : mSelectedList) {
            int index = list.indexOf(file);
            if (index != -1) {
                list.get(index).setSelected(true);
            }
        }
        mAdapter.refresh(list);
    }

    private boolean findAndAddTaken(List<AudioFile> list) {
        for (AudioFile audioFile : list) {
            if (audioFile.getPath().equals(mAudioPath)) {
                mSelectedList.add(audioFile);
                mCurrentNumber++;
                mAdapter.setCurrentNumber(mCurrentNumber);
                tv_count.setText(mCurrentNumber + "/" + mMaxNumber);

                return true;   // taken file was found and added
            }
        }
        return false;    // taken file wasn't found
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constant.REQUEST_CODE_TAKE_AUDIO:
                if (resultCode == RESULT_OK) {
                    if (data.getData() != null) {
                        mAudioPath = data.getData().getPath();
                    }
                    loadData();
                }
                break;
        }
    }
}
