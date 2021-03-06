package modules;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;

import java.util.*;

public class MarkDirect {
    public static String[] openingQuot = {"»", "“", "‘", "›", "„"};
    public static String[] closingQuot = {"«", "”", "’", "‹", "\""};
    boolean debug = true;
    List<String> open;
    List<String> close;


    public MarkDirect() {
        open = Arrays.asList(MarkDirect.openingQuot);
        close = Arrays.asList(MarkDirect.closingQuot);
    }

    public CAS process(CAS mainCas) {
        String tokenKind = "no_cab";

        // CabTokens
        Type cabTokenType = mainCas.getTypeSystem().getType("de.idsma.rw.CabToken");
        Type tokenType = mainCas.getTypeSystem().getType("de.idsma.rw.preprocessing.Token");

        if (tokenKind.equals("cab")) {
            Iterator<AnnotationFS> tokiter = CasUtil.iterator(mainCas, cabTokenType);
            AnnotationFS quotStart = null;
            while (tokiter.hasNext()) {
                AnnotationFS tok = tokiter.next();
                if (debug) {
                    //System.out.println("currTok: " + tok.getCoveredText());
                }

                if (this.open.contains(tok.getCoveredText())) {
                    quotStart = tok;
                    if (debug) {
                        System.out.println("Opening Quote found: " + tok.getCoveredText() + " at " + tok.getBegin());
                    }
                } else {
                    if (this.close.contains(tok.getCoveredText())) {
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
        } else if (tokenKind.equals("no_cab")){
            Iterator<AnnotationFS> tokiter = CasUtil.iterator(mainCas, tokenType);
            AnnotationFS quotStart = null;
            while (tokiter.hasNext()) {
                AnnotationFS tok = tokiter.next();
                if (debug) {
                    //System.out.println("currTok: " + tok.getCoveredText());
                }

                if (this.open.contains(tok.getCoveredText())) {
                    quotStart = tok;
                    if (debug) {
                        System.out.println("Opening Quote found: " + tok.getCoveredText() + " at " + tok.getBegin());
                    }
                } else {
                    if (this.close.contains(tok.getCoveredText())) {
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
        }
    return mainCas;
    }
}
