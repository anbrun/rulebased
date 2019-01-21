package modules;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;

import util.Util;

import java.util.*;
import java.io.*;
import java.net.*;

public class MarkFrame {
    private Boolean removeSTWWord = false;
    private Boolean debug = true;
    private int penaltyLevel = 5;
    String tokenKind = "cab";

    Set<AnnotationFS> toRemove = new HashSet<>();

    public CAS process(CAS mainCas) {
        // first, remove all annotations of this type that are already in the document
        mainCas = Util.removeAllAnnotationsOfType(mainCas, "de.idsma.rw.rule.RuleFrame");
        // toRemove needs to be empty whenever a process task starts
        // otherwise there will be problems when processing multiple documents
        toRemove = new HashSet<>();

        // Sentence annotation
        Type stwwType = mainCas.getTypeSystem().getType("de.idsma.rw.rule.StwWord");
        Iterator<AnnotationFS> stwworditer = CasUtil.iterator(mainCas, stwwType);

        while (stwworditer.hasNext()) {
            AnnotationFS stwWord = stwworditer.next();
            if (this.debug) {
                System.out.println("sTWWord: " + stwWord.getCoveredText());
            }
            boolean marked = this.leadingFrame(mainCas, stwWord);
            // only try to mark a following frame if no leading frame was marked
            if (!marked) {
                marked = this.followingFrameQuote(mainCas, stwWord);
                if (!marked) {
                    this.followingFrame(mainCas, stwWord);
                }
            }
        }

        // the STWWords need to be removed after the iteration, because
        // otherwise the loop breaks down
        if (this.removeSTWWord) {
            for (AnnotationFS stwWord : this.toRemove) {
                if (this.debug) {
                    System.out.println("Remove FrameWord: "
                            + stwWord.getCoveredText());
                }
                mainCas.removeFsFromIndexes(stwWord);
            }
        }
    return mainCas;
    }



    /**
     * Marks a frame that precedes a quotation Only one annotation is done and
     * the patterns are tested in the order below: 1) und entgegnete : (")
     * Mittags komme ich 2) und sagte ihm/Peter : (") Du hast es versprochen
     * returns true, if an annotation was made
     *
     * @param mainCas
     * @param stwWord
     * @return
     */
    private boolean leadingFrame(CAS mainCas, AnnotationFS stwWord) {
        if (tokenKind.equals("cab")){
            String position = "start";
            // CabTokens
            Type cabTokenType = mainCas.getTypeSystem().getType("de.idsma.rw.CabToken");
            Feature cabPOS = cabTokenType.getFeatureByBaseName("Pos");

            List<AnnotationFS> toklist = CasUtil.selectFollowing(mainCas, cabTokenType,
                    stwWord, 2);

            // 1) und entgegnete : (") Mittags komme ich
            if (toklist.get(0).getCoveredText().equals(":")) {
                this.addAnnotation(mainCas, stwWord.getBegin(), stwWord
                        .getEnd(), stwWord, position);
                return true;
            }
            // 2) und sagte ihm/Peter : (") Du hast es versprochen
            else if ((toklist.get(0).getFeatureValueAsString(cabPOS).equals("NE") || toklist
                    .get(0).getFeatureValueAsString(cabPOS).equals("PPER"))
                    && toklist.get(1).getCoveredText().equals(":")) {
                this.addAnnotation(mainCas, stwWord.getBegin(), stwWord.getEnd(), stwWord, position);
                return true;
            } else
                return false;
        } else if (tokenKind.equals("no_cab")){
            String position = "start";
            //Tokens
            Type tokenType = mainCas.getTypeSystem().getType("de.idsma.rw.preprocessing.Token");
            Feature ttPosFeat = tokenType.getFeatureByBaseName("Pos");

            List<AnnotationFS> toklist = CasUtil.selectFollowing(mainCas, tokenType,
                    stwWord, 2);

            // 1) und entgegnete : (") Mittags komme ich
            if (toklist.get(0).getCoveredText().equals(":")) {
                this.addAnnotation(mainCas, stwWord.getBegin(), stwWord
                        .getEnd(), stwWord, position);
                return true;
            }
            // 2) und sagte ihm/Peter : (") Du hast es versprochen
            else if ((toklist.get(0).getFeatureValueAsString(ttPosFeat).equals("NE") || toklist
                    .get(0).getFeatureValueAsString(ttPosFeat).equals("PPER"))
                    && toklist.get(1).getCoveredText().equals(":")) {
                this.addAnnotation(mainCas, stwWord.getBegin(), stwWord.getEnd(), stwWord, position);
                return true;
            } else
                return false;
        }
        return false;
    }

