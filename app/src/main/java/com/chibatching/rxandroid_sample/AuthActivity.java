package com.chibatching.rxandroid_sample;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;


public class AuthActivity extends Activity {
    private ProgressDialog mProgressDialog;

    private RequestToken mRequestToken;
    private Twitter mTwitter;

    private CompositeSubscription mCompositeSubscription = new CompositeSubscription();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_page);

        String consumerKey = getString(R.string.consumer_key);
        String consumerSecret = getString(R.string.consumer_secret);

        mTwitter = TwitterFactory.getSingleton();
        mTwitter.setOAuthConsumer(consumerKey, consumerSecret);
        // Create twitter authentication observable
        mCompositeSubscription.add(Observable.create(
                new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        try {
                            mRequestToken = mTwitter.getOAuthRequestToken();
                            mCompositeSubscription.add(startWebView(mRequestToken.getAuthorizationURL())
                                    .subscribeOn(AndroidSchedulers.mainThread())
                                    .subscribe(verifier -> {
                                        subscriber.onNext(verifier);
                                        subscriber.onCompleted();
                                    }));
                        } catch (TwitterException e) {
                            subscriber.onError(e);
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                        verifier -> {
                            mCompositeSubscription.add(Observable.create(subscriber -> {
                                try {
                                    AccessToken accessToken = mTwitter.getOAuthAccessToken(mRequestToken, verifier);
                                    SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(AuthActivity.this);
                                    preference.edit()
                                            .putString(getString(R.string.key_access_token), accessToken.getToken())
                                            .putString(getString(R.string.key_access_token_secret), accessToken.getTokenSecret())
                                            .commit();
                                    subscriber.onNext(null);
                                    subscriber.onCompleted();
                                } catch (TwitterException e) {
                                    e.printStackTrace();
                                }
                            })
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object -> {
                                        mProgressDialog.dismiss();
                                        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(intent);
                                        AuthActivity.this.finish();
                                    }));
                        }));

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(R.string.connecting));
        mProgressDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeSubscription.unsubscribe();
    }

    private Observable<String> startWebView(String url) {
        mProgressDialog.dismiss();
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                WebView webView = (WebView) findViewById(R.id.WebView);
                webView.setWebViewClient(new WebViewClient() {
                    private boolean flag = false;
                    String callbackUrl = getString(R.string.callback_url);

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        if (!flag && url != null && url.startsWith(callbackUrl)) {
                            // Check url whether Callback URL
                            flag = true;
                            // Clear cache, form, history
                            webView.clearCache(true);
                            webView.clearFormData();
                            webView.clearHistory();
                            CookieManager cmng = CookieManager.getInstance();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                cmng.removeAllCookies(value -> {
                                });
                            } else {
                                cmng.removeAllCookie();
                            }
                            subscriber.onNext(Uri.parse(url).getQueryParameter("oauth_verifier"));
                            subscriber.onCompleted();
                            // WebView の非表示
                            webView.setVisibility(WebView.GONE);
                            webView.destroy();
                            mProgressDialog.show();
                        }
                    }
                });
                webView.getSettings().setJavaScriptEnabled(true);
                webView.loadUrl(url);
                webView.setVisibility(WebView.VISIBLE);
            }
        });
    }
}
