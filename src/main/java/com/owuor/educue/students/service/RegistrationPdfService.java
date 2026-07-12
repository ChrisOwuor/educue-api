package com.owuor.educue.students.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.owuor.educue.academics.entity.Semester;
import com.owuor.educue.academics.entity.SemesterUnit;
import com.owuor.educue.academics.service.PdfFooterPageEvent;
import com.owuor.educue.students.dto.StudentUnitRegistrationFilterRequest;
import com.owuor.educue.students.entity.*;
import com.owuor.educue.students.repository.StudentUnitRegistrationRepository;
import com.owuor.educue.students.repository.StudentUnitRegistrationSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.awt.Color.white;

@Service
@RequiredArgsConstructor
public class RegistrationPdfService {

    private final StudentUnitRegistrationRepository registrationRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public byte[] generateRegistrationsPdf(StudentUnitRegistrationFilterRequest filter) {
        // No Pageable here on purpose - an export should contain every
        // row matching the filter, not just whatever page the user
        // happened to be looking at. Reuses the SAME Specification as
        // the paginated endpoint, so "what you see filtered" and
        // "what you download" can never silently drift apart.
        List<StudentUnitRegistration> registrations = registrationRepository.findAll(
                StudentUnitRegistrationSpecification.withFilters(
                        filter.getSearch(),
                        filter.getCourseId(),
                        filter.getCurriculumId(),
                        filter.getSemesterId(),
                        filter.getSemesterUnitId(),
                        filter.getAttemptType(),
                        filter.getStatus()
                ),
                Sort.by(Sort.Direction.DESC, "registeredAt")
        );

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 50, 60);

            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PdfFooterPageEvent());

            document.open();
            addHeader(document, filter, registrations.size());
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{ 2f, 4.5f, 3f});
            table.setSpacingBefore(8);
            table.setSpacingAfter(10);

// Repeat the header on every page
            table.setHeaderRows(1);

            addTableHeader(table);
            for (StudentUnitRegistration reg : registrations) {
                addRow(table, reg);
            }

            document.add(table);
            document.close();

            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating registrations PDF", e);
        }
    }

    private void addHeader(
            Document document,
            StudentUnitRegistrationFilterRequest filter,
            int rowCount
    ) throws Exception {

        Color primaryColor = new Color(0, 70, 140);

        Font schoolNameFont = new Font(Font.HELVETICA, 18, Font.BOLD, Color.BLACK);
        Font mottoFont = new Font(Font.HELVETICA, 10, Font.ITALIC, Color.DARK_GRAY);
        Font infoFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

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
        separator.setLineColor(Color.DARK_GRAY);
        separator.setLineWidth(1f);

        document.add(separator);

        //=========================================================
        // REPORT TITLE
        //=========================================================

        Font reportTitleFont =
                new Font(Font.HELVETICA, 10, Font.BOLD, Color.DARK_GRAY);

        Paragraph reportTitle =
                new Paragraph("UNIT REGISTRATIONS REPORT", reportTitleFont);

        reportTitle.setAlignment(Element.ALIGN_CENTER);
        reportTitle.setSpacingBefore(5);

        document.add(reportTitle);


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

    private void addTableHeader(PdfPTable table) {

        Color headerColor = new Color(0, 70, 140);

        Font headerFont =
                new Font(Font.HELVETICA, 10, Font.BOLD, Color.darkGray);

        String[] headers = {

                "Admission No.",
                "Unit",
                "Course",
        };

        for (String header : headers) {

            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));

            cell.setBackgroundColor(Color.WHITE);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            cell.setPaddingTop(8);
            cell.setPaddingBottom(8);

            cell.setPaddingLeft(6);
            cell.setPaddingRight(6);

            cell.setBorderColor(new Color(220, 220, 220));

            table.addCell(cell);
        }
    }

    private boolean alternateRow = false;

    private void addRow(PdfPTable table, StudentUnitRegistration registration) {

        Font font = new Font(Font.HELVETICA, 9);

        Enrollment enrollment = registration.getEnrollment();
        SemesterUnit semesterUnit = registration.getSemesterUnit();
        Semester semester = semesterUnit.getSemester();
        var curriculum = enrollment.getCourseCurriculum();

        Color rowColor =
                alternateRow
                        ? new Color(248, 248, 248)
                        : Color.WHITE;

        alternateRow = !alternateRow;



        addCell(
                table,
                enrollment.getStudent().getAdmissionNumber(),
                font,
                Element.ALIGN_LEFT,
                rowColor
        );

        addCell(
                table,
                semesterUnit.getUnit().getCode() ,
                font,
                Element.ALIGN_LEFT,
                rowColor
        );

        addCell(
                table,
                curriculum.getCourse().getName(),
                font,
                Element.ALIGN_LEFT,
                rowColor
        );


    }
    private void addCell(
            PdfPTable table,
            String value,
            Font font,
            int alignment,
            Color background
    ) {

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

    private String formatAttempt(String value) {

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
