package com.chibatching.rxandroid_sample;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import rx.Observable;
import rx.Subscriber;
import rx.subscriptions.Subscriptions;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.auth.AccessToken;

public class TimeLine implements Observable.OnSubscribe<Status> {

    private TwitterStream mTwitterStream;
    private Subscriber<? super Status> mSubscriber;

    public TimeLine(Context context) {
        String consumerKey = context.getString(R.string.consumer_key);
        String consumerSecret = context.getString(R.string.consumer_secret);
        mTwitterStream = TwitterStreamFactory.getSingleton();
        try {
            mTwitterStream.setOAuthConsumer(consumerKey, consumerSecret);
        } catch (IllegalStateException e) {
            Log.d(getClass().getSimpleName(), "Already set consumer key and secret.");
        }
        mTwitterStream.setOAuthAccessToken(getAccessToken(context));
    }

    @Override
    public void call(Subscriber<? super Status> subscriber) {
        mSubscriber = subscriber;
        mSubscriber.add(Subscriptions.create(() -> {
            mTwitterStream.clearListeners();
            mTwitterStream.shutdown();
            mTwitterStream = null;
        }));
        mTwitterStream.addListener(new TimeLineListener());
        mTwitterStream.sample();
    }

    public class TimeLineListener implements StatusListener {
        @Override
        public void onStatus(Status status) {
            mSubscriber.onNext(status);
        }

        @Override
        public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {

        }

        @Override
        public void onTrackLimitationNotice(int numberOfLimitedStatuses) {

        }

        @Override
        public void onScrubGeo(long userId, long upToStatusId) {

        }

        @Override
        public void onStallWarning(StallWarning warning) {

        }

        @Override
        public void onException(Exception ex) {

        }
    }

    public AccessToken getAccessToken(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String token = sp.getString(context.getString(R.string.key_access_token), null);
        String secret = sp.getString(context.getString(R.string.key_access_token_secret), null);

        if (token != null && secret != null) {
            return new AccessToken(token, secret);
        } else {
            return null;
        }
    }
}
