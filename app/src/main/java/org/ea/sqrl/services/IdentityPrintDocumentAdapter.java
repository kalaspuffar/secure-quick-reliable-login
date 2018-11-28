package org.ea.sqrl.services;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.nayuki.qrcodegen.QrCode;

/**
 *
 * @author Daniel Persson
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
public class IdentityPrintDocumentAdapter extends PrintDocumentAdapter {
    private static final String TAG = "IdentityPrint";
    private final Activity activity;
    private final String identityName;
    private final boolean withoutPassword;
    private PrintedPdfDocument mPdfDocument;

    public IdentityPrintDocumentAdapter(Activity activity, String identityName, boolean withoutPassword) {
        this.activity = activity;
        this.identityName = identityName;
        this.withoutPassword = withoutPassword;
    }

    @Override
    public void onLayout(
            PrintAttributes oldAttributes,
            PrintAttributes newAttributes,
            CancellationSignal cancellationSignal,
            LayoutResultCallback callback,
            Bundle metadata
    ) {
        mPdfDocument = new PrintedPdfDocument(activity, newAttributes);
        if (cancellationSignal.isCanceled() ) {
            callback.onLayoutCancelled();
            return;
        }

        PrintDocumentInfo info = new PrintDocumentInfo
                .Builder("identity.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build();
        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(
            final PageRange[] pageRanges,
            final ParcelFileDescriptor destination,
            final CancellationSignal cancellationSignal,
            final WriteResultCallback callback
    ) {
        PdfDocument.Page page = mPdfDocument.startPage(0);

        if (cancellationSignal.isCanceled()) {
            callback.onWriteCancelled();
            mPdfDocument.close();
            mPdfDocument = null;
            return;
        }

        drawPage(page);

        mPdfDocument.finishPage(page);
        try {
            mPdfDocument.writeTo(new FileOutputStream(
                    destination.getFileDescriptor()));
        } catch (IOException e) {
            callback.onWriteFailed(e.toString());
            return;
        } finally {
            mPdfDocument.close();
            mPdfDocument = null;
        }
        callback.onWriteFinished(pageRanges);
    }

    private void drawCenteredText(Canvas canvas, Paint paint, String text, int y, int fontSize) {
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);
        paint.setTextSize(fontSize);

        int middle = canvas.getWidth() / 2;
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        int width = bounds.width();

        canvas.drawText(text, middle - (width / 2), y, paint);
    }

    private int findNextString(String text, Paint paint, int maxWidth) {
        String[] stringArr = text.split(" ");
        Rect bounds = new Rect();
        String nextString = "";
        int lastLen = 0;
        for(String s : stringArr) {
            lastLen = nextString.length();
            nextString += s;
            paint.getTextBounds(nextString, 0, nextString.length(), bounds);
            int width = bounds.width();
            if(width > maxWidth) {
                return lastLen;
            }
            nextString += " ";
        }
        return nextString.trim().length();
    }

    private int drawBlockOfText(Canvas canvas, Paint paint, String text, int y, int fontSize) {
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(false);
        paint.setTextSize(fontSize);

        int margin = 52;
        int maxWidth = canvas.getWidth();

        int i=0;
        while(!text.isEmpty()) {
            int nextEnd = findNextString(text, paint, maxWidth - (margin * 2));
            String output = text.substring(0, nextEnd);
            canvas.drawText(output, margin, y + (i * fontSize), paint);
            text = text.substring(nextEnd).trim();
            i++;
        }

        return y + (i * fontSize);
    }

    private void drawPage(PdfDocument.Page page) {
        SQRLStorage storage = SQRLStorage.getInstance();
        Canvas canvas = page.getCanvas();

        int titleBaseLine = 72;

        Paint paint = new Paint();
        String identityTitle = "\"" + identityName + "\" SQRL Identity";
        drawCenteredText(canvas, paint, identityTitle, titleBaseLine, 16);

        int bodyText = 12;

        int lastBlockY = drawBlockOfText(canvas, paint, activity.getString(R.string.print_identity_desc1), titleBaseLine + 32, bodyText);

        byte[] saveData;
        if(this.withoutPassword) {
            saveData = SQRLStorage.getInstance().createSaveDataWithoutPassword();
        } else {
            saveData = SQRLStorage.getInstance().createSaveData();
        }

        int canvasMiddle = canvas.getWidth() / 2;
        QrCode qrCode = QrCode.encodeBinary(saveData, QrCode.Ecc.MEDIUM);
        Bitmap bitmap = qrCode.toImage(3, 0);

        int bitmapWidth = bitmap.getScaledWidth(canvas);

        canvas.drawBitmap(bitmap, canvasMiddle - (bitmapWidth / 2), lastBlockY + bodyText, paint);

        int bitmapHeight = bitmap.getScaledHeight(canvas);

        int nextTextBlock = lastBlockY + (bodyText * 2) + bitmapHeight;

        lastBlockY = drawBlockOfText(canvas, paint, activity.getString(R.string.print_identity_desc2), nextTextBlock + (bodyText * 2), bodyText);

        int i = 0;
        try {
            paint.setTypeface(Typeface.MONOSPACE);
            for (String s : storage.getVerifyingRecoveryBlock().split("\n")) {
                drawCenteredText(canvas, paint, s, lastBlockY + bodyText + (i * bodyText), bodyText);
                i++;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        paint.setTypeface(Typeface.DEFAULT);
        drawBlockOfText(canvas, paint, activity.getString(R.string.print_identity_desc3), lastBlockY + (bodyText * 2) + (i * bodyText), bodyText);


        StringBuilder versionString = new StringBuilder();
        versionString.append(activity.getString(R.string.print_version_string));
        versionString.append(" ");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        versionString.append(sdf.format(new Date()));
        versionString.append(" * ");
        versionString.append("Version: ");

        Bitmap loadLogo = BitmapFactory.decodeResource(activity.getResources(), R.drawable.sqrl_print_logo);
        Bitmap sqrlLogo = Bitmap.createScaledBitmap(loadLogo, 40, 40, false);

        canvas.drawBitmap(
                sqrlLogo,
                canvasMiddle - (sqrlLogo.getScaledWidth(canvas) / 2),
                canvas.getHeight() - 140,
                paint
        );

        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            versionString.append(pInfo.versionName);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        drawCenteredText(canvas, paint, versionString.toString(), canvas.getHeight() - 80, 10);
        drawCenteredText(canvas, paint, "https://github.com/kalaspuffar/secure-quick-reliable-login", canvas.getHeight() - 65, 10);
    }
}
