package com.vincent.filepicker.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vincent.filepicker.Constant;
import com.vincent.filepicker.DividerGridItemDecoration;
import com.vincent.filepicker.R;
import com.vincent.filepicker.adapter.FolderListAdapter;
import com.vincent.filepicker.adapter.ImagePickAdapter;
import com.vincent.filepicker.adapter.OnSelectStateListener;
import com.vincent.filepicker.filter.FileFilter;
import com.vincent.filepicker.filter.callback.FilterResultCallback;
import com.vincent.filepicker.filter.entity.Directory;
import com.vincent.filepicker.filter.entity.ImageFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Vincent Woo
 * Date: 2016/10/12
 * Time: 16:41
 */

public class ImagePickActivity extends BaseActivity {
    public static final String IS_NEED_CAMERA = "IsNeedCamera";
    public static final String IS_NEED_IMAGE_PAGER = "IsNeedImagePager";
    public static final String IS_TAKEN_AUTO_SELECTED = "IsTakenAutoSelected";

    public static final int DEFAULT_MAX_NUMBER = 9;
    public static final int COLUMN_NUMBER = 3;
    private int mMaxNumber;
    private int mCurrentNumber = 0;
    private RecyclerView mRecyclerView;
    private ImagePickAdapter mAdapter;
    private boolean isNeedCamera;
    private boolean isNeedImagePager;
    private boolean isTakenAutoSelected;
    public ArrayList<ImageFile> mSelectedList = new ArrayList<>();
    private List<Directory<ImageFile>> mAll;

    private TextView tv_count;
    private TextView tv_folder;
    private LinearLayout ll_folder;
    private RelativeLayout rl_done;
    private RelativeLayout tb_pick;

    @Override
    void permissionGranted() {
        loadData();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vw_activity_image_pick);

        mMaxNumber = getIntent().getIntExtra(Constant.MAX_NUMBER, DEFAULT_MAX_NUMBER);
        isNeedCamera = getIntent().getBooleanExtra(IS_NEED_CAMERA, false);
        isNeedImagePager = getIntent().getBooleanExtra(IS_NEED_IMAGE_PAGER, true);
        isTakenAutoSelected = getIntent().getBooleanExtra(IS_TAKEN_AUTO_SELECTED, true);
        initView();
    }

    private void initView() {
        tv_count = (TextView) findViewById(R.id.tv_count);
        tv_count.setText(mCurrentNumber + "/" + mMaxNumber);

        mRecyclerView = (RecyclerView) findViewById(R.id.rv_image_pick);
        final GridLayoutManager layoutManager = new GridLayoutManager(this, COLUMN_NUMBER);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.addItemDecoration(new DividerGridItemDecoration(this));
        mAdapter = new ImagePickAdapter(this, isNeedCamera, isNeedImagePager, mMaxNumber);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnSelectStateListener(new OnSelectStateListener<ImageFile>() {
            @Override
            public void OnSelectStateChanged(boolean state, ImageFile file) {
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
                intent.putParcelableArrayListExtra(Constant.RESULT_PICK_IMAGE, mSelectedList);
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
                        for (Directory<ImageFile> dir : mAll) {
                            if (dir.getPath().equals(directory.getPath())) {
                                List<Directory<ImageFile>> list = new ArrayList<>();
                                list.add(dir);
                                refreshData(list);
                                break;
                            }
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constant.REQUEST_CODE_TAKE_IMAGE:
                if (resultCode == RESULT_OK) {
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    File file = new File(mAdapter.mImagePath);
                    Uri contentUri = Uri.fromFile(file);
                    mediaScanIntent.setData(contentUri);
                    sendBroadcast(mediaScanIntent);

                    loadData();
                } else {
                    //Delete the record in Media DB, when user select "Cancel" during take picture
                    getApplicationContext().getContentResolver().delete(mAdapter.mImageUri, null, null);
                }
                break;
            case Constant.REQUEST_CODE_BROWSER_IMAGE:
                if (resultCode == RESULT_OK) {
                    ArrayList<ImageFile> list = data.getParcelableArrayListExtra(Constant.RESULT_BROWSER_IMAGE);
                    mCurrentNumber = list.size();
                    mAdapter.setCurrentNumber(mCurrentNumber);
                    tv_count.setText(mCurrentNumber + "/" + mMaxNumber);
                    mSelectedList.clear();
                    mSelectedList.addAll(list);

                    for (ImageFile file : mAdapter.getDataSet()) {
                        if (mSelectedList.contains(file)) {
                            file.setSelected(true);
                        } else {
                            file.setSelected(false);
                        }
                    }
                    mAdapter.notifyDataSetChanged();
                }
                break;
        }
    }

    private void loadData() {
        FileFilter.getImages(this, new FilterResultCallback<ImageFile>() {
            @Override
            public void onResult(List<Directory<ImageFile>> directories) {
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

    private void refreshData(List<Directory<ImageFile>> directories) {
        boolean tryToFindTakenImage = isTakenAutoSelected;

        // if auto-select taken image is enabled, make sure requirements are met
        if (tryToFindTakenImage && !TextUtils.isEmpty(mAdapter.mImagePath)) {
            File takenImageFile = new File(mAdapter.mImagePath);
            tryToFindTakenImage = !mAdapter.isUpToMax() && takenImageFile.exists(); // try to select taken image only if max isn't reached and the file exists
        }

        List<ImageFile> list = new ArrayList<>();
        for (Directory<ImageFile> directory : directories) {
            list.addAll(directory.getFiles());

            // auto-select taken images?
            if (tryToFindTakenImage) {
                findAndAddTakenImage(directory.getFiles());   // if taken image was found, we're done
            }
        }

        for (ImageFile file : mSelectedList) {
            int index = list.indexOf(file);
            if (index != -1) {
                list.get(index).setSelected(true);
            }
        }
        mAdapter.refresh(list);
    }

    private boolean findAndAddTakenImage(List<ImageFile> list) {
        for (ImageFile imageFile : list) {
            if (imageFile.getPath().equals(mAdapter.mImagePath)) {
                mSelectedList.add(imageFile);
                mCurrentNumber++;
                mAdapter.setCurrentNumber(mCurrentNumber);
                tv_count.setText(mCurrentNumber + "/" + mMaxNumber);

                return true;   // taken image was found and added
            }
        }
        return false;    // taken image wasn't found
    }

    private void refreshSelectedList(List<ImageFile> list) {
        for (ImageFile file : list) {
            if(file.isSelected() && !mSelectedList.contains(file)) {
                mSelectedList.add(file);
            }
        }
    }
}
