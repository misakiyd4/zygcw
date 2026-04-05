package com.example.gcw;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private WebView myWebView;
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int FILE_CHOOSER_REQUEST_CODE = 2;
    private ValueCallback<Uri[]> mFilePathCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_GCWApp);
        super.onCreate(savedInstanceState);
        
        myWebView = new WebView(this);
        setContentView(myWebView);

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setGeolocationEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        // 允许网页自动播放视频流，这是解决 Android WebView 扫码出画面但没反应的关键
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("tel:")) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                view.loadUrl(url);
                return true;
            }
        });

        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                // 确保在主线程执行
                runOnUiThread(() -> {
                    try {
                        List<String> missingPermissions = new ArrayList<>();
                        for (String resource : request.getResources()) {
                            String permission = null;
                            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                                permission = Manifest.permission.CAMERA;
                            } else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                                permission = Manifest.permission.RECORD_AUDIO;
                            }
                            
                            if (permission != null && ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
                                missingPermissions.add(permission);
                            }
                        }

                        if (!missingPermissions.isEmpty()) {
                            mCurrentPermissionRequest = request;
                            ActivityCompat.requestPermissions(MainActivity.this, missingPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
                        } else {
                            // 如果已经有 Android 权限，直接授权给 WebView
                            request.grant(request.getResources());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, android.webkit.GeolocationPermissions.Callback callback) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                     mGeolocationCallback = callback;
                     mGeolocationOrigin = origin;
                     ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, GEOLOCATION_REQUEST_CODE);
                } else {
                    callback.invoke(origin, true, false);
                }
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                // 如果是请求视频捕获（摄像头），则交给网页自己处理
                if (fileChooserParams != null && fileChooserParams.isCaptureEnabled()) {
                    return false;
                }

                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePathCallback;
                
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
                startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
                return true;
            }
        });
        
        myWebView.loadUrl("https://www.zygcw.com/html/gcw/index.html#/");
    }

    private PermissionRequest mCurrentPermissionRequest;
    private android.webkit.GeolocationPermissions.Callback mGeolocationCallback;
    private String mGeolocationOrigin;
    private static final int GEOLOCATION_REQUEST_CODE = 1002;

    @Override
    public void onBackPressed() {
        if (myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            Log.d(TAG, "onRequestPermissionsResult: " + java.util.Arrays.toString(permissions) + " -> " + java.util.Arrays.toString(grantResults));
            // 每次权限返回后，检查是否是 WebView 的请求
            if (mCurrentPermissionRequest != null) {
                boolean allGranted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    Log.d(TAG, "All permissions granted, granting request");
                    runOnUiThread(() -> {
                        try {
                            mCurrentPermissionRequest.grant(mCurrentPermissionRequest.getResources());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    Log.d(TAG, "Some permissions denied, denying request");
                    runOnUiThread(() -> {
                        try {
                            mCurrentPermissionRequest.deny();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
                mCurrentPermissionRequest = null;
            } else {
                // 如果不是 WebView 直接触发的权限请求，我们需要通知 WebView 重新加载页面或执行 JS
                // 以便网页中的 html5-qrcode 可以重新获取权限状态
                myWebView.evaluateJavascript("if (window.location.href.includes('扫码页面的路由或标识')) { window.location.reload(); }", null);
            }
        } else if (requestCode == GEOLOCATION_REQUEST_CODE) {
             if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                 if (mGeolocationCallback != null) {
                     mGeolocationCallback.invoke(mGeolocationOrigin, true, false);
                 }
             } else {
                 if (mGeolocationCallback != null) {
                     mGeolocationCallback.invoke(mGeolocationOrigin, false, false);
                 }
             }
             mGeolocationCallback = null;
             mGeolocationOrigin = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (mFilePathCallback == null) {
                return;
            }
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
        }
    }
}
