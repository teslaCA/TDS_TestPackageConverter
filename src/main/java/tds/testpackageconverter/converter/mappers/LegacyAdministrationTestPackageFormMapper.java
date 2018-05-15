package tds.testpackageconverter.converter.mappers;

import tds.common.Algorithm;
import tds.testpackage.legacy.model.*;
import tds.testpackage.model.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LegacyAdministrationTestPackageFormMapper {
    public static List<Testform> mapTestForms(final Assessment assessment, final Map<String, Long> formIdToKeyMap, final String version, final int bankKey) {
        return assessment.getSegments().stream()
                .filter(segment -> segment.getAlgorithmType().equalsIgnoreCase(Algorithm.FIXED_FORM.getType()))
                .flatMap(segment -> segment.segmentForms().stream()
                        .flatMap(form -> form.getPresentations().stream()
                                .map(presentation -> {
                                    final Testform testform = new Testform();
                                    // Get the item count of the items with the same language
                                    final long itemCount = form.itemGroups().stream()
                                            .mapToLong(itemGroup -> itemGroup.items().stream()
                                                    .filter(item -> item.getPresentations().contains(presentation))
                                                    .collect(Collectors.toList())
                                                    .size())
                                            .sum();
                                    testform.setLength(BigInteger.valueOf(itemCount));
                                    final Identifier testFormIdentifier = new Identifier();
                                    final String testFormId = String.format("%s:%s-%s", segment.getKey(), form.getCohort(), presentation);
                                    testFormIdentifier.setVersion(new BigDecimal(version));
                                    testFormIdentifier.setUniqueid(testFormId);
                                    testFormIdentifier.setName(testFormId);
                                    testform.getFormpartition().add(mapFormPartition(form, formIdToKeyMap, presentation, version, bankKey));

                                    return testform;
                                })
                        )
                )
                .collect(Collectors.toList());
    }

    private static Formpartition mapFormPartition(final SegmentForm form, final Map<String, Long> formIdToKeyMap, final Presentation presentation,
                                                  final String version, final int bankKey) {
        final Formpartition formPartition = new Formpartition();
        final Identifier formPartitionIdentifier = new Identifier();
        final String formKey = String.format("%s-%s", bankKey, formIdToKeyMap.get(form.getId()));
        formPartitionIdentifier.setUniqueid(formKey);
        formPartitionIdentifier.setName(form.getCohort());
        formPartitionIdentifier.setVersion(new BigDecimal(version));
        formPartition.setIdentifier(formPartitionIdentifier);
        final List<Itemgroup> legacyItemGroups = formPartition.getItemgroup();

        int formPosition = 1;
        for (ItemGroup itemGroup : form.itemGroups()) {
            // If the item group includes item with presentations that do not match, skip it
            if (itemGroup.items().stream().anyMatch(item -> !item.getPresentations().contains(presentation))) {
                continue;
            }

            final Itemgroup legacyItemGroup = new Itemgroup();
            legacyItemGroup.setFormposition(String.valueOf(formPosition));
            legacyItemGroup.setMaxitems(itemGroup.maxItems());
            legacyItemGroup.setMaxresponses(itemGroup.maxResponses());

            final Identifier itemGroupIdentifier = new Identifier();

            final String itemGroupId = itemGroup.items().size() > 1
                    ? String.format("%s:G-%s-%s-0", formKey, bankKey, itemGroup.getId())
                    : String.format("%s:I-%s-%s", formKey, bankKey, itemGroup.getId());
            itemGroupIdentifier.setUniqueid(itemGroupId);
            itemGroupIdentifier.setName(itemGroupId);
            itemGroupIdentifier.setVersion(new BigDecimal(version));

            // Map passage reference
            LegacyAdministrationTestPackageSegmentMapper.mapPassageRef(itemGroup, legacyItemGroup);

            final List<Groupitem> groupItems = legacyItemGroup.getGroupitem();
            // Map items
            for (int itemPositionInGroup = 1; itemPositionInGroup < itemGroup.items().size(); itemPositionInGroup++) {
                final Item item = itemGroup.items().get(itemPositionInGroup);
                final Groupitem groupItem = new Groupitem();
                groupItem.setItemid(item.getKey());
                groupItem.setFormposition(BigInteger.valueOf(formPosition++));
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

        return formPartition;
    }
}
