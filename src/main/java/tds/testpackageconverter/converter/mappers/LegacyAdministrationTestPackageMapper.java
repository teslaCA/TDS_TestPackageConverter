package tds.testpackageconverter.converter.mappers;

import tds.testpackage.legacy.model.Administration;
import tds.testpackage.legacy.model.Identifier;
import tds.testpackage.legacy.model.Property;
import tds.testpackage.legacy.model.Testspecification;
import tds.testpackage.model.Assessment;
import tds.testpackage.model.TestPackage;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class LegacyAdministrationTestPackageMapper {
    public static List<Testspecification> fromNew(final TestPackage testPackage) {
        return testPackage.getAssessments().stream()
                .map(assessment -> mapTestSpecification(testPackage, assessment))
                .collect(Collectors.toList());
    }

    private static Testspecification mapTestSpecification(final TestPackage testPackage, final Assessment assessment) {
        final BigDecimal version = new BigDecimal(testPackage.getVersion());
        final Testspecification testSpecification = new Testspecification();
        testSpecification.setPurpose("administration");
        testSpecification.setPublisher(testPackage.getPublisher());
        testSpecification.setPublishdate(testPackage.getPublishDate());
        testSpecification.setVersion(version);

        // Map Identifier
        final Identifier testSpecIdentifier = new Identifier();
        testSpecIdentifier.setUniqueid(assessment.getKey());
        testSpecIdentifier.setName(assessment.getId());
        testSpecIdentifier.setVersion(version);
        testSpecIdentifier.setLabel(assessment.getLabel());
        testSpecification.setIdentifier(testSpecIdentifier);

        // Map test spec properties
        final List<Property> properties = testSpecification.getProperty();
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
        assessment.getGrades().forEach(grade -> {
            Property gradeProperty = new Property();
            gradeProperty.setName("grade");
            gradeProperty.setValue(grade.getValue());
            gradeProperty.setLabel(grade.getValue());
            properties.add(gradeProperty);
        });

        testSpecification.setAdministration(mapAdministration(testPackage, assessment));

        return testSpecification;
    }

    private static Administration mapAdministration(final TestPackage testPackage, final Assessment assessment) {
        final Map<String, Long> formIdToKeyMap = generateFormKeys(assessment);

        final Administration administration = new Administration();
        administration.setTestblueprint(LegacyAdministrationTestPackageBlueprintMapper.mapBlueprint(assessment, testPackage));
        administration.setItempool(LegacyAdministrationTestPackageItemPoolMapper.mapItemPool(testPackage, assessment));
        administration.getTestform().addAll(LegacyAdministrationTestPackageFormMapper.mapTestForms(assessment,
                formIdToKeyMap, testPackage.getVersion(), testPackage.getBankKey()));
        administration.getAdminsegment().addAll(LegacyAdministrationTestPackageSegmentMapper.mapAdminSegments(testPackage,
                assessment, formIdToKeyMap));
        return administration;
    }

    private static Map<String,Long> generateFormKeys(final Assessment assessment) {
        final Map<String, Long> formKeyMap = new HashMap<>();
        // Down cast the auto-generated long to an int so its a bit more human readable;
        final int generatedKey = (int) UUID.randomUUID().getMostSignificantBits();
        assessment.getSegments()
                .forEach(segment -> segment.segmentForms()
                        .forEach(form -> formKeyMap.put(form.getId(), (long) generatedKey))
                );
        return formKeyMap;
    }
}