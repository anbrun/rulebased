package modules;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import util.Util;

import java.util.regex.*;
import java.util.*;

public class MarkDirect2 {

    private static String double_bracket_left = "\u00AB";
    private static String double_bracket_right = "\u00BB";
    private static String single_bracket_left = "\u2039";
    private static String single_bracket_right = "\u203A";
    private static String double_top_left = "\u201C";
    private static String double_top_left_v2 = "\u301E";  // 〞
    private static String double_top_left_v2_rev = "\u301E"; // 〝
    private static String double_top_right = "\u201D"; // ”
    private static String double_top_right_rev = "\u201F"; // ‟
    private static String single_top_left = "\u2018";
    private static String single_top_right = "\u2019";
    private static String double_bottom_left = "\u201E";
    private static String double_bottom_left_v2 = "\u301F";  // 〟
    private static String single_bottom_left = "\u201A";
    private static String double_ascii = "\"";
    private static String single_ascii = "'";


    // doppelte spitze Anführungszeichen, nach innen zeigend "»[^«]+«"
    private static Pattern quote_switched_french = Pattern.compile("\u00BB[^\u00AB]+\u00AB", Pattern.MULTILINE);
    // spitze Anführungszeichen, nach außen zeigend  (französischer Stil)
    private static Pattern quote_french = Pattern.compile("\u00AB[^\u00BB]+\u00BB", Pattern.MULTILINE);

    // doppelte einfache spitze Anführungszeichen, nach innen zeigend "›[^‹]+‹"
    private static Pattern quote_switched_french_single = Pattern.compile("\u203A[^\u2039]+\u2039", Pattern.MULTILINE);
    // einfache spitze Anführungszeichen, nach außen zeigend (französicher Stil)
    private static Pattern quote_french_single = Pattern.compile("\u2039[^\u203A]+\u203A", Pattern.MULTILINE);

    // doppelte Anführungszeichen oben (englischer Stil) "“[^”]+”"
    private static Pattern quote_english = Pattern.compile("\u201C[^\u201D]+\u201D", Pattern.MULTILINE);
    // einfache Anführungszeichen oben (englischer Stil) "‘[^’]+’"
    private static Pattern quote_english_single = Pattern.compile("\u2018[^\u2019]+\u2019", Pattern.MULTILINE);

    // doppelte Anführungszeichen unten und oben (deutscher Stil) "„[^”]+”"
    private static Pattern quote_german = Pattern.compile("\u201E[^\u2019]+\u2019", Pattern.MULTILINE);
    // einfache Anführungszeichen unten und oben (deutscher Stil) "‚[^’]+’"
    private static Pattern quote_german_single = Pattern.compile("\u201A[^\u2019]+\u2019", Pattern.MULTILINE);


    // ----------------------- nicht korrekte Muster -----------------------
    // deutsches unteres Anführungszeichen als start, ASCII-Quote-Zeichen als end "„[^\"]+\""
    private static Pattern quote_german_bottom_english_end = Pattern.compile("\u201E[^\"]+\"", Pattern.MULTILINE);

    // ----------------- ASCII quotes -------------
    private static Pattern quote_ascii = Pattern.compile("\"[^\"]+\"", Pattern.MULTILINE);
    private static Pattern quote_ascii_single = Pattern.compile("\'[^\']+\'", Pattern.MULTILINE);

    private Map<String, String[]> quoteInfo;


    public MarkDirect2() {
    }



    private List<Integer> getOccurenceInfo(String quoteChar, String documentText) {
        int index = documentText.indexOf(quoteChar);
        List<Integer> occurances = new ArrayList<>();
        while(index >= 0) {
            occurances.add(index);
            index = documentText.indexOf(quoteChar, index+1);
        }
        System.out.println(quoteChar + ": " + occurances);
        return occurances;
    }

    private void occuranceCheck(String documentText) {
        this.getOccurenceInfo(double_bracket_left, documentText);
        this.getOccurenceInfo(double_bracket_right, documentText);
        this.getOccurenceInfo(single_bracket_left, documentText);
        this.getOccurenceInfo(single_bracket_right, documentText);

        this.getOccurenceInfo(double_top_left, documentText);
        this.getOccurenceInfo(double_top_right, documentText);
        this.getOccurenceInfo(single_top_left, documentText);
        this.getOccurenceInfo(single_top_right, documentText);

        this.getOccurenceInfo(double_bottom_left, documentText);
        this.getOccurenceInfo(single_bottom_left, documentText);

        this.getOccurenceInfo(double_ascii, documentText);
        this.getOccurenceInfo(single_ascii, documentText);
    }


    public CAS process(CAS mainCas) {
        // first, remove all annotations of this type that are already in the document
        mainCas = Util.removeAllAnnotationsOfType(mainCas, "de.idsma.rw.rule.RuleDirect");

        //this.occuranceCheck(mainCas.getDocumentText());

        String text = this.annotateQuotPattern(MarkDirect2.quote_switched_french, mainCas.getDocumentText(), mainCas);
        text = this.annotateQuotPattern(MarkDirect2.quote_switched_french_single, text, mainCas);
        text = this.annotateQuotPattern(MarkDirect2.quote_english, text, mainCas);
        text = this.annotateQuotPattern(MarkDirect2.quote_english_single, text, mainCas);
        text = this.annotateQuotPattern(MarkDirect2.quote_german_bottom_english_end, text, mainCas);
        System.out.println(text);

        return mainCas;
    }



    private String annotateQuotPattern(Pattern pat, String text, CAS mainCas) {
        //String text = mainCas.getDocumentText();
        StringBuilder newText = new StringBuilder(text);
        Matcher m = pat.matcher(text);
        //System.out.println(m.find());
        while (m.find()) {
//            System.out.println(m.group());
//            System.out.println(m.start());
//            System.out.println(m.end());
            Type autoStwrType = mainCas.getTypeSystem().getType("de.idsma.rw.rule.RuleDirect");
            // replace the matching quotations with # so that they don't match again
            newText.setCharAt(m.start(), '#');
            newText.setCharAt(m.end()-1, '#');
            AnnotationFS directAnno = mainCas.createAnnotation(autoStwrType, m.start(), m.end());
            mainCas.addFsToIndexes(directAnno);
        }
        return newText.toString();
    }

}
