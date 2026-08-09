package com.owuor.educue.common.report;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.owuor.educue.institution.repository.AcademicYearRepository;
import com.owuor.educue.institution.repository.InstitutionProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.File;

import static com.owuor.educue.common.report.PdfReportHelper.font;
import static com.owuor.educue.common.report.PdfReportHelper.safe;

@Component
@RequiredArgsConstructor
public class InstitutionPdfHeaderRenderer {

    private static final String LOGO_PATH =
            "./storage/logo.png";

    private static final float TOP_SLOT_HEIGHT =
            22f;

    private static final float NAME_SLOT_HEIGHT =
            31f;

    private final InstitutionProfileRepository institutionRepository;
    private final AcademicYearRepository academicYearRepository;

    /**
     * Render the header without a document reference.
     */
    public void render(
            Document document
    ) throws Exception {
        render(document, null);
    }

    /**
     * Render the institutional header.
     *
     * Layout:
     *
     * Left information | Centred logo | Right information
     */
    public void render(
            Document document,
            String documentReference
    ) throws Exception {
        var institution = institutionRepository
                .findById(1L)
                .orElse(null);

        var academicYear = academicYearRepository
                .findByCurrentTrue()
                .orElse(null);

        String institutionName =
                institution == null
                        ? "EDUCUE TRAINING INSTITUTION"
                        : safe(institution.getName())
                        .toUpperCase();

        InstitutionNameParts nameParts =
                splitInstitutionName(
                        institutionName
                );

        /*
         * Left and right columns have identical widths.
         * This keeps the logo in the physical centre
         * of the page.
         */
        PdfPTable header =
                new PdfPTable(3);

        header.setWidthPercentage(100);

        header.setWidths(
                new float[]{
                        3.1f,
                        1.8f,
                        3.1f
                }
        );

        header.setKeepTogether(true);

        header.addCell(
                createLeftCell(
                        nameParts.left(),
                        institution == null
                                ? null
                                : institution.getWebsite(),
                        institution == null
                                ? null
                                : institution.getOfficialEmail()
                )
        );

        header.addCell(
                createLogoCell()
        );

        header.addCell(
                createRightCell(
                        nameParts.right(),
                        documentReference,
                        institution == null
                                ? null
                                : institution.getAddress(),
                        institution == null
                                ? null
                                : institution.getPhone(),
                        institution == null
                                ? null
                                : institution.getRegistrationNumber()
                )
        );

        document.add(header);

        /*
         * The motto and academic year appear below
         * the three-column header.
         */
        if (institution != null &&
            institution.getMotto() != null &&
            !institution.getMotto().isBlank()) {

            Paragraph motto =
                    new Paragraph(
                            institution.getMotto(),
                            font(
                                    7.5f,
                                    Font.ITALIC,
                                    Color.DARK_GRAY
                            )
                    );

            motto.setAlignment(
                    Element.ALIGN_CENTER
            );

            motto.setSpacingBefore(1);
            motto.setSpacingAfter(1);

            document.add(motto);
        }

        if (academicYear != null) {
            Paragraph academicYearLine =
                    new Paragraph(
                            "Academic Year: " +
                            academicYear.getCode(),
                            font(
                                    7,
                                    Font.NORMAL,
                                    Color.DARK_GRAY
                            )
                    );

            academicYearLine.setAlignment(
                    Element.ALIGN_CENTER
            );

            academicYearLine.setSpacingAfter(5);

            document.add(
                    academicYearLine
            );
        }

        /*
         * Single line below the institutional header.
         */
        document.add(
                new LineSeparator(
                        0.8f,
                        100,
                        Color.BLACK,
                        Element.ALIGN_CENTER,
                        0
                )
        );
    }

