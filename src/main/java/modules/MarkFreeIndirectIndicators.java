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

public class MarkFreeIndirectIndicators {
    private Boolean debug;
    private Integer limit;
    private Boolean useAdvIndicator;

    private Set<String> fiWords = new HashSet<String>();
    private Set<String> puncSpec = new HashSet<String>();

    public MarkFreeIndirectIndicators() throws IOException, URISyntaxException {
        this.debug = false;
        this.useAdvIndicator = true;
        this.limit = 2;
        this.fiWords = Util.readWordlistFromURL(Util.getFiWordsList());
        this.puncSpec = Util.readWordlistFromURL(Util.getPuncSpec());
    }

    public CAS process(CAS mainCas) {
        // first, remove all annotations of this type that are already in the document
        //mainCas = Util.removeAllAnnotationsOfType(mainCas, "de.idsma.rw.Stwr");

        Type stwrType = mainCas.getTypeSystem().getType("de.idsma.rw.Stwr");
        Type sentenceType = mainCas.getTypeSystem().getType("de.idsma.rw.Sentence");
        Type stwwType = mainCas.getTypeSystem().getType("de.idsma.rw.rule.StwWord");
        Type dirType = mainCas.getTypeSystem().getType("de.idsma.rw.rule.RuleDirect");
        Type frameType = mainCas.getTypeSystem().getType("de.idsma.rw.rule.RuleFrame");

        // CabToken annotation
        Type cabTokenType = mainCas.getTypeSystem().getType("de.idsma.rw.CabToken");
        Feature cabPOS = cabTokenType.getFeatureByBaseName("Pos");
        Feature cabRfPos = cabTokenType.getFeatureByBaseName("RfPos");
        Feature cabLemma = cabTokenType.getFeatureByBaseName("Lemma");

        //stwr Features
        Feature mediumFeat = stwrType.getFeatureByBaseName("Medium");
        Feature rTypeFeat = stwrType.getFeatureByBaseName("RType");
        Feature levelFeat = stwrType.getFeatureByBaseName("Level");
        Feature nonFactFeat = stwrType.getFeatureByBaseName("NonFact");
        Feature pragFeat = stwrType.getFeatureByBaseName("Prag");
        Feature borderFeat = stwrType.getFeatureByBaseName("Border");
        Feature metaphFeat= stwrType.getFeatureByBaseName("Metaph");
        Feature stwrFeat = stwrType.getFeatureByBaseName("Stwr");
        Feature stwrIDFeat = stwrType.getFeatureByBaseName("StwrID");
        Feature stwrNote = stwrType.getFeatureByBaseName("StwrNote");

        //create a list with all sentence annotations
        AnnotationIndex<AnnotationFS> annotationIndex = mainCas.getAnnotationIndex(sentenceType);
        FSIterator<AnnotationFS> iterator = annotationIndex.iterator();
        List<AnnotationFS> sentenceList = new ArrayList<AnnotationFS>();
        while (iterator.hasNext()) {
            sentenceList.add((AnnotationFS) iterator.next());
        }

        ArrayList<String> deiktikaList = new ArrayList<String>();

        deiktikaList.add("Damals");
        deiktikaList.add("damals");
        deiktikaList.add("Gestern");
        deiktikaList.add("gestern");
        deiktikaList.add("Heute");
        deiktikaList.add("heute");
        deiktikaList.add("Jetzt");
        deiktikaList.add("jetzt");
        deiktikaList.add("Morgen");
        deiktikaList.add("morgen");
        deiktikaList.add("Nun");
        deiktikaList.add("nun");

        deiktikaList.add("Dort");
        deiktikaList.add("dort");
        deiktikaList.add("Hier");
        deiktikaList.add("hier");
        deiktikaList.add("Hierauf");
        deiktikaList.add("hierauf");

        /*
        deiktikaList.add("Soeben");
        deiktikaList.add("soeben");
        deiktikaList.add("Einst");
        deiktikaList.add("einst");
        deiktikaList.add("Neulich");
        deiktikaList.add("neulich");
        deiktikaList.add("Vorhin");
        deiktikaList.add("vorhin");
        deiktikaList.add("Sofort");
        deiktikaList.add("sofort");
        deiktikaList.add("Gleich");
        deiktikaList.add("gleich");
        deiktikaList.add("Nachher");
        deiktikaList.add("nachher");
        deiktikaList.add("Bald");
        deiktikaList.add("bald");
        deiktikaList.add("Demnächst");
        deiktikaList.add("demnächst");
        */


        ArrayList<String> punctuationCharList = new ArrayList<String>();
        punctuationCharList.add("?");
        punctuationCharList.add("–");
        punctuationCharList.add("--");
        punctuationCharList.add("!");
        punctuationCharList.add("'");
        punctuationCharList.add("...");
        punctuationCharList.add("-");

        ArrayList<String>modalParticleList = new ArrayList<String>();
        modalParticleList.add("Deutlich");
        modalParticleList.add("deutlich");
        modalParticleList.add("Doch");
        modalParticleList.add("doch");
        modalParticleList.add("Immerhin");
        modalParticleList.add("immerhin");
        modalParticleList.add("Ja");
        modalParticleList.add("ja");
        modalParticleList.add("Natürlich");
        modalParticleList.add("natürlich");
        modalParticleList.add("Noch");
        modalParticleList.add("noch");
        modalParticleList.add("rasch");
        modalParticleList.add("Rasch");
        modalParticleList.add("Tatsächlich");
        modalParticleList.add("tatsächlich");
        modalParticleList.add("Vielleicht");
        modalParticleList.add("vielleicht");
        modalParticleList.add("Wirklich");
        modalParticleList.add("wirklich");
        modalParticleList.add("Wohl");
        modalParticleList.add("wohl");

        int stwrID = 1;

        for (AnnotationFS sentenceAnno : sentenceList) {
            List<AnnotationFS> coveredDirList = CasUtil.selectCovered(dirType, sentenceAnno);
            List<AnnotationFS> coveringDirList = CasUtil.selectCovering(dirType, sentenceAnno);
            List<AnnotationFS> coveredStwWordList = CasUtil.selectCovered(stwwType, sentenceAnno);
            List<AnnotationFS> coveringStwWordList = CasUtil.selectCovering(stwwType, sentenceAnno);
            List<AnnotationFS> coveredFrameList = CasUtil.selectCovered(frameType, sentenceAnno);
            List<AnnotationFS> coveringFrameList = CasUtil.selectCovering(frameType, sentenceAnno);

            if (coveredDirList.isEmpty() && coveringDirList.isEmpty() && coveredStwWordList.isEmpty()
                    && coveringStwWordList.isEmpty() && coveredFrameList.isEmpty() && coveringFrameList.isEmpty()) {
                List<AnnotationFS> coveredCabList = CasUtil.selectCovered(cabTokenType, sentenceAnno);
                for (AnnotationFS cabAnno : coveredCabList) {
                    if (cabAnno.getFeatureValueAsString(cabRfPos).equals("ITJ") || cabAnno.getFeatureValueAsString(cabRfPos).equals("PTKANT") ||
                            deiktikaList.contains(cabAnno.getFeatureValueAsString(cabLemma)) || punctuationCharList.contains(cabAnno.getCoveredText()) || modalParticleList.contains(cabAnno.getFeatureValueAsString(cabLemma))
                    || (cabAnno.getFeatureValueAsString(cabLemma).equals("werden") && cabAnno.getFeatureValueAsString(cabRfPos).contains("VFIN.Aux."))) {
                        if (!(CasUtil.selectPreceding(mainCas, dirType, sentenceAnno, 1).isEmpty() && CasUtil.selectFollowing(mainCas, dirType, sentenceAnno, 1).isEmpty())) {
                            AnnotationFS freeIndirectIndicatorAnno = mainCas.createAnnotation(stwrType, sentenceAnno.getBegin(), sentenceAnno.getEnd());
                            freeIndirectIndicatorAnno.setFeatureValueFromString(mediumFeat, "thought");
                            freeIndirectIndicatorAnno.setFeatureValueFromString(rTypeFeat, "ruleFreeIndirect");
                            freeIndirectIndicatorAnno.setFeatureValueFromString(levelFeat, "1");
                            freeIndirectIndicatorAnno.setFeatureValueFromString(nonFactFeat, "");
                            freeIndirectIndicatorAnno.setFeatureValueFromString(borderFeat, "");
                            freeIndirectIndicatorAnno.setFeatureValueFromString(pragFeat, "");
                            freeIndirectIndicatorAnno.setFeatureValueFromString(metaphFeat, "");
                            freeIndirectIndicatorAnno.setFeatureValueFromString(stwrFeat, sentenceAnno.getCoveredText());
                            freeIndirectIndicatorAnno.setFeatureValueFromString(stwrNote, "");
                            freeIndirectIndicatorAnno.setFeatureValueFromString(stwrIDFeat, String.valueOf(stwrID));
                            mainCas.addFsToIndexes(freeIndirectIndicatorAnno);
                            stwrID++;
                            break;
                        }
                    }
                }
            }
        }


        for (AnnotationFS sentenceAnno : sentenceList) {

            List<AnnotationFS> precList = CasUtil.selectPreceding(mainCas, sentenceType, sentenceAnno, 1);
            List<AnnotationFS> followList = CasUtil.selectFollowing(mainCas, sentenceType, sentenceAnno, 1);
            List<AnnotationFS> stwrCoveredList = CasUtil.selectCovered(mainCas, stwrType, sentenceAnno);
            List<AnnotationFS> stwrCoveringList = CasUtil.selectCovering(mainCas, stwrType, sentenceAnno);
            List<AnnotationFS> coveredDirList = CasUtil.selectCovered(dirType, sentenceAnno);
            List<AnnotationFS> coveringDirList = CasUtil.selectCovering(dirType, sentenceAnno);
            List<AnnotationFS> coveredStwWordList = CasUtil.selectCovered(stwwType, sentenceAnno);
            List<AnnotationFS> coveringStwWordList = CasUtil.selectCovering(stwwType, sentenceAnno);
            if (coveredDirList.isEmpty() && coveringDirList.isEmpty()
                    && coveredStwWordList.isEmpty() && coveringStwWordList.isEmpty()
                    && stwrCoveredList.isEmpty() && stwrCoveringList.isEmpty()
                    && (!precList.isEmpty()) && (!followList.isEmpty())) {

                if ((!CasUtil.selectCovering(mainCas, stwrType, precList.get(0)).isEmpty()
                        || !CasUtil.selectCovered(mainCas, stwrType, precList.get(0)).isEmpty())
                        && (!CasUtil.selectCovering(mainCas, stwrType, followList.get(0)).isEmpty()
                        || !CasUtil.selectCovered(mainCas, stwrType, followList.get(0)).isEmpty())){

                    List<AnnotationFS> precStwrCoveringList = CasUtil.selectCovering(mainCas, stwrType, precList.get(0));
                    List<AnnotationFS> followStwrCoveringList = CasUtil.selectCovering(mainCas, stwrType, followList.get(0));
                    List<AnnotationFS> precStwrCoveredList = CasUtil.selectCovered(mainCas, stwrType, precList.get(0));
                    List<AnnotationFS> followStwrCoveredList = CasUtil.selectCovered(mainCas, stwrType, followList.get(0));

                    if (precStwrCoveredList.get(0).getFeatureValueAsString(rTypeFeat).equals("ruleFreeIndirect")
                            && followStwrCoveredList.get(0).getFeatureValueAsString(rTypeFeat).equals("ruleFreeIndirect")
                            && precStwrCoveringList.get(0).getFeatureValueAsString(rTypeFeat).equals("ruleFreeIndirect")
                            && followStwrCoveringList.get(0).getFeatureValueAsString(rTypeFeat).equals("ruleFreeIndirect")) {
                        AnnotationFS freeIndirectIndicatorAnno = mainCas.createAnnotation(stwrType, sentenceAnno.getBegin(), sentenceAnno.getEnd());
                        mainCas.addFsToIndexes(freeIndirectIndicatorAnno);
                        freeIndirectIndicatorAnno.setFeatureValueFromString(mediumFeat, "thought");
                        freeIndirectIndicatorAnno.setFeatureValueFromString(rTypeFeat, "ruleFreeIndirect");
                        freeIndirectIndicatorAnno.setFeatureValueFromString(levelFeat, "1");
                        freeIndirectIndicatorAnno.setFeatureValueFromString(nonFactFeat, "");
                        freeIndirectIndicatorAnno.setFeatureValueFromString(borderFeat, "");
                        freeIndirectIndicatorAnno.setFeatureValueFromString(pragFeat, "");
                        freeIndirectIndicatorAnno.setFeatureValueFromString(metaphFeat, "");
                        freeIndirectIndicatorAnno.setFeatureValueFromString(stwrFeat, sentenceAnno.getCoveredText());
                        freeIndirectIndicatorAnno.setFeatureValueFromString(stwrNote, "");
                        stwrID++;
                        freeIndirectIndicatorAnno.setFeatureValueFromString(stwrIDFeat, String.valueOf(stwrID));
                        System.out.println("Stwr2: " + stwrID);
                    }
                }
            }
        }
        return mainCas;
    }
}



