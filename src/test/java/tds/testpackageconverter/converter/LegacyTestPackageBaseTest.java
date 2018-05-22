package tds.testpackageconverter.converter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.junit.Before;
import tds.support.tool.testpackage.configuration.TestPackageObjectMapperConfiguration;
import tds.testpackage.legacy.model.Testspecification;
import tds.testpackage.model.TestPackage;

import java.io.IOException;

public class LegacyTestPackageBaseTest {
    protected TestPackage mockPerfAdminLegacyTestPackage;

    protected TestPackage mockCATAdminLegacyTestPackage;

    protected TestPackage mockCombinedAdministrationPackage;

    protected TestPackage mockFixedMultiSegmentPackage;

    protected XmlMapper xmlMapper;

    @Before
    public void deserializeTestPackages() throws IOException {
        XmlMapper xmlMapper = new TestPackageObjectMapperConfiguration().getXmlMapper();
        mockPerfAdminLegacyTestPackage = xmlMapper.readValue(this.getClass().getResourceAsStream("/IRP-Perf-MATH-11-2015-2016.xml"),
                TestPackage.class);
        mockCATAdminLegacyTestPackage = xmlMapper.readValue(this.getClass().getResourceAsStream("/IRP-CAT-MATH-11-2015-2016.xml"),
                TestPackage.class);
        mockCombinedAdministrationPackage = xmlMapper.readValue(this.getClass().getResourceAsStream("/IRP-COMBINED-MATH-11-2015-2016-NoScoring.xml"),
                TestPackage.class);
        mockFixedMultiSegmentPackage = xmlMapper.readValue(this.getClass().getResourceAsStream("/Practice-Fixed-MATH-6-Fall-2017-2018.xml"),
                TestPackage.class);
    }

}
