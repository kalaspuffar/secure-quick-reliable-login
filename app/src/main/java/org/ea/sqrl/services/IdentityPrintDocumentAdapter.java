package org.ea.sqrl.services;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
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
import android.text.Layout;
import android.text.TextPaint;
import android.util.Log;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.DocumentPrintUtils;
import org.ea.sqrl.utils.Utils;

import java.io.FileOutputStream;
import java.io.IOException;

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
                .Builder("Identity.pdf")
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

    private void drawPage(PdfDocument.Page page) {
        SQRLStorage storage = SQRLStorage.getInstance(activity);
        Canvas canvas = page.getCanvas();
        int marginLeftRight = 35;
        int marginTop = 65;
        int lastBlockY = marginTop;

        TextPaint headlineTextPaint = new TextPaint();
        headlineTextPaint.setTextSize(16);
        headlineTextPaint.setFakeBoldText(true);

        TextPaint bodyTextPaint = new TextPaint();
        bodyTextPaint.setTextSize(12);

        TextPaint identityTextTextPaint = new TextPaint(bodyTextPaint);
        identityTextTextPaint.setTypeface(Typeface.MONOSPACE);
        identityTextTextPaint.setFakeBoldText(true);

        String identityTitle = "\"" + identityName + "\" SQRL Identity";

        lastBlockY += DocumentPrintUtils.drawTextBlock(
                canvas,
                identityTitle,
                Layout.Alignment.ALIGN_CENTER,
                headlineTextPaint,
                marginTop,
                marginLeftRight);

        lastBlockY += DocumentPrintUtils.drawTextBlock(
                canvas,
                activity.getString(R.string.print_identity_desc1),
                Layout.Alignment.ALIGN_NORMAL,
                bodyTextPaint,
                lastBlockY + 20,
                marginLeftRight) + 20;

        byte[] saveData;
        if(this.withoutPassword) {
            saveData = storage.createSaveDataWithoutPassword();
        } else {
            saveData = storage.createSaveData();
        }

        int canvasMiddle = canvas.getWidth() / 2;
        QrCode qrCode = QrCode.encodeBinary(saveData, QrCode.Ecc.MEDIUM);
        Bitmap bitmap = qrCode.toImage(3, 0);

        int bitmapWidth = bitmap.getScaledWidth(canvas);

        canvas.drawBitmap(bitmap, canvasMiddle - (bitmapWidth / 2), lastBlockY + 20, new Paint());

        lastBlockY += bitmap.getScaledHeight(canvas) + 20;

        lastBlockY += DocumentPrintUtils.drawTextBlock(
                canvas,
                activity.getString(R.string.print_identity_desc2),
                Layout.Alignment.ALIGN_NORMAL,
                bodyTextPaint,
                lastBlockY + 20,
                marginLeftRight) + 30;


        int i = 0;
        try {
            Utils.refreshStorageFromDb(activity);
            for (String identityTextBlock : storage.getVerifyingRecoveryBlock().split("\n")) {
                lastBlockY += DocumentPrintUtils.drawTextBlock(
                        canvas,
                        identityTextBlock,
                        Layout.Alignment.ALIGN_CENTER,
                        identityTextTextPaint,
                        lastBlockY + 3,
                        marginLeftRight) + 3;
                i++;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        DocumentPrintUtils.drawTextBlock(
                canvas,
                activity.getString(R.string.print_identity_desc3),
                Layout.Alignment.ALIGN_NORMAL,
                bodyTextPaint,
                lastBlockY + 20,
                marginLeftRight);

        DocumentPrintUtils.drawPrintPageFooter(activity, canvas);
    }
}
