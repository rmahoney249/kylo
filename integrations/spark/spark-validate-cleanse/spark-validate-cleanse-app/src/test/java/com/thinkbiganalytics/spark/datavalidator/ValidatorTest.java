/*
 * Copyright (c) 2016. Teradata Inc.
 */

package com.thinkbiganalytics.spark.datavalidator;

import com.thinkbiganalytics.policy.FieldPolicy;
import com.thinkbiganalytics.policy.standardization.SimpleRegexReplacer;
import com.thinkbiganalytics.policy.standardization.StandardizationPolicy;
import com.thinkbiganalytics.policy.validation.NotNullValidator;
import com.thinkbiganalytics.policy.validation.RangeValidator;
import com.thinkbiganalytics.policy.validation.ValidationPolicy;
import com.thinkbiganalytics.policy.validation.ValidationResult;
import com.thinkbiganalytics.spark.validation.HCatDataType;

import javafx.util.Pair;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ValidatorTest {

    private Validator validator;

    @Before
    public void setUp() throws Exception {
        URL url = getClass().getClassLoader().getResource("example-policy.json");
        Path pathToPolicyFile = Paths.get(url.toURI());
        validator = new Validator();
        validator.setArguments("emp", "sampletable", "20001", pathToPolicyFile.toString());
    }


    @Test
    public void testParseRemainingParameters() {
        String[] args = {"targetDatabase", "entity", "partition", "path-to-policy-file", "-h", "hive.setting.1=value.1", "--hiveConf", "hive.setting.2=value.2"};
        CommandLineParams params = Validator.parseRemainingParameters(args, 4);
        List<Param> hiveParams = params.getHiveParams();

        Param first = hiveParams.get(0);
        assertEquals("hive.setting.1",  first.getName());
        assertEquals("value.1",  first.getValue());

        Param second = hiveParams.get(1);
        assertEquals("hive.setting.2",  second.getName());
        assertEquals("value.2",  second.getValue());
    }

    @Test
    public void testParseRemainingParameters_missingParameters() {
        String[] args = {"targetDatabase", "entity", "partition", "path-to-policy-file"};
        CommandLineParams params = Validator.parseRemainingParameters(args, 4);
        List<Param> hiveParams = params.getHiveParams();

        assertTrue(hiveParams.isEmpty());
    }

    @Test
    public void testValidateRange() {

        assertEquals(Validator.VALID_RESULT, rangeValidate(1, 100, "decimal", "1"));
        assertEquals(Validator.VALID_RESULT, rangeValidate(1, 100, "double", "100.0"));
        assertEquals(Validator.VALID_RESULT, rangeValidate(1, 100, "decimal", "15.55"));
        assertEquals(Validator.VALID_RESULT, rangeValidate(1, 100, "int", "50"));
        assertEquals(Validator.VALID_RESULT, rangeValidate(-50, 0, "real", "-32.123"));

        assertTrue(!rangeValidate(1, 100, "decimal", "0").isValid());
        assertTrue(!rangeValidate(1, 100, "double", "0").isValid());
        assertTrue(!rangeValidate(1, 100, "int", "0").isValid());
    }

    private ValidationResult rangeValidate(Number min, Number max, String dataType, String value) {
        RangeValidator validatorPolicy = new RangeValidator(min, max);
        List<ValidationPolicy> validationPolicies = new ArrayList<>();
        validationPolicies.add(validatorPolicy);
        List<StandardizationPolicy> standardizationPolicies = new ArrayList<>();
        FieldPolicy fieldPolicy = new FieldPolicy("emp", "field1", false, false, validationPolicies, standardizationPolicies, false, 0);
        return validator.validateField(fieldPolicy, HCatDataType.createFromDataType("field1", dataType), value);
    }

    @Test
    public void standardizeRegex() {
        SimpleRegexReplacer standardizer = new SimpleRegexReplacer("(?i)foo", "bar");

        List<ValidationPolicy> validationPolicies = new ArrayList<>();
        List<StandardizationPolicy> standardizationPolicies = new ArrayList<>();
        standardizationPolicies.add(standardizer);
        FieldPolicy fieldPolicy = new FieldPolicy("emp", "field1", false, false, validationPolicies, standardizationPolicies, false, 0);
        assertEquals(validator.standardizeField(fieldPolicy, "aafooaa"), "aabaraa");
        assertNull(validator.standardizeField(fieldPolicy, null));
        assertEquals(validator.standardizeField(fieldPolicy, ""), "");
    }


    @Test
    public void testValidateNotNull() {

        assertNotEquals(Validator.VALID_RESULT, notNullValidate("string", null, false, false));
    }

    private ValidationResult notNullValidate(String dataType, String value, boolean allowEmptyString, boolean trimString) {
        NotNullValidator validatorPolicy = new NotNullValidator(allowEmptyString, trimString);
        return validator.validateValue(validatorPolicy, HCatDataType.createFromDataType("field1", dataType), value);
    }
}