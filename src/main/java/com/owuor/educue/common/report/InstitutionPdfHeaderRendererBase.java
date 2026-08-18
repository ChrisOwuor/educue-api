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
public class InstitutionPdfHeaderRendererBase {

    private static final String LOGO_PATH =
            "./storage/logo.png";

    private static final float TOP_SLOT_HEIGHT =
            14f;

    private static final float NAME_SLOT_HEIGHT =
            26f;

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
     * Render the Nuria College institutional header.
     *
     * Layout (matches the official letterhead):
     *
     * [ Logo ]   NURIA COLLEGE
     *            P.O. BOX ...
     *            Tel: ...
     *            Email: ... / Website: ...
     *            Motto: ...
     *            Academic Year: ...
     *
     * ────────────────────────────────────────
     *
     * Both columns start at the same top edge so the
     * logo and the text block are vertically aligned.
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
                        ? "NURIA COLLEGE"
                        : safe(institution.getName())
                        .toUpperCase();

        String motto =
                institution == null
                        ? null
                        : institution.getMotto();

        String academicYearCode =
                academicYear == null
                        ? null
                        : academicYear.getCode();

        /*
         * Two-column layout:
         * Left  → Logo (vertically centred against the text block)
         * Right → Institution name + all contact & institutional details
         *
         * This produces the classic Nuria College letterhead style
         * shown in the official header image.
         */
        PdfPTable header =
                new PdfPTable(2);

        header.setWidthPercentage(100);

        header.setWidths(
                new float[]{
                        1.45f,
                        4.55f
                }
        );

        header.setKeepTogether(true);
        header.setSpacingAfter(2f);

        /*
         * Both cells are created with the same top padding
         * and vertical alignment so their top edges line up.
         */
        header.addCell(createLogoCell(documentReference));   // pass the reference

        header.addCell(
                createInfoCell(
                        institutionName,
                        documentReference,
                        institution == null
                                ? null
                                : institution.getAddress(),
                        institution == null
                                ? null
                                : institution.getPhone(),
                        institution == null
                                ? null
                                : institution.getOfficialEmail(),
                        institution == null
                                ? null
                                : institution.getWebsite(),
                        institution == null
                                ? null
                                : institution.getRegistrationNumber(),
                        motto,
                        academicYearCode
                )
        );

        document.add(header);

