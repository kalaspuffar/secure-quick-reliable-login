package org.ea.sqrl.services;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
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

import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;
import org.ea.sqrl.utils.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Daniel Persson
 */
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
                .Builder("RescueCode.pdf")
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
        int marginTop = 65;
        int marginLeft = 35;
        int lastBlockY = marginTop;

        TextPaint headlineText = new TextPaint();
        headlineText.setAntiAlias(true);
        headlineText.setTextSize(24);
        headlineText.setFakeBoldText(true);

        TextPaint redBoldText = new TextPaint();
        redBoldText.setAntiAlias(true);
        redBoldText.setTextSize(24);
        redBoldText.setFakeBoldText(true);
        redBoldText.setColor(Color.RED);

        TextPaint stdText = new TextPaint();
        stdText.setAntiAlias(true);
        stdText.setTextSize(14);

        String headline = activity.getResources().getString(R.string.rescue_code_page_headline);
        String warning = "!! " + activity.getResources().getString(R.string.rescue_code_page_warning).toUpperCase() + " !!";
        String description = activity.getResources().getString(R.string.rescue_code_page_description);

        lastBlockY += Utils.drawTextBlock(
                canvas,
                headline,
                Layout.Alignment.ALIGN_CENTER,
                headlineText,
                lastBlockY,
                marginLeft);

        lastBlockY += Utils.drawTextBlock(
                canvas,
                warning,
                Layout.Alignment.ALIGN_CENTER,
                redBoldText,
                lastBlockY + 40,
                marginLeft) + 40;

        lastBlockY += Utils.drawTextBlock(
                canvas,
                description,
                Layout.Alignment.ALIGN_NORMAL,
                stdText,
                lastBlockY + 20,
                marginLeft) + 20;

        List<String> rescueCode = storage.getTempShowableRescueCode();
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : rescueCode) {
            if (!first) sb.append("-");
            sb.append(s);
            first = false;
        }
        String rescueCodeOutput = sb.toString();

        lastBlockY += Utils.drawTextBlock(
                canvas,
                rescueCodeOutput,
                Layout.Alignment.ALIGN_CENTER,
                redBoldText,
                lastBlockY + 60,
                marginLeft) + 60;

        String idName = activity.getResources().getString(R.string.txt_identity_name_hint) +
                ": __________________________________";

        Utils.drawTextBlock(
                canvas,
                idName,
                Layout.Alignment.ALIGN_CENTER,
                stdText,
                lastBlockY + 60,
                marginLeft);

        Utils.drawPrintPageFooter(activity, canvas);
    }
}
