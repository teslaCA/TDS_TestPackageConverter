package tds.testpackageconverter.converter.mappers;

import org.assertj.core.util.Lists;
import tds.common.Algorithm;
import tds.testpackage.legacy.model.Bpelement;
import tds.testpackage.legacy.model.Identifier;
import tds.testpackage.legacy.model.Testblueprint;
import tds.testpackage.model.*;
import tds.testpackageconverter.converter.BlueprintElementCounts;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static tds.testpackageconverter.utils.TestPackageUtils.getBlueprintKeyFromId;

public class LegacyAdministrationTestPackageBlueprintMapper {
    public static Testblueprint mapBlueprint(final Assessment assessment, final TestPackage testPackage) {
        // Get the access counts for each blueprint element
        final Map<String, BlueprintElementCounts> blueprintElementReferenceCount = countBlueprintElementReferences(assessment);
        // Group the segment blueprint elements, containing total counts of exam and field test items
        // In cases where there are multiple segments that reference the same blueprint element, we will have multiple values
        final Map<String, List<SegmentBlueprintElement>> segBpElMap = assessment.getSegments().stream()
                .flatMap(segment -> segment.segmentBlueprint().stream())
                .collect(Collectors.groupingBy(SegmentBlueprintElement::getIdRef));

        final Testblueprint testBlueprint = new Testblueprint();
        final List<Bpelement> bpElements = testBlueprint.getBpelement();

        // Map test and segment blueprints
        final List<SegmentBlueprintElement> testBpEls = segBpElMap.get(assessment.getId());
        final BlueprintElementCounts counts = blueprintElementReferenceCount.get(assessment.getId());
        final Bpelement testBpElement = new Bpelement();
        testBpElement.setElementtype("test");
        testBpElement.setMinftitems(BigInteger.valueOf(testBpEls.stream().mapToInt(SegmentBlueprintElement::minFieldTestItems).sum()));
        testBpElement.setMaxftitems(BigInteger.valueOf(testBpEls.stream().mapToInt(SegmentBlueprintElement::maxFieldTestItems).sum()));
        testBpElement.setMinopitems(BigInteger.valueOf(testBpEls.stream().mapToInt(SegmentBlueprintElement::getMinExamItems).sum()));
        testBpElement.setMaxopitems(BigInteger.valueOf(testBpEls.stream().mapToInt(SegmentBlueprintElement::getMaxExamItems).sum()));
        testBpElement.setOpitemcount(BigInteger.valueOf(counts.getExamItemCount()));
        testBpElement.setFtitemcount(BigInteger.valueOf(counts.getFieldTestItemCount()));

        final Identifier testBpElIdentifier = new Identifier();
        testBpElIdentifier.setName(assessment.getId());
        testBpElIdentifier.setUniqueid(assessment.getKey());
        testBpElIdentifier.setVersion(new BigDecimal(testPackage.getVersion()));
        testBpElement.setIdentifier(testBpElIdentifier);

        bpElements.add(testBpElement);

        for (Segment segment : assessment.getSegments()) {
            final List<SegmentBlueprintElement> segmentBpEls = segBpElMap.get(segment.getId());
            final BlueprintElementCounts segmentCounts = blueprintElementReferenceCount.get(segment.getId());

            final Bpelement segmentBpElement = new Bpelement();
            segmentBpElement.setElementtype("segment");
            segmentBpElement.setMinftitems(BigInteger.valueOf(segmentBpEls.stream().mapToInt(SegmentBlueprintElement::minFieldTestItems).sum()));
            segmentBpElement.setMaxftitems(BigInteger.valueOf(segmentBpEls.stream().mapToInt(SegmentBlueprintElement::maxFieldTestItems).sum()));
            segmentBpElement.setMinopitems(BigInteger.valueOf(segmentBpEls.stream().mapToInt(SegmentBlueprintElement::getMinExamItems).sum()));
            segmentBpElement.setMaxopitems(BigInteger.valueOf(segmentBpEls.stream().mapToInt(SegmentBlueprintElement::getMaxExamItems).sum()));
            segmentBpElement.setOpitemcount(BigInteger.valueOf(segmentCounts.getExamItemCount()));
            segmentBpElement.setFtitemcount(BigInteger.valueOf(segmentCounts.getFieldTestItemCount()));

            final Identifier segmentBpElIdentifier = new Identifier();
            segmentBpElIdentifier.setName(segment.getId());
            segmentBpElIdentifier.setUniqueid(segment.getKey());
            segmentBpElIdentifier.setVersion(new BigDecimal(testPackage.getVersion()));
            segmentBpElement.setIdentifier(segmentBpElIdentifier);

            bpElements.add(segmentBpElement);
        }

        bpElements.addAll(mapStrandAndContentLevelBlueprints(testPackage, segBpElMap, blueprintElementReferenceCount));
        return testBlueprint;
    }

