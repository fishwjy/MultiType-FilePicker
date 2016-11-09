package com.vincent.filepicker.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.vincent.filepicker.Constant;
import com.vincent.filepicker.DividerGridItemDecoration;
import com.vincent.filepicker.R;
import com.vincent.filepicker.adapter.ImagePickAdapter;
import com.vincent.filepicker.adapter.OnSelectStateListener;
import com.vincent.filepicker.filter.FileFilter;
import com.vincent.filepicker.filter.callback.FilterResultCallback;
import com.vincent.filepicker.filter.entity.Directory;
import com.vincent.filepicker.filter.entity.ImageFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.vincent.filepicker.activity.ImageBrowserActivity.IMAGE_BROWSER_SELECTED_NUMBER;

/**
 * Created by Vincent Woo
 * Date: 2016/10/12
 * Time: 16:41
 */

public class ImagePickActivity extends BaseActivity {
    public static final String IS_NEED_CAMERA = "IsNeedCamera";

    public static final int DEFAULT_MAX_NUMBER = 9;
    public static final int COLUMN_NUMBER = 3;
    private int mMaxNumber;
    private int mCurrentNumber = 0;
    private Toolbar mTbImagePick;
    private RecyclerView mRecyclerView;
    private ImagePickAdapter mAdapter;
    private boolean isNeedCamera;
    private ArrayList<ImageFile> mSelectedList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_image_pick);
        mMaxNumber = getIntent().getIntExtra(Constant.MAX_NUMBER, DEFAULT_MAX_NUMBER);
        isNeedCamera = getIntent().getBooleanExtra(IS_NEED_CAMERA, false);
        initView();
        super.onCreate(savedInstanceState);
    }

    @Override
    void permissionGranted() {
        loadData();
    }

    private void initView() {
        mTbImagePick = (Toolbar) findViewById(R.id.tb_image_pick);
        mTbImagePick.setTitle(mCurrentNumber + "/" + mMaxNumber);
        setSupportActionBar(mTbImagePick);
        mTbImagePick.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.rv_image_pick);
        GridLayoutManager layoutManager = new GridLayoutManager(this, COLUMN_NUMBER);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.addItemDecoration(new DividerGridItemDecoration(this));
        mAdapter = new ImagePickAdapter(this, isNeedCamera, mMaxNumber);
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
                mTbImagePick.setTitle(mCurrentNumber + "/" + mMaxNumber);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constant.REQUEST_CODE_TAKE_IMAGE:
                if (resultCode == RESULT_OK) {
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    File file = new File(mAdapter.mImagePath);
                    Uri contentUri = Uri.fromFile(file);
                    mediaScanIntent.setData(contentUri);
                    sendBroadcast(mediaScanIntent);

                    loadData();
                }
                break;
            case Constant.REQUEST_CODE_BROWSER_IMAGE:
                if (resultCode == RESULT_OK) {
                    ArrayList<ImageFile> list = data.getParcelableArrayListExtra(Constant.RESULT_BROWSER_IMAGE);
                    mCurrentNumber = data.getIntExtra(IMAGE_BROWSER_SELECTED_NUMBER, 0);
                    mAdapter.setCurrentNumber(mCurrentNumber);
                    mTbImagePick.setTitle(mCurrentNumber + "/" + mMaxNumber);
                    refreshSelectedList(list);
                    mAdapter.refresh(list);
                }
                break;
        }
    }

    private void loadData() {
        FileFilter.getImages(this, new FilterResultCallback<ImageFile>() {
            @Override
            public void onResult(List<Directory<ImageFile>> directories) {
                List<ImageFile> list = new ArrayList<>();
                for (Directory<ImageFile> directory : directories) {
                    list.addAll(directory.getFiles());
                }

                for (ImageFile file : mSelectedList) {
                    int index = list.indexOf(file);
                    if (index != -1) {
                        list.get(index).setSelected(true);
                    }
                }
                mAdapter.refresh(list);
            }
        });
    }

    private void refreshSelectedList(List<ImageFile> list) {
        for (ImageFile file : list) {
            if(file.isSelected() && !mSelectedList.contains(file)) {
                mSelectedList.add(file);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_image_pick, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_done) {
            Intent intent = new Intent();
            intent.putParcelableArrayListExtra(Constant.RESULT_PICK_IMAGE, mSelectedList);
            setResult(RESULT_OK, intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
