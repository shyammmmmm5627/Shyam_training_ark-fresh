
package com.shyam.trainingarc;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import android.provider.OpenableColumns;

import java.io.*;
import java.util.*;

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

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().setStatusBarColor(Color.rgb(15, 12, 25));
        getWindow().setNavigationBarColor(Color.rgb(15, 12, 25));

        android.content.SharedPreferences prefs =
                getSharedPreferences("training_arc", MODE_PRIVATE);

        xp = prefs.getInt("xp", 0);
        completed = prefs.getInt("completed", 0);

        buildScreen();
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(Color.WHITE);
        t.setTextSize(size);
        t.setPadding(dp(4), dp(6), dp(4), dp(6));
        return t;
    }

    private void addButton(LinearLayout box, String label,
                           Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.rgb(94, 52, 160)));
        box.addView(b, new LinearLayout.LayoutParams(
                -1, -2));
        b.setOnClickListener(v -> action.run());
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

        xpText = text("", 19);
        root.addView(xpText);

        rankText = text("", 16);
        root.addView(rankText);

        refreshProgress();

        root.addView(text("📚  STUDY LIBRARY", 21));

        pdfName = text(selectedPdfName, 15);
        root.addView(pdfName);

        addButton(root, "＋ ADD PDF FROM PHONE",
                this::pickPdf);

        addButton(root, "◀ PREVIOUS PAGE",
                () -> showPage(pageIndex - 1));

        pageText = text("No document open", 14);
        root.addView(pageText);

        pdfImage = new ImageView(this);
        pdfImage.setAdjustViewBounds(true);
        pdfImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(pdfImage, new LinearLayout.LayoutParams(
                -1, dp(420)));

        addButton(root, "NEXT PAGE ▶",
                () -> showPage(pageIndex + 1));

        root.addView(text("⏰  STUDY REMINDERS", 21));

        addButton(root, "＋ SET STUDY REMINDER",
                this::createReminder);

        root.addView(text("⚡  DAILY MISSIONS", 21));

        addButton(root, "📖 Complete Study Session  (+20 XP)",
                () -> completeMission(20));

        addButton(root, "🎨 Complete Drawing  (+10 XP)",
                () -> completeMission(10));

        addButton(root, "💪 Complete Exercise  (+10 XP)",
                () -> completeMission(10));

        addButton(root, "🌙 Complete Sleep Goal  (+10 XP)",
                () -> completeMission(10));

        addButton(root, "📵 Stop Scrolling  (+10 XP)",
                () -> completeMission(10));

        root.addView(text(
                "Your progress is saved on this phone.", 12));
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

    private void completeMission(int reward) {
        xp += reward;
        completed++;

        getSharedPreferences("training_arc", MODE_PRIVATE)
                .edit()
                .putInt("xp", xp)
                .putInt("completed", completed)
                .apply();

        refreshProgress();

        Toast.makeText(this,
                "+" + reward + " XP earned!",
                Toast.LENGTH_SHORT).show();
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
    protected void onActivityResult(int requestCode,
            int resultCode, Intent data) {
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
        } catch (Exception ignored) { }

        selectedPdfName = getFileName(selectedPdf);
        pdfName.setText("📄 " + selectedPdfName);

        try {
            openPdf(selectedPdf);
        } catch (Exception e) {
            Toast.makeText(this,
                    "Could not open this PDF.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private String getFileName(Uri uri) {
        String name = "Study PDF";

        try (android.database.Cursor c =
                getContentResolver().query(uri,
                        null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int index = c.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME);
                if (index >= 0) name = c.getString(index);
            }
        } catch (Exception ignored) { }

        return name;
    }

    private void openPdf(Uri uri) throws Exception {
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
            Toast.makeText(this,
                    "Add a PDF first.",
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

        } catch (Exception e) {
            Toast.makeText(this,
                    "Unable to render this page.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void createReminder() {
        EditText subjectInput = new EditText(this);
        subjectInput.setHint("Subject (e.g. Political Science)");
        subjectInput.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle("New Study Reminder")
                .setMessage(selectedPdfName)
                .setView(subjectInput)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Choose time", (dialog, which) -> {
                    String subject = subjectInput.getText()
                            .toString().trim();

                    if (subject.isEmpty()) {
                        subject = "Study Session";
                    }

                    chooseReminderTime(subject);
                })
                .show();
    }

    private void chooseReminderTime(String subject) {
        Calendar now = Calendar.getInstance();

        TimePickerDialog picker = new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    Intent intent = new Intent(this,
                            ReminderReceiver.class);

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
                now.get(Calendar.MINUTE),
                true);

        picker.setTitle("Choose daily reminder time");
        picker.show();
    }

    private void closePdf() {
        try {
            if (currentPage != null) currentPage.close();
        } catch (Exception ignored) { }

        currentPage = null;

        try {
            if (renderer != null) renderer.close();
        } catch (Exception ignored) { }

        renderer = null;

        try {
            if (pdfDescriptor != null) pdfDescriptor.close();
        } catch (Exception ignored) { }

        pdfDescriptor = null;
    }

    @Override
    protected void onDestroy() {
        closePdf();
        super.onDestroy();
    }
            }
                                 
