package com.vincent.filepicker.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.vincent.filepicker.Constant;
import com.vincent.filepicker.R;
import com.vincent.filepicker.activity.ImageBrowserActivity;
import com.vincent.filepicker.activity.ImagePickActivity;
import com.vincent.filepicker.filter.entity.ImageFile;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static android.os.Environment.DIRECTORY_DCIM;
import static com.vincent.filepicker.activity.ImageBrowserActivity.IMAGE_BROWSER_INIT_INDEX;
import static com.vincent.filepicker.activity.ImageBrowserActivity.IMAGE_BROWSER_LIST;
import static com.vincent.filepicker.activity.ImageBrowserActivity.IMAGE_BROWSER_SELECTED_NUMBER;

/**
 * Created by Vincent Woo
 * Date: 2016/10/13
 * Time: 16:07
 */

public class ImagePickAdapter extends BaseAdapter<ImageFile, ImagePickAdapter.ImagePickViewHolder> {
    private boolean isNeedCamera;
    private int mMaxNumber;
    private int mCurrentNumber = 0;
    public String mImagePath;

    public ImagePickAdapter(Context ctx, boolean needCamera, int max) {
        this(ctx, new ArrayList<ImageFile>(), needCamera, max);
    }

    public ImagePickAdapter(Context ctx, ArrayList<ImageFile> list, boolean needCamera, int max) {
        super(ctx, list);
        isNeedCamera = needCamera;
        mMaxNumber = max;
    }

    @Override
    public ImagePickViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.layout_item_image_pick, parent, false);
        ViewGroup.LayoutParams params = itemView.getLayoutParams();
        if (params != null) {
            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            int width = wm.getDefaultDisplay().getWidth();
            params.height = width / ImagePickActivity.COLUMN_NUMBER;
        }
        return new ImagePickViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ImagePickViewHolder holder, int position) {
        if (isNeedCamera && position == 0) {
            holder.mIvCamera.setVisibility(View.VISIBLE);
            holder.mIvThumbnail.setVisibility(View.INVISIBLE);
            holder.mCbx.setVisibility(View.INVISIBLE);
            holder.mShadow.setVisibility(View.INVISIBLE);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
                    File file = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_DCIM).getAbsolutePath()
                            + "/IMG_" + timeStamp + ".jpg");
                    mImagePath = file.getAbsolutePath();
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
                    ((Activity) mContext).startActivityForResult(intent, Constant.REQUEST_CODE_TAKE_IMAGE);
                }
            });
        } else {
            holder.mIvCamera.setVisibility(View.INVISIBLE);
            holder.mIvThumbnail.setVisibility(View.VISIBLE);
            holder.mCbx.setVisibility(View.VISIBLE);

            ImageFile file;
            if (isNeedCamera) {
                file = mList.get(position - 1);
            } else {
                file = mList.get(position);
            }
            Glide.with(mContext)
                    .load(file.getPath())
                    .centerCrop()
                    .crossFade()
                    .into(holder.mIvThumbnail);

            if (file.isSelected()) {
                holder.mCbx.setSelected(true);
                holder.mShadow.setVisibility(View.VISIBLE);
            } else {
                holder.mCbx.setSelected(false);
                holder.mShadow.setVisibility(View.INVISIBLE);
            }

            holder.mCbx.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!v.isSelected() && isUpToMax()) {
                        return;
                    }

                    if (v.isSelected()) {
                        holder.mShadow.setVisibility(View.INVISIBLE);
                        holder.mCbx.setSelected(false);
                        mCurrentNumber--;
                    } else {
                        holder.mShadow.setVisibility(View.VISIBLE);
                        holder.mCbx.setSelected(true);
                        mCurrentNumber++;
                    }

                    int index = isNeedCamera ? holder.getAdapterPosition() - 1 : holder.getAdapterPosition();
                    mList.get(index).setSelected(holder.mCbx.isSelected());

                    if (mListener != null) {
                        mListener.OnSelectStateChanged(holder.mCbx.isSelected(), mList.get(index));
                    }
                }
            });

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, ImageBrowserActivity.class);
                    intent.putExtra(Constant.MAX_NUMBER, mMaxNumber);
                    intent.putExtra(IMAGE_BROWSER_INIT_INDEX,
                            isNeedCamera ? holder.getAdapterPosition() - 1 : holder.getAdapterPosition());
                    intent.putParcelableArrayListExtra(IMAGE_BROWSER_LIST, mList);
                    intent.putExtra(IMAGE_BROWSER_SELECTED_NUMBER, mCurrentNumber);
                    ((Activity) mContext).startActivityForResult(intent, Constant.REQUEST_CODE_BROWSER_IMAGE);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return isNeedCamera ? mList.size() + 1 : mList.size();
    }

    class ImagePickViewHolder extends RecyclerView.ViewHolder {
        private ImageView mIvCamera;
        private ImageView mIvThumbnail;
        private View mShadow;
        private ImageView mCbx;

        public ImagePickViewHolder(View itemView) {
            super(itemView);
            mIvCamera = (ImageView) itemView.findViewById(R.id.iv_camera);
            mIvThumbnail = (ImageView) itemView.findViewById(R.id.iv_thumbnail);
            mShadow = itemView.findViewById(R.id.shadow);
            mCbx = (ImageView) itemView.findViewById(R.id.cbx);
        }
    }

    private boolean isUpToMax() {
        return mCurrentNumber >= mMaxNumber;
    }

    public void setCurrentNumber(int number) {
        mCurrentNumber = number;
    }
}
