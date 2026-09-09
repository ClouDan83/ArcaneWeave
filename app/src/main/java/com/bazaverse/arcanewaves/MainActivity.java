package com.bazaverse.arcanewaves;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gameView = new WebView(this);
        gameView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        WebSettings settings = gameView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        gameView.setWebViewClient(new WebViewClient());
        gameView.setWebChromeClient(new WebChromeClient());
        gameView.loadUrl("file:///android_asset/game.html");

        setContentView(gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) gameView.onResume();
    }

    @Override
    protected void onPause() {
        if (gameView != null) gameView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (gameView != null) {
            gameView.loadUrl("about:blank");
            gameView.stopLoading();
            gameView.setWebChromeClient(null);
            gameView.setWebViewClient(null);
            gameView.destroy();
            gameView = null;
        }
        super.onDestroy();
    }
}