    /**
     * Left side of the header.
     *
     * Example:
     *
     * EGERTON
     * www.egerton.ac.ke
     * raca@egerton.ac.ke
     */
    private PdfPCell createLeftCell(
            String institutionName,
            String website,
            String email
    ) {
        PdfPCell outerCell =
                baseHeaderCell();

        PdfPTable content =
                new PdfPTable(1);

        content.setWidthPercentage(100);

        /*
         * Blank slot matching the document-reference
         * slot on the right side.
         */
        content.addCell(
                createTopSlot(
                        null,
                        Element.ALIGN_RIGHT
                )
        );

        content.addCell(
                createInstitutionNameCell(
                        institutionName,
                        Element.ALIGN_RIGHT
                )
        );

        addDetailCell(
                content,
                website,
                Element.ALIGN_RIGHT,
                Font.UNDERLINE
        );

        addDetailCell(
                content,
                email,
                Element.ALIGN_RIGHT,
                Font.NORMAL
        );

        outerCell.addElement(content);

        return outerCell;
    }

    /**
     * Right side of the header.
     *
     * Example:
     *
     *            EU/AA/FM/30C
     * UNIVERSITY
     * P.O. Box ...
     * Tel: ...
     */
    private PdfPCell createRightCell(
            String institutionName,
            String documentReference,
            String address,
            String phone,
            String registrationNumber
    ) {
        PdfPCell outerCell =
                baseHeaderCell();

        PdfPTable content =
                new PdfPTable(1);

        content.setWidthPercentage(100);

        content.addCell(
                createTopSlot(
                        documentReference,
                        Element.ALIGN_RIGHT
                )
        );

        content.addCell(
                createInstitutionNameCell(
                        institutionName,
                        Element.ALIGN_LEFT
                )
        );

        addDetailCell(
                content,
                address,
                Element.ALIGN_LEFT,
                Font.NORMAL
        );

        addDetailCell(
                content,
                formatPhone(phone),
                Element.ALIGN_LEFT,
                Font.NORMAL
        );

        if (registrationNumber != null &&
            !registrationNumber.isBlank()) {

            addDetailCell(
                    content,
                    "Reg. No: " +
                    registrationNumber,
                    Element.ALIGN_LEFT,
                    Font.NORMAL
            );
        }

        outerCell.addElement(content);

        return outerCell;
    }

    /**
     * Centred institution logo.
     */
    private PdfPCell createLogoCell()
            throws Exception {
        PdfPCell cell =
                baseHeaderCell();

        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setPaddingTop(0);
        cell.setPaddingBottom(0);
        cell.setPaddingLeft(2);
        cell.setPaddingRight(2);

        File logo =
                new File(LOGO_PATH);

        if (!logo.isFile()) {
            cell.addElement(
                    new Phrase("")
            );

            return cell;
        }

        Image image =
                Image.getInstance(
                        logo.getAbsolutePath()
                );

        image.scaleToFit(
                105,
                105
        );

        image.setAlignment(
                Image.ALIGN_CENTER
        );

        cell.addElement(image);

        return cell;
    }

    /**
     * Base cell used by the three main header columns.
     */
    private PdfPCell baseHeaderCell() {
        PdfPCell cell =
                new PdfPCell();

        cell.setBorder(
                Rectangle.NO_BORDER
        );

        cell.setVerticalAlignment(
                Element.ALIGN_TOP
        );

        cell.setPaddingTop(0);
        cell.setPaddingBottom(2);
        cell.setPaddingLeft(7);
        cell.setPaddingRight(7);

        return cell;
    }

