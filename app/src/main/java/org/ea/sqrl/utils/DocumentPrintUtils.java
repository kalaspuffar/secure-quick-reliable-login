package org.ea.sqrl.utils;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

import org.ea.sqrl.R;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class hosts utility functions for drawing print documents.
 *
 * @author Alex Hauser
 */
public class DocumentPrintUtils {
    private static final String TAG = "DocumentPrintUtils";

    public static int drawTextBlock(Canvas canvas, String text, Layout.Alignment alignment, TextPaint textPaint, int y, int margin) {
        int width = canvas.getWidth() - (margin * 2);

        StaticLayout staticLayout = new StaticLayout(
                text,
                textPaint,
                width,
                alignment,
                1,
                0,
                false);

        canvas.save();
        canvas.translate(margin, y);
        staticLayout.draw(canvas);
        canvas.restore();

        return staticLayout.getHeight();
    }

    public static void drawPrintPageFooter(Activity activity, Canvas canvas) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        StringBuilder versionString = new StringBuilder();
        versionString.append(activity.getString(R.string.print_version_string));
        versionString.append(" ");
        versionString.append(sdf.format(new Date()));
        versionString.append(" * ");
        versionString.append("Version: ");

        Bitmap loadLogo = BitmapFactory.decodeResource(activity.getResources(), R.drawable.sqrl_print_logo);
        Bitmap sqrlLogo = Bitmap.createScaledBitmap(loadLogo, 40, 40, false);

        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(10);
        textPaint.setFakeBoldText(false);
        textPaint.setColor(Color.DKGRAY);

        canvas.drawBitmap(
                sqrlLogo,
                (canvas.getWidth()/2) - (sqrlLogo.getScaledWidth(canvas) / 2),
                canvas.getHeight() - 140,
                new Paint()
        );

        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            versionString.append(pInfo.versionName);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        drawTextBlock(
                canvas,
                versionString.toString(),
                Layout.Alignment.ALIGN_CENTER,
                textPaint,
                canvas.getHeight() - 80,
                20);

        drawTextBlock(canvas,
                "https://github.com/kalaspuffar/secure-quick-reliable-login",
                Layout.Alignment.ALIGN_CENTER,
                textPaint,
                canvas.getHeight() - 65,
                20);
    }
}
