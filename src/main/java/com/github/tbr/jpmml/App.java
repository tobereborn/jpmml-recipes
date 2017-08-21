package com.github.tbr.jpmml;

import com.google.common.collect.RangeSet;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Value;
import org.jpmml.evaluator.FieldValueUtil;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.model.PMMLUtil;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) throws IOException, JAXBException, SAXException {
        PMML pmml;
        try (InputStream in = App.class.getResourceAsStream("/single_audit_kmeans_empty_model.xml")) {
            pmml = PMMLUtil.unmarshal(in);
        }

        ModelEvaluatorFactory factory = ModelEvaluatorFactory.newInstance();
        ModelEvaluator<?> evaluator = factory.newModelEvaluator(pmml);
        List<InputField> inputFields = evaluator.getInputFields();
        for(InputField inputField : inputFields){
            System.out.println(inputField);
            org.dmg.pmml.DataField pmmlDataField = (org.dmg.pmml.DataField)inputField.getField();
            org.dmg.pmml.MiningField pmmlMiningField = inputField.getMiningField();
            System.out.println(pmmlDataField);
            System.out.println(pmmlMiningField);

            org.dmg.pmml.DataType dataType = inputField.getDataType();
            org.dmg.pmml.OpType opType = inputField.getOpType();

            switch(opType){
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

    }
}
