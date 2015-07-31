package org.commcare.api.process;

import org.commcare.api.persistence.SqlSandbox;
import org.commcare.data.xml.DataModelPullParser;
import org.commcare.data.xml.TransactionParser;
import org.commcare.data.xml.TransactionParserFactory;
import org.commcare.xml.CaseXmlParser;
import org.commcare.xml.LedgerXmlParsers;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by wpride1 on 7/21/15.
 */
public class FormRecordProcessor {

    public static void processXML(SqlSandbox sandbox, String fileText) throws IOException, XmlPullParserException, UnfullfilledRequirementsException, InvalidStructureException {
        System.out.println("First: " + fileText);

        try {
            InputStream stream = new ByteArrayInputStream(fileText.getBytes(StandardCharsets.UTF_8));
            process(sandbox, stream);
        }catch(Exception e){
            System.out.println("e1: " + e);
            e.printStackTrace();
        }
    }

    public static void processFile(SqlSandbox sandbox, File record) throws IOException, XmlPullParserException, UnfullfilledRequirementsException, InvalidStructureException {
        System.out.println("Second: " + record);
        try {
            InputStream stream = new FileInputStream(record);
            process(sandbox, stream);
        }catch(Exception e){
            System.out.println("e2: " + e);
            e.printStackTrace();
        }
    }

    public static void process(SqlSandbox sandbox, InputStream stream) throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException, StorageFullException {
        try {
            final SqlSandbox mSandbox = sandbox;

            InputStream is = stream;

            DataModelPullParser parser = new DataModelPullParser(is, new TransactionParserFactory() {
                public TransactionParser getParser(KXmlParser parser) {
                    if (LedgerXmlParsers.STOCK_XML_NAMESPACE.equals(parser.getNamespace())) {
                        return new LedgerXmlParsers(parser, mSandbox.getLedgerStorage());
                    } else if ("case".equalsIgnoreCase(parser.getName())) {
                        return new CaseXmlParser(parser, mSandbox.getCaseStorage());
                    }
                    return null;
                }

            }, true, true);

            parser.parse();
            is.close();
        } catch(Exception e){
            System.out.println("e3: " + e);
            e.printStackTrace();
        }
    }
}
