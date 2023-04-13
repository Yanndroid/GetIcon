package de.dlyt.yanndroid.geticon;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private PackageManager packageManager;
    private List<ApplicationInfo> packages;

    private int maskMenuId;
    private int monoMenuId;
    private boolean exportMask = true;
    private boolean exportMono = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU;

    private int size = 512;

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
            holder.itemView.setOnClickListener(v -> MainActivity.this.getIcons(packageName));
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

    private void getIcons(String packageName) {
        try {
            long timeStamp = System.currentTimeMillis();
            Drawable icon = packageManager.getApplicationIcon(packageName);

            Bitmap def = buildIcon(icon, size, false, false);
            if (def != null) {
                saveBitmap(def, packageName, timeStamp, "default");
                Toast.makeText(this, "default", Toast.LENGTH_SHORT).show();
            }
            if (exportMask) {
                Bitmap mask = buildIcon(icon, size, false, true);
                if (mask != null) {
                    saveBitmap(mask, packageName, timeStamp, "mask");
                    Toast.makeText(this, "mask", Toast.LENGTH_SHORT).show();
                }
            }
            if (exportMono) {
                Bitmap mono = buildIcon(icon, size, true, false);
                if (mono != null) {
                    saveBitmap(mono, packageName, timeStamp, "mono");
                    Toast.makeText(this, "mono", Toast.LENGTH_SHORT).show();
                }
            }
            if (exportMask && exportMono) {
                Bitmap mask_mono = buildIcon(icon, size, true, true);
                if (mask_mono != null) {
                    saveBitmap(mask_mono, packageName, timeStamp, "mask_mono");
                    Toast.makeText(this, "mask_mono", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (PackageManager.NameNotFoundException | IOException e) {
            Toast.makeText(this, "Error" + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @SuppressLint("InlinedApi")
    private Bitmap buildIcon(Drawable drawable, int size, boolean mono, boolean mask) {
        if (mono && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU)
            return null;
        if (drawable instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable adaptiveDrawable = (AdaptiveIconDrawable) drawable;
            adaptiveDrawable.setBounds(0, 0, size, size);

            Drawable background = adaptiveDrawable.getBackground().mutate();
            Drawable foreground = mono ? adaptiveDrawable.getMonochrome() : adaptiveDrawable.getForeground().mutate();
            if (background == null || foreground == null) return null;

            if (mono) {
                boolean darkMode = isDarkMode();
                background.setTint(getColor(darkMode ? android.R.color.system_accent1_800 : android.R.color.system_accent1_300));
                foreground.setTint(darkMode ? getColor(android.R.color.system_accent1_50) : Color.WHITE);
            }

            Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            if (mask) canvas.clipPath(adaptiveDrawable.getIconMask());
            background.draw(canvas);
            foreground.draw(canvas);
            return output;
        }
        return null;
    }

    private boolean isDarkMode() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    @SuppressLint("DefaultLocale")
    private void saveBitmap(Bitmap bitmap, String packageName, long timeStamp, String suffix) throws IOException {
        String fileName = String.format("%s_%d_%s.png", packageName, timeStamp, suffix);
        OutputStream os = new FileOutputStream(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fileName));
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, os);
        os.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        maskMenuId = menu.add("Export masked").setCheckable(true).setChecked(exportMask).getItemId();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
            monoMenuId = menu.add("Export monochrome").setCheckable(true).setChecked(exportMono).getItemId();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == maskMenuId) item.setChecked(exportMask = !exportMask);
        if (item.getItemId() == monoMenuId) item.setChecked(exportMono = !exportMono);
        return true;
    }
}