package com.owuor.educue.common.report;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.owuor.educue.results.dto.StudentResultResponse;
import com.owuor.educue.results.dto.TranscriptYearGroup;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.owuor.educue.common.report.PdfReportHelper.BORDER_COLOR;
import static com.owuor.educue.common.report.PdfReportHelper.MUTED_TEXT;
import static com.owuor.educue.common.report.PdfReportHelper.font;
import static com.owuor.educue.common.report.PdfReportHelper.formatPercentage;
import static com.owuor.educue.common.report.PdfReportHelper.legendLine;
import static com.owuor.educue.common.report.PdfReportHelper.plainContainerCell;
import static com.owuor.educue.common.report.PdfReportHelper.safe;

@Component
public class TranscriptPdfSections {

    public void addTitle(
            Document document
    ) throws DocumentException {

        Paragraph heading = new Paragraph(
                "PROVISIONAL ACADEMIC TRANSCRIPT",
                font(
                        12,
                        Font.BOLD,
                        Color.BLACK
                )
        );

        heading.setAlignment(
                Element.ALIGN_CENTER
        );

        heading.setSpacingBefore(8);
        heading.setSpacingAfter(2);

        document.add(heading);

        Paragraph subtitle = new Paragraph(
                "STATEMENT OF PROVISIONAL ACADEMIC RESULTS",
                font(
                        7.5f,
                        Font.NORMAL,
                        MUTED_TEXT
                )
        );

        subtitle.setAlignment(
                Element.ALIGN_CENTER
        );

        subtitle.setSpacingAfter(10);

        document.add(subtitle);
    }

    public void addStudentDetails(
            Document document,
            LinkedHashMap<String, String> details
    ) throws DocumentException {

        if (details == null || details.isEmpty()) {
            return;
        }

        PdfPTable information = new PdfPTable(4);

        information.setWidthPercentage(100);

        information.setWidths(
                new float[]{
                        1.35f,
                        2.35f,
                        1.45f,
                        3.0f
                }
        );

        List<Map.Entry<String, String>> entries =
                new ArrayList<>(details.entrySet());

        for (
                int index = 0;
                index < entries.size();
                index += 2
        ) {
            Map.Entry<String, String> first =
                    entries.get(index);

            addInformationLabel(
                    information,
                    first.getKey()
            );

            addInformationValue(
                    information,
                    first.getValue()
            );

            if (index + 1 < entries.size()) {
                Map.Entry<String, String> second =
                        entries.get(index + 1);

                addInformationLabel(
                        information,
                        second.getKey()
                );

                addInformationValue(
                        information,
                        second.getValue()
                );
            } else {
                addInformationLabel(
                        information,
                        ""
                );

                addInformationValue(
                        information,
                        ""
                );
            }
        }

        information.setSpacingAfter(10);

        document.add(information);
    }

    public void addYear(
            Document document,
            TranscriptYearGroup year
    ) throws DocumentException {

        addYearHeading(
                document,
                year
        );

        addUnitsTable(
                document,
                year.units()
        );

        addYearAverages(
                document,
                year
        );
    }

    public void addRecommendation(
            Document document,
            String recommendation
    ) throws DocumentException {

        Paragraph heading = new Paragraph(
                "RECOMMENDATION:",
                font(
                        8,
                        Font.BOLD | Font.UNDERLINE,
                        Color.BLACK
                )
        );

        heading.setSpacingBefore(6);
        heading.setSpacingAfter(4);

        document.add(heading);

        Paragraph value = new Paragraph(
                recommendation == null ||
                recommendation.isBlank()
                        ? "PENDING ACADEMIC REVIEW"
                        : recommendation,
                font(
                        8,
                        Font.BOLD,
                        Color.BLACK
                )
        );

        value.setAlignment(
                Element.ALIGN_CENTER
        );

        value.setLeading(11);
        value.setSpacingAfter(10);

        document.add(value);
    }

