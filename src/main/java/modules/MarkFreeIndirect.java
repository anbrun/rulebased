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

public class MarkFreeIndirect {
    private Boolean debug;
    private Integer limit;
    private Boolean useAdvIndicator;

    private Set<String> fiWords = new HashSet<String>();
    private Set<String> puncSpec = new HashSet<String>();

    public MarkFreeIndirect() throws IOException, URISyntaxException{
        this.debug = false;
        this.useAdvIndicator = true;
        this.limit = 2;
        this.fiWords = Util.readWordlistFromURL(Util.getFiWordsList());
        this.puncSpec = Util.readWordlistFromURL(Util.getPuncSpec());
    }

    public CAS process(CAS mainCas) {
        // first, remove all annotations of this type that are already in the document
        mainCas = Util.removeAllAnnotationsOfType(mainCas, "de.idsma.rw.rule.RuleFreeIndirect");

        // Sentence annotation
        Type sentType = mainCas.getTypeSystem().getType("de.idsma.rw.Sentence");
        // CabToken annotation
        Type cabTokenType = mainCas.getTypeSystem().getType("de.idsma.rw.CabToken");
        Feature cabPOS = cabTokenType.getFeatureByBaseName("Pos");
        Feature cabRfPos = cabTokenType.getFeatureByBaseName("RfPos");
        Feature cabLemma = cabTokenType.getFeatureByBaseName("Lemma");

        // get a map with Sentences as keys an the list of their covered CabTokens as values
        Map<AnnotationFS, Collection<AnnotationFS>> sentsWithToks = CasUtil.indexCovered(mainCas, sentType, cabTokenType);

        StringBuilder resTable = new StringBuilder();
        resTable.append("Nr\tSent"
                + "\tscore"
                + "\tfi"
                + "\tpunc"
                + "\tadv"
                + "\titj"
                + "\tother"
                + "\tblocked\n");

        int counter = 0;
        for (AnnotationFS sent : sentsWithToks.keySet()) {

            if (this.debug) {
                System.out.println("Sentence: " + counter);
            }

            List<AnnotationFS> tokList = new ArrayList<>(sentsWithToks.get(sent));

            int indicatorCounter = 0;
            int advCounter = 0;
            int puncSpecCounter = 0;
            int fiWordCounter = 0;
            int otherCounter = 0;
            int itjCounter = 0;

            boolean blocked = false;

            // Negative indicator:
            // if sentence length is above average: decrement indicatorCounter
            // (20 is about average for the Erzaehltextkorpus)
//			if (tokList.size() > 20) {
//				indicatorCounter = indicatorCounter - 4;
//				if (debug) {
//					System.out.println("long sent (-4): " + indicatorCounter);
//				}
//			} else if (tokList.size() > 10) {
//				indicatorCounter = indicatorCounter - 2;
//				if (debug) {
//					System.out.println("long sent (-2): " + indicatorCounter);
//				}
//			}

            // Loop through all the tokens in the current sentence
            for (AnnotationFS token : tokList) {

                // collect indicators from the POS markup
                String ttFeat = token.getFeatureValueAsString(cabPOS);

                // Negative indicators:
                //
                // if the sentence contains a 1st or 2nd person pronoun:
                // reset indicatorCounter and break the loop
                if (token.getFeatureValueAsString(cabRfPos).matches(
                        "PRO\\.Pers\\.Subst\\.[12]\\..*")) {
                    indicatorCounter = 0;
                    blocked = true;
                    if (debug) {
                        System.out.println("1st/2nd Person: blocked");
                    }
                    //break;
                }
                // if there is a quotation mark: reset the counter and break the
                // loop
                else if (ttFeat.equals("$(")) {
                    indicatorCounter = 0;
                    blocked = true;
                    if (debug) {
                        System.out.println("$(: blocked");
                    }
                    //break;
                }
                // Positive indicators:
                // if the token is in the puncSpec list: +2
                else if (this.puncSpec
                        .contains((String) token.getCoveredText())) {
                    indicatorCounter += 2;
                    puncSpecCounter += 1;
                    if (debug) {
                        System.out
                                .println("PuncSpec (+1): " + indicatorCounter);
                    }
                }
                // if the token is in the fiWords list: + 2
                else if (this.fiWords.contains((String) token.getFeatureValueAsString(cabLemma))) {
                    indicatorCounter = indicatorCounter + 2;
                    fiWordCounter +=1;
                    if (debug) {
                        System.out.println("FiWord (+1): " + indicatorCounter);
                    }
                } else if (token.getCoveredText().matches("würde(n)*")) {
                    indicatorCounter = indicatorCounter + 1;
                    otherCounter += 1;
                    if (debug) {
                        System.out.println("würde (+1): " + indicatorCounter);
                    }
                }
                // if the token is ITJ: +3
                else if (ttFeat.equals("ITJ")) {
                    itjCounter += 1;
                    indicatorCounter = indicatorCounter + 3;

                    if (debug) {
                        System.out.println("ITJ (+2): " + indicatorCounter);
                    }
                }

                if (this.useAdvIndicator) {
                    // if the token is ADV: add +1 to advCounter
                    if (ttFeat.equals("ADV")) {
                        advCounter++;
                    }
                }

            }

            //if (!blocked) {
            // calculate how much to add from the advCounter:
            // if ADV is between 5 and 15% of tokens -> add 1
            // if ADV is between 15 and 25% of tokens -> add 2 etc.
//				Float relAdv = Float.valueOf(advCounter)
//						/ Float.valueOf(tokList.size()) * 10;
//				if (debug) {
//					System.out.println("relAdv: " + relAdv);
//					System.out.println("--- " + Math.round(relAdv));
//					System.out.println("--- " + relAdv.intValue());
//				}
//				indicatorCounter = indicatorCounter + relAdv.intValue();
//				if (debug) {
//					System.out.println("ADV.rel (+ " + relAdv + "): "
//							+ indicatorCounter);
//				}
            //}


            indicatorCounter += advCounter;

            // if debug is true, show the score for each sentence
            if (debug) {
                System.out.println("Sentence: " + sent.getCoveredText() + "\n score: " + indicatorCounter + " blocked: " + blocked);
            }

            resTable.append(counter + "\t" + sent.getCoveredText().replaceAll("\\s", " ") + "\t"
                    + indicatorCounter + "\t"
                    + fiWordCounter + "\t"
                    + puncSpecCounter + "\t"
                    + advCounter + "\t"
                    + itjCounter + "\t"
                    + otherCounter + "\t"
                    + blocked + "\n");

            // if indicator > limit, add an annotation
            if (indicatorCounter >= this.limit) {
                // get the STWWord annotation
                Type fiType = mainCas.getTypeSystem().getType("de.idsma.rw.rule.RuleFreeIndirect");
                Feature fiScore = fiType.getFeatureByBaseName("Score");

                AnnotationFS freeIndirectAnno = mainCas.createAnnotation(fiType, sent.getBegin(), sent.getEnd());
                freeIndirectAnno.setFeatureValueFromString(fiScore, new Integer(indicatorCounter).toString());

                mainCas.addFsToIndexes(freeIndirectAnno);
            }

            counter++;

        }

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("resTab.csv"), "utf-8"))) {
            writer.write(resTable.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return mainCas;
    }

}



