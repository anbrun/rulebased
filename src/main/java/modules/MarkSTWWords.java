package modules;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import util.Util;

import java.net.URISyntaxException;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class MarkSTWWords {
    private Boolean debug;
    private File fileSTWWords;
    private int penaltyLevel;


    // collection of stwwords
    private ArrayList<String> stwWords;
    // additional infos for the stwwords
    private ArrayList<String> stwWordsCat = new ArrayList<String>();
    private ArrayList<String> stwWordsSource = new ArrayList<String>();
    private ArrayList<Integer> stwWordsPenalty = new ArrayList<Integer>();
    private ArrayList<String> stwWordsMark = new ArrayList<String>();
    private ArrayList<Integer> stwWordsFreq = new ArrayList<Integer>();


    public MarkSTWWords() throws IOException, URISyntaxException {
        this.debug = false;
        this.fileSTWWords = Util.getSTWWordList();
        this.penaltyLevel = 5;

        System.out.println("FILE; " + this.fileSTWWords.getAbsolutePath());
        readWordList();
    }

    public MarkSTWWords(File fileSTWWords, int penaltyLevel, Boolean debug)
        throws IOException {
        this.debug = debug;
        this.fileSTWWords = fileSTWWords;
        this.penaltyLevel = penaltyLevel;

        this.readWordList();
    }


    public CAS process(CAS mainCas){
        // get the CabToken annotation
        Type cabTokType = mainCas.getTypeSystem().getType("de.idsma.rw.CabToken");
        AnnotationIndex<AnnotationFS> cabTokTypeIndex = mainCas.getAnnotationIndex(cabTokType);
        Feature cabLemma = cabTokType.getFeatureByBaseName("Lemma");

        // get the STWWord annotation
        Type stwwType = mainCas.getTypeSystem().getType("de.idsma.rw.rule.StwWord");
        Feature stwwLemma =  stwwType.getFeatureByBaseName("Lemma");
        Feature stwwPenalty = stwwType.getFeatureByBaseName("Penalty");
        Feature stwwMedium = stwwType.getFeatureByBaseName("Medium");
        Feature stwwMarker = stwwType.getFeatureByBaseName("Marker");


        for (AnnotationFS tok : cabTokTypeIndex) {

//            if (this.debug) {
//                System.out.println("Curr Token: '" + tok.getCoveredText() + "' Lemma: '"
//                        +  tok.getFeatureValueAsString(cabLemma) + "'");
//            }
            String lemmaVal = tok.getFeatureValueAsString(cabLemma);
            if (stwWords.contains(lemmaVal)) {
                int pos = stwWords.indexOf(lemmaVal);
                AnnotationFS stwwAnno = mainCas.createAnnotation(stwwType, tok.getBegin(), tok.getEnd());
                stwwAnno.setFeatureValueFromString(stwwPenalty, this.stwWordsPenalty.get(pos).toString());
                stwwAnno.setFeatureValueFromString(stwwMedium, this.stwWordsCat.get(pos));
                stwwAnno.setFeatureValueFromString(stwwMarker, this.stwWordsMark.get(pos));
                mainCas.addFsToIndexes(stwwAnno);

                if (this.debug) {
                    System.out.println("This Token: " + tok.getCoveredText() + " was annotated");
                }

            }
        }
        return mainCas;
    }


    private void readWordList() throws IOException {
        try {
            // Open a reader for the rw word file so we can load
            // the list of rw words
            Path path = fileSTWWords.toPath();
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

            if (this.debug) {
                System.out.println("VerbUrl: " + fileSTWWords.getAbsolutePath());
            }
            this.stwWords = new ArrayList<String>();


            for (String line : lines) {
                // while there is still data in the file...

                String[] linefields = line.trim().split("\\s+");
                // when there are 5 elements they should be: 1:lemma. 2:source,
                // 3:penalty, 4:cat; 5: marker
                if (linefields.length >= 5) {
                    // if a penalty value is defined, penalty level
                    // need to be checked
                    Integer penalty = Integer.parseInt(linefields[2]);
                    if (penalty <= this.penaltyLevel) {

                        this.stwWords.add(linefields[0]);
                        this.stwWordsSource.add(linefields[1]);
                        this.stwWordsPenalty.add(penalty);
                        this.stwWordsCat.add(linefields[3]);
                        this.stwWordsMark.add(linefields[4]);
                        this.stwWordsFreq.add(Integer.parseInt(linefields[5]));
                    }
                }
                // otherwise assume that the first element is the lemma and
                // ignore the rest
                else {
                    stwWords.add(linefields[0]);
                    // all other fields get default values
                    this.stwWordsSource.add("");
                    this.stwWordsPenalty.add(0);
                    this.stwWordsCat.add("");
                    this.stwWordsMark.add("");
                    this.stwWordsFreq.add(0);
                }

            }
            if (this.debug) {
                System.out.println("rwWords read: " + stwWords);
            }
        }

        catch (Exception e) {
            // if an error occurred then throw an exception so that the user
            // knows
            e.printStackTrace();
            throw new IOException(
                    "Unable to read the stwword file: "
                            + e.getMessage());

        }

    }




}