    public void addLegend(
            Document document
    ) throws DocumentException {

        PdfPTable legend = new PdfPTable(2);

        legend.setWidthPercentage(100);

        legend.setWidths(
                new float[]{
                        1f,
                        1f
                }
        );

        legend.setKeepTogether(true);
        legend.setSpacingBefore(4);
        legend.setSpacingAfter(9);

        PdfPCell grades = plainContainerCell();

        grades.addElement(
                new Paragraph(
                        "Legend:",
                        font(
                                7.5f,
                                Font.BOLD,
                                Color.BLACK
                        )
                )
        );

        grades.addElement(
                legendLine(
                        "A = 70% - 100%   - Excellent"
                )
        );

        grades.addElement(
                legendLine(
                        "B = 60% - 69%    - Very Good"
                )
        );

        grades.addElement(
                legendLine(
                        "C = 50% - 59%    - Good"
                )
        );

        grades.addElement(
                legendLine(
                        "D = 40% - 49%    - Fair"
                )
        );

        grades.addElement(
                legendLine(
                        "F = 0% - 39%     - Fail"
                )
        );

        legend.addCell(grades);

        PdfPCell resultCodes = plainContainerCell();

        resultCodes.addElement(
                new Paragraph(
                        "Result Codes:",
                        font(
                                7.5f,
                                Font.BOLD,
                                Color.BLACK
                        )
                )
        );

        resultCodes.addElement(
                legendLine(
                        "PASSED  - Unit successfully completed"
                )
        );

        resultCodes.addElement(
                legendLine(
                        "FAILED  - Resit or retake may be required"
                )
        );

        resultCodes.addElement(
                legendLine(
                        "CT      - Credit transfer"
                )
        );

        resultCodes.addElement(
                legendLine(
                        "-       - Result not available"
                )
        );

        legend.addCell(resultCodes);

        document.add(legend);
    }

    public void addProvisionalNotice(
            Document document
    ) throws DocumentException {

        document.add(
                new LineSeparator(
                        0.5f,
                        100,
                        BORDER_COLOR,
                        Element.ALIGN_CENTER,
                        0
                )
        );

        Paragraph notice = new Paragraph(
                "PROVISIONAL RESULTS NOTICE: "
                + "These results are issued for informational purposes only "
                + "and remain subject to verification, correction and formal "
                + "approval by the institution. This document is not a final "
                + "graduation transcript or certificate.",
                font(
                        6.8f,
                        Font.ITALIC,
                        MUTED_TEXT
                )
        );

        notice.setAlignment(
                Element.ALIGN_JUSTIFIED
        );

        notice.setLeading(9);
        notice.setSpacingBefore(4);
        notice.setSpacingAfter(15);

        document.add(notice);
    }

    public void addSignatures(
            Document document
    ) throws DocumentException {

        PdfPTable signatures = new PdfPTable(2);

        signatures.setWidthPercentage(100);

        signatures.setWidths(
                new float[]{
                        1.5f,
                        1f
                }
        );

        signatures.setKeepTogether(true);

        signatures.addCell(
                createSignatureCell()
        );

        signatures.addCell(
                createStampCell()
        );

        document.add(signatures);
    }

    public void addNoResults(
            Document document
    ) throws DocumentException {

        Paragraph empty = new Paragraph(
                "No academic results are currently available.",
                font(
                        9,
                        Font.NORMAL,
                        MUTED_TEXT
                )
        );

        empty.setAlignment(
                Element.ALIGN_CENTER
        );

        empty.setSpacingBefore(25);
        empty.setSpacingAfter(25);

        document.add(empty);
    }

    private void addYearHeading(
            Document document,
            TranscriptYearGroup year
    ) throws DocumentException {

        PdfPTable heading = new PdfPTable(1);

        heading.setWidthPercentage(100);
        heading.setKeepTogether(true);

        heading.setSpacingBefore(10);
        heading.setSpacingAfter(3);

        PdfPCell cell = new PdfPCell(
                new Phrase(
                        safe(year.yearTitle()).toUpperCase(),
                        font(
                                9,
                                Font.BOLD,
                                Color.BLACK
                        )
                )
        );

        /*
         * Only underline the academic year.
         */
        cell.setBorder(
                Rectangle.BOTTOM
        );

        cell.setBorderColor(
                BORDER_COLOR
        );

        cell.setBorderWidthBottom(
                0.8f
        );

        cell.setPaddingTop(3);
        cell.setPaddingBottom(4);
        cell.setPaddingLeft(0);
        cell.setPaddingRight(0);

        heading.addCell(cell);

        document.add(heading);
    }

