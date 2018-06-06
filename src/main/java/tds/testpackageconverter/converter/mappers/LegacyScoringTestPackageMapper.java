package tds.testpackageconverter.converter.mappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tds.testpackage.legacy.model.*;
import tds.testpackage.model.BlueprintElement;
import tds.testpackage.model.TestPackage;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static tds.testpackageconverter.utils.TestPackageUtils.formatDate;

public class LegacyScoringTestPackageMapper {
    private static final Logger log = LoggerFactory.getLogger(LegacyScoringTestPackageMapper.class);

    public static Testspecification fromNew(final TestPackage testPackage,
                                            final List<Testspecification> administrationPackages) {
        final BigDecimal version = new BigDecimal(testPackage.getVersion());
        final Testspecification scoringSpecification = new Testspecification();
        scoringSpecification.setPurpose("administration");
        scoringSpecification.setPublisher(testPackage.getPublisher());
        try {
            scoringSpecification.setPublishdate(formatDate(Instant.parse(testPackage.getPublishDate())));
        } catch (ParseException e) {
            log.warn("Unable to parse publish date time. Adding date without parsing");
            scoringSpecification.setPublishdate(testPackage.getPublishDate());
        }
        scoringSpecification.setVersion(new BigDecimal(1));

        // Map Identifier
        final Identifier testSpecIdentifier = new Identifier();
        testSpecIdentifier.setUniqueid(String.format("(%s)%s-%s", testPackage.getPublisher(),
                testPackage.getId(), testPackage.getAcademicYear()));
        testSpecIdentifier.setName(testPackage.getId());
        testSpecIdentifier.setVersion(version);
        testSpecIdentifier.setLabel(testPackage.getId());
        scoringSpecification.setIdentifier(testSpecIdentifier);

        // Map test spec properties
        final List<Property> properties = scoringSpecification.getProperty();
        // Subject
        final Property subjectProperty = new Property();
        subjectProperty.setName("subject");
        subjectProperty.setValue(testPackage.getSubject());
        subjectProperty.setLabel(testPackage.getSubject());
        properties.add(subjectProperty);

        // Test type
        final Property typeProperty = new Property();
        typeProperty.setName("type");
        typeProperty.setValue(testPackage.getType());
        typeProperty.setLabel(testPackage.getType());
        properties.add(typeProperty);

        //Grade(s)
        testPackage.getAssessments().forEach(assessment -> assessment.getGrades()
                .stream().peek(grade -> {
                    Property gradeProperty = new Property();
                    gradeProperty.setName("grade");
                    gradeProperty.setValue(grade.getValue());
                    gradeProperty.setLabel(grade.getValue());
                    properties.add(gradeProperty);
                })
        );

        scoringSpecification.setScoring(mapScoring(testPackage, administrationPackages));

        return scoringSpecification;
    }

    private static Scoring mapScoring(final TestPackage testPackage,
                                      final List<Testspecification> administrationPackages) {
        final Scoring scoring = new Scoring();
        scoring.setTestblueprint(LegacyScoringTestPackageBlueprintMapper.mapBlueprint(testPackage, administrationPackages));
        scoring.setItempool(LegacyScoringTestPackageItemPoolMapper.mapItemPool(testPackage, administrationPackages));
        scoring.setPerformancelevels(mapPerformanceLevels(testPackage));
        scoring.getTestform().addAll(LegacyScoringTestPackageFormMapper.mapTestForms(testPackage, administrationPackages));
        return scoring;
    }

    private static Performancelevels mapPerformanceLevels(final TestPackage testPackage) {
        final Map<String, BlueprintElement> blueprintMap = testPackage.getBlueprintMap();

        final Performancelevels levels = new Performancelevels();
        blueprintMap.values().stream()
                .filter(blueprintElement -> blueprintElement.getScoring().isPresent())
                .forEach(blueprintElement -> blueprintElement.getScoring().get().performanceLevels()
                        .forEach(pl -> {
                            final Performancelevel level = new Performancelevel();
                            level.setBpelementid(blueprintElement.getId()); //TODO: Check if this should be key
                            level.setPlevel(BigInteger.valueOf(pl.getPLevel()));
                            level.setScaledlo((float)pl.getScaledLo());
                            level.setScaledhi((float)pl.getScaledHi());
                            levels.getPerformancelevel().add(level);
                        }));
    }


}
