package tds.testpackageconverter.converter.mappers;

import tds.testpackage.legacy.model.Testform;
import tds.testpackage.legacy.model.Testspecification;
import tds.testpackage.model.TestPackage;
import tds.testpackageconverter.utils.TestPackageUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LegacyScoringTestPackageFormMapper {
    public static List<Testform> mapTestForms(final TestPackage testPackage,
                                              final List<Testspecification> administrationPackages) {
        final Map<String, String> segmentBpElements = administrationPackages.stream()
                .flatMap(adminPackage -> adminPackage.getAdministration().getTestblueprint().getBpelement().stream()
                        .filter(bpEl -> bpEl.getElementtype().equals("segment") || bpEl.getElementtype().equals("test")))
                .collect(Collectors.toMap(
                        bpEl -> bpEl.getIdentifier().getUniqueid(),
                        bpEl -> TestPackageUtils.getCombinedKey(testPackage, bpEl.getIdentifier().getName())));

        if (administrationPackages.size() == 1) {
            return administrationPackages.get(0).getAdministration().getTestform().stream()
                    .map(form -> replaceFormKeys(form, segmentBpElements))
                    .collect(Collectors.toList());
        } else {
            return Stream.concat(
                    administrationPackages.get(0).getAdministration().getTestform().stream()
                            .map(form -> replaceFormKeys(form, segmentBpElements)),
                    administrationPackages.get(1).getAdministration().getTestform().stream()
                            .map(form -> replaceFormKeys(form, segmentBpElements)))
                    .collect(Collectors.toList()); // Combine the two passage lists
        }
    }

    private static Testform replaceFormKeys(final Testform form, final Map<String, String> segmentBpElements) {
        final String parsedSegmentKey = form.getIdentifier().getName().split(":")[0];
        final String newFormKey = form.getIdentifier().getName()
                .replace(parsedSegmentKey, segmentBpElements.get(parsedSegmentKey));
        form.getIdentifier().setName(newFormKey);

        return form;
    }
}
