package modules;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.util.CasUtil;

import util.Util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.*;
import java.io.*;
import java.net.*;

public class RemoveFIOverlapDirect {
    private Boolean debug;
    private Integer limit;
    private Boolean useAdvIndicator;

    private Set<String> fiWords = new HashSet<String>();
    private Set<String> puncSpec = new HashSet<String>();

    public RemoveFIOverlapDirect() throws IOException, URISyntaxException {
        this.debug = false;
        this.useAdvIndicator = true;
        this.limit = 2;
        this.fiWords = Util.readWordlistFromURL(Util.getFiWordsList());
        this.puncSpec = Util.readWordlistFromURL(Util.getPuncSpec());
    }

    public CAS process(CAS mainCas) {

        String tokenKind = "no_cab";
        // first, remove all annotations of this type that are already in the document
        //mainCas = Util.removeAllAnnotationsOfType(mainCas, "de.idsma.rw.Stwr");

        Type stwrType = mainCas.getTypeSystem().getType("de.idsma.rw.Stwr");
        Type dirType = mainCas.getTypeSystem().getType("de.idsma.rw.rule.RuleDirect");
        Type tokenType = mainCas.getTypeSystem().getType("de.idsma.rw.preprocessing.Token");

        // CabToken annotation
        Type cabTokenType = mainCas.getTypeSystem().getType("de.idsma.rw.CabToken");

        //create a list with all fi annotations
        AnnotationIndex<AnnotationFS> annotationIndex = mainCas.getAnnotationIndex(stwrType);
        FSIterator<AnnotationFS> iterator = annotationIndex.iterator();
        List<AnnotationFS> fiList = new ArrayList<AnnotationFS>();
        while (iterator.hasNext()) {
            fiList.add((AnnotationFS) iterator.next());
        }

        //create a list with all ruleDirect annotations
        AnnotationIndex<AnnotationFS> annotationIndex1 = mainCas.getAnnotationIndex(dirType);
        FSIterator<AnnotationFS> iterator1 = annotationIndex1.iterator();
        List<AnnotationFS> ruleDirectList = new ArrayList<AnnotationFS>();
        while (iterator1.hasNext()) {
            ruleDirectList.add((AnnotationFS) iterator1.next());
        }

        for (AnnotationFS freeIndirectAnno : fiList){
            int fiBegin = freeIndirectAnno.getBegin();
            int fiEnd = freeIndirectAnno.getEnd();
            for (AnnotationFS ruleDirectAnno : ruleDirectList){
                if (ruleDirectAnno.getBegin() >= fiBegin && ruleDirectAnno.getBegin() <= fiEnd){
                    System.out.println(freeIndirectAnno.getCoveredText());
                    mainCas.removeFsFromIndexes(freeIndirectAnno);
                }
            }
        }




        return mainCas;
    }
}





