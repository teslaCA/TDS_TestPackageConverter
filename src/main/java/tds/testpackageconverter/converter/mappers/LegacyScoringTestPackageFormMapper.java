package tds.testpackageconverter.converter.mappers;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tds.testpackage.legacy.model.*;
import tds.testpackage.model.TestPackage;
import tds.testpackageconverter.utils.TestPackageUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LegacyScoringTestPackageFormMapper {

    public static List<Testform> mapTestForms(final TestPackage testPackage,
                                              final List<Testspecification> administrationPackages) {
        final Map<String, String> assessmentBpElements = administrationPackages.stream()
                .flatMap(adminPackage -> adminPackage.getAdministration().getTestblueprint().getBpelement().stream()
                        .filter(bpEl -> bpEl.getElementtype().equals("test")))
                .collect(Collectors.toMap(
                        bpEl -> bpEl.getIdentifier().getUniqueid(),
                        bpEl -> TestPackageUtils.getCombinedKey(testPackage, bpEl.getIdentifier().getName())));

        if (administrationPackages.size() == 1) {
            return administrationPackages.get(0).getAdministration().getTestform().stream()
                    .map(form -> replaceFormKeys(form, assessmentBpElements))
                    .collect(Collectors.toList());
        } else {
            return mergeCombinedTestForms(testPackage, administrationPackages); // Combine the two passage lists
        }
    }

    private static List<Testform> mergeCombinedTestForms(final TestPackage testPackage,
                                                         final List<Testspecification> administrationPackages) {
        final List<Testform> combinedForms = new ArrayList<>();

        // language -> formpartitions map
        final Multimap<String, Formpartition> formPartitionsByCohortLanguage = ArrayListMultimap.create();
        administrationPackages
                .forEach(admin -> admin.getAdministration().getTestform()
                        .forEach(testForm -> {
                            formPartitionsByCohortLanguage.putAll(parseCohortLanguage(testForm.getIdentifier().getUniqueid()),
                                    testForm.getFormpartition());
                        })
                );

        formPartitionsByCohortLanguage.asMap().forEach((cohortLanguage, formPartitions) -> {
            final Testform combinedTestForm = new Testform();
            long formLength = formPartitions.stream()
                    .mapToLong(formPartition -> formPartition.getItemgroup().stream()
                            .map(Itemgroup::getGroupitem)
                            .collect(Collectors.toList())
                            .size())
                    .sum();

            combinedTestForm.setLength(BigInteger.valueOf(formLength));
            final String formKey = String.format("%s:%s", TestPackageUtils.getAssessmentKey(testPackage, testPackage.getId()),
                    cohortLanguage);
            final Identifier identifier = new Identifier();
            identifier.setUniqueid(formKey);
            identifier.setName(formKey);
            identifier.setVersion(new BigDecimal(testPackage.getVersion()));
            combinedTestForm.setIdentifier(identifier);

            // Modify the form keys/id
            for (Formpartition formPartition : formPartitions) {
                // Generate a new form key - apparently this cannot be the same as the original "forms" and needs to be
                // manually mapped in the TIS database
                formPartition.getIdentifier().setUniqueid(String.format("%s-%s", testPackage.getBankKey(),
                        TestPackageUtils.generateFormKey()));
                formPartition.getIdentifier().setName(formPartition.getIdentifier().getName() + " COMBINED");
            }

            combinedTestForm.getFormpartition().addAll(formPartitions);
            combinedForms.add(combinedTestForm);
        });

        return combinedForms;
    }

    private static String parseCohortLanguage(final String formKey) {
        // Example: "(SBAC)SBAC-ICA-FIXED-G7E-COMBINED-2017-Winter-2016-2017:Default-ENU" should return "Default-ENU"
        return formKey.split(":")[1];
    }

    private static Testform replaceFormKeys(final Testform form, final Map<String, String> assessmentBpElements) {
        final String parsedSegmentKey = form.getIdentifier().getName().split(":")[0];
        final String newFormKey = form.getIdentifier().getName()
                .replace(parsedSegmentKey, assessmentBpElements.get(parsedSegmentKey));
        form.getIdentifier().setName(newFormKey);

        return form;
    }
}
