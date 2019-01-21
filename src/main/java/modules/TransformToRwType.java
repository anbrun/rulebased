package modules;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.util.CasUtil;

import java.util.ArrayList;
import java.util.List;

public class TransformToRwType {

    public CAS process(CAS mainCas, String rwType) {
        Type stwrType = mainCas.getTypeSystem().getType("de.idsma.rw.Stwr");
        Type dirType = mainCas.getTypeSystem().getType("de.idsma.rw.rule.RuleDirect");

        //stwr Features
        Feature mediumFeat = stwrType.getFeatureByBaseName("Medium");
        Feature rTypeFeat = stwrType.getFeatureByBaseName("RType");
        Feature levelFeat = stwrType.getFeatureByBaseName("Level");
        Feature nonFactFeat = stwrType.getFeatureByBaseName("NonFact");
        Feature pragFeat = stwrType.getFeatureByBaseName("Prag");
        Feature borderFeat = stwrType.getFeatureByBaseName("Border");
        Feature metaphFeat = stwrType.getFeatureByBaseName("Metaph");
        Feature stwrFeat = stwrType.getFeatureByBaseName("Stwr");
        Feature stwrIDFeat = stwrType.getFeatureByBaseName("StwrID");
        Feature stwrNote = stwrType.getFeatureByBaseName("StwrNote");

        //create a list with all sentence annotations
        AnnotationIndex<AnnotationFS> annotationIndex = mainCas.getAnnotationIndex(dirType);
        FSIterator<AnnotationFS> iterator = annotationIndex.iterator();
        List<AnnotationFS> ruleDirList = new ArrayList<AnnotationFS>();
        while (iterator.hasNext()) {
            ruleDirList.add((AnnotationFS) iterator.next());
        }

        if (rwType.equals("direct")){
            int stwrID = 0;
            for (AnnotationFS ruleDirAnno : ruleDirList){
                AnnotationFS dirAnno = mainCas.createAnnotation(stwrType, ruleDirAnno.getBegin(), ruleDirAnno.getEnd());
                dirAnno.setFeatureValueFromString(mediumFeat, "speech");
                dirAnno.setFeatureValueFromString(rTypeFeat, "direct");
                dirAnno.setFeatureValueFromString(levelFeat, "1");
                dirAnno.setFeatureValueFromString(nonFactFeat, "");
                dirAnno.setFeatureValueFromString(borderFeat, "");
                dirAnno.setFeatureValueFromString(pragFeat, "");
                dirAnno.setFeatureValueFromString(metaphFeat, "");
                dirAnno.setFeatureValueFromString(stwrFeat, ruleDirAnno.getCoveredText());
                dirAnno.setFeatureValueFromString(stwrNote, "");
                dirAnno.setFeatureValueFromString(stwrIDFeat, String.valueOf(stwrID));
                stwrID++;
                mainCas.addFsToIndexes(dirAnno);
            }
        }
        return mainCas;
    }
}
