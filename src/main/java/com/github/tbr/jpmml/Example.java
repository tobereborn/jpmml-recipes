package com.githb.tbr.jpmml;

import com.google.common.collect.BiMap;
import com.google.common.collect.RangeSet;
import org.dmg.pmml.Entity;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Value;
import org.jpmml.evaluator.*;
import org.jpmml.model.PMMLUtil;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Example {
    private Evaluator evaluator;

    public Example(String fileName) throws IOException, JAXBException, SAXException {
        try (InputStream in = Example.class.getResourceAsStream("/" + fileName)) {
            PMML pmml = PMMLUtil.unmarshal(in);
            ModelEvaluatorFactory factory = ModelEvaluatorFactory.newInstance();
            evaluator = (Evaluator) factory.newModelEvaluator(pmml);
        }
    }

    public void showInputFields() {
        System.out.println("=================== input fields ====================");
        List<InputField> inputFields = evaluator.getInputFields();
        for (InputField inputField : inputFields) {
            System.out.println(inputField);
            org.dmg.pmml.DataField pmmlDataField = (org.dmg.pmml.DataField) inputField.getField();
            org.dmg.pmml.MiningField pmmlMiningField = inputField.getMiningField();
            org.dmg.pmml.DataType dataType = inputField.getDataType();
            org.dmg.pmml.OpType opType = inputField.getOpType();
            switch (opType) {
                case CONTINUOUS:
                    RangeSet<Double> validArgumentRanges = FieldValueUtil.getValidRanges(pmmlDataField);
                    System.out.println("-----" + validArgumentRanges.asRanges());
                    break;
                case CATEGORICAL:
                case ORDINAL:
                    List<Value> validArgumentValues = FieldValueUtil.getValidValues(pmmlDataField);
                    for (Value val : validArgumentValues)
                        System.out.println("-----" + val.getValue());
                    break;
                default:
                    break;
            }
        }

    }

    public void showTargetFields() {
        System.out.println("=================== target fields ====================");
        List<TargetField> targetFields = evaluator.getTargetFields();
        for (TargetField targetField : targetFields) {
            System.out.println(targetField);
            org.dmg.pmml.DataField pmmlDataField = targetField.getDataField();
            org.dmg.pmml.MiningField pmmlMiningField = targetField.getMiningField(); // Could be null
            org.dmg.pmml.Target pmmlTarget = targetField.getTarget(); // Could be null
            System.out.println("MiningField: " + pmmlMiningField + ", target: " + pmmlTarget);
            org.dmg.pmml.DataType dataType = targetField.getDataType();
            org.dmg.pmml.OpType opType = targetField.getOpType();
            switch (opType) {
                case CONTINUOUS:
                    RangeSet<Double> validArgumentRanges = FieldValueUtil.getValidRanges(pmmlDataField);
                    System.out.println("-----" + validArgumentRanges.asRanges());
                    break;
                case CATEGORICAL:
                case ORDINAL:
                    List<Value> validResultValues = FieldValueUtil.getValidValues(pmmlDataField);
                    for (Value val : validResultValues) {
                        System.out.println("-----" + val.getValue());
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void showOutputFields() {
        System.out.println("=================== output fields ====================");
        List<OutputField> outputFields = evaluator.getOutputFields();
        for (OutputField outputField : outputFields) {
            org.dmg.pmml.OutputField pmmlOutputField = outputField.getOutputField();
            org.dmg.pmml.DataType dataType = outputField.getDataType(); // Could be null
            org.dmg.pmml.OpType opType = outputField.getOpType(); // Could be null
            System.out.println("dataType: " + dataType + ", opType: " + opType);
            boolean finalResult = outputField.isFinalResult();
            if (!finalResult) {
                System.out.println("Not final");
                continue;
            }
        }
    }

    public void evaluate() {
        System.out.println("=================== evaluation ====================");
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("Age", "16");
        raw.put("Income", "0.0");
        raw.put("Deductions", "3000.0");
        raw.put("Hours", 80);
        raw.put("Employment", "Unemployed");
        raw.put("Education", "XXXX");
        raw.put("Marital", null);
        raw.put("Occupation", "Protective");
        raw.put("Gender", "Female");
        Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();
        List<InputField> inputFields = evaluator.getInputFields();
        for (InputField inputField : inputFields) {
            FieldName inputFieldName = inputField.getName();
            // The raw (ie. user-supplied) value could be any Java primitive value
            Object rawValue = raw.get(inputFieldName.getValue());
            // The raw value is passed through: 1) outlier treatment, 2) missing value treatment, 3) invalid value treatment and 4) type conversion
            FieldValue inputFieldValue = inputField.prepare(rawValue);
            System.out.println(inputFieldName.getValue() + "=> raw: " + rawValue + ", prepared: " + (inputFieldValue == null ? null : inputFieldValue.getValue()));
            arguments.put(inputFieldName, inputFieldValue);
        }
        // Performing the evaluation:
        Map<FieldName, ?> results = evaluator.evaluate(arguments);
        // Extracting primary results from the result data record:
        List<TargetField> targetFields = evaluator.getTargetFields();
        for (TargetField targetField : targetFields) {
            System.out.println("target: " + targetField);
            FieldName targetFieldName = targetField.getName();
            Object targetFieldValue = results.get(targetFieldName);
            // The target value is either a Java primitive value (as a wrapper object) or an instance of org.jpmml.evaluator.Computable:
            if (targetFieldValue instanceof Computable) {
                Computable computable = (Computable) targetFieldValue;
                Object unboxedTargetFieldValue = computable.getResult();
                System.out.println("unboxedTargetFieldValue: " + unboxedTargetFieldValue);
            }
            // Test for "entityId" result feature
            if (targetFieldValue instanceof HasEntityId) {
                HasEntityId hasEntityId = (HasEntityId) targetFieldValue;
                HasEntityRegistry<?> hasEntityRegistry = (HasEntityRegistry<?>) evaluator;
                BiMap<String, ? extends Entity> entities = hasEntityRegistry.getEntityRegistry();
                Entity winner = entities.get(hasEntityId.getEntityId());
                // Test for "probability" result feature
                if (targetFieldValue instanceof HasProbability) {
                    HasProbability hasProbability = (HasProbability) targetFieldValue;
                    Double winnerProbability = hasProbability.getProbability(winner.getId());
                    System.out.println("winnerProbability: " + winnerProbability);
                }
            }
        }

        List<OutputField> outputFields = evaluator.getOutputFields();
        for (OutputField outputField : outputFields) {
            FieldName outputFieldName = outputField.getName();
            Object outputFieldValue = results.get(outputFieldName);
            System.out.println("output: " + outputFieldValue);
        }
    }

    public static void main(String[] args) throws JAXBException, SAXException, IOException {
        Example example = new Example("single_audit_kmeans_java_model.xml");
        example.showInputFields();
        example.showTargetFields();
        example.showOutputFields();
        example.evaluate();
    }
}
