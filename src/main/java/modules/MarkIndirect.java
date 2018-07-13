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

public class MarkIndirect {
    private File conjVerbsURL;
    private File quotMarksURL;

    private Boolean debug;
    private Boolean removeSTWWord;
    private Boolean ignoreRepWords;
    private Boolean useConjVerbList;
    private int penaltySTWW;

    Set<AnnotationFS> toRemove = new HashSet<>();

    private Set<String> conj;
    private Set<String> conjVerbs;
    private Set<String> quotMarks;
    // private ArrayList<String> forbiddenConj;

    // penalty will be incremented, if the case is unsure
    private int penalty = 0;
    // store the type of the recognized indirect representation
    private String irType = new String();

    public MarkIndirect() throws IOException, URISyntaxException{
        this.conj = Util.readWordlistFromURL(Util.getConjList());
        this.conjVerbs = Util.readWordlistFromURL(Util.getConjVerbsList());
        this.quotMarks = Util.readWordlistFromURL(Util.getQuotationsMarksList());
        this.debug = false;
        this.useConjVerbList = true;
        this.ignoreRepWords = true;
        this.removeSTWWord = false;
        this.penaltySTWW = 2;
    }

    public CAS process(CAS mainCas) {
        // Sentence annotation
        Type sentType = mainCas.getTypeSystem().getType("de.idsma.rw.Sentence");
        // STWWord annotation
        Type stwwType = mainCas.getTypeSystem().getType("de.idsma.rw.rule.StwWord");
        Feature stwwPenalty = stwwType.getFeatureByBaseName("Penalty");
        Feature stwwMarker = stwwType.getFeatureByBaseName("Marker");
        // CabTokens
        Type cabTokenType = mainCas.getTypeSystem().getType("de.idsma.rw.CabToken");
        Feature cabPOS = cabTokenType.getFeatureByBaseName("Pos");
        Feature cabRfPos = cabTokenType.getFeatureByBaseName("RfPos");
        Feature cabLemma = cabTokenType.getFeatureByBaseName("Lemma");

        // get a map with Sentences as keys an the list of their covered STWWords as values
        Map<AnnotationFS, Collection<AnnotationFS>> sentsWithStww = CasUtil.indexCovered(mainCas, sentType, stwwType);

        for (AnnotationFS sent : sentsWithStww.keySet()) {
            // contains STWWords?
            Collection<AnnotationFS> coveredStwWords = sentsWithStww.get(sent);

            // detemine the relevant stwwords
            Set<AnnotationFS> relevantStwWords = new HashSet<>();
            for (AnnotationFS stww : coveredStwWords) {
                int penalty =  Integer.parseInt(stww.getFeatureValueAsString(stwwPenalty));
                if (penalty <= this.penaltySTWW) {
                    if (ignoreRepWords) {
                        if (! stww.getFeatureValueAsString(stwwMarker).equals("rep")) {
                            relevantStwWords.add(stww);
                        }
                    }
                    else {
                        relevantStwWords.add(stww);
                    }
                }
            }

//            // Remove words with repMarker if ignoreRepWords is true
//            if (ignoreRepWords) {
//                for (AnnotationFS stww : coveredStwWords) {
//                    if (stww.getFeatureValueAsString(stwwMarker).equals("rep")) {
//                        coveredStwWords.remove(stww);
//                    }
//                }
//            }
//            // remove all Words with a penalty higher than the one specified
//            if (this.penaltySTWW < 5) {
//                for (AnnotationFS stww : coveredStwWords) {
//                    if (Integer.parseInt(stww.getFeatureValueAsString(stwwPenalty)) > this.penaltySTWW) {
//                        coveredStwWords.remove(stww);
//                    }
//                }
//            }

            if (relevantStwWords.size() > 0) {
                // get the Tokens of this sentence
                List<AnnotationFS> toks = CasUtil.selectCovered(cabTokenType, sent);

                // try to find indirect structure for each covered stwWord
                for (AnnotationFS currSTWWord : coveredStwWords) {
                    //System.out.println("sTW: " + currSTWWord);

                    // get the Token that is identical with STWWord
                    // and a list of all Tokens following it
                    List<AnnotationFS> tokList = new ArrayList<>();
                    AnnotationFS startTok = null;
                    for (AnnotationFS tok : toks) {
                        if (currSTWWord.getBegin() == tok.getBegin()) {
                            startTok = tok;
                        }
                        else if (currSTWWord.getBegin() < tok.getBegin()) {
                            tokList.add(tok);
                        }
                    }

                    // these variables are for remembering certain positions later
                    AnnotationFS commaTok = null; // Position of the comma (must be before conjunction)
                    AnnotationFS zuTok = null; // Position of "zu" (must be before infinitive verb)
                    this.irType = ""; // reset irType
                    this.penalty = 0; // reset penalty

                    // start in stage 1 for each STWWord
                    int stageStatus = 1;
                    // starting point is the Token after the current STWWord
                    for (AnnotationFS tok : tokList) {

                        if (this.debug) {
                            System.out.println("Index: " + tok.getBegin() + " Wort: "
                                    + tok.getCoveredText());
                        }

                        // *********** STAGE 1 ******************** //

                        if (stageStatus == 1) {

                            // If rfTagger is available:
                            // first word after stw word is a coordination
                            // conjunction --> break
                            if (tokList.indexOf(tok) == 0) {
                                String rfVal = tok.getFeatureValueAsString(cabRfPos);
                                if (rfVal != null && rfVal.matches(".*Coord.*")) {
                                    if (this.debug) {
                                        System.out.println("Abbruch stage 1: Koordination nach rw-Wort");
                                    }
                                    break;
                                }
                            }

                            // Colon found (can replace comma) --> Stage 2
                            // (must be checked first, because : has the same
                            // category as sentence
                            // end)
                            if (tok.getFeatureValueAsString(cabLemma).equals(":")) {
                                stageStatus = 2;
                                commaTok = tok;
                                if (this.debug) {
                                    System.out
                                            .println("Doppelpunkt gefunden: stage 2");
                                }

                            }
                            // Sentence end found --> break
                            else if (tok.getFeatureValueAsString(cabPOS).equals("$.")) {
                                //stageStatus = 0;
                                if (this.debug) {
                                    System.out.println("Abbruch stage 1: Satzende");
                                }
                                break;
                            }

                            // Quotation mark found --> break
                            else if (this.quotMarks.contains((String) tok.getCoveredText())) {
                                //stageStatus = 0;
                                if (this.debug) {
                                    System.out
                                            .println("Abbruch stage 1: Anführungszeichen");
                                }
                                break;
                            }

                            // comma found --> stage 2
                            else if (tok.getFeatureValueAsString(cabPOS).equals("$,")) {
                                stageStatus = 2;
                                commaTok = tok;
                                if (this.debug) {
                                    System.out.println("Komma gefunden: stage 2");
                                }
                            }

                            // "zu" found --> stage 3 (and wait for infinitive verb)
                            else if (tok.getFeatureValueAsString(cabLemma).equals("zu")) {
                                if (this.debug) {
                                    System.out.println("\"zu\" gefunden");
                                }
                                stageStatus = 3;
                                zuTok = tok;
                            }

                            // infinitive with merged "zu" found --> success, add
                            // annotation
                            else if (tok.getFeatureValueAsString(cabPOS).equals("VVIZU")) {
                                if (this.debug) {
                                    System.out.println("eingebettes zu gefunden");
                                }
                                //stageStatus = 0;
                                this.irType = "zu";

                                // start Annotation after the comma tok, if one was found
                                // or directly after the stwrTok otherwise
                                int start = tokList.get(0).getBegin();
                                if (commaTok != null) {
                                    start = tokList.get(tokList.indexOf(commaTok) + 1).getBegin();
                                }
                                addAnnotation(mainCas, start, tok.getEnd(), currSTWWord);

                                if (this.debug) {
                                    System.out.println("Erkennung abgeschlossen");
                                }
                                break;

                            }

                        }

                        // *********** STAGE 2 ******************** //
                        // Stage 2: Comma or Colon was found

                        else if (stageStatus == 2) {
                            // sentence end found --> break
                            if (tok.getFeatureValueAsString(cabPOS).equals("$.")) {
                                //stageStatus = 0;
                                if (this.debug) {
                                    System.out.println("Satzende: Abbruch stage 2");
                                }
                                break;
                            }

                            // Quotation mark found: break
                            else if (this.quotMarks.contains((String) tok.getCoveredText())) {
                                //stageStatus = 0;
                                if (this.debug) {
                                    System.out
                                            .println("Abbruch stage 2: Anfuehrungszeichen");
                                }
                                break;
                            }

                            // additional comma found --> break
                            // (problematic as subordinate clauses may sometimes be
                            // possible here,
                            // but often
                            // prevents wrong annotations)
                            else if (tok.getFeatureValueAsString(cabPOS).equals("$,")) {
                                if (this.debug) {
                                    System.out.println("weiteres Komma: Abbruch stage 2");
                                    //System.out.println("weiteres Komma: penalty");
                                }
                                //stageStatus = 0;
                                break;

                            }
                            // "zu" found --> stage 3 (wait for verb in infinitive)
                            else if (tok.getFeatureValueAsString(cabLemma).equals("zu")) {
                                if (this.debug) {
                                    System.out.println("zu gefunden");
                                }
                                stageStatus = 3;
                                zuTok = tok;
                            }
                            // infinitive with merged "zu" found --> success, add
                            // annotation
                            else if (tok.getFeatureValueAsString(cabPOS).equals("VVIZU")) {
                                if (this.debug) {
                                    System.out.println("eingebettes zu gefunden");
                                }
                                //stageStatus = 0;
                                this.irType = "zu";

                                // start Annotation after the comma tok, if one was found
                                // or directly after the stwrTok otherwise
                                int start = tokList.get(0).getBegin();
                                if (commaTok != null) {
                                    start = tokList.get(tokList.indexOf(commaTok) + 1).getBegin();
                                }
                                this.addAnnotation(mainCas, start, tok.getEnd(), currSTWWord);
                                if (this.debug) {
                                    System.out.println("Erkennung abgeschlossen");
                                }
                                break;

                            }

                            // TODO
                            // "conjunctive verb" found (with RF-Tagger) -->
                            // success, add annotation
                            // else if (
                            // this.useRFTagger &&
                            // ((String)tok.getFeatures().get("featsRF")).matches(".*\\.Subj")
                            // )
                            // {
                            // if (this.debug) {
                            // System.out.println("conjunction verb found");
                            // }
                            // stageStatus = 0;
                            // this.irType = "conjVerb";
                            // this.addAnnotation(outputAS, inputAS, tokens, start,
                            // pIndex,
                            // currSTWWord);
                            // if (this.debug) {
                            // System.out.println("Erkennung abgeschlossen");
                            // }
                            // }
                            //
                            // "conjunctive verb" found (with conjunctive verb list)
                            // --> success, add
                            // annotation
                            else if (this.useConjVerbList
                                    && this.conjVerbs.contains((String) tok.getCoveredText())) {
                                if (this.debug) {
                                    System.out.println("conjunction verb found");
                                }
                                //stageStatus = 0;
                                this.irType = "conjVerb";

                                // start Annotation after the comma tok, if one was found
                                // or directly after the stwrTok otherwise
                                int start = tokList.get(0).getBegin();
                                if (commaTok != null) {
                                    start = tokList.get(tokList.indexOf(commaTok) + 1).getBegin();
                                }
                                this.addAnnotation(mainCas, start, tok.getEnd(),
                                        currSTWWord);
                                if (this.debug) {
                                    System.out.println("Erkennung abgeschlossen");
                                }
                                break;
                            }

                            // position directly behind comma / colon
                            else if (commaTok != null &&
                                    tokList.indexOf(tok) == tokList.indexOf(commaTok) + 1) {
                                if (this.debug) {
                                    System.out.println("Check Pos hinter Komma: "
                                            + tok.getCoveredText());
                                }
                                // conjunction found (with conj list) --> stage 4
                                if (this.conj.contains((String) tok
                                        .getCoveredText())) {
                                    if (this.debug) {
                                        System.out.println("Konjunktion gefunden: "
                                                + tok
                                                .getCoveredText());
                                    }
                                    stageStatus = 4;
                                    irType = "konj";
                                }
                                // Konjunktion gefunden, die nicht in der Liste ist
                                // -> Abbruch
                                else if (tok.getFeatureValueAsString(cabPOS)
                                        .equals("KOUS")
                                        || tok.getFeatureValueAsString(cabPOS)
                                        .equals("KOUI") //
                                        || tok.getFeatureValueAsString(cabPOS)
                                        .equals("KOKOM") //
                                        // Vergleichspartikel
                                        || tok.getFeatureValueAsString(cabPOS)
                                        .equals("KON") //
                                    // Nebenordnende Konj
                                        ) {
                                    if (this.debug) {
                                        System.out
                                                .println("Falsche Konjunktion: Abbruch stage 2");
                                    }
                                    //stageStatus = 0;
                                    break;
                                }
                            }

                        }

                        // *********** STAGE 3 ******************** //

                        // Stage 3: "zu" found
                        else if (stageStatus == 3) {
                            if (this.debug) {
                                System.out.println("Stage 3, Token:"
                                        + tok.getFeatureValueAsString(cabPOS));
                            }
                            // too far (verb may be just one position behind "zu")
                            // --> break
                            if (zuTok != null &&
                                    tokList.indexOf(tok) > tokList.indexOf(zuTok) + 1) {
                                if (this.debug) {
                                    System.out.println("zu weit: Abbruch stage 3");
                                }
                                //stageStatus = 0;
                                zuTok = null;
                                break;
                            }
                            // infinitive found --> success, add annotation
                            else if (tok.getFeatureValueAsString(cabPOS).equals("VVINF")
                                    || tok.getFeatureValueAsString(cabPOS).equals("VAINF")
                                    || tok.getFeatureValueAsString(cabPOS).equals("VMINF")) {
                                if (this.debug) {
                                    System.out.println("VINF gefunden: "
                                            + tok.getFeatureValueAsString(cabLemma));
                                }
                                //stageStatus = 0;
                                irType = "zu";
                                // start Annotation after the comma tok, if one was found
                                // or directly after the stwrTok otherwise
                                int start = tokList.get(0).getBegin();
                                if (commaTok != null) {
                                    start = tokList.get(tokList.indexOf(commaTok) + 1).getBegin();
                                }
                                this.addAnnotation(mainCas, start, tok.getEnd(),
                                        currSTWWord);
                                if (this.debug) {
                                    System.out.println("Erkennung abgeschlossen");
                                }
                                break;

                            }
                            // sentence end found : break
                            else if (tok.getFeatureValueAsString(cabPOS).equals("$.")) {
                                if (this.debug) {
                                    System.out.println("Abbruch stage 3: Satzende");
                                }
                                //stageStatus = 0;
                                break;
                            }
                        }

                        // *********** STAGE 4 ******************** //

                        // Stage 4: conjunction found
                        else if (stageStatus == 4) {
                            if (this.debug) {
                                System.out.println("in Stage 4");
                            }
                            // finite verb found --> success, add annotation
                            if (tok.getFeatureValueAsString(cabPOS).equals("VVFIN")
                                    || tok.getFeatureValueAsString(cabPOS).equals("VAFIN")
                                    || tok.getFeatureValueAsString(cabPOS).equals("VMFIN")) {
                                //stageStatus = 0;
                                // start Annotation after the comma tok, if one was found
                                // or directly after the stwrTok otherwise
                                int start = tokList.get(0).getBegin();
                                if (commaTok != null) {
                                    start = tokList.get(tokList.indexOf(commaTok) + 1).getBegin();
                                }
                                this.addAnnotation(mainCas, start, tok.getEnd(),
                                        currSTWWord);
                                if (this.debug) {
                                    System.out.println("Erkennung abgeschlossen");
                                }
                                break;
                            }
                            // sentence end found --> success (but with penalty),
                            // add annotation
                            // (annotation may end here, because it is highly likely
                            // that this is an
                            // indirect
                            // representation and there was just a mistake - e.g.
                            // the finite verb is
                            // not tagged
                            // correctly -, but annotation is penalized)
                            else if (tok.getFeatureValueAsString(cabPOS).equals("$.")) {
                                // penalty is incremented
                                this.penalty += 1;
                                //stageStatus = 0;
                                // start Annotation after the comma tok, if one was found
                                // or directly after the stwrTok otherwise
                                int start = tokList.get(0).getBegin();
                                if (commaTok != null) {
                                    start = tokList.get(tokList.indexOf(commaTok) + 1).getBegin();
                                }
                                this.addAnnotation(mainCas, start, tok.getEnd(),
                                        currSTWWord);
                                if (this.debug) {
                                    System.out
                                            .println("Erkennung abgeschlossen (ohne finites Verb)");
                                }
                                break;
                            }

                        }
                    }

                }

            }

        }

        // if removeRWWords = true
        // the STWWords need to be removed *after* the iteration, because
        // otherwise the loop breaks down
        if (this.removeSTWWord) {
            for (AnnotationFS stwWord : this.toRemove) {
                if (this.debug) {
                    System.out.println("Remove FrameWord: " + stwWord.getCoveredText());
                }
                mainCas.removeFsFromIndexes(stwWord);
            }
            // reset the toRemove list afterwards
            this.toRemove = new HashSet<>();
        }

        return mainCas;
    }


    private void addAnnotation(CAS mainCas, int start, int end, AnnotationFS rwWord) {
        // get the STWWord annotation
        Type indirectType = mainCas.getTypeSystem().getType("de.idsma.rw.rule.RuleIndirect");
        Feature indPenalty = indirectType.getFeatureByBaseName("Penalty");
        Feature indType = indirectType.getFeatureByBaseName("IndType");

        AnnotationFS indirectAnno = mainCas.createAnnotation(indirectType, start, end);
        indirectAnno.setFeatureValueFromString(indPenalty, new Integer(this.penalty).toString());
        System.out.println("Ind type: " + irType);
        indirectAnno.setFeatureValueFromString(indType, this.irType);

        mainCas.addFsToIndexes(indirectAnno);

        if (this.debug) {
            System.out.println("Add Indirect annotation: " + indirectAnno.getCoveredText());
        }

        // if removeFrameWord is true, remove the STWWord annotation
        if (this.removeSTWWord) {
            if (this.debug) {
                System.out.println("Remove FrameWord");
            }
            this.toRemove.add(rwWord);
        }

    }



}
