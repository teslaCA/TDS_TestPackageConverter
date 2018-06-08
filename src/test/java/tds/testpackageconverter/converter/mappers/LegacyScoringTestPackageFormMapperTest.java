package tds.testpackageconverter.converter.mappers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import tds.testpackage.legacy.model.Itemgroup;
import tds.testpackage.legacy.model.Testform;
import tds.testpackage.legacy.model.Testspecification;
import tds.testpackageconverter.converter.LegacyTestPackageBaseTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class LegacyScoringTestPackageFormMapperTest extends LegacyTestPackageBaseTest {
    private List<Testspecification> adminTestPackages;

    @Before
    public void setup() {
        adminTestPackages = LegacyAdministrationTestPackageMapper.fromNew(mockTestPackageWithScoring);
    }

    @Test
    public void shouldMapSegmentForms() {
        List<Testform> testForms = LegacyScoringTestPackageFormMapper.mapTestForms(mockTestPackageWithScoring, adminTestPackages);
        assertThat(testForms).hasSize(2);


    }
}
