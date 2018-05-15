package tds.testpackageconverter.converter.mappers;

import tds.common.Algorithm;
import tds.testpackage.legacy.model.*;
import tds.testpackage.legacy.model.Property;
import tds.testpackage.model.*;
import tds.testpackageconverter.utils.TestPackageUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LegacyAdministrationTestPackageSegmentMapper {
    public static List<Adminsegment> mapAdminSegments(final TestPackage testPackage, final Assessment assessment,
                                                      final Map<String, Long> formIdToKeyMap) {
        final List<Adminsegment> adminSegments = new ArrayList<>();

        for (Segment segment : assessment.getSegments()) {
            final Adminsegment adminSegment = new Adminsegment();
            adminSegment.setSegmentid(segment.getKey());
            adminSegment.setPosition(String.valueOf(segment.position()));
            adminSegment.setItemselection(segment.getAlgorithmType());

            final List<Segmentbpelement> segmentBpElements = adminSegment.getSegmentblueprint().getSegmentbpelement();

            segment.segmentBlueprint().forEach(segmentBlueprintElement -> {
                final Segmentbpelement segmentBpEl = new Segmentbpelement();
                segmentBpEl.setBpelementid(segmentBlueprintElement.getIdRef());
                segmentBpEl.setMinopitems(BigInteger.valueOf(segmentBlueprintElement.getMinExamItems()));
                segmentBpEl.setMaxopitems(BigInteger.valueOf(segmentBlueprintElement.getMaxExamItems()));
                segmentBpEl.setMinftitems(BigInteger.valueOf(segmentBlueprintElement.minFieldTestItems()));
                segmentBpEl.setMaxftitems(BigInteger.valueOf(segmentBlueprintElement.maxFieldTestItems()));
                segmentBpElements.add(segmentBpEl);
            });

            adminSegment.setItemselector(mapItemSelector(testPackage, segment));

            if (segment.getAlgorithmType().equalsIgnoreCase(Algorithm.FIXED_FORM.getType())) {
                final List<Segmentform> segmentForms = segment.segmentForms().stream()
                        .map(segmentForm -> {
                            final Segmentform form = new Segmentform();
                            form.setFormpartitionid(String.format("%s-%s", testPackage.getBankKey(), formIdToKeyMap.get(segmentForm.getId())));
                            return form;
                        })
                        .collect(Collectors.toList());

                adminSegment.getSegmentform().addAll(segmentForms);

            } else { // Adaptive
                final Segmentpool segmentPool = adminSegment.getSegmentpool();
                segmentPool.getItemgroup().addAll(mapItemGroups(segment, testPackage.getBankKey(), testPackage.getVersion()));
            }

            adminSegments.add(adminSegment);
        }

        return adminSegments;
    }

    private static List<Itemgroup> mapItemGroups(final Segment segment, final int bankKey, final String version) {
        final List<Itemgroup> legacyItemGroups = new ArrayList<>();

        for (ItemGroup itemGroup : segment.pool()) {
            final Itemgroup legacyItemGroup = new Itemgroup();
            legacyItemGroup.setMaxitems(itemGroup.maxItems());
            legacyItemGroup.setMaxresponses(itemGroup.maxResponses());

            final Identifier itemGroupIdentifier = new Identifier();

            final String itemGroupId = itemGroup.items().size() > 1
                    ? String.format("G-%s-%s-0", bankKey, itemGroup.getId())
                    : String.format("I-%s-%s", bankKey, itemGroup.getId());
            itemGroupIdentifier.setUniqueid(itemGroupId);
            itemGroupIdentifier.setName(itemGroupId);
            itemGroupIdentifier.setVersion(new BigDecimal(version));

            mapPassageRef(itemGroup, legacyItemGroup);

            final List<Groupitem> groupItems = legacyItemGroup.getGroupitem();

            // Map items
            for (int itemPositionInGroup = 1; itemPositionInGroup < itemGroup.items().size(); itemPositionInGroup++) {
                final Item item = itemGroup.items().get(itemPositionInGroup);
                final Groupitem groupItem = new Groupitem();
                groupItem.setItemid(item.getKey());
                groupItem.setGroupposition(String.valueOf(itemPositionInGroup));
                groupItem.setAdminrequired(String.valueOf(item.administrationRequired()));
                groupItem.setResponserequired(String.valueOf(item.responseRequired()));
                groupItem.setIsactive(String.valueOf(item.active()));
                groupItem.setIsfieldtest(String.valueOf(item.fieldTest()));
                groupItem.setBlockid("A");
                groupItems.add(groupItem);
            }

            legacyItemGroups.add(legacyItemGroup);
        }

        return legacyItemGroups;
    }

    private static Itemselector mapItemSelector(final TestPackage testPackage, final Segment segment) {
        final Itemselector itemSelector = new Itemselector();
        itemSelector.setType(segment.getAlgorithmType());
        final Identifier itemSelectorIdentifier = new Identifier();
        itemSelectorIdentifier.setUniqueid(segment.getAlgorithmImplementation());
        itemSelectorIdentifier.setName(segment.getAlgorithmImplementation());
        itemSelectorIdentifier.setLabel(segment.getAlgorithmImplementation());
        itemSelectorIdentifier.setVersion(new BigDecimal(testPackage.getVersion()));
        itemSelector.setIdentifier(itemSelectorIdentifier);

        final Map<String, BlueprintElement> blueprintMap = testPackage.getBlueprintMap();

        final List<Itemselectionparameter> parameters = segment.segmentBlueprint().stream()
                .filter(segmentBlueprintElement -> !segmentBlueprintElement.itemSelection().isEmpty())
                .map(segmentBlueprintElement -> {
                    Itemselectionparameter param = new Itemselectionparameter();
                    param.setBpelementid(TestPackageUtils.getBlueprintKeyFromId(blueprintMap.get(segmentBlueprintElement.getIdRef()),
                            segmentBlueprintElement.getIdRef()));

                    param.getProperty().addAll(segmentBlueprintElement.itemSelection().stream()
                            .map(prop -> {
                                Property property = new Property();
                                property.setName(prop.getName());
                                property.setValue(prop.getValue());
                                return property;
                            }).collect(Collectors.toList())
                    );

                    return param;
                })
                .collect(Collectors.toList());

        itemSelector.getItemselectionparameter().addAll(parameters);
        return itemSelector;
    }

    public static void mapPassageRef(final ItemGroup itemGroup, final Itemgroup legacyItemGroup) {
        if (itemGroup.getStimulus().isPresent()) {
            final Stimulus stimulus = itemGroup.getStimulus().get();
            final Passageref ref = new Passageref();
            ref.setContent(stimulus.getKey());
            legacyItemGroup.getPassageref().add(ref);
        }
    }
}