    private static Map<String, BlueprintElementCounts> countBlueprintElementReferences(final Assessment assessment) {
        final Map<String, BlueprintElementCounts> blueprintElementCountsMap = new HashMap<>();

        // Get a flat list of all the items
        final List<Item> items = assessment.getSegments().stream()
                .flatMap(segment -> {
                    if (segment.getAlgorithmType().equalsIgnoreCase(Algorithm.FIXED_FORM.getType())) {
                        return segment.segmentForms().stream()
                                .flatMap(form -> form.itemGroups().stream()
                                        .flatMap(itemGroup -> itemGroup.items().stream()
                                        )
                                );
                    } else {
                        return segment.pool().stream()
                                .flatMap(itemGroup -> itemGroup.items().stream());
                    }
                })
                .collect(Collectors.toList());

        // Go through each of the items and increment their respective reference counts
        items.forEach(item -> item.getBlueprintReferences()
                .forEach(ref -> {
                    if (!blueprintElementCountsMap.containsKey(ref.getIdRef())) {
                        blueprintElementCountsMap.put(ref.getIdRef(), new BlueprintElementCounts(ref.getIdRef()));
                    } else {
                        final BlueprintElementCounts counts = blueprintElementCountsMap.get(ref.getIdRef());
                        if (item.fieldTest()) {
                            counts.incrementFieldTestItemCount();
                        } else {
                            counts.incrementExamItemCount();
                        }
                    }
                })
        );

        return blueprintElementCountsMap;
    }

    private static List<Bpelement> mapStrandAndContentLevelBlueprints(final TestPackage testPackage,
                                                                      final Map<String, List<SegmentBlueprintElement>> segmentBlueprintElementMap,
                                                                      final Map<String, BlueprintElementCounts> blueprintElementReferenceCount) {
        final List<Bpelement> bpElements = new ArrayList<>();
        final List<BlueprintElement> allBlueprintElements = Lists.newArrayList(testPackage.getBlueprintMap().values());

        for (BlueprintElement blueprintElement : allBlueprintElements) {
            // Skip non-claim/target blueprint elements
            if (!BlueprintElementTypes.CLAIM_AND_TARGET_TYPES.contains(blueprintElement.getType())) {
                continue;
            }

            final List<SegmentBlueprintElement> segmentBpEls = segmentBlueprintElementMap.get(blueprintElement.getId());
            final BlueprintElementCounts segmentCounts = blueprintElementReferenceCount.get(blueprintElement.getId());

            final Bpelement bpElement = new Bpelement();
            bpElement.setElementtype(blueprintElement.getType().equalsIgnoreCase(BlueprintElementTypes.CLAIM)
                    ? BlueprintElementTypes.STRAND
                    : BlueprintElementTypes.CONTENT_LEVEL);
            bpElement.setMinftitems(BigInteger.valueOf(segmentBpEls.stream().mapToInt(SegmentBlueprintElement::minFieldTestItems).sum()));
            bpElement.setMaxftitems(BigInteger.valueOf(segmentBpEls.stream().mapToInt(SegmentBlueprintElement::maxFieldTestItems).sum()));
            bpElement.setMinopitems(BigInteger.valueOf(segmentBpEls.stream().mapToInt(SegmentBlueprintElement::getMinExamItems).sum()));
            bpElement.setMaxopitems(BigInteger.valueOf(segmentBpEls.stream().mapToInt(SegmentBlueprintElement::getMaxExamItems).sum()));
            bpElement.setOpitemcount(BigInteger.valueOf(segmentCounts.getExamItemCount()));
            bpElement.setFtitemcount(BigInteger.valueOf(segmentCounts.getFieldTestItemCount()));

            final Identifier bpElementIdentifier = new Identifier();
            bpElementIdentifier.setName(blueprintElement.getId());
            bpElementIdentifier.setUniqueid(getBlueprintKeyFromId(blueprintElement, testPackage.getPublisher()));
            bpElementIdentifier.setVersion(new BigDecimal(testPackage.getVersion()));
            bpElement.setIdentifier(bpElementIdentifier);
        }

        return bpElements;
    }

}
