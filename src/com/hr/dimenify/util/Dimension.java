package com.hr.dimenify.util;

import com.intellij.psi.PsiClass;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.TagValue;

/**
 * Created by anvithbhat on 09/06/17.
 */
public interface Dimension extends com.intellij.util.xml.DomElement {
    @TagValue
    String getValue();

    @TagValue
    void setValue(String s);

    @Attribute("name")
    GenericAttributeValue<PsiClass> getNameValue();

}