    private void addUnitsTable(
            Document document,
            List<StudentResultResponse> units
    ) throws DocumentException {

        PdfPTable table = new PdfPTable(5);

        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        table.setSplitLate(false);
        table.setSplitRows(true);

        table.setWidths(
                new float[]{
                        1.20f, // Course code
                        5.20f, // Course description
                        0.80f, // Credits
                        0.75f, // Grade
                        1.00f  // Outcome
                }
        );

        addHeaderCell(
                table,
                "Course Code",
                Element.ALIGN_LEFT
        );

        addHeaderCell(
                table,
                "Course Description",
                Element.ALIGN_LEFT
        );

        addHeaderCell(
                table,
                "Credits",
                Element.ALIGN_CENTER
        );

        addHeaderCell(
                table,
                "Grade",
                Element.ALIGN_CENTER
        );

        addHeaderCell(
                table,
                "Outcome",
                Element.ALIGN_CENTER
        );

        List<StudentResultResponse> orderedUnits =
                units == null
                        ? List.of()
                        : units.stream()
                        .sorted(
                                Comparator.comparing(
                                        StudentResultResponse::unitCode,
                                        Comparator.nullsLast(
                                                String.CASE_INSENSITIVE_ORDER
                                        )
                                )
                        )
                        .toList();

        for (StudentResultResponse result :
                orderedUnits) {

            addBodyCell(
                    table,
                    result.unitCode(),
                    Element.ALIGN_LEFT
            );

            addBodyCell(
                    table,
                    result.unitName(),
                    Element.ALIGN_LEFT
            );

            addBodyCell(
                    table,
                    result.creditHours() == null
                            ? "-"
                            : String.valueOf(
                            result.creditHours()
                    ),
                    Element.ALIGN_CENTER
            );

            addBodyCell(
                    table,
                    result.grade() == null
                            ? "-"
                            : result.grade(),
                    Element.ALIGN_CENTER
            );

            addBodyCell(
                    table,
                    result.passed()
                            ? "PASSED"
                            : "FAILED",
                    Element.ALIGN_CENTER
            );
        }

        if (orderedUnits.isEmpty()) {
            PdfPCell empty = new PdfPCell(
                    new Phrase(
                            "No unit results available for this academic year.",
                            font(
                                    7,
                                    Font.ITALIC,
                                    MUTED_TEXT
                            )
                    )
            );

            empty.setColspan(5);

            empty.setBorder(
                    Rectangle.NO_BORDER
            );

            empty.setHorizontalAlignment(
                    Element.ALIGN_CENTER
            );

            empty.setPadding(10);

            table.addCell(empty);
        }

        table.setSpacingAfter(3);

        document.add(table);
    }

    private void addYearAverages(
            Document document,
            TranscriptYearGroup year
    ) throws DocumentException {

        Phrase averages = new Phrase();

        averages.add(
                new Chunk(
                        "Current: ",
                        font(
                                7.5f,
                                Font.BOLD,
                                Color.BLACK
                        )
                )
        );

        averages.add(
                new Chunk(
                        formatPercentage(
                                year.currentAverage()
                        ),
                        font(
                                7.5f,
                                Font.NORMAL,
                                Color.BLACK
                        )
                )
        );

        /*
         * The renderer sets cumulativeAverage to null
         * until the second period of the year is available.
         */
        if (year.cumulativeAverage() != null) {
            averages.add(
                    new Chunk(
                            "     Cumulative: ",
                            font(
                                    7.5f,
                                    Font.BOLD,
                                    Color.BLACK
                            )
                    )
            );

            averages.add(
                    new Chunk(
                            formatPercentage(
                                    year.cumulativeAverage()
                            ),
                            font(
                                    7.5f,
                                    Font.NORMAL,
                                    Color.BLACK
                            )
                    )
            );
        }

        PdfPTable table = new PdfPTable(1);

        table.setWidthPercentage(100);
        table.setSpacingBefore(2);
        table.setSpacingAfter(9);

        PdfPCell cell = new PdfPCell(
                averages
        );

        cell.setBorder(
                Rectangle.NO_BORDER
        );

        cell.setHorizontalAlignment(
                Element.ALIGN_LEFT
        );

        cell.setPaddingTop(3);
        cell.setPaddingBottom(3);
        cell.setPaddingLeft(3);
        cell.setPaddingRight(3);

        table.addCell(cell);

        document.add(table);
    }

