package tds.testpackageconverter.converter.mappers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import tds.testpackage.legacy.model.Itempool;
import tds.testpackage.legacy.model.Testspecification;
import tds.testpackageconverter.converter.LegacyTestPackageBaseTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class LegacyScoringTestPackageItemPoolMapperTest extends LegacyTestPackageBaseTest {

    private List<Testspecification> adminTestPackages;

    @Before
    public void setup() {
        adminTestPackages = LegacyAdministrationTestPackageMapper.fromNew(mockTestPackageWithScoring);
    }

    @Test
    public void shouldMapItemPoolCombined() {
        Itempool itempool = LegacyScoringTestPackageItemPoolMapper.mapItemPool(mockTestPackageWithScoring, adminTestPackages);

        assertThat(itempool).isNotNull();
//        assertThat(itempool.getPassage()).hasSize(9);

        assertThat(itempool.getPassage().get(0).getIdentifier().getUniqueid()).isEqualTo("200-1433");
        assertThat(itempool.getPassage().get(0).getIdentifier().getVersion()).isEqualTo("9787");
        assertThat(itempool.getPassage().get(0).getFilename()).isEqualTo("stim-200-1433.xml");

        assertThat(itempool.getTestitem()).hasSize(48);
    }
}
