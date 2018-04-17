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

import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import io.nayuki.qrcodegen.QrCode;

@RequiresApi(Build.VERSION_CODES.KITKAT)
public class RescueCodePrintDocumentAdapter extends PrintDocumentAdapter {
    private static final String TAG = "RescueCodePrintDocumentAdapter";
    private final Activity activity;
    private PrintedPdfDocument mPdfDocument;

    public RescueCodePrintDocumentAdapter(Activity activity) {
        this.activity = activity;
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
                .Builder("rescueCode.pdf")
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
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        SQRLStorage storage = SQRLStorage.getInstance();
        List<String> rescueCode = storage.getTempShowableRescueCode();

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(String s : rescueCode) {
            if(!first) sb.append("-");
            sb.append(s);
            first = false;
        }

        String rescueCodeOutput = sb.toString();

        int fontSize = 24;

        paint.setColor(Color.RED);
        paint.setFakeBoldText(true);
        paint.setTextSize(fontSize);

        int middleX = canvas.getWidth() / 2;
        int middleY = canvas.getHeight() / 2;
        Rect bounds = new Rect();
        paint.getTextBounds(rescueCodeOutput, 0, rescueCodeOutput.length(), bounds);
        int width = bounds.width();

        canvas.drawText(rescueCodeOutput, middleX - (width / 2), middleY - (fontSize / 2), paint);
    }
}
