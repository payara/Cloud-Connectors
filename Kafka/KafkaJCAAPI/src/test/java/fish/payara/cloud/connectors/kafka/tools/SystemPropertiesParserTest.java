package fish.payara.cloud.connectors.kafka.tools;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class SystemPropertiesParserTest {
    public static final String LONG_PROPERTY = "longProperty";
    public static final String STRING_PROPERTY = "stringProperty";
    public static final String INT_PROPERTY = "intProperty";
    public static final String PREFIX = "prefix";
    public static final String UNKNOWN_PROPERTY = "unknown";
    private SystemPropertiesParser parser;
    private TestObject testObject;

    @BeforeMethod
    public void setUp() {
        parser = new SystemPropertiesParser(LONG_PROPERTY, STRING_PROPERTY, INT_PROPERTY);
        testObject = new TestObject();
    }

    @AfterMethod
    public void tearDown() {
        System.clearProperty(PREFIX + "." + LONG_PROPERTY);
        System.clearProperty(PREFIX + "."+ STRING_PROPERTY);
        System.clearProperty(PREFIX + "."+ INT_PROPERTY);
    }

    @Test
    public void checkLongProperty() {
        System.setProperty(PREFIX + "." + LONG_PROPERTY, Long.toString(10L));
        parser.applySystemProperties(testObject, PREFIX);
        assertEquals(Long.valueOf(10L), testObject.getLongProperty());
    }

    @Test
    public void checkStringProperty() {
        System.setProperty(PREFIX + "." + STRING_PROPERTY, "ABC");
        parser.applySystemProperties(testObject, PREFIX);
        assertEquals("ABC", testObject.getStringProperty());
    }

    @Test
    public void checkIntProperty() {
        System.setProperty(PREFIX + "." + INT_PROPERTY, Integer.toString(15));
        parser.applySystemProperties(testObject, PREFIX);
        assertEquals(Integer.valueOf(15), testObject.getIntProperty());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void invalidValueType() {
        System.setProperty(PREFIX + "." + INT_PROPERTY, "Hello");
        parser.applySystemProperties(testObject, PREFIX);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void missingProperty() {
        System.setProperty(PREFIX + "." + UNKNOWN_PROPERTY, Integer.toString(15));
        new SystemPropertiesParser(UNKNOWN_PROPERTY).applySystemProperties(testObject, PREFIX);
    }

    private static class TestObject {
        private Long longProperty;
        private String stringProperty;
        private Integer intProperty;

        public Long getLongProperty() {
            return longProperty;
        }

        public void setLongProperty(Long longProperty) {
            this.longProperty = longProperty;
        }

        public String getStringProperty() {
            return stringProperty;
        }

        public void setStringProperty(String stringProperty) {
            this.stringProperty = stringProperty;
        }

        public Integer getIntProperty() {
            return intProperty;
        }

        public void setIntProperty(Integer intProperty) {
            this.intProperty = intProperty;
        }
    }
}
