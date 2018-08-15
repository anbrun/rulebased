package modules;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;

import java.lang.annotation.Annotation;
import java.util.*;

public class ExtractNounGenitive {

    public List<String> extract(CAS mainCas) {
        List<String> reslist = new ArrayList<>();

        Type cabTokenType = mainCas.getTypeSystem().getType("de.idsma.rw.CabToken");
        Iterator<AnnotationFS> tokiter = CasUtil.iterator(mainCas, cabTokenType);
        Feature cabRfPos = cabTokenType.getFeatureByBaseName("RfPos");

        List<String> collect = new ArrayList<>();
        int stage = 0;
        while (tokiter.hasNext()) {
            AnnotationFS tok = tokiter.next();
            String rfPos = tok.getFeatureValueAsString(cabRfPos);
            //System.out.println("stage " + stage + ": " + rfPos);
            if (rfPos != null) {
                if (stage == 0 && rfPos.matches("^N\\..*")) {
                    collect.add(tok.getCoveredText());
                    stage = 1;
                } else if (stage == 1) {
                    if (rfPos.matches("^(POS|ART).*\\.Gen\\..*")) {
                        collect.add(tok.getCoveredText());
                        stage = 2;
                    } else {
                        // reset
                        collect = new ArrayList<>();
                        stage = 0;
                    }
                } else if (stage == 2) {
                    if (rfPos.matches("^N\\..*\\.Gen\\..*")) {
                        collect.add(tok.getCoveredText());
                        reslist.add(String.join(" ", collect));
                        // reset
                        collect = new ArrayList<>();
                        stage = 0;
                    } else {
                        // reset
                        collect = new ArrayList<>();
                        stage = 0;
                    }
                }
            }
        }
        return reslist;
    }
}
