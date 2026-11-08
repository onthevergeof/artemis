package com.artemis.android;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.http.SslError;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class MainActivity extends Activity {
    private static final String PREFS = "artemis_console";
    private static final String KEY_BACKEND = "backend_url";
    private static final String DEFAULT_BACKEND = "http://127.0.0.1:8000";

    private LinearLayout root;
    private WebView webView;
    private EditText backendInput;
    private ValueCallback<Uri[]> uploadCallback;
    private String activeUrl = DEFAULT_BACKEND;
    private TextView runtimeStatusView;
    private volatile String runtimeSummary = "Embedded Python: starting…";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(245, 247, 251));
        getWindow().setNavigationBarColor(Color.rgb(245, 247, 251));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        String saved = getPreferences(MODE_PRIVATE).getString(KEY_BACKEND, null);
        if (saved == null || saved.trim().isEmpty()) {
            showSetup(DEFAULT_BACKEND);
        } else {
            activeUrl = saved;
            showConsole(saved);
        }
        startEmbeddedPython();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView label(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private void prepareRoot() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245, 247, 251));
        setContentView(root);
    }

    private void showSetup(String initialValue) {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        prepareRoot();

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(36), dp(24), dp(24));
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView title = label("ARTEMIS", 29, Color.rgb(28, 38, 67));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(title);

        TextView subtitle = label("Mobile console", 16, Color.rgb(54, 89, 217));
        subtitle.setPadding(0, dp(4), 0, dp(24));
        content.addView(subtitle);

        TextView heading = label("Connect to your Artemis server", 20, Color.rgb(32, 41, 60));
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(heading);

        TextView explanation = label(
                "Enter the address where the original ARTEMIS server is running. "
                + "The existing dashboard, task execution, traces and diagnostics are served by that backend.",
                14, Color.rgb(90, 100, 119));
        explanation.setPadding(0, dp(10), 0, dp(18));
        content.addView(explanation);

        backendInput = new EditText(this);
        backendInput.setSingleLine(true);
        backendInput.setTextSize(15);
        backendInput.setHint("https://your-artemis-server");
        backendInput.setInputType( android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        backendInput.setText(initialValue);
        backendInput.setSelectAllOnFocus(true);
        content.addView(backendInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        Button connect = new Button(this);
        connect.setText("Connect");
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        buttonParams.topMargin = dp(16);
        content.addView(connect, buttonParams);
        connect.setOnClickListener(v -> connectToBackend());

        TextView note = label(
                "Important: this APK includes an embedded Python interpreter baseline, but the ARTEMIS "
                + "agent modules and dependencies are not bundled yet. Start the existing server "
                + "to use the console. HTTP is allowed only for localhost; use HTTPS remotely.",
                12, Color.rgb(107, 115, 132));
        note.setPadding(0, dp(18), 0, 0);
        content.addView(note);

        TextView helper = label("Default on-device address: " + DEFAULT_BACKEND, 12,
                Color.rgb(107, 115, 132));
        helper.setPadding(0, dp(12), 0, 0);
        content.addView(helper);

        runtimeStatusView = label(runtimeSummary, 12, Color.rgb(54, 89, 217));
        runtimeStatusView.setPadding(0, dp(14), 0, 0);
        content.addView(runtimeStatusView);
    }

    private void connectToBackend() {
        String input = backendInput.getText().toString().trim();
        Uri uri = Uri.parse(input);
        String scheme = uri.getScheme();
        String host = uri.getHost();

        if (host == null || (!"http".equalsIgnoreCase(scheme)
                && !"https".equalsIgnoreCase(scheme))
                || uri.getUserInfo() != null) {
            Toast.makeText(this, "Enter a valid HTTP or HTTPS server URL.", Toast.LENGTH_LONG).show();
            return;
        }

        boolean localHost = "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host)
                || "10.0.2.2".equals(host);
        if ("http".equalsIgnoreCase(scheme) && !localHost) {
            Toast.makeText(this,
                    "For remote servers, use HTTPS. Plain HTTP is restricted to localhost.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String normalized = uri.toString();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        activeUrl = normalized;
        getPreferences(MODE_PRIVATE).edit().putString(KEY_BACKEND, activeUrl).apply();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(backendInput.getWindowToken(), 0);
        showConsole(activeUrl);
    }

    private void showConsole(String url) {
        activeUrl = url;
        prepareRoot();

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(12), dp(6), dp(12), dp(6));
        toolbar.setBackgroundColor(Color.WHITE);
        root.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        Button serverButton = new Button(this);
        serverButton.setText("Server");
        toolbar.addView(serverButton);
        serverButton.setOnClickListener(v -> showSetup(activeUrl));

        TextView address = label(Uri.parse(activeUrl).getHost(), 13, Color.rgb(57, 66, 86));
        address.setSingleLine(true);
        address.setGravity(Gravity.CENTER_VERTICAL);
        address.setPadding(dp(8), 0, dp(8), 0);
        toolbar.addView(address, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1));

        Button reload = new Button(this);
        reload.setText("Reload");
        toolbar.addView(reload);
        reload.setOnClickListener(v -> {
            if (webView != null) webView.reload();
        });

        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        root.addView(progress, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));
        runtimeStatusView = label(runtimeSummary, 11, Color.rgb(76, 87, 108));
        runtimeStatusView.setPadding(dp(12), dp(4), dp(12), dp(4));
        root.addView(runtimeStatusView);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            settings.setSafeBrowsingEnabled(true);
        }

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                    FileChooserParams params) {
                if (uploadCallback != null) uploadCallback.onReceiveValue(null);
                uploadCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), 42);
                    return true;
                } catch (ActivityNotFoundException ex) {
                    uploadCallback = null;
                    Toast.makeText(MainActivity.this, "No file picker is available.", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri destination = request.getUrl();
                if (sameOrigin(Uri.parse(activeUrl), destination)) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, destination));
                } catch (ActivityNotFoundException ex) {
                    Toast.makeText(MainActivity.this, "No app can open this link.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                    WebResourceError error) {
                if (request.isForMainFrame()) {
                    Toast.makeText(MainActivity.this,
                            "Could not reach ARTEMIS. Check that the server is running and the URL is correct.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
            }
        });

        root.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        webView.loadUrl(url);
    }

    private void startEmbeddedPython() {
        Thread worker = new Thread(() -> {
            String result;
            try {
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(getApplication()));
                }
                String json = Python.getInstance()
                        .getModule("artemis_android.runtime_probe")
                        .callAttr("runtime_info")
                        .toString();
                org.json.JSONObject info = new org.json.JSONObject(json);
                result = "Embedded Python " + info.optString("python", "")
                        + " ready (" + info.optString("machine", "Android") + "). "
                        + (info.optBoolean("agent_integrated", false)
                        ? "ARTEMIS engine loaded."
                        : "ARTEMIS engine packages are not bundled yet.");
            } catch (Exception error) {
                result = "Embedded Python could not start: " + error.getClass().getSimpleName();
            }
            runtimeSummary = result;
            runOnUiThread(() -> {
                if (runtimeStatusView != null) runtimeStatusView.setText(runtimeSummary);
            });
        }, "artemis-python-startup");
        worker.start();
    }

    private boolean sameOrigin(Uri first, Uri second) {
        return first.getScheme() != null && first.getScheme().equalsIgnoreCase(second.getScheme())
                && first.getHost() != null && first.getHost().equalsIgnoreCase(second.getHost())
                && first.getPort() == second.getPort();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 42 && uploadCallback != null) {
            Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            uploadCallback.onReceiveValue(result);
            uploadCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else if (webView != null) {
            showSetup(activeUrl);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (uploadCallback != null) {
            uploadCallback.onReceiveValue(null);
            uploadCallback = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
