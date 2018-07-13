package modules;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;

import java.util.*;

public class MarkDirect {
    public static String openingQuot = "»";
    public static String closingQuot = "«";
    boolean debug = true;

    public CAS process(CAS mainCas) {
        // CabTokens
        Type cabTokenType = mainCas.getTypeSystem().getType("de.idsma.rw.CabToken");
        Iterator<AnnotationFS> tokiter = CasUtil.iterator(mainCas, cabTokenType);

        AnnotationFS quotStart = null;
        while (tokiter.hasNext()) {
            AnnotationFS tok = tokiter.next();
            if (debug) {
                System.out.println("currTok: " + tok.getCoveredText());
            }

            if (tok.getCoveredText().equals(MarkDirect.openingQuot)) {
                quotStart = tok;
                if (debug) {
                    System.out.println("Opening Quote found: " + tok.getCoveredText() + " at " + tok.getBegin());
                }
            }
            else {
                if (tok.getCoveredText().equals(MarkDirect.closingQuot)) {
                    if (debug) {
                        System.out.println("Closing Quote found: " + tok.getCoveredText() + " at " + tok.getBegin());
                    }
                    if (quotStart != null) {
                        // get the STWWord annotation
                        Type autoStwrType = mainCas.getTypeSystem().getType("de.idsma.rw.rule.RuleDirect");
                        AnnotationFS directAnno = mainCas.createAnnotation(autoStwrType, quotStart.getBegin(), tok.getEnd());
                        mainCas.addFsToIndexes(directAnno);
                        quotStart = null;
                    }
                }
            }
        }
    return mainCas;
    }

}
