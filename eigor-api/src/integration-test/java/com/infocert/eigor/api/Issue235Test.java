package com.infocert.eigor.api;

import com.google.common.base.Preconditions;
import it.infocert.eigor.api.configuration.ConfigurationException;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class Issue235Test {

    @ClassRule
    public static TemporaryFolder tmp = new TemporaryFolder();
    private static ConversionUtil conversion;
    private static EigorApi api = null;
    private File ublInvoice;

    @Parameterized.Parameters(name= "{index}: {1}")
    public static List<Object[]> data() {
        File folderWithExamples = new File(Preconditions.checkNotNull(Issue235Test.class.getResource("/issues/235")).getFile());
        assertTrue( folderWithExamples.exists() );
        assertTrue( folderWithExamples.isDirectory() );

        File[] ublInvoices = Arrays.stream(folderWithExamples.listFiles()).filter( f -> f.isFile() ).sorted().toArray(File[]::new);

        assertTrue( ublInvoices.length >= 1 );

        ArrayList<Object[]> list = new ArrayList<>();
        for (File ublInvoice : ublInvoices) {
            list.add( new Object[]{
                    ublInvoice,
                    ublInvoice.getName()
            });
        }


        return list;
    }

    public Issue235Test(File ublInvoice, String fileName) {
        this.ublInvoice = ublInvoice;
    }

    @BeforeClass
    public static void initApi() throws IOException, ConfigurationException {

        api = new EigorApiBuilder()
                .enableAutoCopy()
                .withOutputFolder(tmp.newFolder())
                .enableForce()
                .build();

        conversion = new ConversionUtil(api);
    }

    @Test
    public void test() {
        conversion.assertConversionWithoutErrors( "/issues/235/" + ublInvoice.getName(), "ubl", "fatturapa" );
    }


}
