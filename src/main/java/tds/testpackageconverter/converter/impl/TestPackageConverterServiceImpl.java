package tds.testpackageconverter.converter.impl;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import tds.testpackageconverter.converter.TestPackageConverterService;
import tds.testpackageconverter.converter.TestPackageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tds.support.tool.testpackage.configuration.TestPackageObjectMapperConfiguration;
import tds.testpackage.legacy.model.Testspecification;
import tds.testpackage.model.TestPackage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class TestPackageConverterServiceImpl implements TestPackageConverterService {
    private final Logger log = LoggerFactory.getLogger(TestPackageConverterServiceImpl.class);
    private final XmlMapper xmlMapper;

    @Autowired
    public TestPackageConverterServiceImpl(final TestPackageObjectMapperConfiguration testPackageObjectMapperConfiguration) {
        this.xmlMapper = testPackageObjectMapperConfiguration.getLegacyTestSpecXmlMapper();
    }

    @Override
    public String extractAndConvertTestSpecifications(final String testPackageName, final File file) throws IOException, ParseException {
        ZipFile zipFile = new ZipFile(file);

        List<Testspecification> specifications = zipFile.stream()
                .filter(entry -> !entry.isDirectory() && entry.getName().endsWith(".xml")
                        && !entry.getName().startsWith("__")) // Ignore __MACOSX folder if it exists
                .map(entry -> unzipAndRead(zipFile, entry))
                .collect(Collectors.toList());

        if (specifications.isEmpty()) {
            throw new IOException("No testspecification XML files were located within the zip file");
        }

        TestPackage testPackage = TestPackageMapper.toNew(testPackageName, specifications);
        return xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(testPackage);
    }

    private Testspecification unzipAndRead(final ZipFile zipFile, final ZipEntry entry) {
        try {
            InputStream inputStream = zipFile.getInputStream(entry);
            return xmlMapper.readValue(inputStream, Testspecification.class);
        } catch (IOException e) {
            log.warn("An exception occurred: {}", e);
            throw new RuntimeException(e);
        }
    }
}
