package fish.payara.cloud.connectors.kafka.tools;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Properties;

import static org.testng.Assert.assertEquals;

public class AdditionalPropertiesParserTest {

    @DataProvider(name = "ParsingResultProvider")
    public Object[][] createParsingResult() {
        return new Object[][]{
                {null, new Properties()},
                {"", new Properties()},
                {"A=aValue", properties(new String[]{"A"}, new String[]{"aValue"})},
                {"A=aValue , B=bValue", properties(new String[]{"A", "B"}, new String[]{"aValue","bValue"})},
                {"A=aValue , B=b1Value , b2Value", properties(new String[]{"A", "B"}, new String[]{"aValue","b1Value,b2Value"})},
                {"A=aValue , B=b1Value , B=b2Value", properties(new String[]{"A", "B"}, new String[]{"aValue","b1Value,b2Value"})},
                {"A='aValue , b1Value' , B=b2Value", properties(new String[]{"A", "B"}, new String[]{"aValue , b1Value","b2Value"})},
                {"A='aValue B=b1Value' , B=b2Value", properties(new String[]{"A", "B"}, new String[]{"aValue B=b1Value","b2Value"})},
                {"A='aValue , B=b1Value' , B=b2Value", properties(new String[]{"A", "B"}, new String[]{"aValue , B=b1Value","b2Value"})},
                {"A='aValue , '' B=b1Value' , B=b2Value", properties(new String[]{"A", "B"}, new String[]{"aValue , ' B=b1Value","b2Value"})},
                {"A='''aValue , B=b1Value''' , B=b2Value", properties(new String[]{"A", "B"}, new String[]{"'aValue , B=b1Value'","b2Value"})},
        };
    }

    @DataProvider(name = "MergingResultProvider")
    public Object[][] createMergingResult() {
        return new Object[][]{
                {properties(new String[]{"A"}, new String[]{"aValue"}), new Properties(), properties(new String[]{"A"}, new String[]{"aValue"})},
                {properties(new String[]{"A"}, new String[]{"aValue"}), properties(new String[]{"B"}, new String[]{"bValue"}), properties(new String[]{"A", "B"}, new String[]{"aValue", "bValue"})},
                {properties(new String[]{"A"}, new String[]{"a1Value"}), properties(new String[]{"A"}, new String[]{"a2Value"}), properties(new String[]{"A"}, new String[]{"a1Value"})},
        };
    }

    private Properties properties(String[] keys, String[] values){
        Properties properties = new Properties();
        for(int i = 0; i < keys.length; i++){
            properties.setProperty(keys[i], values[i]);
        }
        return properties;
    }

    @Test(dataProvider = "ParsingResultProvider")
    public void testParse(String propertiesString, Properties resultingProperties) throws Exception {
        assertEquals(resultingProperties, new AdditionalPropertiesParser(propertiesString).parse(), "Wrong properties from parsing found.");
    }

    @Test(dataProvider = "MergingResultProvider")
    public void testMerge(Properties base, Properties additional, Properties expectedMergeResult) throws Exception {
        assertEquals(expectedMergeResult, AdditionalPropertiesParser.merge(base, additional), "Wrong result properties from merging found.");
    }
}