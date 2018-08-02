import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import util.Util;
import modules.*;

public class Main {

    public void ruleBasedPipeline(File inFolder, File outFolder)
            throws ResourceInitializationException, SAXException, IOException, URISyntaxException {

        TypeSystemDescription tsd = Util.getStandardTypesystem();

        if (!inFolder.isDirectory() || !outFolder.isDirectory()) {
            System.out.println("infolder: " + inFolder + " outfolder: " + outFolder);
            throw new IllegalArgumentException("Please provide 2 folders");
        }

        // initialize the rule-based components that should be used
//        MarkSTWWords markSTWWords = new MarkSTWWords();
//        MarkIndirect markIndirect = new MarkIndirect();
//        MarkFreeIndirect markFreeIndirect = new MarkFreeIndirect();
//        MarkDirect markDirect = new MarkDirect();
//        MarkFrame markFrame = new MarkFrame();

        MarkDirect2 markDirect2 = new MarkDirect2();

        for (File f : inFolder.listFiles()) {
            if (!f.getName().endsWith(".xmi"))
                continue;
            System.out.println("Processing: " + f.getName());
            CAS mainCas = CasCreationUtils.createCas(tsd, null, null);
            // will remove any annotation that is not part of the RWTypesystem
            XmiCasDeserializer.deserialize(new FileInputStream(f), mainCas, true);

            //mainCas = markSTWWords.process(mainCas);
            //mainCas = markFrame.process(mainCas);
            //mainCas = markIndirect.process(mainCas);
            //mainCas = markFreeIndirect.process(mainCas);
            mainCas = markDirect2.process(mainCas);

            FileOutputStream outStream = new FileOutputStream(new File(outFolder + "/" + f.getName()));
            XmiCasSerializer.serialize(mainCas, outStream);
        }
    }

    public static void main(String[] args) {
        //String infolder = "E:\\Git_RW\\myrepo\\7_final\\final\\alle_metaclean_preproc";
        //String outfolder = "E:\\Git_RW\\myrepo\\7_final\\final\\alle_rulebased_newdirect";

        String infolder = "E:\\Git_RW\\myrepo\\7_final\\final\\test_preproc";
        String outfolder = "E:\\Git_RW\\myrepo\\7_final\\final\\test_rulebased";

        Main main = new Main();
        try {
            main.ruleBasedPipeline(new File(infolder), new File(outfolder));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
