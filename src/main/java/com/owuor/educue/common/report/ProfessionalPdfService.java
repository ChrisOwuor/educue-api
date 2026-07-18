package com.owuor.educue.common.report;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.owuor.educue.academics.service.PdfFooterPageEvent;
import com.owuor.educue.institution.repository.AcademicYearRepository;
import com.owuor.educue.institution.repository.InstitutionProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;

/** Shared monochrome A4 renderer for official institutional reports. */
@Service
@RequiredArgsConstructor
public class ProfessionalPdfService {
    private final InstitutionProfileRepository institutionRepository;
    private final AcademicYearRepository academicYearRepository;

    public byte[] tableReport(String title, LinkedHashMap<String, String> details,
                              List<String> headers, List<List<String>> rows) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 30, 30, 32, 48);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PdfFooterPageEvent());
            document.open();
            addInstitutionHeader(document);

            Paragraph heading = new Paragraph(title.toUpperCase(), new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK));
            heading.setAlignment(Element.ALIGN_CENTER);
            heading.setSpacingBefore(8);
            heading.setSpacingAfter(10);
            document.add(heading);

            if (details != null && !details.isEmpty()) {
                PdfPTable info = new PdfPTable(2);
                info.setWidthPercentage(100);
                info.setWidths(new float[]{1.5f, 4.5f});
                for (var entry : details.entrySet()) {
                    cell(info, entry.getKey(), true, new Color(235, 235, 235), Element.ALIGN_LEFT, 7);
                    cell(info, entry.getValue(), false, Color.WHITE, Element.ALIGN_LEFT, 7);
                }
                info.setSpacingAfter(10);
                document.add(info);
            }

            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            for (String header : headers) cell(table, header, true, new Color(215, 215, 215), Element.ALIGN_LEFT, 7);
            int index = 0;
            for (List<String> row : rows) {
                Color background = index++ % 2 == 0 ? Color.WHITE : new Color(247, 247, 247);
                for (String value : row) cell(table, value, false, background, Element.ALIGN_LEFT, 7);
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException("Could not generate PDF report", exception);
        }
    }

    private void addInstitutionHeader(Document document) throws Exception {
        var institution = institutionRepository.findById(1L).orElse(null);
        var academicYear = academicYearRepository.findByCurrentTrue().orElse(null);

        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1.2f, 5f});

        File logo = new File("./storage/logo.png");
        PdfPCell logoCell;

        if (logo.isFile()) {
            Image image = Image.getInstance(logo.getAbsolutePath());
            image.scaleToFit(68, 68);
            logoCell = new PdfPCell(image);
        } else {
            logoCell = new PdfPCell(new Phrase(""));
        }

        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.addCell(logoCell);

        PdfPCell details = new PdfPCell();
        details.setBorder(Rectangle.NO_BORDER);

        String institutionName = institution == null
                ? "EduCue Training Institution"
                : institution.getName();

        Paragraph school = new Paragraph(
                institutionName,
                new Font(Font.HELVETICA, 16, Font.BOLD, Color.BLACK)
        );
        school.setAlignment(Element.ALIGN_CENTER);
        details.addElement(school);

        if (institution != null) {
            addCentered(details, institution.getMotto(), true);
            addCentered(details, institution.getAddress(), false);

            addCentered(
                    details,
                    join(
                            "  |  ",
                            institution.getPhone(),
                            institution.getOfficialEmail()
                    ),
                    false
            );

            addCentered(
                    details,
                    join(
                            "  |  ",
                            institution.getWebsite(),
                            institution.getRegistrationNumber()
                    ),
                    false
            );
        }

        // Display the current academic year
        if (academicYear != null) {
            addCentered(
                    details,
                    "Academic Year: " + academicYear.getCode(),
                    false
            );
        }

        header.addCell(details);
        header.setSpacingAfter(5);

        document.add(header);

        document.add(
                new com.lowagie.text.pdf.draw.LineSeparator(
                        0.8f,
                        100,
                        Color.GRAY,
                        Element.ALIGN_CENTER,
                        0
                )
        );
    }
    private void addCentered(PdfPCell cell, String value, boolean italic) {
        if (value == null || value.isBlank()) return;
        Paragraph line = new Paragraph(value, new Font(Font.HELVETICA, 8, italic ? Font.ITALIC : Font.NORMAL, Color.DARK_GRAY));
        line.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(line);
    }

    private String join(String separator, String... values) {
        return java.util.Arrays.stream(values).filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.joining(separator));
    }

    private void cell(PdfPTable table, String value, boolean bold, Color background, int alignment, int padding) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value,
                new Font(Font.HELVETICA, 8, bold ? Font.BOLD : Font.NORMAL, Color.BLACK)));
        cell.setBackgroundColor(background);
        cell.setBorderColor(new Color(185, 185, 185));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(padding);
        table.addCell(cell);
    }
}