    /**
     * Fixed-height row above the institution names.
     *
     * The left side receives an empty slot while the
     * right side receives the document reference.
     *
     * This guarantees that the left and right names
     * begin on the same horizontal line.
     */
    private PdfPCell createTopSlot(
            String documentReference,
            int alignment
    ) {
        PdfPCell cell =
                new PdfPCell();

        cell.setBorder(
                Rectangle.NO_BORDER
        );

        cell.setFixedHeight(
                TOP_SLOT_HEIGHT
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setHorizontalAlignment(
                alignment
        );

        cell.setPaddingTop(0);
        cell.setPaddingBottom(0);
        cell.setPaddingLeft(0);
        cell.setPaddingRight(0);

        if (documentReference != null &&
            !documentReference.isBlank()) {

            Paragraph reference =
                    new Paragraph(
                            documentReference,
                            font(
                                    7,
                                    Font.BOLD,
                                    Color.BLACK
                            )
                    );

            reference.setAlignment(
                    alignment
            );

            reference.setLeading(8);

            cell.addElement(reference);
        }

        return cell;
    }

    /**
     * Fixed-height institution-name row.
     *
     * Both the left and right names use this same row
     * height, so they remain vertically aligned.
     */
    private PdfPCell createInstitutionNameCell(
            String institutionName,
            int alignment
    ) {
        PdfPCell cell =
                new PdfPCell();

        cell.setBorder(
                Rectangle.NO_BORDER
        );

        cell.setMinimumHeight(
                NAME_SLOT_HEIGHT
        );

        cell.setVerticalAlignment(
                Element.ALIGN_BOTTOM
        );

        cell.setHorizontalAlignment(
                alignment
        );

        cell.setPaddingTop(0);
        cell.setPaddingBottom(5);
        cell.setPaddingLeft(0);
        cell.setPaddingRight(0);

        Paragraph name =
                new Paragraph(
                        safe(institutionName),
                        font(
                                14,
                                Font.BOLD,
                                Color.BLACK
                        )
                );

        name.setAlignment(
                alignment
        );

        name.setLeading(15);

        cell.addElement(name);

        return cell;
    }

    /**
     * Add a website, email, postal address, phone or
     * registration-number row.
     */
    private void addDetailCell(
            PdfPTable table,
            String value,
            int alignment,
            int style
    ) {
        if (value == null ||
            value.isBlank()) {
            return;
        }

        PdfPCell cell =
                new PdfPCell();

        cell.setBorder(
                Rectangle.NO_BORDER
        );

        cell.setHorizontalAlignment(
                alignment
        );

        cell.setVerticalAlignment(
                Element.ALIGN_TOP
        );

        cell.setPaddingTop(0);
        cell.setPaddingBottom(2);
        cell.setPaddingLeft(0);
        cell.setPaddingRight(0);

        Paragraph line =
                new Paragraph(
                        value,
                        font(
                                7,
                                style,
                                Color.BLACK
                        )
                );

        line.setAlignment(
                alignment
        );

        line.setLeading(9);

        cell.addElement(line);

        table.addCell(cell);
    }

    private String formatPhone(
            String phone
    ) {
        if (phone == null ||
            phone.isBlank()) {
            return null;
        }

        return "Tel: " + phone;
    }

    /**
     * Split the institution name around the centred logo.
     *
     * Examples:
     *
     * EGERTON UNIVERSITY
     * left  = EGERTON
     * right = UNIVERSITY
     *
     * EDUCUE TRAINING INSTITUTION
     * left  = EDUCUE
     * right = TRAINING INSTITUTION
     */
    private InstitutionNameParts splitInstitutionName(
            String institutionName
    ) {
        String normalized =
                institutionName == null
                        ? ""
                        : institutionName
                        .trim()
                        .replaceAll(
                                "\\s+",
                                " "
                        );

        if (normalized.isBlank()) {
            return new InstitutionNameParts(
                    "",
                    ""
            );
        }

        String[] words =
                normalized.split(" ");

        if (words.length == 1) {
            return new InstitutionNameParts(
                    words[0],
                    ""
            );
        }

        int splitIndex =
                Math.max(
                        1,
                        words.length / 2
                );

        StringBuilder left =
                new StringBuilder();

        StringBuilder right =
                new StringBuilder();

        for (int index = 0;
             index < words.length;
             index++) {

            StringBuilder target =
                    index < splitIndex
                            ? left
                            : right;

            if (target.length() > 0) {
                target.append(" ");
            }

            target.append(
                    words[index]
            );
        }

        return new InstitutionNameParts(
                left.toString(),
                right.toString()
        );
    }

    private record InstitutionNameParts(
            String left,
            String right
    ) {
    }
}
