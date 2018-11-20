
package ru.malakhov.nytimes.ui;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import com.bumptech.glide.Glide;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ru.malakhov.nytimes.R;
import ru.malakhov.nytimes.data.room.NewsEntity;

public class ActivityFullNews extends AppCompatActivity {

    private static final int LAYOUT = R.layout.activity_full_news;
    private static final String EXTRA_ID = "EXTRA_ID";

    private Context mContext;
    private NewsEntity mNewsEntity;
    private Disposable mGetNews;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    @Override
    protected void onStop() {
        super.onStop();
        mCompositeDisposable.clear();
    }

    public static void start(@NonNull Context context, @NonNull String newsId) {
        context.startActivity(new Intent(context, ActivityFullNews.class)
                .putExtra(EXTRA_ID, newsId));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(LAYOUT);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getNews();
    }

    private void init() {
        mContext = this;
        setHomeButton();
    }

    private void getNews(){
        mGetNews = Single.fromCallable(() -> ConverterNews.getNewsById(mContext, getIntent().getStringExtra(EXTRA_ID)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(v -> {
                    mNewsEntity = v;
                    setDataViews();
                });
        mCompositeDisposable.add(mGetNews);
    }

    private void setHomeButton() {
        final Toolbar toolbar = findViewById(R.id.details_toolbar);
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setDataViews() {
        ((TextView) findViewById(R.id.details_title)).setText(mNewsEntity.getTitle());
        Glide.with(this).load(mNewsEntity.getImageUrl()).into(((ImageView) findViewById(R.id.details_image)));
        ((CollapsingToolbarLayout) findViewById(R.id.details_category)).setTitle(mNewsEntity.getSubsection());
        ((TextView) findViewById(R.id.details_date)).setText(mNewsEntity.getPublishedDate());
        ((TextView) findViewById(R.id.details_text)).setText(mNewsEntity.getAbstract());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_full_news, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_edit:
                ActivityEditorNews.start(this, mNewsEntity.getId());
                break;
            case R.id.menu_delete:
                Disposable deleteNews = Single.fromCallable(() -> {
                    ConverterNews.deleteNewsFromDb(mContext, mNewsEntity);
                    return true;
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe();
                mCompositeDisposable.add(deleteNews);
                finish();
                break;
            default: throw new IllegalArgumentException(getString(R.string.error_no_id)+": "+item.getItemId());
        }
        return super.onOptionsItemSelected(item);
    }
}
