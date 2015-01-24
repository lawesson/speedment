package com.speedment.codegen.model.class_;

import com.speedment.codegen.model.modifier.InterfaceModifier_;

/**
 *
 * @author Duncan
 */
public class Interface_ extends ClassAndInterfaceBase<Interface_, InterfaceModifier_> {

    public Interface_() {
        super(InterfaceModifier_.class);
    }

    @Override
    public Type getModelType() {
        return Type.INTERFACE;
    }
}