        /*
         * Single solid line below the institutional header.
         * Matches the thin rule that appears under the
         * official Nuria College letterhead.
         */
        document.add(
                new LineSeparator(
                        1.0f,
                        100,
                        Color.BLACK,
                        Element.ALIGN_CENTER,
                        -2
                )
        );
    }

    /**
     * Left side of the header – the institution logo.
     *
     * The cell uses the same top padding as the right
     * column so both columns begin on the same
     * horizontal line.
     */
    private PdfPCell createLogoCell(String documentReference) throws Exception {
        PdfPCell cell = baseHeaderCell();
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_TOP);

        // Dynamic padding to match the right column's first visible element
        float topPadding;
        if (documentReference != null && !documentReference.isBlank()) {
            // Reference text is now at the top of the slot → align with outer padding (4)
            topPadding = 4f;
        } else {
            // Institution name sits below the empty top slot + name slot spacing
            // Calculation: outer(4) + topSlot(14) + (nameSlot(26) - nameFontSize(17)) = 27
            topPadding = 27f;
        }
        cell.setPaddingTop(topPadding);
        cell.setPaddingBottom(2);
        cell.setPaddingLeft(2);
        cell.setPaddingRight(8);

        File logo = new File(LOGO_PATH);
        if (!logo.isFile()) {
            cell.addElement(new Phrase(""));
            return cell;
        }

        Image image = Image.getInstance(logo.getAbsolutePath());
        image.scaleToFit(100, 100);   // increased size as requested
        image.setAlignment(Image.ALIGN_CENTER);
        cell.addElement(image);
        return cell;
    }
    /**
     * Right side of the header.
     *
     * Contains (in order):
     *   - optional document reference (top right)
     *   - institution name (large, bold – matches the image)
     *   - postal address
     *   - telephone numbers
     *   - email / website
     *   - registration number (if present)
     *   - motto (if present)
     *   - academic year (if present)
     *
     * All of these lines live inside the same column
     * so the header remains compact and matches the
     * official letterhead layout.
     */
    private PdfPCell createInfoCell(
            String institutionName,
            String documentReference,
            String address,
            String phone,
            String email,
            String website,
            String registrationNumber,
            String motto,
            String academicYearCode
    ) {
        PdfPCell outerCell =
                baseHeaderCell();

        outerCell.setVerticalAlignment(
                Element.ALIGN_TOP
        );

        /*
         * Same top padding as the logo cell so both
         * columns share a common top edge.
         */
        outerCell.setPaddingTop(4);
        outerCell.setPaddingBottom(2);
        outerCell.setPaddingLeft(2);
        outerCell.setPaddingRight(4);

        PdfPTable content =
                new PdfPTable(1);

        content.setWidthPercentage(100);

        /*
         * Top slot – used for the document reference
         * so that the institution name always starts
         * at a consistent vertical position.
         */
        content.addCell(
                createTopSlot(
                        documentReference,
                        Element.ALIGN_RIGHT
                )
        );

        /*
         * Institution name – large and bold.
         * Styled to match the official “NURIA COLLEGE”
         * appearance in the letterhead image.
         */
        content.addCell(
                createInstitutionNameCell(
                        institutionName,
                        Element.ALIGN_LEFT
                )
        );

        /*
         * Postal address
         */
        addDetailCell(
                content,
                address,
                Element.ALIGN_LEFT,
                Font.NORMAL,
                8.5f
        );

        /*
         * Telephone
         */
        addDetailCell(
                content,
                formatPhone(phone),
                Element.ALIGN_LEFT,
                Font.NORMAL,
                8.5f
        );

        /*
         * Email and website on one line when both exist
         */
        String contactLine =
                buildContactLine(email, website);

        addDetailCell(
                content,
                contactLine,
                Element.ALIGN_LEFT,
                Font.NORMAL,
                8f
        );

        /*
         * Registration number
         */
        if (registrationNumber != null &&
            !registrationNumber.isBlank()) {

            addDetailCell(
                    content,
                    "Reg. No: " +
                    registrationNumber,
                    Element.ALIGN_LEFT,
                    Font.NORMAL,
                    8f
            );
        }

        /*
         * Motto – placed with the rest of the
         * institutional information (same column).
         */
        if (motto != null &&
            !motto.isBlank()) {

            addDetailCell(
                    content,
                    motto,
                    Element.ALIGN_LEFT,
                    Font.ITALIC,
                    7.5f
            );
        }

        /*
         * Academic year – also in the same column
         * so everything stays together under the name.
         */
        if (academicYearCode != null &&
            !academicYearCode.isBlank()) {

            addDetailCell(
                    content,
                    "Academic Year: " +
                    academicYearCode,
                    Element.ALIGN_LEFT,
                    Font.NORMAL,
                    7.5f
            );
        }

        outerCell.addElement(content);

        return outerCell;
    }

    /**
     * Base cell used by the two main header columns.
     *
     * Both the logo cell and the info cell inherit
     * from this method so they share identical
     * border and default padding behaviour.
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
        cell.setPaddingLeft(4);
        cell.setPaddingRight(4);

        return cell;
    }

    /**
     * Fixed-height row above the institution name.
     *
     * Holds the optional document reference on the
     * right side.  An empty slot is still created so
     * that the name always begins at the same
     * vertical position regardless of whether a
     * reference is supplied.
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
                Element.ALIGN_TOP
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
                                    7.5f,
                                    Font.BOLD,
                                    Color.BLACK
                            )
                    );

            reference.setAlignment(
                    alignment
            );

            reference.setLeading(9);

            cell.addElement(reference);
        }

        return cell;
    }

    /**
     * Fixed-height institution-name row.
     *
     * Keeps the name vertically consistent and
     * produces the large, bold appearance that
     * matches the official Nuria College letterhead.
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
        cell.setPaddingBottom(2);
        cell.setPaddingLeft(0);
        cell.setPaddingRight(0);

        Paragraph name =
                new Paragraph(
                        safe(institutionName),
                        font(
                                17,
                                Font.BOLD,
                                Color.BLACK
                        )
                );

        name.setAlignment(
                alignment
        );

        name.setLeading(18);

        cell.addElement(name);

        return cell;
    }

    /**
     * Add a single detail line (address, phone, email,
     * motto, academic year, etc.).
     *
     * The font size is supplied by the caller so that
     * the name can stay large while supporting lines
     * remain compact, exactly as they appear in the
     * official header image.
     */
    private void addDetailCell(
            PdfPTable table,
            String value,
            int alignment,
            int style,
            float fontSize
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
        cell.setPaddingBottom(1.2f);
        cell.setPaddingLeft(0);
        cell.setPaddingRight(0);

        Paragraph line =
                new Paragraph(
                        value,
                        font(
                                fontSize,
                                style,
                                Color.BLACK
                        )
                );

        line.setAlignment(
                alignment
        );

        line.setLeading(fontSize + 1.5f);

        cell.addElement(line);

        table.addCell(cell);
    }

    /**
     * Format the telephone number with the "Tel:" prefix.
     */
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
     * Build a single contact line that combines
     * email and website when both are present.
     *
     * Examples that match the official letterhead:
     *
     *   Email:info@nuriacollege.ac.ke / Website: nuriacollege.ac.ke
     *   Email: info@nuriacollege.ac.ke
     *   Website: nuriacollege.ac.ke
     */
    private String buildContactLine(
            String email,
            String website
    ) {
        boolean hasEmail =
                email != null && !email.isBlank();

        boolean hasWebsite =
                website != null && !website.isBlank();

        if (hasEmail && hasWebsite) {
            return "Email:" + email +
                   " / Website: " + website;
        }

        if (hasEmail) {
            return "Email: " + email;
        }

        if (hasWebsite) {
            return "Website: " + website;
        }

        return null;
    }
}
