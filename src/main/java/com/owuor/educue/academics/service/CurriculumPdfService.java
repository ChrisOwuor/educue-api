package com.owuor.educue.academics.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;

import com.lowagie.text.pdf.draw.LineSeparator;
import com.owuor.educue.academics.entity.CourseCurriculum;
import com.owuor.educue.academics.entity.Semester;
import com.owuor.educue.academics.entity.SemesterUnit;
import com.owuor.educue.academics.repository.CourseCurriculumRepository;
import com.owuor.educue.academics.repository.SemesterRepository;
import com.owuor.educue.academics.repository.SemesterUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.awt.Color.white;

@Service
@Transactional(readOnly = true)
public class CurriculumPdfService {

    private final CourseCurriculumRepository curriculumRepository;
    private final SemesterRepository semesterRepository;
    private final SemesterUnitRepository semesterUnitRepository;

    public CurriculumPdfService(
            CourseCurriculumRepository curriculumRepository,
            SemesterRepository semesterRepository,
            SemesterUnitRepository semesterUnitRepository
    ) {
        this.curriculumRepository = curriculumRepository;
        this.semesterRepository = semesterRepository;
        this.semesterUnitRepository = semesterUnitRepository;
    }

    private void addInfoRow(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont
    ) {

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setPadding(6);
        labelCell.setBackgroundColor(Color.WHITE);
        labelCell.setBorder(Color.DARK_GRAY.getRGB());
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setPadding(6);
        valueCell.setBackgroundColor(white);
        valueCell.setBorder(Color.DARK_GRAY.getRGB());
        table.addCell(valueCell);

    }

    public byte[] generateCurriculumPdf(Long curriculumId) {

        CourseCurriculum curriculum = curriculumRepository.findById(curriculumId)
                .orElseThrow(() -> new RuntimeException("Curriculum not found"));

        List<Semester> semesters =
                semesterRepository.findByCourseCurriculumId(curriculumId);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Document document = new Document(PageSize.A4, 36, 36, 50, 60);

            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PdfFooterPageEvent());

            document.open();
            // ================= HEADER =================
            addHeader(document, curriculum);

            document.add(Chunk.NEWLINE);

            // ================= CURRICULUM TABLE =================
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 5f, 2f, 2.2f, 1.3f});
            table.setSpacingBefore(8);
            table.setSpacingAfter(10);

