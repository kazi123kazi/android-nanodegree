package com.example.xyzreader.ui;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.util.Toolbox;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class ArticleListAdapter extends RecyclerView.Adapter<ArticleListAdapter.ArticleListViewHolder> {

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    private Activity mActivity;
    private Cursor mCursor;
    private ArticleListViewHolderListener mViewHolderListener;

    /**
     * This monitors item clicks as well as entire view holder events
     */
    interface ArticleListViewHolderListener {

        /**
         * For handling click events. The purpose of this is to launch the appropriate fragment
         * pertaining to the itemId clicked, and also setting the shared element image view.
         * @param iv
         * @param itemId
         */
        void onItemClicked(ImageView iv, long itemId, int adapterPosition);

        /**
         * This is called for each view, after image is loaded. The purpose of this is to check
         * when the shared image has loaded. When this is the case, start the shared elements
         * transition
         *
         * @param view
         * @param adapterPosition
         */
        void onLoadCompleted(ImageView view, int adapterPosition);
    }

    public ArticleListAdapter(Activity activity, Cursor cursor) {
        mActivity = activity;
        mCursor = cursor;
        mViewHolderListener = new ArticleListViewHolderListenerImpl(activity);
    }

    public ArticleListAdapter(Activity activity) {
        mActivity = activity;
        mViewHolderListener = new ArticleListViewHolderListenerImpl(activity);
    }

    public void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @NonNull
    @Override
    public ArticleListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mActivity).inflate(R.layout.list_item_article, parent, false);
        return new ArticleListViewHolder(view);
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Timber.e(ex);
            Timber.i("passing today's date");
            return new Date();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final ArticleListViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        holder.authorView.setText(mCursor.getString(ArticleLoader.Query.AUTHOR));
        holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
        String bodyPreview = mCursor.getString(ArticleLoader.Query.BODY);
        int bodyCharLimit = holder.itemView.getContext().getResources().getInteger(R.integer.body_preview_upper_limit);
        if (bodyPreview.length() > bodyCharLimit) {
            // limit the body preview to lessen load on OS
            bodyPreview = bodyPreview.substring(0, bodyCharLimit);
        }
        holder.bodyPreviewView.setText(bodyPreview);
        Date publishedDate = parsePublishedDate();
        if (!publishedDate.before(START_OF_EPOCH.getTime())) {

            holder.subtitleView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            publishedDate.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString())
            );
        } else {
            holder.subtitleView.setText(Html.fromHtml(
                    outputFormat.format(publishedDate))
            );
        }

        RequestListener<Bitmap> listener = new RequestListener<Bitmap>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                mViewHolderListener.onLoadCompleted(holder.thumbnailView, holder.getAdapterPosition());
                holder.pbThumbnail.setVisibility(View.GONE);
                Timber.e(e, "There was a problem loading the list image thumbnail");
                return false;
            }

            @Override
            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                mViewHolderListener.onLoadCompleted(holder.thumbnailView, holder.getAdapterPosition());
                holder.pbThumbnail.setVisibility(View.GONE);
                return false;
            }
        };
        holder.pbThumbnail.setVisibility(View.VISIBLE);
        Toolbox.loadThumbnailFromUrl(holder.itemView.getContext(),
                mCursor.getString(ArticleLoader.Query.THUMB_URL),
                holder.thumbnailView, listener);
        // For shared elements transitions, make sure the transition name is unique per item + view
        //ViewCompat.setTransitionName(holder.thumbnailView, "image" + getItemId(position));
        holder.thumbnailView.setTransitionName("image" + getItemId(holder.getAdapterPosition()));

        // set-up article actions button
        holder.ibActions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toolbox.showArticleActionsMenuPopup(mActivity, v, mCursor, holder.getLayoutPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        } else {
            return 0;
        }
    }

    private class ArticleListViewHolderListenerImpl implements ArticleListViewHolderListener {
        private Activity activity;
        private AtomicBoolean enterTransitionStarted;
        private SharedPreferences sp;

        ArticleListViewHolderListenerImpl(Activity activity) {
            this.activity = activity;
            this.enterTransitionStarted = new AtomicBoolean();
            this.sp = activity.getSharedPreferences(ArticleListActivity.SHARED_PREFERENCES,
                    Context.MODE_PRIVATE);
        }

        @Override
        public void onItemClicked(ImageView iv, long itemId, int adapterPosition) {
            // Note the position needs to be updated before calling makeSceneTransitionAnimation(),
            // otherwise the old position will be used when animating the elements
            sp.edit()
                    .putInt(ArticleListActivity.KEY_CURRENT_POSITION, adapterPosition)
                    .apply();

            // TROD: This intent doesn't fire up the detail activity directly. It launches it
            // through URI
            Intent intent = new Intent(Intent.ACTION_VIEW, ItemsContract.Items.buildItemUri(itemId));
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity, iv, ViewCompat.getTransitionName(iv)); // after this, SharedElementCallback is triggered

            //activity.startActivity(intent, options.toBundle());
//                startActivity(intent);
            activity.startActivityForResult(intent, 100, options.toBundle());
        }

        @Override
        public void onLoadCompleted(ImageView view, int adapterPosition) {
            if (sp.getInt(ArticleListActivity.KEY_CURRENT_POSITION, 0) != adapterPosition) return;
            if (enterTransitionStarted.getAndSet(true)) return;
            activity.startPostponedEnterTransition();
        }
    }

    public class ArticleListViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        @BindView(R.id.article_thumbnail)
        ImageView thumbnailView;
        @BindView(R.id.details_article_title)
        TextView titleView;
        @BindView(R.id.article_date)
        TextView subtitleView;
        @BindView(R.id.details_article_author)
        TextView authorView;
        @BindView(R.id.article_body_preview)
        TextView bodyPreviewView;
        @BindView(R.id.ib_action_menu)
        ImageButton ibActions;
        @BindView(R.id.pb_article_thumbnail)
        ProgressBar pbThumbnail;

        ArticleListViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mViewHolderListener.onItemClicked(thumbnailView,
                    ArticleListAdapter.this.getItemId(getAdapterPosition()), getAdapterPosition());
        }
    }
}