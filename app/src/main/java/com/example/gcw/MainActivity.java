package com.example.gcw;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private WebView myWebView;
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int PICK_IMAGE_REQUEST = 1;

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
        // Enable Geolocation
        webSettings.setGeolocationEnabled(true);
        // Allow file access
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("tel:")) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                    return true; // WebView 不处理该 URL
                }
                view.loadUrl(url);
                return true;
            }
        });
        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                // Check and request permissions if not granted
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
                    // Store the request to grant it later
                    mCurrentPermissionRequest = request;
                    ActivityCompat.requestPermissions(MainActivity.this, missingPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
                } else {
                    // All permissions granted, proceed
                    runOnUiThread(() -> request.grant(request.getResources()));
                }
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
        });
        
        myWebView.loadUrl("https://www.qsgl.net/html/gcw/index.html#/");
        
        // Removed checkAndRequestPermissions() to allow on-demand requests
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
            if (mCurrentPermissionRequest != null) {
                boolean allGranted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    runOnUiThread(() -> mCurrentPermissionRequest.grant(mCurrentPermissionRequest.getResources()));
                } else {
                    runOnUiThread(() -> mCurrentPermissionRequest.deny());
                }
                mCurrentPermissionRequest = null;
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

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            // 在这里处理选中的图片，例如显示在 ImageView 中
            // imageView.setImageURI(selectedImage);
        }
    }

    /*
     * Removed the bulk permission request method
     */
}