// Repeat header on every page
            table.setHeaderRows(1);

            addTableHeader(table);

            // ================= DATA =================
            for (Semester semester : semesters) {

                List<SemesterUnit> units =
                        semesterUnitRepository.findBySemesterId(semester.getId());

                for (SemesterUnit su : units) {
                    addRow(table, semester, su);
                }
            }


            document.add(table);

            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    private void addHeader1(Document document, CourseCurriculum curriculum) throws DocumentException {

        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(30, 30, 30));
        Font subFont = new Font(Font.HELVETICA, 12, Font.NORMAL, Color.GRAY);

        Paragraph title = new Paragraph("EDCUE ACADEMIC SYSTEM", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);

        Paragraph subtitle = new Paragraph("Course Curriculum Report", subFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);

        Paragraph curriculumName = new Paragraph(
                curriculum.getName(),
                new Font(Font.HELVETICA, 14, Font.BOLD)
        );
        curriculumName.setAlignment(Element.ALIGN_CENTER);

        document.add(title);
        document.add(subtitle);
        document.add(curriculumName);
    }

    private void addHeader(Document document, CourseCurriculum curriculum) throws Exception {

        Color primaryColor = new Color(0, 70, 140);
        Color lightGray = new Color(245, 245, 245);

        Font schoolNameFont = new Font(Font.HELVETICA, 18, Font.BOLD, Color.BLACK);
        Font mottoFont = new Font(Font.HELVETICA, 10, Font.ITALIC, Color.DARK_GRAY);
        Font infoFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        Font reportTitleFont = new Font(Font.HELVETICA, 14, Font.BOLD, Color.BLACK);

        //=========================================================
        // HEADER TABLE
        //=========================================================

        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1.3f, 5f});
        headerTable.setSpacingAfter(10);

        //---------------------------------------------------------
        // LOGO
        //---------------------------------------------------------

        PdfPCell logoCell;

        File logoFile = new File("./storage/img2.png");

        if (logoFile.exists()) {

            Image logo = Image.getInstance(logoFile.getAbsolutePath());
            logo.scaleToFit(75, 75);

            logoCell = new PdfPCell(logo);
        } else {

            logoCell = new PdfPCell(new Phrase(""));
        }

        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setPaddingBottom(8);

        headerTable.addCell(logoCell);

        //---------------------------------------------------------
        // SCHOOL DETAILS
        //---------------------------------------------------------

        PdfPCell detailsCell = new PdfPCell();
        detailsCell.setBorder(Rectangle.NO_BORDER);

        Paragraph schoolName =
                new Paragraph("Joan School Of Nursing", schoolNameFont);
        schoolName.setAlignment(Element.ALIGN_CENTER);

        Paragraph motto =
                new Paragraph("\"Training for Better Health\"", mottoFont);
        motto.setAlignment(Element.ALIGN_CENTER);

        Paragraph address =
                new Paragraph(
                        "P.O. Box 269-40105, Kisumu, Kenya",
                        infoFont);
        address.setAlignment(Element.ALIGN_CENTER);

        Paragraph contacts =
                new Paragraph(
                        "Tel: +254 20 2725711   |   Email: info@json.ac.ke",
                        infoFont);
        contacts.setAlignment(Element.ALIGN_CENTER);

        Paragraph website =
                new Paragraph(
                        "www.json.ac.ke",
                        infoFont);
        website.setAlignment(Element.ALIGN_CENTER);

        detailsCell.addElement(schoolName);
        detailsCell.addElement(motto);
        detailsCell.addElement(address);
        detailsCell.addElement(contacts);
        detailsCell.addElement(website);

        headerTable.addCell(detailsCell);

        document.add(headerTable);

        //=========================================================
        // DIVIDER
        //=========================================================

        LineSeparator separator = new LineSeparator();
        separator.setLineColor(Color.BLACK);
        separator.setLineWidth(1f);

        document.add(separator);

        document.add(Chunk.NEWLINE);



        //=========================================================
        // CURRICULUM INFORMATION
        //=========================================================

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1.5f, 4f});
        infoTable.setSpacingAfter(20);


        Font labelFont = new Font(Font.HELVETICA, 9);
        Font valueFont = new Font(Font.HELVETICA, 9);

        addInfoRow(infoTable, "Curriculum", curriculum.getName(), labelFont, valueFont);
        addInfoRow(infoTable, "Course", curriculum.getCourse().getName(), labelFont, valueFont);
        addInfoRow(infoTable, "Department", "School of Health Sciences", labelFont, valueFont);
        addInfoRow(infoTable, "Duration", "3 Years", labelFont, valueFont);
        addInfoRow(infoTable, "Academic Year", "2026", labelFont, valueFont);
        addInfoRow(
                infoTable,
                "Generated On",
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                labelFont,
                valueFont
        );

        document.add(infoTable);
    }

    private void addTableHeader(PdfPTable table) {

        Color headerColor = new Color(Color.WHITE.getRGB());

        Font headerFont =
                new Font(Font.HELVETICA, 10, Font.BOLD, Color.DARK_GRAY);

        String[] headers = {
                "Year / Sem",
                "Unit Name",
                "Unit Code",
                "Category",
                "Credits"
        };

        for (String header : headers) {

            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));

            cell.setBackgroundColor(headerColor);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            cell.setPaddingTop(8);
            cell.setPaddingBottom(8);

            cell.setBorderColor(new Color(220, 220, 220));

            table.addCell(cell);
        }
    }

    private void addCell(PdfPTable table,
                         String value,
                         Font font,
                         int alignment,
                         Color background) {

        PdfPCell cell = new PdfPCell(new Phrase(value, font));

        cell.setBackgroundColor(background);

        cell.setPaddingTop(7);
        cell.setPaddingBottom(7);
        cell.setPaddingLeft(6);
        cell.setPaddingRight(6);

        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        cell.setBorderColor(new Color(220, 220, 220));

        table.addCell(cell);
    }

    private boolean alternateRow = false;

    private void addRow(PdfPTable table,
                        Semester semester,
                        SemesterUnit su) {

        Font font = new Font(Font.HELVETICA, 9);

        Color rowColor =
                alternateRow
                        ? new Color(248, 248, 248)
                        : Color.WHITE;

        alternateRow = !alternateRow;

        addCell(
                table,
                "Y" + semester.getYearNumber()
                        + " S" + semester.getSemesterNumber(),
                font,
                Element.ALIGN_CENTER,
                rowColor
        );

        addCell(
                table,
                su.getUnit().getName(),
                font,
                Element.ALIGN_LEFT,
                rowColor
        );

        addCell(
                table,
                su.getUnit().getCode(),
                font,
                Element.ALIGN_CENTER,
                rowColor
        );

        addCell(
                table,
                formatCategory(su.getCategory().name()),
                font,
                Element.ALIGN_CENTER,
                rowColor
        );

        addCell(
                table,
                "3",
                font,
                Element.ALIGN_CENTER,
                rowColor
        );
    }

    private String formatCategory(String value) {

        String formatted = value.toLowerCase().replace("_", " ");

        StringBuilder builder = new StringBuilder();

        for (String word : formatted.split(" ")) {

            builder.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
        }

        return builder.toString().trim();
    }

}
