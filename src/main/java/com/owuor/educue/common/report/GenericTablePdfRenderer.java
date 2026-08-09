package com.owuor.educue.common.report;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.owuor.educue.academics.service.PdfFooterPageEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;

import static com.owuor.educue.common.report.PdfReportHelper.*;

@Component
@RequiredArgsConstructor
public class GenericTablePdfRenderer {

    private final InstitutionPdfHeaderRenderer headerRenderer;

    public byte[] render(
            String title,
            LinkedHashMap<String, String> details,
            List<String> headers,
            List<List<String>> rows
    ) {
        return render(title, details, headers, rows, null);
    }

    public byte[] render(
            String title,
            LinkedHashMap<String, String> details,
            List<String> headers,
            List<List<String>> rows,
            float[] columnWidths
    ) {
        try {
            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            Document document = new Document(
                    PageSize.A4,
                    30,
                    30,
                    32,
                    48
            );

            PdfWriter writer =
                    PdfWriter.getInstance(
                            document,
                            out
                    );

            writer.setPageEvent(
                    new PdfFooterPageEvent()
            );

            document.open();

            headerRenderer.render(document);

            addTitle(document, title);
            addDetails(document, details);
            addTable(document, headers, rows, columnWidths);

            document.close();

            return out.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Could not generate PDF report",
                    exception
            );
        }
    }

    private void addTitle(
            Document document,
            String title
    ) throws DocumentException {
        Paragraph heading = new Paragraph(
                safe(title).toUpperCase(),
                font(
                        11,
                        Font.BOLD,
                        Color.BLACK
                )
        );

        heading.setAlignment(
                Element.ALIGN_CENTER
        );

        heading.setSpacingBefore(8);
        heading.setSpacingAfter(10);

        document.add(heading);
    }

    private void addDetails(
            Document document,
            LinkedHashMap<String, String> details
    ) throws DocumentException {
        if (details == null || details.isEmpty()) {
            return;
        }

        PdfPTable info = new PdfPTable(2);

        info.setWidthPercentage(100);
        info.setWidths(
                new float[]{1.5f, 4.5f}
        );

        for (var entry : details.entrySet()) {
            addGenericCell(
                    info,
                    entry.getKey(),
                    true,
                    new Color(235, 235, 235),
                    Element.ALIGN_LEFT,
                    7
            );

            addGenericCell(
                    info,
                    entry.getValue(),
                    false,
                    Color.WHITE,
                    Element.ALIGN_LEFT,
                    7
            );
        }

        info.setSpacingAfter(10);

        document.add(info);
    }

    private void addTable(
            Document document,
            List<String> headers,
            List<List<String>> rows,
            float[] columnWidths
    ) throws DocumentException {
        PdfPTable table =
                new PdfPTable(headers.size());

        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        if (columnWidths != null) {
            if (columnWidths.length != headers.size()) {
                throw new IllegalArgumentException("Column widths must match the number of headers");
            }
            table.setWidths(columnWidths);
        }

        for (String header : headers) {
            addGenericCell(
                    table,
                    header,
                    true,
                    new Color(215, 215, 215),
                    Element.ALIGN_LEFT,
                    7
            );
        }

        int rowIndex = 0;

        for (List<String> row : rows) {
            Color background =
                    rowIndex++ % 2 == 0
                            ? Color.WHITE
                            : new Color(
                            247,
                            247,
                            247
                    );

            for (String value : row) {
                addGenericCell(
                        table,
                        value,
                        false,
                        background,
                        Element.ALIGN_LEFT,
                        7
                );
            }
        }

        document.add(table);
    }
}
