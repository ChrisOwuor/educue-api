package com.owuor.educue.graduation.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.owuor.educue.academics.service.PdfFooterPageEvent;
import com.owuor.educue.graduation.entity.GraduationCandidate;
import com.owuor.educue.graduation.enums.AwardClassification;
import com.owuor.educue.institution.repository.InstitutionProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class FinalGraduationBookletPdfService {
    private static final Color NAVY = new Color(25, 52, 77), GOLD = new Color(177, 137, 52), PALE = new Color(245, 247, 249);
    private final InstitutionProfileRepository profiles;

    public byte[] generate(String academicYear, List<GraduationCandidate> candidates) {
        try {
            var out = new ByteArrayOutputStream();
            var document = new Document(PageSize.A4, 48, 48, 46, 52);
            var writer = PdfWriter.getInstance(document, out); writer.setPageEvent(new PdfFooterPageEvent()); document.open();
            var profile = profiles.findById(1L).orElse(null);
            String school = profile == null ? "EduCue Training Institution" : profile.getName();
            String motto = profile == null ? "Knowledge, Integrity and Service" : profile.getMotto();
            cover(document, school, motto, academicYear, candidates.size());
            welcome(document, school);
            programme(document);
            conferment(document);
            graduates(document, candidates);
            messagePage(document, "VOTE OF THANKS", "On behalf of the graduating class, we express our sincere appreciation to the institution's leadership, faculty, staff, parents, guardians, sponsors and friends. Your sacrifice, counsel and encouragement have shaped this achievement. We also thank every guest for sharing this important occasion with us.");
            messagePage(document, "CLOSING REMARKS", "Today marks both a conclusion and a beginning. As this ceremony draws to a close, may every graduate leave with confidence, humility and a lasting commitment to professional excellence and service to society.");
            messagePage(document, "A WORD TO THE GRADUATES", "Your qualification is evidence of discipline and perseverance, but its greatest value will be seen in how you use it. Remain curious, act with integrity, lift others as you advance and let your work improve the communities you serve.");
            farewell(document, school);
            document.close(); return out.toByteArray();
        } catch (Exception exception) { throw new IllegalStateException("Could not generate final graduation booklet", exception); }
    }

    private void cover(Document d,String school,String motto,String year,int count)throws Exception{d.add(new Paragraph("\n"));logo(d,115);center(d,school.toUpperCase(),22,Font.BOLD,NAVY,18);if(motto!=null)center(d,motto,10,Font.ITALIC,Color.DARK_GRAY,8);center(d,"OFFICIAL GRADUATION BOOKLET",19,Font.BOLD,GOLD,55);center(d,"ACADEMIC YEAR "+year,14,Font.BOLD,NAVY,15);center(d,"Celebrating academic achievement, character and service",11,Font.NORMAL,Color.DARK_GRAY,42);center(d,count+" CANDIDATES APPROVED FOR GRADUATION",10,Font.BOLD,GOLD,26);}
    private void welcome(Document d,String school)throws Exception{d.newPage();title(d,"WELCOME TO "+school);ceremonyImage(d,165);section(d,"About the Institution","Founded to advance accessible, relevant and excellent education, "+school+" nurtures competent professionals, ethical leaders and lifelong learners. The institution combines rigorous scholarship, practical training, innovation and community engagement in preparing graduates for a changing world.");section(d,"Message from the Vice-Chancellor","To our graduates: today we celebrate your courage, discipline and growth. Your families and lecturers have walked this journey with you, but the achievement is yours to carry forward. Use your knowledge responsibly, remain teachable and become a source of hope wherever your profession takes you. Congratulations on this defining milestone.");section(d,"Introduction","This booklet presents the order of ceremony and the candidates approved for graduation during the academic year. The names and award classifications shown are drawn from the institution's final approved graduation records.");}
    private void programme(Document d)throws Exception{d.newPage();title(d,"ORDER OF PROCEEDINGS");var table=new PdfPTable(new float[]{1.1f,1.9f});table.setWidthPercentage(100);var left=cell();left.addElement(new Paragraph("CEREMONY NOTES",font(11,Font.BOLD,GOLD)));left.addElement(body("Guests are requested to remain seated during the academic procession, conferment of awards and recession. Mobile devices should be kept silent throughout the ceremony."));var right=cell();right.setBackgroundColor(PALE);String[] order={"Arrival and seating of guests","Academic procession","National and institutional anthems","Opening prayer","Welcome and introductions","Vice-Chancellor's address","Conferment of degrees and award of qualifications","Presentation of graduates","Vote of thanks","Closing remarks","Academic recession"};int i=1;for(String item:order)right.addElement(new Paragraph((i++)+".  "+item,font(10,Font.NORMAL,Color.BLACK)));table.addCell(left);table.addCell(right);d.add(table);}
    private void conferment(Document d)throws Exception{d.newPage();title(d,"CONFERMENT OF DEGREES AND AWARDS");section(d,"Academic Declaration","By the authority vested in the institution and upon the recommendation of the academic organs, the candidates whose names appear in this official booklet are presented for the respective qualifications and classifications for which they have been approved.");section(d,"Presentation","Candidates will be presented by their respective academic departments. Upon conferment, graduates are admitted to the rights, privileges and responsibilities associated with their awards.");section(d,"Charge to Graduates","Let the knowledge and skills represented by these awards be exercised with integrity, compassion, courage and respect for the dignity of every person.");}
    private void graduates(Document d,List<GraduationCandidate> candidates)throws Exception{for(var department:group(candidates).entrySet()){d.newPage();title(d,department.getKey());paragraph(d,"The candidates listed below have satisfied the prescribed requirements and have been approved for graduation in the classifications indicated.");for(var classification:department.getValue().entrySet()){var h=new Paragraph(classification.getKey().toUpperCase(),font(11,Font.BOLD,GOLD));h.setSpacingBefore(13);h.setSpacingAfter(5);d.add(h);for(var candidate:classification.getValue()){var name=new Paragraph("•  "+candidate.getGraduationName(),font(10,Font.BOLD,NAVY));name.setIndentationLeft(12);name.setSpacingAfter(3);d.add(name);var award=new Paragraph(candidate.getAwardTitle(),font(8.5f,Font.NORMAL,Color.DARK_GRAY));award.setIndentationLeft(25);award.setSpacingAfter(7);d.add(award);}}}}
    private Map<String,LinkedHashMap<String,List<GraduationCandidate>>> group(List<GraduationCandidate> list){var result=new TreeMap<String,LinkedHashMap<String,List<GraduationCandidate>>>(String.CASE_INSENSITIVE_ORDER);list.stream().sorted(Comparator.comparing((GraduationCandidate a)->a.getEnrollment().getCourse().getDepartment().getName(),String.CASE_INSENSITIVE_ORDER).thenComparingInt(a->order(a.getAwardClassification())).thenComparing(GraduationCandidate::getGraduationName,String.CASE_INSENSITIVE_ORDER)).forEach(a->{String department=a.getEnrollment().getCourse().getDepartment().getName();String classification=a.getAwardClassification()==null?"Classification Pending":a.getAwardClassification().getDisplayName();result.computeIfAbsent(department,x->new LinkedHashMap<>()).computeIfAbsent(classification,x->new ArrayList<>()).add(a);});return result;}
    private int order(AwardClassification value){if(value==null)return 99;return switch(value){case FIRST_CLASS_HONOURS,DISTINCTION->1;case SECOND_CLASS_HONOURS_UPPER_DIVISION,MERIT->2;case SECOND_CLASS_HONOURS_LOWER_DIVISION,CREDIT->3;case PASS->4;};}
    private void messagePage(Document d,String heading,String text)throws Exception{d.newPage();logo(d,75);title(d,heading);var p=body(text);p.setIndentationLeft(35);p.setIndentationRight(35);p.setAlignment(Element.ALIGN_JUSTIFIED);p.setLeading(20);p.setSpacingBefore(35);d.add(p);}
    private void farewell(Document d,String school)throws Exception{d.newPage();logo(d,80);center(d,"CONGRATULATIONS, GRADUATING CLASS",18,Font.BOLD,NAVY,25);ceremonyImage(d,220);center(d,"May your future be filled with purpose, opportunity and meaningful service.",12,Font.NORMAL,Color.DARK_GRAY,18);center(d,"We wish all graduates, families and guests a safe journey back home.",12,Font.BOLD,GOLD,20);center(d,"With best wishes from "+school,10,Font.ITALIC,NAVY,25);}
    private void logo(Document d,float size)throws Exception{var file=new File("./storage/logo.png");if(file.isFile()){var image=Image.getInstance(file.getAbsolutePath());image.scaleToFit(size,size);image.setAlignment(Element.ALIGN_CENTER);d.add(image);}}
    private void ceremonyImage(Document d,float height)throws Exception{try(var stream=getClass().getResourceAsStream("/graduation-ceremony.jpg")){if(stream!=null){var image=Image.getInstance(stream.readAllBytes());image.scaleToFit(480,height);image.setAlignment(Element.ALIGN_CENTER);image.setSpacingBefore(8);image.setSpacingAfter(10);d.add(image);}}}
    private void title(Document d,String text)throws Exception{center(d,text,15,Font.BOLD,NAVY,12);}
    private void section(Document d,String heading,String text)throws Exception{var h=new Paragraph(heading.toUpperCase(),font(10.5f,Font.BOLD,GOLD));h.setSpacingBefore(15);h.setSpacingAfter(5);d.add(h);paragraph(d,text);}
    private void paragraph(Document d,String text)throws Exception{var p=body(text);p.setAlignment(Element.ALIGN_JUSTIFIED);p.setLeading(16);p.setSpacingAfter(10);d.add(p);}
    private Paragraph body(String text){return new Paragraph(text,font(10,Font.NORMAL,Color.DARK_GRAY));}
    private PdfPCell cell(){var c=new PdfPCell();c.setBorder(Rectangle.NO_BORDER);c.setPadding(18);return c;}
    private void center(Document d,String text,float size,int style,Color color,float before)throws Exception{var p=new Paragraph(text,font(size,style,color));p.setAlignment(Element.ALIGN_CENTER);p.setSpacingBefore(before);p.setLeading(size+5);d.add(p);}
    private Font font(float size,int style,Color color){return new Font(Font.HELVETICA,size,style,color);}
}