    private void addInformationLabel(
            PdfPTable table,
            String value
    ) {
        String text =
                value == null || value.isBlank()
                        ? ""
                        : value.toUpperCase() + ":";

        PdfPCell cell = new PdfPCell(
                new Phrase(
                        text,
                        font(
                                7.5f,
                                Font.BOLD,
                                Color.BLACK
                        )
                )
        );

        cell.setBorder(
                Rectangle.NO_BORDER
        );

        cell.setPaddingTop(2);
        cell.setPaddingBottom(3);
        cell.setPaddingLeft(0);
        cell.setPaddingRight(4);

        table.addCell(cell);
    }

    private void addInformationValue(
            PdfPTable table,
            String value
    ) {
        PdfPCell cell = new PdfPCell(
                new Phrase(
                        safe(value),
                        font(
                                7.5f,
                                Font.NORMAL,
                                Color.BLACK
                        )
                )
        );

        cell.setBorder(
                Rectangle.NO_BORDER
        );

        cell.setPaddingTop(2);
        cell.setPaddingBottom(3);
        cell.setPaddingLeft(0);
        cell.setPaddingRight(6);

        table.addCell(cell);
    }

    private void addHeaderCell(
            PdfPTable table,
            String value,
            int alignment
    ) {
        PdfPCell cell = new PdfPCell(
                new Phrase(
                        value,
                        font(
                                7,
                                Font.BOLD,
                                Color.BLACK
                        )
                )
        );

        /*
         * Only one underline below the table header.
         */
        cell.setBorder(
                Rectangle.BOTTOM
        );

        cell.setBorderColor(
                BORDER_COLOR
        );

        cell.setBorderWidthBottom(
                0.7f
        );

        cell.setHorizontalAlignment(
                alignment
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setPaddingTop(5);
        cell.setPaddingBottom(5);
        cell.setPaddingLeft(3);
        cell.setPaddingRight(3);

        table.addCell(cell);
    }

    private void addBodyCell(
            PdfPTable table,
            String value,
            int alignment
    ) {
        PdfPCell cell = new PdfPCell(
                new Phrase(
                        safe(value),
                        font(
                                7.2f,
                                Font.NORMAL,
                                Color.BLACK
                        )
                )
        );

        cell.setBorder(
                Rectangle.NO_BORDER
        );

        cell.setHorizontalAlignment(
                alignment
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        /*
         * Extra vertical space between unit rows.
         */
        cell.setPaddingTop(4.5f);
        cell.setPaddingBottom(4.5f);
        cell.setPaddingLeft(3);
        cell.setPaddingRight(3);

        table.addCell(cell);
    }

    private PdfPCell createSignatureCell() {
        PdfPCell signature =
                plainContainerCell();

        signature.addElement(
                new Paragraph(
                        "Signed: __________________________________________",
                        font(
                                7,
                                Font.NORMAL,
                                Color.BLACK
                        )
                )
        );

        Paragraph role = new Paragraph(
                "Registrar / Academic Officer",
                font(
                        7.5f,
                        Font.BOLD,
                        Color.BLACK
                )
        );

        role.setIndentationLeft(45);
        role.setSpacingBefore(2);

        signature.addElement(role);

        Paragraph date = new Paragraph(
                "Date: __________________________",
                font(
                        7,
                        Font.NORMAL,
                        Color.BLACK
                )
        );

        date.setIndentationLeft(45);
        date.setSpacingBefore(4);

        signature.addElement(date);

        return signature;
    }

    private PdfPCell createStampCell() {
        PdfPCell stampCell =
                plainContainerCell();

        Paragraph printedOn = new Paragraph(
                "Printed on: "
                + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern(
                                "dd/MM/yyyy"
                        )
                ),
                font(
                        7,
                        Font.BOLD,
                        Color.BLACK
                )
        );

        printedOn.setAlignment(
                Element.ALIGN_RIGHT
        );

        stampCell.addElement(
                printedOn
        );

        Paragraph stamp = new Paragraph(
                "\nOfficial Stamp:\n\n________________________",
                font(
                        7,
                        Font.NORMAL,
                        Color.BLACK
                )
        );

        stamp.setAlignment(
                Element.ALIGN_RIGHT
        );

        stampCell.addElement(stamp);

        return stampCell;
    }
}
