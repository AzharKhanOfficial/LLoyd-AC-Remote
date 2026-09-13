package com.azhar.lloydremote;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * A small, dependency-free Lloyd-compatible AC remote for phones with an IR emitter.
 *
 * The signal format is the ZH/JT-03 format used by several Lloyd-compatible units:
 * 38 kHz carrier, 6234/7392 us header, complemented bytes, and 608/608 or
 * 608/1832 us pulse-distance bits.
 */
public class MainActivity extends Activity {
    private static final int BG = Color.rgb(16, 18, 22);
    private static final int PANEL = Color.rgb(29, 33, 40);
    private static final int TEXT = Color.rgb(239, 244, 248);
    private static final int MUTED = Color.rgb(166, 178, 188);
    private static final int BLUE = Color.rgb(54, 180, 224);
    private static final int RED = Color.rgb(226, 86, 95);

    private ConsumerIrManager ir;
    private TextView status;
    private TextView temperature;
    private TextView selectedMode;
    private TextView selectedFan;
    private TextView selectedSwing;
    private boolean powerOn = false;
    private int temp = 24;
    private String mode = "cool";
    private String fan = "smart";
    private String swing = "horizontal";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ir = (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(28));
        scroll.addView(root);

        TextView title = label("LLOYD AC REMOTE", 24, TEXT);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title, fullWidth(0));
        root.addView(label("GLS18I3FWBEW  •  IR control", 13, MUTED), fullWidth(4));

        status = label("Checking IR emitter…", 13, MUTED);
        root.addView(status, fullWidth(18));
        updateIrStatus();

        Button power = button("POWER", RED, 58);
        root.addView(power, fullWidth(0));
        power.setOnClickListener(v -> {
            powerOn = !powerOn;
            send(0xFF00);
            power.setText(powerOn ? "POWER  •  ON" : "POWER  •  OFF");
        });

        root.addView(section("TEMPERATURE"), fullWidth(22));
        LinearLayout tempRow = row();
        Button minus = button("−", PANEL, 54);
        Button plus = button("+", PANEL, 54);
        temperature = label("24°C", 32, TEXT);
        temperature.setGravity(Gravity.CENTER);
        tempRow.addView(minus, weight(1, 54));
        tempRow.addView(temperature, weight(1.5f, 54));
        tempRow.addView(plus, weight(1, 54));
        root.addView(tempRow, fullWidth(0));
        minus.setOnClickListener(v -> changeTemperature(-1));
        plus.setOnClickListener(v -> changeTemperature(1));

        root.addView(section("MODE"), fullWidth(22));
        LinearLayout modes = row();
        selectedMode = label("COOL", 14, BLUE);
        addChoice(modes, "COOL", "cool", selectedMode);
        addChoice(modes, "DRY", "dry", selectedMode);
        addChoice(modes, "FAN", "fan", selectedMode);
        addChoice(modes, "AUTO", "auto", selectedMode);
        root.addView(modes, fullWidth(0));

        root.addView(section("FAN SPEED"), fullWidth(22));
        LinearLayout fans = row();
        selectedFan = label("SMART", 14, BLUE);
        addChoice(fans, "LOW", "slow", selectedFan);
        addChoice(fans, "MED", "medium", selectedFan);
        addChoice(fans, "HIGH", "fast", selectedFan);
        addChoice(fans, "SMART", "smart", selectedFan);
        root.addView(fans, fullWidth(0));

        root.addView(section("SWING"), fullWidth(22));
        LinearLayout swings = row();
        selectedSwing = label("HORIZONTAL", 14, BLUE);
        addChoice(swings, "HORIZONTAL", "horizontal", selectedSwing);
        addChoice(swings, "FIXED", "fixed", selectedSwing);
        addChoice(swings, "NATURAL", "natural", selectedSwing);
        root.addView(swings, fullWidth(0));

        TextView note = label("Point the top of the phone at the indoor unit.\n" +
                "This app sends the Lloyd-compatible 38 kHz IR protocol.", 12, MUTED);
        note.setPadding(0, dp(24), 0, 0);
        root.addView(note, fullWidth(0));

        setContentView(scroll);
    }

    private void updateIrStatus() {
        try {
            boolean available = ir != null && ir.hasIrEmitter();
            status.setText(available ? "IR emitter ready" : "No IR emitter detected on this phone");
            status.setTextColor(available ? Color.rgb(102, 214, 140) : Color.rgb(255, 183, 77));
        } catch (RuntimeException ex) {
            status.setText("IR service unavailable");
            status.setTextColor(Color.rgb(255, 183, 77));
        }
    }

    private void changeTemperature(int delta) {
        temp = Math.max(16, Math.min(31, temp + delta));
        temperature.setText(temp + "°C");
        send(delta > 0 ? 0xBF40 : 0x3FC0);
    }

    private void addChoice(LinearLayout parent, String text, String value, TextView selected) {
        Button b = button(text, PANEL, 48);
        parent.addView(b, weight(1, 48));
        b.setOnClickListener(v -> {
            if (selected == selectedMode) mode = value;
            else if (selected == selectedFan) fan = value;
            else swing = value;
            selected.setText(text);
            selected.setTextColor(BLUE);
            int command = selected == selectedMode ? 0x7F80 :
                    selected == selectedFan ? 0x5FA0 : 0xDF20;
            send(command);
        });
    }

    private void send(int mainCommand) {
        try {
            if (ir == null || !ir.hasIrEmitter()) {
                Toast.makeText(this, "This phone has no usable IR emitter", Toast.LENGTH_SHORT).show();
                return;
            }

            int[] pattern = LloydProtocol.frame(mainCommand, powerOn, temp, mode, fan, swing);
            ir.transmit(38000, pattern);
            status.setText("Sent  •  " + temp + "°C  •  " + mode.toUpperCase());
            status.setTextColor(Color.rgb(102, 214, 140));
        } catch (RuntimeException ex) {
            String message = ex.getMessage();
            if (message == null || message.trim().isEmpty()) message = "IR transmission failed";
            status.setText("IR error: " + message);
            status.setTextColor(Color.rgb(255, 183, 77));
            Toast.makeText(this, "IR transmission failed", Toast.LENGTH_LONG).show();
        }
    }

    private TextView section(String text) {
        TextView t = label(text, 12, MUTED);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private TextView label(String text, int size, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private Button button(String text, int color, int height) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(12);
        b.setTextColor(TEXT);
        b.setAllCaps(false);
        b.setBackgroundColor(color);
        return b;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        return row;
    }

    private LinearLayout.LayoutParams fullWidth(int topMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(topMargin);
        return p;
    }

    private LinearLayout.LayoutParams weight(float weight, int height) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(height), weight);
        p.setMargins(dp(3), 0, dp(3), 0);
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** Protocol encoder for Lloyd-compatible ZH/JT-03 AC remotes. */
    static final class LloydProtocol {
        private static final int HEADER_MARK = 6234;
        private static final int HEADER_SPACE = 7392;
        private static final int BIT_MARK = 608;
        private static final int ZERO_SPACE = 608;
        private static final int ONE_SPACE = 1832;

        static int[] frame(int main, boolean on, int temp, String mode, String fan, String swing) {
            int timer = 0xFF;
            int extra = 0xFF;
            int fanByte = fanByte(on, fan, swing);
            int tempMode = (tempNibble(temp) << 4) | modeNibble(mode, temp);

            // Each protocol byte is followed by its bitwise complement.
            int[] bytes = {
                    timer, extra, (main >> 8) & 0xFF, fanByte, tempMode, 0x54
            };
            List<Integer> raw = new ArrayList<>();
            raw.add(HEADER_MARK);
            raw.add(HEADER_SPACE);
            for (int value : bytes) {
                addByte(raw, value);
                addByte(raw, (~value) & 0xFF);
            }
            raw.add(BIT_MARK);
            raw.add(7372);
            raw.add(616);

            int[] result = new int[raw.size()];
            for (int i = 0; i < raw.size(); i++) result[i] = raw.get(i);
            return result;
        }

        private static void addByte(List<Integer> raw, int value) {
            for (int bit = 7; bit >= 0; bit--) {
                raw.add(BIT_MARK);
                raw.add(((value >> bit) & 1) == 1 ? ONE_SPACE : ZERO_SPACE);
            }
        }

        private static int fanByte(boolean on, String fan, String swing) {
            int swingPart;
            if (!on) swingPart = swing.equals("fixed") ? 0xF : swing.equals("natural") ? 0xD : 0xE;
            else swingPart = swing.equals("fixed") ? 0xB : swing.equals("natural") ? 0x9 : 0xA;
            int speedPart;
            switch (fan) {
                case "slow": speedPart = 0x9; break;
                case "medium": speedPart = 0xD; break;
                case "fast": speedPart = 0xB; break;
                default: speedPart = 0xF; break;
            }
            return (swingPart << 4) | speedPart;
        }

        private static int tempNibble(int temp) {
            final int[] nibbles = {0xF, 0x7, 0xB, 0x3, 0xD, 0x5, 0x9, 0x1,
                    0xE, 0x6, 0xA, 0x2, 0xC, 0x4, 0x8, 0x0};
            return nibbles[Math.max(16, Math.min(31, temp)) - 16];
        }

        private static int modeNibble(String mode, int temp) {
            if ("auto".equals(mode)) return 0xF;
            if ("dry".equals(mode)) return 0xD;
            if ("fan".equals(mode)) return temp == 32 ? 0xE : 0x9;
            return temp == 32 ? 0x3 : 0xB;
        }
    }
}
