package com.owuor.educue.academics.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PdfFooterPageEvent extends PdfPageEventHelper {

    private final Font footerFont =
            new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);

    private final Font systemFont =
            new Font(Font.HELVETICA, 8, Font.BOLD, new Color(Color.darkGray.getRGB()));

    @Override
    public void onEndPage(PdfWriter writer, Document document) {

        PdfContentByte canvas = writer.getDirectContent();

        float left = document.left();
        float right = document.right();
        float footerY = document.bottom() - 15;

        //==================================================
        // Divider Line
        //==================================================

        canvas.setColorStroke(new Color(180, 180, 180));
        canvas.setLineWidth(0.7f);

        canvas.moveTo(left, footerY + 18);
        canvas.lineTo(right, footerY + 18);
        canvas.stroke();

        //==================================================
        // Left
        //==================================================



        //==================================================
        // Center
        //==================================================

        String generated =
                "Generated On : " +
                        LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));

        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_LEFT,
                new Phrase(generated, footerFont),
                left,
                footerY,
                0
        );

        //==================================================
        // Right
        //==================================================

        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_RIGHT,
                new Phrase("Page " + writer.getPageNumber(), footerFont),
                right,
                footerY,
                0
        );
    }
}