    /**
     * marks a frame that follows the quotation Only one annotation is done and
     * the patterns are tested in the order below:
     *  3) " , sagte
     *  4) " sagte
     *
     * returns true if an annotation was made
     *
     * @param mainCas
     * @param stwWord
     * @return
     */
    private boolean followingFrameQuote(CAS mainCas, AnnotationFS stwWord) {
        boolean matchFound = false;
        if (tokenKind.equals("cab")){
            String position = "end";
            // CabTokens
            Type cabTokenType = mainCas.getTypeSystem().getType("de.idsma.rw.CabToken");
            Feature cabPOS = cabTokenType.getFeatureByBaseName("Pos");

            List<AnnotationFS> tokBefore = CasUtil.selectPreceding(mainCas, cabTokenType,
                    stwWord, 2);
            //System.out.println("Tok before: " + tokBefore);
            //boolean matchFound = false;
            if (tokBefore.size() == 2) {
                // 1) " , sagte
                if (tokBefore.get(0).getFeatureValueAsString(cabPOS).equals("$(")
                        && tokBefore.get(1).getFeatureValueAsString(cabPOS).equals("$,")) {
                    this.addAnnotation(mainCas, stwWord.getBegin(),
                            stwWord.getEnd(), stwWord, position);
                    matchFound = true;
                }
                // 2) " sagte
                else if (tokBefore.get(1).getFeatureValueAsString(cabPOS).equals("$(")) {
                    this.addAnnotation(mainCas, stwWord.getBegin(),
                            stwWord.getEnd(), stwWord, position);
                    matchFound = true;
                }
            }
            return matchFound;

        }else if (tokenKind.equals("no_cab")){
            String position = "end";
            Type tokenType = mainCas.getTypeSystem().getType("de.idsma.rw.preprocessing.Token");
            Feature ttPosFeat = tokenType.getFeatureByBaseName("Pos");


            List<AnnotationFS> tokBefore = CasUtil.selectPreceding(mainCas, tokenType,
                    stwWord, 2);
            //System.out.println("Tok before: " + tokBefore);
            //boolean matchFound = false;
            if (tokBefore.size() == 2) {
                // 1) " , sagte
                if (tokBefore.get(0).getFeatureValueAsString(ttPosFeat).equals("$(")
                        && tokBefore.get(1).getFeatureValueAsString(ttPosFeat).equals("$,")) {
                    this.addAnnotation(mainCas, stwWord.getBegin(),
                            stwWord.getEnd(), stwWord, position);
                    matchFound = true;
                }
                // 2) " sagte
                else if (tokBefore.get(1).getFeatureValueAsString(ttPosFeat).equals("$(")) {
                    this.addAnnotation(mainCas, stwWord.getBegin(),
                            stwWord.getEnd(), stwWord, position);
                    matchFound = true;
                }
            }
            return matchFound;
        }
        return matchFound;
    }

