package com.owuor.educue.common.report;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class PdfReportHelper {

    public static final Color BORDER_COLOR =
            new Color(70, 70, 70);

    public static final Color MUTED_TEXT =
            new Color(75, 75, 75);

    private PdfReportHelper() {
    }

    public static Font font(
            float size,
            int style,
            Color color
    ) {
        return new Font(
                Font.HELVETICA,
                size,
                style,
                color
        );
    }

    public static String safe(String value) {
        return value == null ? "" : value;
    }

    public static String join(
            String separator,
            String... values
    ) {
        return Arrays.stream(values)
                .filter(value ->
                        value != null &&
                        !value.isBlank()
                )
                .collect(
                        Collectors.joining(separator)
                );
    }

    public static String formatNumber(
            BigDecimal value
    ) {
        return value == null
                ? "-"
                : value.setScale(
                2,
                RoundingMode.HALF_UP
        ).toPlainString();
    }

    public static String formatPercentage(
            BigDecimal value
    ) {
        return value == null
                ? "-"
                : value.setScale(
                2,
                RoundingMode.HALF_UP
        ).toPlainString() + "%";
    }

    public static String formatAttempt(
            String attemptType
    ) {
        if (attemptType == null) {
            return "-";
        }

        return switch (attemptType.toUpperCase()) {
            case "RESIT" -> "Resit";
            case "RETAKE" -> "Retake";
            case "SUPPLEMENTARY" -> "Supp.";
            default -> "Normal";
        };
    }

    public static PdfPCell plainContainerCell() {
        PdfPCell cell = new PdfPCell();

        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(2);
        cell.setPaddingBottom(2);
        cell.setPaddingLeft(0);
        cell.setPaddingRight(8);

        return cell;
    }

    public static void addCentered(
            PdfPCell cell,
            String value,
            boolean italic
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        Paragraph line = new Paragraph(
                value,
                font(
                        7.5f,
                        italic
                                ? Font.ITALIC
                                : Font.NORMAL,
                        MUTED_TEXT
                )
        );

        line.setAlignment(
                Element.ALIGN_CENTER
        );

        cell.addElement(line);
    }

    public static void addGenericCell(
            PdfPTable table,
            String value,
            boolean bold,
            Color background,
            int alignment,
            int padding
    ) {
        PdfPCell cell = new PdfPCell(
                new Phrase(
                        safe(value),
                        font(
                                8,
                                bold
                                        ? Font.BOLD
                                        : Font.NORMAL,
                                Color.BLACK
                        )
                )
        );

        cell.setBackgroundColor(background);
        cell.setBorderColor(
                new Color(185, 185, 185)
        );
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(padding);

        table.addCell(cell);
    }

    public static Paragraph legendLine(
            String value
    ) {
        Paragraph paragraph = new Paragraph(
                value,
                font(
                        7,
                        Font.NORMAL,
                        Color.BLACK
                )
        );

        paragraph.setLeading(9);

        return paragraph;
    }
}
