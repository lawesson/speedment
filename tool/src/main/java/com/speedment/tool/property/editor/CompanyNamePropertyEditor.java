package com.speedment.tool.property.editor;

import com.speedment.tool.config.ProjectProperty;
import com.speedment.tool.property.PropertyEditor;
import com.speedment.tool.property.item.SimpleTextFieldItem;
import java.util.stream.Stream;

/**
 *
 * @author Simon
 * @param <T>  the document type
 */
public class CompanyNamePropertyEditor <T extends ProjectProperty> implements PropertyEditor<T>{

    @Override
    public Stream<Item> fieldsFor(T document) {
         return Stream.of(new SimpleTextFieldItem(
                "Company Name",
                document.companyNameProperty(),
                "The company name that should be used for this project. It is used in the generated code."
            )
         );
    }
    
}