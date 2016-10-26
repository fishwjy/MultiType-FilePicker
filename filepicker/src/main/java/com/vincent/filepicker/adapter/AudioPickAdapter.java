package com.vincent.filepicker.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.vincent.filepicker.R;
import com.vincent.filepicker.Util;
import com.vincent.filepicker.filter.entity.AudioFile;

import java.util.ArrayList;

/**
 * Created by Vincent Woo
 * Date: 2016/10/25
 * Time: 10:57
 */

public class AudioPickAdapter extends BaseAdapter<AudioFile, AudioPickAdapter.AudioPickViewHolder> {
    private int mMaxNumber;
    private int mCurrentNumber = 0;

    public AudioPickAdapter(Context ctx, int max) {
        this(ctx, new ArrayList<AudioFile>(), max);
    }

    public AudioPickAdapter(Context ctx, ArrayList<AudioFile> list, int max) {
        super(ctx, list);
        mMaxNumber = max;
    }

    @Override
    public AudioPickViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.layout_item_audio_pick, parent, false);
        return new AudioPickViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final AudioPickViewHolder holder, final int position) {
        final AudioFile file = mList.get(position);

        holder.mTvTitle.setText(file.getName());
        holder.mTvTitle.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        if (holder.mTvTitle.getMeasuredWidth() >
                Util.getScreenWidth(mContext) - Util.dip2px(mContext, 10 + 32 + 10 + 48 + 10 * 2)) {
            holder.mTvTitle.setLines(2);
        } else {
            holder.mTvTitle.setLines(1);
        }

        holder.mTvDuration.setText(Util.getDurationString(file.getDuration()));
        if (file.isSelected()) {
            holder.mCbx.setSelected(true);
        } else {
            holder.mCbx.setSelected(false);
        }

        holder.mCbx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!v.isSelected() && isUpToMax()) {
                    return;
                }

                if (v.isSelected()) {
                    holder.mCbx.setSelected(false);
                    mCurrentNumber--;
                } else {
                    holder.mCbx.setSelected(true);
                    mCurrentNumber++;
                }

                mList.get(holder.getAdapterPosition()).setSelected(holder.mCbx.isSelected());

                if (mListener != null) {
                    mListener.OnSelectStateChanged(holder.mCbx.isSelected(), mList.get(holder.getAdapterPosition()));
                }
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("file://" + file.getPath());
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "audio/mp3");
                if (Util.detectIntent(mContext, intent)) {
                    mContext.startActivity(intent);
                } else {
                    Toast.makeText(mContext, "No Application exists for audio!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    class AudioPickViewHolder extends RecyclerView.ViewHolder {
        private TextView mTvTitle;
        private TextView mTvDuration;
        private ImageView mCbx;

        public AudioPickViewHolder(View itemView) {
            super(itemView);
            mTvTitle = (TextView) itemView.findViewById(R.id.tv_audio_title);
            mTvDuration = (TextView) itemView.findViewById(R.id.tv_duration);
            mCbx = (ImageView) itemView.findViewById(R.id.cbx);
        }
    }

    private boolean isUpToMax() {
        return mCurrentNumber >= mMaxNumber;
    }
}
