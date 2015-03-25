package com.chibatching.rxandroid_sample;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import twitter4j.Status;


public class TimeLineFragment extends ListFragment {
    CardListAdapter mAdapter;

    private TimeLine mTimeLine;
    private CompositeSubscription mCompositeSubscription = new CompositeSubscription();
    private List<Status> mStatusList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mStatusList = new ArrayList<>();
        mTimeLine = new TimeLine(getActivity());

        return inflater.inflate(R.layout.fragment_timeline, null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCompositeSubscription.unsubscribe();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setDivider(null);

        // Create and subscribe timeline observable
        mCompositeSubscription.add(Observable.create(mTimeLine)
                .subscribeOn(Schedulers.io())
//                .filter(status -> status.getText().contains("google"))
                .buffer(1000, TimeUnit.MILLISECONDS, 20)
                .filter(list -> list.size() > 0)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        status -> {
                            mStatusList.addAll(0, status);
                            mAdapter.notifyDataSetChanged();
                        },
                        error -> Toast.makeText(getActivity(), R.string.timeline_error, Toast.LENGTH_SHORT).show()));

        mAdapter = new CardListAdapter(mStatusList);
        setListAdapter(mAdapter);
    }

    public class CardListAdapter extends ArrayAdapter<Status> {

        public CardListAdapter(List<Status> list) {
            super(getActivity(), R.layout.list_item_card, list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.list_item_card, parent, false);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd hh:mm", Locale.getDefault());
            Status status = mStatusList.get(position);
            viewHolder.userName.setText(status.getUser().getName());
            viewHolder.screenName.setText(status.getUser().getScreenName());
            viewHolder.tweetText.setText(status.getText());
            viewHolder.tweetTime.setText(sdf.format(status.getCreatedAt()));
            Picasso.with(getActivity()).load(status.getUser().getBiggerProfileImageURL())
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .into(viewHolder.iconImage);

            return convertView;
        }

        class ViewHolder {
            TextView userName;
            TextView screenName;
            TextView tweetText;
            TextView tweetTime;
            ImageView iconImage;

            public ViewHolder(View view) {
                userName = (TextView) view.findViewById(R.id.user_name);
                screenName = (TextView) view.findViewById(R.id.screen_name);
                tweetText = (TextView) view.findViewById(R.id.tweet_text);
                tweetTime = (TextView) view.findViewById(R.id.tweet_time);
                iconImage = (ImageView) view.findViewById(R.id.icon);
            }
        }

    }
}
