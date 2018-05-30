package tds.testpackageconverter.converter.impl;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import tds.testpackage.diff.TestPackageDiff;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;
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
        xmlMapper.writeValue(convertedTestPackageFile, testPackage);

        convertedTestPackageFile.createNewFile();
    }

    @Override
    public void convertTestSpecifications(final String testPackageName, final List<String> adminAndScoringFileNames) throws IOException, ParseException {
        convertTestSpecifications(testPackageName, adminAndScoringFileNames, Optional.empty());
    }

    @Override
    public void convertTestSpecifications(final String testPackageName, final List<String> adminAndScoringFileNames, final String diffFileName) throws IOException, ParseException {
        convertTestSpecifications(testPackageName, adminAndScoringFileNames, Optional.of(diffFileName));
    }

    private void convertTestSpecifications(final String testPackageName, final List<String> adminAndScoringFileNames,
                                          final Optional<String> diffFileName) throws IOException, ParseException {
        final File convertedTestPackageFile = new File(testPackageName);

        final List<Testspecification> specifications = adminAndScoringFileNames.stream()
            .map(this::read)
            .collect(Collectors.toList());
        final Optional<TestPackageDiff> diff = diffFileName.map(this::readDiff);

        final TestPackage testPackage = TestPackageMapper.toNew(testPackageName, specifications, diff);
        xmlMapper.writeValue(convertedTestPackageFile, testPackage);

        convertedTestPackageFile.createNewFile();
    }


    @Override
    public void convertTestPackage(final TestPackage testPackage) {
        List<Testspecification> administrationPackages = LegacyAdministrationTestPackageMapper.fromNew(testPackage);
    }

    private TestPackageDiff readDiff(final String fileName) {
        try {
            return xmlMapper.readValue(new File(fileName), TestPackageDiff.class);
        } catch (IOException e) {
            log.error("An exception occurred while reading the file: {}", fileName, e);
            throw new RuntimeException(e);
        }
    }


    private Testspecification read(final String fileName) {
        try {
            return xmlMapper.readValue(new File(fileName), Testspecification.class);
        } catch (IOException e) {
            log.error("An exception occurred while reading the file: {}", fileName, e);
            throw new RuntimeException(e);
        }
    }

    private Testspecification unzipAndRead(final ZipFile zipFile, final ZipEntry entry) {
        try {
            System.out.println("Reading file in zip: " + entry.getName());
            InputStream inputStream = zipFile.getInputStream(entry);
            return xmlMapper.readValue(inputStream, Testspecification.class);
        } catch (IOException e) {
            log.error("An exception occurred: {}", e);
            throw new RuntimeException(e);
        }
    }
}
