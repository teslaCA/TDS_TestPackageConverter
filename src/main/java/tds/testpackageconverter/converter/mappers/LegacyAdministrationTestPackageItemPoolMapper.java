package tds.testpackageconverter.converter.mappers;

import tds.common.Algorithm;
import tds.testpackage.legacy.model.*;
import tds.testpackage.model.Assessment;
import tds.testpackage.model.Item;
import tds.testpackage.model.ItemScoreDimension;
import tds.testpackage.model.TestPackage;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

public class LegacyAdministrationTestPackageItemPoolMapper {
    public static Itempool mapItemPool(final TestPackage testPackage, final Assessment assessment) {
        final Itempool itempool = new Itempool();
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

        final List<Testitem> testItems = items.stream().map(item -> {
            Testitem testItem = new Testitem();
            testItem.setFilename(String.format("item-%s-%s.xml", testPackage.getBankKey(), item.getId()));
            testItem.setItemtype(item.getType());

            Identifier testItemIdentifier = new Identifier();
            testItemIdentifier.setUniqueid(String.format("%s-%s", testPackage.getBankKey(), item.getId()));
            testItemIdentifier.setVersion(new BigDecimal(testPackage.getVersion()));
            testItem.setIdentifier(testItemIdentifier);

            List<Bpref> bpRefs = testItem.getBpref();

            item.getBlueprintReferences().forEach(bpRef -> {
                Bpref ref = new Bpref();
                ref.setContent(bpRef.getIdRef());
                bpRefs.add(ref);
            });

            List<Poolproperty> itemProperties = testItem.getPoolproperty();

            item.getPresentations().forEach(presentation -> {
                Poolproperty languageProp = new Poolproperty();
                languageProp.setProperty("Language");
                languageProp.setValue(presentation.getCode());
                languageProp.setLabel(presentation.label());
                itemProperties.add(languageProp);
            });

            Poolproperty itemTypeProperty = new Poolproperty();
            itemTypeProperty.setProperty("--ITEMTYPE--");
            itemTypeProperty.setValue(item.getType());
            itemTypeProperty.setLabel("ItemType = " + item.getType());
            itemProperties.add(itemTypeProperty);

            testItem.getItemscoredimension().add(mapItemScoreDimensions(item.getItemScoreDimension()));

            return testItem;
        }).collect(Collectors.toList());

        itempool.getTestitem().addAll(testItems);
        return itempool;
    }

    private static Itemscoredimension mapItemScoreDimensions(final ItemScoreDimension itemScoreDimension) {
        final Itemscoredimension legacyItemScoreDimension = new Itemscoredimension();
        legacyItemScoreDimension.setMeasurementmodel(itemScoreDimension.getMeasurementModel());
        legacyItemScoreDimension.setScorepoints(BigInteger.valueOf(itemScoreDimension.getScorePoints()));
        legacyItemScoreDimension.setWeight((float) itemScoreDimension.getWeight());

        if (itemScoreDimension.getDimension().isPresent()) {
            legacyItemScoreDimension.setDimension(itemScoreDimension.getDimension().get());
        }

        final List<Itemscoreparameter> legacyParams = legacyItemScoreDimension.getItemscoreparameter();
        itemScoreDimension.itemScoreParameters().forEach(
                param -> {
                    Itemscoreparameter legacyParam = new Itemscoreparameter();
                    legacyParam.setMeasurementparameter(param.getMeasurementParameter());
                    legacyParam.setValue((float) param.getValue());
                    legacyParams.add(legacyParam);
                }
        );

        return legacyItemScoreDimension;
    }
}
