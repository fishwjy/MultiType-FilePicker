package com.vincent.filepicker.adapter;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.vincent.filepicker.R;
import com.vincent.filepicker.ToastUtil;
import com.vincent.filepicker.Util;
import com.vincent.filepicker.activity.VideoPickActivity;
import com.vincent.filepicker.filter.entity.VideoFile;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static android.os.Environment.DIRECTORY_DCIM;
import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static com.vincent.filepicker.Constant.REQUEST_CODE_TAKE_VIDEO;

/**
 * Created by Vincent Woo
 * Date: 2016/10/21
 * Time: 14:13
 */

public class VideoPickAdapter extends BaseAdapter<VideoFile, VideoPickAdapter.VideoPickViewHolder> {
    private boolean isNeedCamera;
    private int mMaxNumber;
    private int mCurrentNumber = 0;
    public String mVideoPath;

    public VideoPickAdapter(Context ctx, boolean needCamera, int max) {
        this(ctx, new ArrayList<VideoFile>(), needCamera, max);
    }

    public VideoPickAdapter(Context ctx, ArrayList<VideoFile> list, boolean needCamera, int max) {
        super(ctx, list);
        isNeedCamera = needCamera;
        mMaxNumber = max;
    }

    @Override
    public VideoPickAdapter.VideoPickViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.vw_layout_item_video_pick, parent, false);
        ViewGroup.LayoutParams params = itemView.getLayoutParams();
        if (params != null) {
            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            int width = wm.getDefaultDisplay().getWidth();
            params.height = width / VideoPickActivity.COLUMN_NUMBER;
        }
        return new VideoPickViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final VideoPickViewHolder holder, int position) {
        if (isNeedCamera && position == 0) {
            holder.mIvCamera.setVisibility(View.VISIBLE);
            holder.mIvThumbnail.setVisibility(View.INVISIBLE);
            holder.mCbx.setVisibility(View.INVISIBLE);
            holder.mShadow.setVisibility(View.INVISIBLE);
            holder.mDurationLayout.setVisibility(View.INVISIBLE);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
                    File file = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_DCIM).getAbsolutePath()
                            + "/VID_" + timeStamp + ".mp4");
                    mVideoPath = file.getAbsolutePath();

                    ContentValues contentValues = new ContentValues(1);
                    contentValues.put(MediaStore.Images.Media.DATA, mVideoPath);
                    Uri uri = mContext.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

                    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                    intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
                    if (Util.detectIntent(mContext, intent)) {
                        ((Activity) mContext).startActivityForResult(intent, REQUEST_CODE_TAKE_VIDEO);
                    } else {
                        ToastUtil.getInstance(mContext).showToast(mContext.getString(R.string.vw_no_video_app));
                    }
                }
            });
        } else {
            holder.mIvCamera.setVisibility(View.INVISIBLE);
            holder.mIvThumbnail.setVisibility(View.VISIBLE);
            holder.mCbx.setVisibility(View.VISIBLE);
            holder.mDurationLayout.setVisibility(View.VISIBLE);

            final VideoFile file;
            if (isNeedCamera) {
                file = mList.get(position - 1);
            } else {
                file = mList.get(position);
            }

            RequestOptions options = new RequestOptions();
            Glide.with(mContext)
                    .load(file.getPath())
                    .apply(options.centerCrop())
                    .transition(withCrossFade())
//                    .transition(new DrawableTransitionOptions().crossFade(500))
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
                        ToastUtil.getInstance(mContext).showToast(R.string.vw_up_to_max);
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
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri uri;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        File f = new File(file.getPath());
                        uri = FileProvider.getUriForFile(mContext, mContext.getApplicationContext().getPackageName() + ".provider", f);
                    }else{
                        uri = Uri.parse("file://" + file.getPath());
                    }
                    intent.setDataAndType(uri, "video/mp4");
                    if (Util.detectIntent(mContext, intent)) {
                        mContext.startActivity(intent);
                    } else {
                        ToastUtil.getInstance(mContext).showToast(mContext.getString(R.string.vw_no_video_play_app));
                    }
                }
            });

            holder.mDuration.setText(Util.getDurationString(file.getDuration()));
        }
    }

    @Override
    public int getItemCount() {
        return isNeedCamera ? mList.size() + 1 : mList.size();
    }

    class VideoPickViewHolder extends RecyclerView.ViewHolder {
        private ImageView mIvCamera;
        private ImageView mIvThumbnail;
        private View mShadow;
        private ImageView mCbx;
        private TextView mDuration;
        private RelativeLayout mDurationLayout;

        public VideoPickViewHolder(View itemView) {
            super(itemView);
            mIvCamera = (ImageView) itemView.findViewById(R.id.iv_camera);
            mIvThumbnail = (ImageView) itemView.findViewById(R.id.iv_thumbnail);
            mShadow = itemView.findViewById(R.id.shadow);
            mCbx = (ImageView) itemView.findViewById(R.id.cbx);
            mDuration = (TextView) itemView.findViewById(R.id.txt_duration);
            mDurationLayout = (RelativeLayout) itemView.findViewById(R.id.layout_duration);
        }
    }

    public boolean isUpToMax() {
        return mCurrentNumber >= mMaxNumber;
    }

    public void setCurrentNumber(int number) {
        mCurrentNumber = number;
    }
}
