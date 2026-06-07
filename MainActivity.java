package com.chioma.agent;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    // ── Colors ──────────────────────────────────────────────
    static final int C_BG        = 0xFF080812;
    static final int C_SURFACE   = 0xFF0F0F1E;
    static final int C_SURFACE2  = 0xFF161628;
    static final int C_SURFACE3  = 0xFF1E1E35;
    static final int C_BORDER    = 0xFF2A1F4A;
    static final int C_PRIMARY   = 0xFF7C3AED;
    static final int C_ACCENT    = 0xFF00E5FF;
    static final int C_TEXT      = 0xFFE8E8F0;
    static final int C_DIM       = 0xFF8888A8;
    static final int C_MUTED     = 0xFF555570;
    static final int C_SUCCESS   = 0xFF00FF88;
    static final int C_ERROR     = 0xFFFF4466;
    static final int C_USER_BUB  = 0xFF2D2D44;

    static final String MCP_HOST = "http://127.0.0.1:8080";

    // ── State ─────────────────────────────────────────────────
    enum Screen { WELCOME, CONNECT, CHAT }
    Screen currentScreen = Screen.WELCOME;
    boolean connected = false;

    // ── Root layout ───────────────────────────────────────────
    FrameLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(C_BG);
        root = new FrameLayout(this);
        root.setBackgroundColor(C_BG);
        setContentView(root);
        showWelcome();
    }

    // ═══════════════════════════════════════════════════════════
    //  WELCOME SCREEN
    // ═══════════════════════════════════════════════════════════
    void showWelcome() {
        currentScreen = Screen.WELCOME;
        root.removeAllViews();
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(C_BG);
        sv.setFillViewport(true);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER);
        ll.setPadding(dp(32), dp(60), dp(32), dp(40));
        ll.setBackgroundColor(C_BG);

        // Logo circle
        FrameLayout logo = makeLogo(dp(100));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(100), dp(100));
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.bottomMargin = dp(28);
        ll.addView(logo, lp);

        // Title
        TextView title = new TextView(this);
        title.setText("CHIOMA AI AGENT");
        title.setTextSize(28);
        title.setTextColor(C_ACCENT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setLetterSpacing(0.08f);
        ll.addView(title, centerWrap(0, dp(4)));

        // Tagline
        TextView tag = new TextView(this);
        tag.setText("Intelligence Without Limits");
        tag.setTextSize(13);
        tag.setTextColor(C_PRIMARY);
        tag.setGravity(Gravity.CENTER);
        tag.setLetterSpacing(0.05f);
        ll.addView(tag, centerWrap(0, dp(20)));

        // Description
        TextView desc = new TextView(this);
        desc.setText("Your personal AI agent that controls your Android device through Termux.\n\nNo limits. No premium. Completely free forever.");
        desc.setTextSize(14);
        desc.setTextColor(C_DIM);
        desc.setGravity(Gravity.CENTER);
        desc.setLineSpacing(dp(4), 1f);
        ll.addView(desc, centerWrap(0, dp(28)));

        // Badges row
        LinearLayout badges = new LinearLayout(this);
        badges.setOrientation(LinearLayout.HORIZONTAL);
        badges.setGravity(Gravity.CENTER);
        badges.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        for (String[] b : new String[][]{{"∞", "Unlimited"}, {"🔓", "Free"}, {"🚀", "Full Features"}}) {
            badges.addView(makeBadge(b[0] + " " + b[1]));
        }
        ll.addView(badges, centerWrap(0, dp(32)));

        // Start button
        Button btn = makeButton("Start Using Chioma →");
        btn.setOnClickListener(v -> showConnect());
        ll.addView(btn, centerWrap(0, dp(16)));

        // Note
        TextView note = new TextView(this);
        note.setText("Requires Termux + Node.js on this device");
        note.setTextSize(11);
        note.setTextColor(C_MUTED);
        note.setGravity(Gravity.CENTER);
        ll.addView(note, centerWrap(0, 0));

        sv.addView(ll);
        root.addView(sv, matchParent());
    }

    // ═══════════════════════════════════════════════════════════
    //  CONNECT SCREEN
    // ═══════════════════════════════════════════════════════════
    void showConnect() {
        currentScreen = Screen.CONNECT;
        root.removeAllViews();

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackgroundColor(C_BG);

        // Header
        outer.addView(makeHeader("Setup", null));

        // Scrollable content
        ScrollView sv = new ScrollView(this);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(dp(20), dp(16), dp(20), dp(24));

        TextView h = new TextView(this);
        h.setText("Connect Chioma to Termux");
        h.setTextSize(20);
        h.setTextColor(C_ACCENT);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        ll.addView(h, wrap(0, dp(20)));

        // Step cards
        ll.addView(makeStepCard("01", "Install Node.js in Termux",
            "Open Termux and run:", "pkg install nodejs -y", null));
        ll.addView(makeStepCard("02", "Start the MCP Bridge",
            "Paste this into Termux:", "node ~/.chioma/server.js",
            "If ~/.chioma/server.js doesn't exist yet, get it from the Chioma web app."));
        ll.addView(makeStepCard("03", "Install Termux:API app",
            "Get from F-Droid for device control (battery, camera, SMS etc):",
            "f-droid.org/packages/com.termux.api", null));

        // Connect button
        TextView statusTv = new TextView(this);
        statusTv.setTextSize(13);
        statusTv.setGravity(Gravity.CENTER);
        statusTv.setPadding(0, dp(8), 0, 0);

        Button connectBtn = makeButton("Connect to Chioma (localhost:8080)");
        connectBtn.setOnClickListener(v -> {
            connectBtn.setEnabled(false);
            connectBtn.setText("Connecting...");
            statusTv.setText("");
            new PingTask(success -> {
                connectBtn.setEnabled(true);
                if (success) {
                    connected = true;
                    statusTv.setTextColor(C_SUCCESS);
                    statusTv.setText("✓ Connected! Chioma is ready.");
                    connectBtn.postDelayed(() -> showChat(), 800);
                } else {
                    connectBtn.setText("Connect to Chioma (localhost:8080)");
                    statusTv.setTextColor(C_ERROR);
                    statusTv.setText("✗ Can't reach localhost:8080\nMake sure the bridge is running in Termux");
                }
            }).execute();
        });

        ll.addView(connectBtn, fullWrap(0, dp(20)));
        ll.addView(statusTv, fullWrap(0, 0));

        // Skip link
        Button skip = new Button(this);
        skip.setText("Skip → Go to Chat");
        skip.setBackgroundColor(Color.TRANSPARENT);
        skip.setTextColor(C_DIM);
        skip.setTextSize(13);
        skip.setOnClickListener(v -> showChat());
        ll.addView(skip, centerWrap(dp(8), 0));

        sv.addView(ll);
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        outer.addView(sv, svLp);
        root.addView(outer, matchParent());
    }

    // ═══════════════════════════════════════════════════════════
    //  CHAT SCREEN
    // ═══════════════════════════════════════════════════════════
    LinearLayout chatList;
    ScrollView chatScroll;

    void showChat() {
        currentScreen = Screen.CHAT;
        root.removeAllViews();

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackgroundColor(C_BG);

        // Header with ∞ badge
        outer.addView(makeChatHeader());

        // Messages area
        chatScroll = new ScrollView(this);
        chatScroll.setBackgroundColor(C_BG);
        chatList = new LinearLayout(this);
        chatList.setOrientation(LinearLayout.VERTICAL);
        chatList.setPadding(dp(12), dp(12), dp(12), dp(12));
        chatScroll.addView(chatList);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        outer.addView(chatScroll, scrollLp);

        // Input bar
        LinearLayout inputBar = new LinearLayout(this);
        inputBar.setOrientation(LinearLayout.HORIZONTAL);
        inputBar.setBackgroundColor(C_SURFACE);
        inputBar.setPadding(dp(12), dp(10), dp(12), dp(10));
        inputBar.setGravity(Gravity.CENTER_VERTICAL);

        EditText et = new EditText(this);
        et.setHint("Ask Chioma anything...");
        et.setHintTextColor(C_MUTED);
        et.setTextColor(C_TEXT);
        et.setTextSize(14);
        et.setBackground(makeRoundRect(C_SURFACE2, C_BORDER, dp(12)));
        et.setPadding(dp(14), dp(11), dp(14), dp(11));
        et.setSingleLine(true);
        et.setImeOptions(EditorInfo.IME_ACTION_SEND);
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        inputBar.addView(et, etLp);

        Button sendBtn = new Button(this);
        sendBtn.setText("➤");
        sendBtn.setTextSize(18);
        sendBtn.setTextColor(Color.WHITE);
        sendBtn.setBackground(makeRoundRect(C_PRIMARY, 0, dp(12)));
        LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(dp(46), dp(46));
        sendLp.leftMargin = dp(8);
        inputBar.addView(sendBtn, sendLp);

        outer.addView(inputBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(outer, matchParent());

        // Chioma greeting
        addChiomaMessage("Nnoo! I'm Chioma. 🌟\n\nI can control your Android device through Termux. No limits, no fees — ever.\n\nTry: \"battery\", \"torch on\", \"what time\", \"list files\", or just ask me anything!", null, null);

        Runnable sendAction = () -> {
            String msg = et.getText().toString().trim();
            if (TextUtils.isEmpty(msg)) return;
            et.setText("");
            hideKeyboard(et);
            addUserMessage(msg);
            sendToChioma(msg);
        };

        sendBtn.setOnClickListener(v -> sendAction.run());
        et.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                sendAction.run();
                return true;
            }
            return false;
        });
    }

    // ── Add message bubbles ───────────────────────────────────
    void addUserMessage(String text) {
        runOnUiThread(() -> {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.END);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.topMargin = dp(8);

            TextView tv = new TextView(this);
            tv.setText(text);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(14);
            tv.setBackground(makeRoundRect(C_USER_BUB, 0, dp(18)));
            tv.setPadding(dp(14), dp(10), dp(14), dp(10));
            tv.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.78));

            row.addView(tv);
            chatList.addView(row, rowLp);
            scrollToBottom();
        });
    }

    void addChiomaMessage(String text, String command, String output) {
        runOnUiThread(() -> {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            colLp.topMargin = dp(8);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.START | Gravity.BOTTOM);

            // Avatar
            TextView av = new TextView(this);
            av.setText("C");
            av.setTextColor(Color.WHITE);
            av.setTextSize(13);
            av.setTypeface(Typeface.DEFAULT_BOLD);
            av.setGravity(Gravity.CENTER);
            av.setBackground(makeCircle(C_PRIMARY));
            LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(dp(28), dp(28));
            avLp.rightMargin = dp(8);
            avLp.bottomMargin = dp(2);
            row.addView(av, avLp);

            // Bubble
            TextView tv = new TextView(this);
            tv.setText(text);
            tv.setTextColor(C_TEXT);
            tv.setTextSize(14);
            tv.setBackground(makeRoundRect(C_SURFACE2, C_BORDER, dp(18)));
            tv.setPadding(dp(14), dp(10), dp(14), dp(10));
            tv.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.78));
            tv.setLineSpacing(dp(3), 1f);
            row.addView(tv);
            col.addView(row);

            // Command + output block
            if (command != null) {
                LinearLayout cmdBlock = new LinearLayout(this);
                cmdBlock.setOrientation(LinearLayout.VERTICAL);
                cmdBlock.setBackground(makeRoundRect(C_SURFACE3, C_BORDER, dp(8)));
                LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                cbLp.topMargin = dp(6);
                cbLp.leftMargin = dp(36);

                TextView cmdTv = new TextView(this);
                cmdTv.setText("$ " + command);
                cmdTv.setTextColor(C_ACCENT);
                cmdTv.setTextSize(11);
                cmdTv.setTypeface(Typeface.MONOSPACE);
                cmdTv.setPadding(dp(10), dp(7), dp(10), dp(7));
                cmdBlock.addView(cmdTv);

                if (output != null && !output.isEmpty()) {
                    View divider = new View(this);
                    divider.setBackgroundColor(C_BORDER);
                    cmdBlock.addView(divider, new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, 1));

                    TextView outTv = new TextView(this);
                    outTv.setText(output.length() > 600 ? output.substring(0, 600) + "\n…(tap to copy full)" : output);
                    outTv.setTextColor(C_DIM);
                    outTv.setTextSize(11);
                    outTv.setTypeface(Typeface.MONOSPACE);
                    outTv.setPadding(dp(10), dp(7), dp(10), dp(7));
                    outTv.setLineSpacing(dp(2), 1f);
                    // Tap to copy
                    final String fullOutput = output;
                    outTv.setOnClickListener(v -> copyToClipboard(fullOutput));
                    cmdBlock.addView(outTv);
                }

                col.addView(cmdBlock, cbLp);
            }

            chatList.addView(col, colLp);
            scrollToBottom();
        });
    }

    void addTypingIndicator() {
        runOnUiThread(() -> {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.START | Gravity.BOTTOM);
            row.setTag("typing");
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.topMargin = dp(8);

            TextView av = new TextView(this);
            av.setText("C");
            av.setTextColor(Color.WHITE);
            av.setTextSize(13);
            av.setTypeface(Typeface.DEFAULT_BOLD);
            av.setGravity(Gravity.CENTER);
            av.setBackground(makeCircle(C_PRIMARY));
            LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(dp(28), dp(28));
            avLp.rightMargin = dp(8);
            row.addView(av, avLp);

            TextView tv = new TextView(this);
            tv.setText("● ● ●");
            tv.setTextColor(C_ACCENT);
            tv.setTextSize(12);
            tv.setBackground(makeRoundRect(C_SURFACE2, C_BORDER, dp(18)));
            tv.setPadding(dp(14), dp(12), dp(14), dp(12));
            row.addView(tv);

            chatList.addView(row, rowLp);
            scrollToBottom();
        });
    }

    void removeTypingIndicator() {
        runOnUiThread(() -> {
            for (int i = chatList.getChildCount() - 1; i >= 0; i--) {
                View v = chatList.getChildAt(i);
                if ("typing".equals(v.getTag())) {
                    chatList.removeViewAt(i);
                    break;
                }
            }
        });
    }

    void scrollToBottom() {
        chatScroll.post(() -> chatScroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    // ═══════════════════════════════════════════════════════════
    //  COMMAND PARSER
    // ═══════════════════════════════════════════════════════════
    String parseCommand(String input) {
        String t = input.toLowerCase().trim();

        if (t.matches(".*(battery|check battery|battery status|how.s my battery).*"))
            return "termux-battery-status";
        if (t.matches(".*(torch on|flashlight on|turn on light|light on).*"))
            return "termux-torch on";
        if (t.matches(".*(torch off|flashlight off|turn off light|light off).*"))
            return "termux-torch off";
        if (t.matches(".*(volume up|louder|increase volume).*"))
            return "termux-volume music 10";
        if (t.matches(".*(volume down|quieter|decrease volume).*"))
            return "termux-volume music 3";
        if (t.matches(".*(mute|silent).*"))
            return "termux-volume music 0";
        if (t.matches(".*(vibrate|buzz).*"))
            return "termux-vibrate -d 500";
        if (t.matches(".*(my location|where am i|find me|gps|location).*"))
            return "termux-location";
        if (t.matches(".*(list files|show files|ls|what.s in here).*"))
            return "ls -la ~/storage/ 2>/dev/null || ls -la ~";
        if (t.matches(".*(storage|disk space).*"))
            return "df -h";
        if (t.matches(".*(processes|running apps|ps aux).*"))
            return "ps aux | head -15";
        if (t.matches(".*(what time|current time|tell time).*"))
            return "date '+%I:%M %p'";
        if (t.matches(".*(what date|today.s date|what day).*"))
            return "date '+%A, %B %d, %Y'";
        if (t.matches(".*(weather|temperature|forecast).*"))
            return "curl -s 'wttr.in?format=%C+%t'";
        if (t.matches(".*(whoami|current user).*"))
            return "whoami";
        if (t.matches(".*(hostname|device name).*"))
            return "hostname";
        if (t.matches(".*(ip address|local ip|my ip|wifi ip).*"))
            return "ip addr show | grep 'inet '";
        if (t.matches(".*(public ip|external ip).*"))
            return "curl -s ifconfig.me";
        if (t.matches(".*(take picture|take photo|capture photo).*"))
            return "termux-camera-photo -c 0 ~/storage/pictures/chioma_photo.jpg && echo 'Photo saved to ~/storage/pictures/'";
        if (t.matches(".*(screenshot|capture screen).*"))
            return "screencap -p ~/storage/pictures/screenshot_$(date +%s).png && echo 'Screenshot saved'";
        if (t.matches(".*(clipboard paste|get clipboard|read clipboard).*"))
            return "termux-clipboard-get";
        if (t.matches(".*(contacts|show contacts|list contacts).*"))
            return "termux-contact-list | head -30";
        if (t.matches(".*(wifi on|enable wifi).*"))
            return "termux-wifi-enable true";
        if (t.matches(".*(wifi off|disable wifi).*"))
            return "termux-wifi-enable false";
        if (t.matches(".*(update|upgrade packages|pkg update).*"))
            return "pkg update -y && pkg upgrade -y";
        if (t.matches(".*(my notes|show notes|read notes).*"))
            return "cat ~/chioma_notes.txt 2>/dev/null || echo 'No notes yet'";
        if (t.matches(".*(clear notes|delete notes).*"))
            return "rm -f ~/chioma_notes.txt && echo 'Notes cleared'";
        if (t.matches(".*(environment|env vars|env variables).*"))
            return "env | head -20";
        if (t.matches(".*(reboot|restart phone).*"))
            return "termux-reboot";
        if (t.matches(".*(full brightness|max brightness).*"))
            return "termux-brightness 255";
        if (t.matches(".*(dim screen|low brightness).*"))
            return "termux-brightness 50";
        if (t.matches(".*(sensor|sensors).*"))
            return "termux-sensor -l";
        if (t.matches(".*(memory|ram).*"))
            return "free -h";
        if (t.matches(".*(cpu|processor).*"))
            return "cat /proc/cpuinfo | grep 'model name\\|Hardware\\|Processor' | head -5";

        // Dynamic patterns
        if (t.matches(".*install (.+)")) {
            String pkg = t.replaceAll(".*install (.+)", "$1").trim();
            return "pkg install " + pkg + " -y";
        }
        if (t.matches(".*ping (.+)")) {
            String host = t.replaceAll(".*ping (.+)", "$1").trim();
            return "ping -c 4 " + host;
        }
        if (t.matches(".*(note|remember) (.+)")) {
            String note = t.replaceAll(".*(note|remember) (.+)", "$2").trim();
            return "echo '" + note + "' >> ~/chioma_notes.txt && echo 'Note saved!'";
        }
        if (t.matches(".*(open|browse) (.+)")) {
            String url = t.replaceAll(".*(open|browse) (.+)", "$2").trim();
            return "termux-open-url " + url;
        }
        if (t.matches(".*(brightness|set brightness) (\\d+).*")) {
            String val = t.replaceAll(".*(brightness|set brightness) (\\d+).*", "$2");
            return "termux-brightness " + val;
        }
        if (t.matches(".*(download|wget) (.+)")) {
            String url = t.replaceAll(".*(download|wget) (.+)", "$2").trim();
            return "wget " + url;
        }

        return null; // no match → AI handles it
    }

    // ═══════════════════════════════════════════════════════════
    //  SEND TO CHIOMA (MCP + fallback response)
    // ═══════════════════════════════════════════════════════════
    void sendToChioma(String userMessage) {
        addTypingIndicator();
        String command = parseCommand(userMessage);

        new AsyncTask<Void, Void, String[]>() {
            String usedCommand = command;
            @Override
            protected String[] doInBackground(Void... v) {
                if (usedCommand != null) {
                    // Execute directly
                    String[] result = mcpRun(usedCommand);
                    String output = result[0];
                    int exitCode = Integer.parseInt(result[1]);
                    String reply = buildReply(userMessage, usedCommand, output, exitCode == 0);
                    return new String[]{reply, usedCommand, output};
                } else {
                    // Ask MCP server to interpret via AI
                    try {
                        JSONObject req = new JSONObject();
                        req.put("message", userMessage);
                        String resp = httpPost(MCP_HOST + "/interpret", req.toString());
                        if (resp != null) {
                            JSONObject j = new JSONObject(resp);
                            String cmd = j.optString("command", null);
                            String reply = j.optString("reply", "");
                            if (cmd != null && !cmd.isEmpty()) {
                                usedCommand = cmd;
                                String[] runResult = mcpRun(cmd);
                                String out = runResult[0];
                                return new String[]{buildReply(userMessage, cmd, out, Integer.parseInt(runResult[1]) == 0), cmd, out};
                            }
                            return new String[]{reply.isEmpty() ? naturalReply(userMessage) : reply, null, null};
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                    return new String[]{naturalReply(userMessage), null, null};
                }
            }

            @Override
            protected void onPostExecute(String[] result) {
                removeTypingIndicator();
                addChiomaMessage(result[0], result[1], result[2]);
            }
        }.execute();
    }

    String[] mcpRun(String command) {
        try {
            JSONObject body = new JSONObject();
            body.put("command", command);
            String resp = httpPost(MCP_HOST + "/run", body.toString());
            if (resp != null) {
                JSONObject j = new JSONObject(resp);
                return new String[]{j.optString("output", "Done"), String.valueOf(j.optInt("exitCode", 0))};
            }
        } catch (Exception e) {
            // fall through
        }
        return new String[]{"Could not reach Termux bridge.\nMake sure the bridge is running:\n  node ~/.chioma/server.js", "1"};
    }

    String buildReply(String msg, String cmd, String output, boolean ok) {
        String t = msg.toLowerCase();
        if (t.contains("battery")) return ok ? "Battery info retrieved! ⚡\n" + summarize(output) : "Couldn't read battery — is Termux:API installed?";
        if (t.contains("torch") || t.contains("flashlight")) return ok ? "Done! Flashlight toggled. 💡" : "Couldn't toggle flashlight. Is Termux:API installed?";
        if (t.contains("location")) return ok ? "Got your location! 📍\n" + summarize(output) : "Location unavailable. Check GPS and Termux:API.";
        if (t.contains("time")) return "It's " + output + " 🕐";
        if (t.contains("date")) return "Today is " + output;
        if (t.contains("weather")) return ok ? "Weather: " + output + " 🌤" : "Couldn't reach weather service. Check internet.";
        if (t.contains("vibrat") || t.contains("buzz")) return ok ? "Buzzed! 📳" : "Vibration failed. Check Termux:API.";
        if (t.contains("volume")) return ok ? "Volume updated! 🔊" : "Volume change failed.";
        if (t.contains("screenshot")) return ok ? "Screenshot saved! 📸\n" + output : "Screenshot failed: " + output;
        if (t.contains("note") || t.contains("remember")) return ok ? "Note saved! 📝" : "Couldn't save note: " + output;
        if (t.contains("my notes") || t.contains("show notes")) return ok ? "Your notes:\n" + output : "No notes found.";
        if (!ok) return "Hmm, that didn't work:\n" + output + "\n\nMake sure the bridge is running in Termux.";
        return "Done! ⚡\n" + (output.length() > 0 && output.length() < 200 ? output : "");
    }

    String summarize(String out) {
        if (out == null || out.isEmpty()) return "";
        return out.length() > 300 ? out.substring(0, 300) + "…" : out;
    }

    String naturalReply(String msg) {
        String t = msg.toLowerCase();
        if (t.contains("hello") || t.contains("hi ") || t.equals("hi")) return "Nnoo! 👋 I'm Chioma. Ask me to control your device — torch, battery, location, files, and much more. All free, no limits!";
        if (t.contains("help") || t.contains("what can you do")) return "I can control your Android via Termux! Try:\n\n• battery\n• torch on / torch off\n• volume up / down / mute\n• my location\n• list files\n• take picture\n• what time / what date\n• weather\n• screenshot\n• contacts\n• vibrate\n• install [package]\n• ping [host]\n• note [text]\n• my notes\n• whoami / hostname\n\n…and more! Just ask naturally. 🌟";
        if (t.contains("thank")) return "Ije ọma! Always here for you. No limits. 😊";
        if (t.contains("free") || t.contains("cost") || t.contains("price")) return "Completely free! ∞\n\nNo subscriptions. No premium tiers. No usage limits. Chioma is yours forever.";
        return "I'm not sure how to handle \"" + msg + "\" yet.\n\nTry: battery, torch on/off, volume up/down, my location, list files, what time, weather, contacts, vibrate, screenshot, take picture, or \"help\" for the full list.";
    }

    // ═══════════════════════════════════════════════════════════
    //  NETWORK
    // ═══════════════════════════════════════════════════════════
    String httpPost(String urlStr, String jsonBody) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            InputStream is = code < 400 ? conn.getInputStream() : conn.getErrorStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString().trim();
        } catch (Exception e) {
            return null;
        }
    }

    interface PingCallback { void done(boolean success); }

    class PingTask extends AsyncTask<Void, Void, Boolean> {
        PingCallback cb;
        PingTask(PingCallback cb) { this.cb = cb; }
        @Override protected Boolean doInBackground(Void... v) {
            try {
                URL url = new URL(MCP_HOST + "/ping");
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);
                return c.getResponseCode() == 200;
            } catch (Exception e) { return false; }
        }
        @Override protected void onPostExecute(Boolean ok) { cb.done(ok); }
    }

    // ═══════════════════════════════════════════════════════════
    //  UI HELPERS
    // ═══════════════════════════════════════════════════════════
    View makeChatHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setBackgroundColor(C_SURFACE);
        h.setPadding(dp(16), dp(40), dp(16), dp(12));
        h.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout logo = makeLogo(dp(36));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(36), dp(36));
        lp.rightMargin = dp(10);
        h.addView(logo, lp);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        TextView name = new TextView(this);
        name.setText("CHIOMA AI");
        name.setTextColor(C_TEXT);
        name.setTextSize(15);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        info.addView(name);
        TextView sub = new TextView(this);
        sub.setText(connected ? "● Connected to Termux" : "● Tap Connect in Setup");
        sub.setTextColor(connected ? C_SUCCESS : C_MUTED);
        sub.setTextSize(11);
        info.addView(sub);
        h.addView(info, infoLp);

        TextView badge = new TextView(this);
        badge.setText("∞ Unlimited");
        badge.setTextColor(C_ACCENT);
        badge.setTextSize(11);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setPadding(dp(10), dp(5), dp(10), dp(5));
        badge.setBackground(makeRoundRect(0x1500E5FF, C_BORDER, dp(20)));
        h.addView(badge);

        return h;
    }

    View makeHeader(String title, String subtitle) {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setBackgroundColor(C_SURFACE);
        h.setPadding(dp(16), dp(40), dp(16), dp(12));
        h.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout logo = makeLogo(dp(32));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(32), dp(32));
        lp.rightMargin = dp(10);
        h.addView(logo, lp);

        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(C_TEXT);
        tv.setTextSize(16);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        h.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return h;
    }

    FrameLayout makeLogo(int size) {
        FrameLayout f = new FrameLayout(this);
        f.setBackground(makeCircle(C_PRIMARY));
        f.setLayoutParams(new FrameLayout.LayoutParams(size, size));

        TextView c = new TextView(this);
        c.setText("C");
        c.setTextColor(Color.WHITE);
        c.setTextSize(size * 0.38f);
        c.setTypeface(Typeface.DEFAULT_BOLD);
        c.setGravity(Gravity.CENTER);
        f.addView(c, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView inf = new TextView(this);
        inf.setText("∞");
        inf.setTextColor(C_ACCENT);
        inf.setTextSize(size * 0.2f);
        FrameLayout.LayoutParams infLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infLp.gravity = Gravity.BOTTOM | Gravity.END;
        infLp.bottomMargin = (int)(size * 0.05f);
        infLp.rightMargin = (int)(size * 0.05f);
        f.addView(inf, infLp);
        return f;
    }

    View makeStepCard(String num, String title, String desc, String code, String sub) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackground(makeRoundRect(C_SURFACE2, C_BORDER, dp(14)));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(12);
        card.setLayoutParams(cardLp);

        TextView numTv = new TextView(this);
        numTv.setText(num);
        numTv.setTextColor(Color.WHITE);
        numTv.setTextSize(13);
        numTv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        numTv.setGravity(Gravity.CENTER);
        numTv.setBackground(makeRoundRect(C_PRIMARY, 0, dp(10)));
        LinearLayout.LayoutParams numLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        numLp.rightMargin = dp(14);
        numLp.topMargin = dp(2);
        card.addView(numTv, numLp);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView titleTv = new TextView(this);
        titleTv.setText(title);
        titleTv.setTextColor(C_TEXT);
        titleTv.setTextSize(15);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(titleTv, wrap(0, dp(4)));

        TextView descTv = new TextView(this);
        descTv.setText(desc);
        descTv.setTextColor(C_DIM);
        descTv.setTextSize(12);
        descTv.setLineSpacing(dp(2), 1f);
        content.addView(descTv, wrap(0, dp(6)));

        // Code block with tap-to-copy
        LinearLayout codeBlock = new LinearLayout(this);
        codeBlock.setBackground(makeRoundRect(C_SURFACE3, C_BORDER, dp(8)));
        codeBlock.setPadding(dp(10), dp(8), dp(10), dp(8));
        TextView codeTv = new TextView(this);
        codeTv.setText(code);
        codeTv.setTextColor(C_ACCENT);
        codeTv.setTextSize(12);
        codeTv.setTypeface(Typeface.MONOSPACE);
        codeBlock.addView(codeTv);
        codeBlock.setOnClickListener(v -> {
            copyToClipboard(code);
            codeTv.setTextColor(C_SUCCESS);
            codeTv.postDelayed(() -> codeTv.setTextColor(C_ACCENT), 1200);
        });
        content.addView(codeBlock, wrap(0, sub != null ? dp(6) : 0));

        if (sub != null) {
            TextView subTv = new TextView(this);
            subTv.setText(sub);
            subTv.setTextColor(C_MUTED);
            subTv.setTextSize(11);
            subTv.setLineSpacing(dp(2), 1f);
            content.addView(subTv, wrap(0, 0));
        }

        card.addView(content, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return card;
    }

    View makeBadge(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(C_ACCENT);
        tv.setTextSize(11);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(dp(12), dp(5), dp(12), dp(5));
        tv.setBackground(makeRoundRect(0x1500E5FF, C_BORDER, dp(20)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(6);
        tv.setLayoutParams(lp);
        return tv;
    }

    Button makeButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(15);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackground(makeRoundRect(C_PRIMARY, 0, dp(14)));
        btn.setPadding(dp(28), dp(14), dp(28), dp(14));
        btn.setAllCaps(false);
        return btn;
    }

    android.graphics.drawable.GradientDrawable makeRoundRect(int fill, int stroke, int radius) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        d.setCornerRadius(radius);
        d.setColor(fill);
        if (stroke != 0) d.setStroke(2, stroke);
        return d;
    }

    android.graphics.drawable.GradientDrawable makeCircle(int color) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    void copyToClipboard(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("chioma", text));
        Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show();
    }

    void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    LinearLayout.LayoutParams matchParent() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    LinearLayout.LayoutParams wrap(int top, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = top; lp.bottomMargin = bottom;
        return lp;
    }

    LinearLayout.LayoutParams fullWrap(int top, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = top; lp.bottomMargin = bottom;
        return lp;
    }

    LinearLayout.LayoutParams centerWrap(int top, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.topMargin = top; lp.bottomMargin = bottom;
        return lp;
    }
}
