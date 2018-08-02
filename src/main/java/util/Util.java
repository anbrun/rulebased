package util;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

public class Util {

    public static CAS removeAllAnnotationsOfType(CAS mainCas, String typeName) {
        Type myType = mainCas.getTypeSystem().getType(typeName);
        // remove sent and text annotations if they exist already
        // (must create a list first, because uima does not allow removing annos
        // while iterating over the annotation index)
        AnnotationIndex<AnnotationFS> myTypeIndex = mainCas.getAnnotationIndex(myType);
        List<AnnotationFS> myList = new ArrayList<>();
        for (AnnotationFS anno : myTypeIndex) {
            myList.add(anno);
        }
        for (AnnotationFS anno : myList) {
            mainCas.removeFsFromIndexes(anno);
        }
        return mainCas;
    }


    public static TypeSystemDescription getStandardTypesystem(){

        URL urlTS = ClassLoader.getSystemClassLoader().getResource("redeWiedergabeTypesystem_compare_tei_cab.xml");
        TypeSystemDescription tsds = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(urlTS.toString());

        return tsds;
    }

    public static File getSTWWordList() throws URISyntaxException{
        URL urlSTWW = ClassLoader.getSystemClassLoader().getResource("mstwwords.csv");
        File file = null;
        file = new File(urlSTWW.toURI());
        return file;
    }

    public static URL getConjList() throws URISyntaxException{
        return ClassLoader.getSystemClassLoader().getResource("conjunctions.list");
    }

    public static URL getConjVerbsList() throws URISyntaxException{
        return ClassLoader.getSystemClassLoader().getResource("conj_verbs.list");
    }

    public static URL getFiWordsList() throws URISyntaxException{
        return ClassLoader.getSystemClassLoader().getResource("fiWords.txt");
    }

    public static URL getQuotationsMarksList() throws URISyntaxException{
        return ClassLoader.getSystemClassLoader().getResource("quotation_marks.list");
    }

    public static URL getPuncSpec() throws URISyntaxException{
        return ClassLoader.getSystemClassLoader().getResource("punc_spec.txt");
    }

    public static Set<String> readWordlistFromURL(URL fileUrl) throws IOException {
        Set<String> res = new HashSet<>();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                fileUrl.openStream(), "UTF8"));
        // read in the first line of the file
        String line = in.readLine();
        while (line != null) {
            res.add(line.trim());
            line = in.readLine();
        }
        return res;
    }
}