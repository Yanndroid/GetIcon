package de.dlyt.yanndroid.geticon;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int ICON_SIZE = 512;

    private PackageManager packageManager;
    private List<ApplicationInfo> packages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RecyclerView list = new RecyclerView(this);
        list.setVerticalScrollBarEnabled(true);
        setContentView(list);
        packageManager = getPackageManager();
        packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        packages.sort(Comparator.comparing(o -> o.packageName));

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new ListAdapter());
    }

    private void laodAll(String packageName) {
        try {
            long timeStamp = System.currentTimeMillis();
            Drawable base = packageManager.getApplicationIcon(packageName);

            loadIconVariation(packageName, timeStamp, "default", base, false, false, false);
            loadIconVariation(packageName, timeStamp, "default_mask", base, true, false, false);

            loadIconVariation(packageName, timeStamp, "mono", base, false, true, false);
            loadIconVariation(packageName, timeStamp, "mono_mask", base, true, true, false);

            loadIconVariation(packageName, timeStamp, "mono_dark", base, false, true, true);
            loadIconVariation(packageName, timeStamp, "mono_dark_mask", base, true, true, true);

            Toast.makeText(this, packageName, Toast.LENGTH_SHORT).show();
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void loadIconVariation(String packageName, long timeStamp, String suffix, Drawable base, boolean mask, boolean mono, boolean darkMode) {
        Bitmap icon = buildIcon(base, ICON_SIZE, mask, mono, darkMode);
        try {
            saveBitmap(icon, packageName, timeStamp, suffix);
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private Bitmap buildIcon(Drawable drawable, int size, boolean mask, boolean mono, boolean darkMode) {
        if (drawable instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable adaptiveDrawable = (AdaptiveIconDrawable) drawable;
            adaptiveDrawable.setBounds(0, 0, size, size);

            Drawable background = adaptiveDrawable.getBackground().mutate();
            Drawable foreground = adaptiveDrawable.getForeground().mutate();

            if (mono && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Drawable monochrome = adaptiveDrawable.getMonochrome();
                if (monochrome != null) foreground = monochrome.mutate();
            }

            if (mono) {
                int bg_tint = Color.parseColor(darkMode ? "#003549" : "#4BB6E8");
                int fg_tint = darkMode ? Color.parseColor("#E0F3FF") : Color.WHITE;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    bg_tint = getColor(darkMode ? android.R.color.system_accent1_800 : android.R.color.system_accent1_300);
                    fg_tint = darkMode ? getColor(android.R.color.system_accent1_50) : Color.WHITE;
                }

                background.setTint(bg_tint);
                foreground.setTint(fg_tint);
            }

            Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            if (mask) canvas.clipPath(adaptiveDrawable.getIconMask());
            background.draw(canvas);
            foreground.draw(canvas);
            return output;
        }

        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        drawable.draw(canvas);
        return output;
    }

    @SuppressLint("DefaultLocale")
    private void saveBitmap(Bitmap bitmap, String packageName, long timeStamp, String suffix) throws IOException {
        String fileName = String.format("%s_%d_%s.png", packageName, timeStamp, suffix);
        OutputStream os = Files.newOutputStream(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fileName).toPath());
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, os);
        os.close();
    }

    private class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {
        @NonNull
        @Override
        public ListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(MainActivity.this);
            textView.setTextSize(20);
            textView.setSingleLine();
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setPadding(10, 10, 10, 10);
            return new ViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull ListAdapter.ViewHolder holder, int position) {
            String packageName = packages.get(position).packageName;
            ((TextView) holder.itemView).setText(packageName);
            holder.itemView.setOnClickListener(v -> MainActivity.this.laodAll(packageName));
        }

        @Override
        public int getItemCount() {
            return packages.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }

}