    /**
     * marks a frame that follows the quotation Only one annotation is done and
     * the patterns are tested in the order below: 1) , sagte er/Peter 2) , sagte der Mann
     * returns true if an annotation was made
     *
     * @param mainCas
     * @param stwWord
     * @return
     */
    private boolean followingFrame(CAS mainCas, AnnotationFS stwWord) {
        boolean matchFound = false;
        if (tokenKind.equals("cab")){
            String position = "end";
            // CabTokens
            Type cabTokenType = mainCas.getTypeSystem().getType("de.idsma.rw.CabToken");
            Feature cabPOS = cabTokenType.getFeatureByBaseName("Pos");

            List<AnnotationFS> tokAfter = CasUtil.selectFollowing(mainCas, cabTokenType,
                    stwWord, 2);
            List<AnnotationFS> tokBefore = CasUtil.selectPreceding(mainCas, cabTokenType,
                    stwWord, 2);
            //boolean matchFound = false;
            if (tokBefore.size() == 2 && tokAfter.size() == 2) {
                if (tokBefore.get(1).getCoveredText().equals(",")) {
                    // 1) , sagte er/Peter
                    if (tokAfter.get(0).getFeatureValueAsString(cabPOS).equals("NE")
                            || tokAfter.get(0).getFeatureValueAsString(cabPOS).equals("NN")
                            || tokAfter.get(0).getFeatureValueAsString(cabPOS).equals("PPER")) {
                        this.addAnnotation(mainCas, stwWord.getBegin(), stwWord
                                .getEnd(), stwWord, position);
                        matchFound = true;
                    }
                    // 2) , sagte der Mann
                    else if (tokAfter.get(0).getFeatureValueAsString(cabPOS).equals("ART")
                            && tokAfter.get(1).getFeatureValueAsString(cabPOS).equals("NN")) {
                        this.addAnnotation(mainCas, stwWord.getBegin(), stwWord
                                .getEnd(), stwWord, position);
                        matchFound = true;
                    }
                }
            }
            return matchFound;
        }else if (tokenKind.equals("no_cab")){
            String position = "end";

            Type tokenType = mainCas.getTypeSystem().getType("de.idsma.rw.preprocessing.Token");
            Feature ttPosFeat = tokenType.getFeatureByBaseName("Pos");


            List<AnnotationFS> tokAfter = CasUtil.selectFollowing(mainCas, tokenType,
                    stwWord, 2);
            List<AnnotationFS> tokBefore = CasUtil.selectPreceding(mainCas, tokenType,
                    stwWord, 2);
            //boolean matchFound = false;
            if (tokBefore.size() == 2 && tokAfter.size() == 2) {
                if (tokBefore.get(1).getCoveredText().equals(",")) {
                    // 1) , sagte er/Peter
                    if (tokAfter.get(0).getFeatureValueAsString(ttPosFeat).equals("NE")
                            || tokAfter.get(0).getFeatureValueAsString(ttPosFeat).equals("NN")
                            || tokAfter.get(0).getFeatureValueAsString(ttPosFeat).equals("PPER")) {
                        this.addAnnotation(mainCas, stwWord.getBegin(), stwWord
                                .getEnd(), stwWord, position);
                        matchFound = true;
                    }
                    // 2) , sagte der Mann
                    else if (tokAfter.get(0).getFeatureValueAsString(ttPosFeat).equals("ART")
                            && tokAfter.get(1).getFeatureValueAsString(ttPosFeat).equals("NN")) {
                        this.addAnnotation(mainCas, stwWord.getBegin(), stwWord
                                .getEnd(), stwWord, position);
                        matchFound = true;
                    }
                }
            }
            return matchFound;
        }
        return matchFound;
    }

    private void addAnnotation(CAS mainCas, int start, int end, AnnotationFS stwWord, String position) {
        Type frameType = mainCas.getTypeSystem().getType("de.idsma.rw.rule.RuleFrame");
        Feature rType = frameType.getFeatureByBaseName("Pos");

        AnnotationFS frameAnno = mainCas.createAnnotation(frameType, start, end);
        frameAnno.setFeatureValueFromString(rType, position);

        mainCas.addFsToIndexes(frameAnno);

        // if removeFrameWord is true, schedule the STWWord to be removed
        // (cannot be removed immediately, because then the iteration
        // in 'process' breaks down :-/)
        if (this.removeSTWWord) {
            this.toRemove.add(stwWord);
        }
    }
}
