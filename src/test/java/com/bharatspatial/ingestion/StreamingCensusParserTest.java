package com.bharatspatial.ingestion;

import com.bharatspatial.model.AdministrativeNode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StreamingCensusParserTest {

    private final StreamingCensusParser parser = new StreamingCensusParser();

    @Test
    void testParseStreamWithValidCsv() {
        String csv = """
                mddsCode,lgdCode,name,localName,level,stateCode,stateName,districtCode,districtName,subDistrictCode,subDistrictName,gramPanchayatName,latitude,longitude,population,areaSqKm,pincode,category
                1901001,WB01001,Kolkata GPO,কলকাতা,URBAN_BODY,19,West Bengal,311,Kolkata,02214,Kolkata Municipal Corp,Ward 45,22.572646,88.363895,4496694,206.08,700001,URBAN
                1901007,WB01007,Shyamsundar,শ্যামসুন্দর,VILLAGE,19,West Bengal,314,Purba Bardhaman,02219,Raina I,Shyamsundar GP,23.109200,87.892000,8540,6.45,713424,RURAL
                """;

        ByteArrayInputStream is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        StreamingCensusParser.IngestionReport report = new StreamingCensusParser.IngestionReport();
        List<AdministrativeNode> nodes = parser.parseFromStream(is, report);

        assertEquals(2, report.getTotalProcessed());
        assertEquals(2, report.getTotalValid());
        assertEquals(0, report.getTotalAnomalies());
        assertEquals(2, nodes.size());

        AdministrativeNode n1 = nodes.get(0);
        assertEquals("1901001", n1.getMddsCode());
        assertEquals("Kolkata GPO", n1.getName());
        assertEquals("West Bengal", n1.getStateName());
        assertEquals(4496694, n1.getPopulation());
    }

    @Test
    void testAnomalyFilteringOutOfRangeCoordinates() {
        String csv = """
                mddsCode,lgdCode,name,localName,level,stateCode,stateName,districtCode,districtName,subDistrictCode,subDistrictName,gramPanchayatName,latitude,longitude,population,areaSqKm,pincode,category
                9999999,XX00001,Atlantic Ocean Post,None,VILLAGE,99,NoState,999,NoDist,99999,NoSub,NoGP,0.000000,-25.000000,10,1.0,000000,RURAL
                """;

        ByteArrayInputStream is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        StreamingCensusParser.IngestionReport report = new StreamingCensusParser.IngestionReport();
        List<AdministrativeNode> nodes = parser.parseFromStream(is, report);

        assertEquals(1, report.getTotalProcessed());
        assertEquals(0, report.getTotalValid());
        assertEquals(1, report.getTotalAnomalies());
        assertTrue(nodes.isEmpty());
    }

    @Test
    void testScaleDatasetGenerator() {
        String csv = """
                mddsCode,lgdCode,name,localName,level,stateCode,stateName,districtCode,districtName,subDistrictCode,subDistrictName,gramPanchayatName,latitude,longitude,population,areaSqKm,pincode,category
                1901001,WB01001,Kolkata GPO,কলকাতা,URBAN_BODY,19,West Bengal,311,Kolkata,02214,Kolkata Municipal Corp,Ward 45,22.572646,88.363895,4496694,206.08,700001,URBAN
                """;
        ByteArrayInputStream is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        StreamingCensusParser.IngestionReport report = new StreamingCensusParser.IngestionReport();
        List<AdministrativeNode> seeds = parser.parseFromStream(is, report);

        List<AdministrativeNode> scaled = parser.generateScaleDataset(seeds, 100);
        assertEquals(100, scaled.size());
        for (AdministrativeNode n : scaled) {
            assertTrue(n.getLocation().isWithinIndiaBounds());
        }
    }
}
