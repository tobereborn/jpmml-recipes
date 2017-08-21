package com.github.tbr.jpmml;

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

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) throws IOException, JAXBException, SAXException {
        PMML pmml;
        try (InputStream in = App.class.getResourceAsStream("/single_audit_kmeans.xml")) {
            pmml = PMMLUtil.unmarshal(in);
        }

        ModelEvaluatorFactory factory = ModelEvaluatorFactory.newInstance();
        ModelEvaluator<?> evaluator = factory.newModelEvaluator(pmml);
        List<InputField> inputFields = evaluator.getInputFields();
        for (InputField inputField : inputFields) {
            System.out.println(inputField);
            org.dmg.pmml.DataField pmmlDataField = (org.dmg.pmml.DataField) inputField.getField();
            org.dmg.pmml.MiningField pmmlMiningField = inputField.getMiningField();
            System.out.println(pmmlDataField);
            System.out.println(pmmlMiningField);

            org.dmg.pmml.DataType dataType = inputField.getDataType();
            org.dmg.pmml.OpType opType = inputField.getOpType();

            switch (opType) {
                case CONTINUOUS:
                    RangeSet<Double> validArgumentRanges = FieldValueUtil.getValidRanges(pmmlDataField);
                    System.out.println(validArgumentRanges.asRanges());
                    break;
                case CATEGORICAL:
                case ORDINAL:
                    List<Value> validArgumentValues = FieldValueUtil.getValidValues(pmmlDataField);
                    System.out.println(validArgumentValues);
                    break;
                default:
                    break;
            }
        }

        List<TargetField> targetFields = evaluator.getTargetFields();
        for (TargetField targetField : targetFields) {
            org.dmg.pmml.DataField pmmlDataField = targetField.getDataField();
            org.dmg.pmml.MiningField pmmlMiningField = targetField.getMiningField(); // Could be null
            org.dmg.pmml.Target pmmlTarget = targetField.getTarget(); // Could be null

            org.dmg.pmml.DataType dataType = targetField.getDataType();
            org.dmg.pmml.OpType opType = targetField.getOpType();

            switch (opType) {
                case CONTINUOUS:
                    break;
                case CATEGORICAL:
                case ORDINAL:
                    List<Value> validResultValues = FieldValueUtil.getValidValues(pmmlDataField);
                    break;
                default:
                    break;
            }
        }

        List<OutputField> outputFields = evaluator.getOutputFields();
        for (OutputField outputField : outputFields) {
            org.dmg.pmml.OutputField pmmlOutputField = outputField.getOutputField();

            org.dmg.pmml.DataType dataType = outputField.getDataType(); // Could be null
            org.dmg.pmml.OpType opType = outputField.getOpType(); // Could be null

            boolean finalResult = outputField.isFinalResult();
            if (!finalResult) {
                continue;
            }
        }

        Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();

//        List<InputField> inputFields = evaluator.getInputFields();
        for (InputField inputField : inputFields) {
            FieldName inputFieldName = inputField.getName();

            // The raw (ie. user-supplied) value could be any Java primitive value
            Object rawValue = null;

            // The raw value is passed through: 1) outlier treatment, 2) missing value treatment, 3) invalid value treatment and 4) type conversion
            FieldValue inputFieldValue = inputField.prepare(rawValue);

            arguments.put(inputFieldName, inputFieldValue);
        }

//        Performing the evaluation:

        Map<FieldName, ?> results = evaluator.evaluate(arguments);

//        Extracting primary results from the result data record:

//        List<TargetField> targetFields = evaluator.getTargetFields();
        for (TargetField targetField : targetFields) {
            FieldName targetFieldName = targetField.getName();


            Object targetFieldValue = results.get(targetFieldName);
            //        The target value is either a Java primitive value (as a wrapper object) or an instance of org.jpmml.evaluator.Computable:

            if (targetFieldValue instanceof Computable) {
                Computable computable = (Computable) targetFieldValue;

                Object unboxedTargetFieldValue = computable.getResult();
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
                }
            }

        }

//        List<OutputField> outputFields = evaluator.getOutputFields();
        for (OutputField outputField : outputFields) {
            FieldName outputFieldName = outputField.getName();

            Object outputFieldValue = results.get(outputFieldName);
            System.out.println(outputFieldValue);
        }

    }
}
