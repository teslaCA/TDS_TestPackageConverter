package tds.testpackageconverter.converter.impl;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import tds.testpackageconverter.converter.mappers.LegacyAdministrationTestPackageMapper;
import tds.testpackageconverter.converter.TestPackageConverterService;
import tds.testpackageconverter.converter.mappers.TestPackageMapper;
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
    private final XmlMapper legacyXmlMapper;
    private final XmlMapper testPackageMapper;

    @Autowired
    public TestPackageConverterServiceImpl(final TestPackageObjectMapperConfiguration testPackageObjectMapperConfiguration) {
        this.legacyXmlMapper = testPackageObjectMapperConfiguration.getLegacyTestSpecXmlMapper();
        this.testPackageMapper = testPackageObjectMapperConfiguration.getXmlMapper();
    }

    @Override
    public void extractAndConvertTestSpecifications(final String testPackageName, final File file) throws IOException, ParseException {
        ZipFile zipFile = new ZipFile(file);
        File convertedTestPackageFile = new File(testPackageName);

        List<Testspecification> specifications = zipFile.stream()
                .filter(entry -> !entry.isDirectory() && entry.getName().endsWith(".xml")
                        && !entry.getName().startsWith("__")) // Ignore __MACOSX folder if it exists
                .map(entry -> unzipAndRead(zipFile, entry))
                .collect(Collectors.toList());

        if (specifications.isEmpty()) {
            throw new IOException("No testspecification XML files were located within the zip file");
        }

        TestPackage testPackage = TestPackageMapper.toNew(testPackageName, specifications);
        legacyXmlMapper.writeValue(convertedTestPackageFile, testPackage);

        convertedTestPackageFile.createNewFile();
    }

    @Override
    public void convertTestSpecifications(final String testPackageName, final List<String> adminAndScoringFileNames) throws IOException, ParseException {
        convertTestSpecifications(testPackageName, adminAndScoringFileNames, null);
    }

    @Override
    public void convertTestSpecifications(final String testPackageName, final List<String> adminAndScoringFileNames,
                                          final String diffFileName) throws IOException, ParseException {
        File convertedTestPackageFile = new File(testPackageName);

        List<Testspecification> specifications = adminAndScoringFileNames.stream()
                .map(this::readTestSpecification)
                .collect(Collectors.toList());

        TestPackage testPackage = diffFileName == null
                ? TestPackageMapper.toNew(testPackageName, specifications)
                : TestPackageMapper.toNew(testPackageName, specifications); //TODO: Update this conditional to include the diff
        legacyXmlMapper.writeValue(convertedTestPackageFile, testPackage);

        convertedTestPackageFile.createNewFile();
    }

    @Override
    public void convertTestPackage(final String testPackagePath) {
        final TestPackage testPackage = readTestPackage(testPackagePath);
        final List<Testspecification> administrationPackages = LegacyAdministrationTestPackageMapper.fromNew(testPackage);

        administrationPackages.forEach(testSpecification -> {
            final String administrationOutputFilename = testSpecification.getIdentifier().getUniqueid() + ".xml";
            final File administrationFile = new File(administrationOutputFilename);
            try {
                legacyXmlMapper.writeValue(administrationFile, testSpecification);
                administrationFile.createNewFile();
                System.out.println("Successfully created the administration testspecification file " + administrationOutputFilename);
            } catch (IOException e) {
                log.error("An exception occurred while creating the file: {}", administrationOutputFilename, e);
                throw new RuntimeException(e);
            }
        });
    }

    private Testspecification readTestSpecification(final String filePath) {
        try {
            return legacyXmlMapper.readValue(new File(filePath), Testspecification.class);
        } catch (IOException e) {
            log.error("An exception occurred while reading the file: {}", filePath, e);
            throw new RuntimeException(e);
        }
    }

    private TestPackage readTestPackage(final String filePath) {
        try {
            return testPackageMapper.readValue(new File(filePath), TestPackage.class);
        } catch (IOException e) {
            log.error("An exception occurred while reading the file: {}", filePath, e);
            throw new RuntimeException(e);
        }
    }

    private Testspecification unzipAndRead(final ZipFile zipFile, final ZipEntry entry) {
        try {
            System.out.println("Reading file in zip: " + entry.getName());
            InputStream inputStream = zipFile.getInputStream(entry);
            return legacyXmlMapper.readValue(inputStream, Testspecification.class);
        } catch (IOException e) {
            log.error("An exception occurred: {}", e);
            throw new RuntimeException(e);
        }
    }
}
