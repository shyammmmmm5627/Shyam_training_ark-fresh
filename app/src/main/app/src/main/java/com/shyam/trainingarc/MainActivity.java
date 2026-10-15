
package com.shyam.trainingarc;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {

    private LinearLayout root;
    private TextView xpText, rankText, pdfName, pageText;
    private ImageView pdfImage;

    private PdfRenderer renderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor pdfDescriptor;

    private int pageIndex = 0;
    private int xp = 0;
    private int completed = 0;

    private Uri selectedPdf;
    private String selectedPdfName = "No PDF selected";

    private static final int PICK_PDF = 1001;
    private static final int NOTIFICATION_PERMISSION_CODE = 2001;

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        Thread.setDefaultUncaughtExceptionHandler(
                (thread, error) -> {
                    saveCrashLog(error);
                    android.os.Process.killProcess(
                            android.os.Process.myPid());
                    System.exit(10);
                });

        try {
            getWindow().setStatusBarColor(Color.rgb(15, 12, 25));
            getWindow().setNavigationBarColor(Color.rgb(15, 12, 25));

            SharedPreferences prefs = getSharedPreferences(
                    "training_arc", MODE_PRIVATE);

            xp = prefs.getInt("xp", 0);
            completed = prefs.getInt("completed", 0);

            buildScreen();
            requestNotificationPermission();

        } catch (Throwable error) {
            saveCrashLog(error);
            showCrashDetails(error);
        }
    }

    private void saveCrashLog(Throwable error) {
        try {
            File file = new File(getFilesDir(), "crash-log.txt");

            FileWriter writer = new FileWriter(file, true);
            writer.write("\n\n--- SHYAM TRAINING ARC CRASH ---\n");
            writer.write("Time: " + System.currentTimeMillis() + "\n");
            writer.write("Thread: " + Thread.currentThread().getName() + "\n");
            writer.write(android.util.Log.getStackTraceString(error));
            writer.write("\n");
            writer.close();
        } catch (Throwable ignored) {
        }
    }

    private void showCrashDetails(Throwable error) {
        try {
            ScrollView scroll = new ScrollView(this);
            scroll.setBackgroundColor(Color.rgb(35, 12, 20));

            TextView details = new TextView(this);
            details.setTextColor(Color.WHITE);
            details.setTextSize(14);
            details.setPadding(dp(18), dp(18), dp(18), dp(18));

            details.setText(
                    "SHYAM TRAINING ARC\nDIAGNOSTIC ERROR\n\n"
                    + android.util.Log.getStackTraceString(error)
                    + "\n\nCrash log saved in app private storage.");

            scroll.addView(details);
            setContentView(scroll);

        } catch (Throwable ignored) {
            android.util.Log.e("TrainingArc", "Startup failure", error);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_CODE);
        }
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(Color.WHITE);
        t.setTextSize(size);
        t.setPadding(dp(4), dp(6), dp(4), dp(6));
        return t;
    }

    private void addButton(
            LinearLayout box, String label, Runnable action) {

        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setBackgroundTintList(
                ColorStateList.valueOf(Color.rgb(94, 52, 160)));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(4), 0, dp(4));
        box.addView(button, params);

        button.setOnClickListener(v -> {
            try {
                action.run();
            } catch (Throwable error) {
                saveCrashLog(error);
                showCrashDetails(error);
            }
        });
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(24));
        root.setBackgroundColor(Color.rgb(15, 12, 25));

        scroll.addView(root);
        setContentView(scroll);

        TextView title = text("⚔ SHYAM TRAINING ARC", 25);
        title.setTypeface(null, 1);
        root.addView(title);
        root.addView(text("YOUR JOURNEY • YOUR DISCIPLINE", 12));

        xpText = text("", 20);
        rankText = text("", 16);
        root.addView(xpText);
        root.addView(rankText);
        refreshProgress();

        root.addView(text("━━━━━━━━━━━━━━━━━━", 12));
        root.addView(text("📚 STUDY LIBRARY", 21));

        pdfName = text(selectedPdfName, 15);
        root.addView(pdfName);
        addButton(root, "＋ ADD PDF FROM PHONE", this::pickPdf);

        pageText = text("No document open", 14);
        root.addView(pageText);

        pdfImage = new ImageView(this);
        pdfImage.setAdjustViewBounds(true);
        pdfImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(pdfImage,
                new LinearLayout.LayoutParams(-1, dp(420)));

        addButton(root, "◀ PREVIOUS PAGE",
                () -> showPage(pageIndex - 1));
        addButton(root, "NEXT PAGE ▶",
                () -> showPage(pageIndex + 1));

        root.addView(text("━━━━━━━━━━━━━━━━━━", 12));
        root.addView(text("⏰ STUDY REMINDERS", 21));
        addButton(root, "＋ SET DAILY STUDY REMINDER",
                this::createReminder);

        root.addView(text("━━━━━━━━━━━━━━━━━━", 12));
        root.addView(text("⚡ DAILY MISSIONS", 21));

        addButton(root, "📖 Complete Study Session (+20 XP)",
                () -> completeMission("Study", 20));
        addButton(root, "🎨 Complete Drawing (+10 XP)",
                () -> completeMission("Drawing", 10));
        addButton(root, "💪 Complete Exercise (+10 XP)",
                () -> completeMission("Exercise", 10));
        addButton(root, "🌙 Complete Sleep Goal (+10 XP)",
                () -> completeMission("Sleep", 10));
        addButton(root, "📵 Stop Scrolling (+10 XP)",
                () -> completeMission("Stop Scrolling", 10));
        addButton(root, "＋ ADD CUSTOM MISSION",
                this::createCustomMission);

        root.addView(text("━━━━━━━━━━━━━━━━━━", 12));
        root.addView(text(
                "XP and completed missions are saved on this phone.",
                12));

        addButton(root, "🛠 SHOW LAST CRASH LOG",
                this::showSavedCrashLog);
    }

    private void refreshProgress() {
        if (xpText != null) {
            xpText.setText("⚡ TOTAL XP: " + xp);
        }

        if (rankText != null) {
            String rank;

            if (xp >= 500) rank = "S-Rank • Shadow Master";
            else if (xp >= 250) rank = "A-Rank • Elite";
            else if (xp >= 100) rank = "B-Rank • Warrior";
            else if (xp >= 40) rank = "C-Rank • Apprentice";
            else rank = "E-Rank • Beginner";

            rankText.setText("🏆 RANK: " + rank
                    + "\n✅ Missions completed: " + completed);
        }
    }

    private void completeMission(String name, int reward) {
        xp += reward;
        completed++;

        getSharedPreferences("training_arc", MODE_PRIVATE)
                .edit()
                .putInt("xp", xp)
                .putInt("completed", completed)
                .apply();

        refreshProgress();
        Toast.makeText(this,
                name + " completed! +" + reward + " XP",
                Toast.LENGTH_SHORT).show();
    }

    private void createCustomMission() {
        EditText input = new EditText(this);
        input.setHint("Enter your mission");
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle("Create Custom Mission")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Complete (+10 XP)",
                        (dialog, which) -> {
                            String name = input.getText()
                                    .toString().trim();

                            if (name.isEmpty()) {
                                Toast.makeText(this,
                                        "Enter a mission name first.",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            completeMission(name, 10);
                        })
                .show();
    }

    private void pickPdf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_PDF);
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != PICK_PDF || resultCode != RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }

        selectedPdf = data.getData();

        try {
            getContentResolver().takePersistableUriPermission(
                    selectedPdf,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
        }

        selectedPdfName = getFileName(selectedPdf);
        pdfName.setText("📄 " + selectedPdfName);

        try {
            openPdf(selectedPdf);
        } catch (Exception error) {
            saveCrashLog(error);
            closePdf();
            Toast.makeText(this,
                    "Could not open this PDF. Try another file.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private String getFileName(Uri uri) {
        String name = "Study PDF";

        try (android.database.Cursor cursor =
                     getContentResolver().query(
                             uri, null, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME);

                if (index >= 0) {
                    String result = cursor.getString(index);
                    if (result != null && !result.isEmpty()) {
                        name = result;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return name;
    }

    private void openPdf(Uri uri) throws IOException {
        closePdf();

        pdfDescriptor = getContentResolver()
                .openFileDescriptor(uri, "r");

        if (pdfDescriptor == null) {
            throw new IOException("Cannot read PDF");
        }

        renderer = new PdfRenderer(pdfDescriptor);
        pageIndex = 0;
        showPage(0);
    }

    private void showPage(int index) {
        if (renderer == null) {
            Toast.makeText(this, "Add a PDF first.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (index < 0 || index >= renderer.getPageCount()) {
            return;
        }

        try {
            if (currentPage != null) {
                currentPage.close();
                currentPage = null;
            }

            currentPage = renderer.openPage(index);
            pageIndex = index;

            Bitmap bitmap = Bitmap.createBitmap(
                    currentPage.getWidth(),
                    currentPage.getHeight(),
                    Bitmap.Config.ARGB_8888);

            bitmap.eraseColor(Color.WHITE);

            currentPage.render(bitmap, null, null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            pdfImage.setImageBitmap(bitmap);
            pageText.setText("Page " + (pageIndex + 1)
                    + " of " + renderer.getPageCount());

        } catch (Exception error) {
            saveCrashLog(error);
            Toast.makeText(this,
                    "Unable to display this PDF page.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void createReminder() {
        EditText subjectInput = new EditText(this);
        subjectInput.setHint("Subject, e.g. Political Science");
        subjectInput.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle("New Study Reminder")
                .setView(subjectInput)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Choose Time", (dialog, which) -> {
                    String subject = subjectInput.getText()
                            .toString().trim();

                    if (subject.isEmpty()) subject = "Study Session";
                    chooseReminderTime(subject);
                })
                .show();
    }

    private void chooseReminderTime(String subject) {
        Calendar now = Calendar.getInstance();

        TimePickerDialog picker = new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    Intent intent = new Intent(
                            this, ReminderReceiver.class);

                    intent.setAction(
                            "com.shyam.trainingarc.STUDY_REMINDER");
                    intent.putExtra("subject", subject);
                    intent.putExtra("pdfName", selectedPdfName);

                    int requestCode = (subject + hour + minute)
                            .hashCode() & 0x7fffffff;

                    PendingIntent pending = PendingIntent.getBroadcast(
                            this, requestCode, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                                    | PendingIntent.FLAG_IMMUTABLE);

                    Calendar time = Calendar.getInstance();
                    time.set(Calendar.HOUR_OF_DAY, hour);
                    time.set(Calendar.MINUTE, minute);
                    time.set(Calendar.SECOND, 0);
                    time.set(Calendar.MILLISECOND, 0);

                    if (time.getTimeInMillis()
                            <= System.currentTimeMillis()) {
                        time.add(Calendar.DAY_OF_YEAR, 1);
                    }

                    AlarmManager alarms = (AlarmManager)
                            getSystemService(ALARM_SERVICE);

                    if (alarms == null) {
                        Toast.makeText(this,
                                "Reminder service unavailable.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    alarms.setInexactRepeating(
                            AlarmManager.RTC_WAKEUP,
                            time.getTimeInMillis(),
                            AlarmManager.INTERVAL_DAY,
                            pending);

                    Toast.makeText(this,
                            String.format(Locale.getDefault(),
                                    "Daily reminder set for %02d:%02d",
                                    hour, minute),
                            Toast.LENGTH_LONG).show();
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE), true);

        picker.setTitle("Choose Daily Reminder Time");
        picker.show();
    }

    private void showSavedCrashLog() {
        try {
            File file = new File(getFilesDir(), "crash-log.txt");

            if (!file.exists()) {
                new AlertDialog.Builder(this)
                        .setTitle("Crash Log")
                        .setMessage("No crash log has been saved yet.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }

            StringBuilder content = new StringBuilder();
            java.io.BufferedReader reader =
                    new java.io.BufferedReader(
                            new java.io.FileReader(file));

            String line;
            while ((line = reader.readLine()) != null
                    && content.length() < 12000) {
                content.append(line).append('\n');
            }
            reader.close();

            EditText output = new EditText(this);
            output.setText(content.toString());
            output.setTextIsSelectable(true);
            output.setMinLines(8);

            new AlertDialog.Builder(this)
                    .setTitle("Last Crash Log")
                    .setView(output)
                    .setPositiveButton("Close", null)
                    .show();

        } catch (Exception error) {
            Toast.makeText(this,
                    "Unable to read crash log.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void closePdf() {
        try {
            if (currentPage != null) currentPage.close();
        } catch (Exception ignored) {
        }
        currentPage = null;

        try {
            if (renderer != null) renderer.close();
        } catch (Exception ignored) {
        }
        renderer = null;

        try {
            if (pdfDescriptor != null) pdfDescriptor.close();
        } catch (Exception ignored) {
        }
        pdfDescriptor = null;
    }

    @Override
    protected void onDestroy() {
        closePdf();
        super.onDestroy();
    }
                  }
        
