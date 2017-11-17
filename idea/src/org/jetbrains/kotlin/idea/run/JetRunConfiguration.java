/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.run;

import com.intellij.execution.CommonJavaRunConfigurationParameters;
import com.intellij.execution.ExternalizablePath;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RefactoringListenerProvider;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.xmlb.annotations.Transient;
import org.jdom.Element;
import org.jdom.Verifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @deprecated Will be dropped in 1.2.20. Use KotlinRunConfiguration instead.
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated
public abstract class JetRunConfiguration extends ModuleBasedConfiguration<JavaRunConfigurationModule>
        implements CommonJavaRunConfigurationParameters, RefactoringListenerProvider {
    public String MAIN_CLASS_NAME;

    public JetRunConfiguration(String name, JavaRunConfigurationModule runConfigurationModule, ConfigurationFactory factory) {
        super(name, runConfigurationModule, factory);
    }

    // Copied from DefaultJDOMExternalizer.writeExternal
    protected static void writeExternal(
            @NotNull Object data,
            @NotNull Field[] fields,
            @NotNull Element parentNode,
            @Nullable("null means all elements accepted") DefaultJDOMExternalizer.JDOMFilter filter) throws
                                                                                                     WriteExternalException {
        for (Field field : fields) {
            if (field.getName().indexOf('$') >= 0) continue;
            int modifiers = field.getModifiers();
            if (!(Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers) &&
                          /*!Modifier.isFinal(modifiers) &&*/ !Modifier.isTransient(modifiers) &&
                  field.getAnnotation(Transient.class) == null)) continue;

            field.setAccessible(true); // class might be non-public
            Class type = field.getType();
            if (filter != null && !filter.isAccept(field) || field.getDeclaringClass().getAnnotation(Transient.class) != null) {
                continue;
            }
            String value = null;
            try {
                if (type.isPrimitive()) {
                    if (type.equals(byte.class)) {
                        value = Byte.toString(field.getByte(data));
                    }
                    else if (type.equals(short.class)) {
                        value = Short.toString(field.getShort(data));
                    }
                    else if (type.equals(int.class)) {
                        value = Integer.toString(field.getInt(data));
                    }
                    else if (type.equals(long.class)) {
                        value = Long.toString(field.getLong(data));
                    }
                    else if (type.equals(float.class)) {
                        value = Float.toString(field.getFloat(data));
                    }
                    else if (type.equals(double.class)) {
                        value = Double.toString(field.getDouble(data));
                    }
                    else if (type.equals(char.class)) {
                        value = String.valueOf(field.getChar(data));
                    }
                    else if (type.equals(boolean.class)) {
                        value = Boolean.toString(field.getBoolean(data));
                    }
                    else {
                        continue;
                    }
                }
                else if (type.equals(String.class)) {
                    value = filterXMLCharacters((String)field.get(data));
                }
                else if (type.isEnum()) {
                    value = field.get(data).toString();
                }
                else if (type.equals(Color.class)) {
                    Color color = (Color)field.get(data);
                    if (color != null) {
                        value = Integer.toString(color.getRGB() & 0xFFFFFF, 16);
                    }
                }
                else if (ReflectionUtil.isAssignable(JDOMExternalizable.class, type)) {
                    Element element = new Element("option");
                    parentNode.addContent(element);
                    element.setAttribute("name", field.getName());
                    JDOMExternalizable domValue = (JDOMExternalizable)field.get(data);
                    if (domValue != null) {
                        Element valueElement = new Element("value");
                        element.addContent(valueElement);
                        domValue.writeExternal(valueElement);
                    }
                    continue;
                }
                else {
                    continue;
                }
            }
            catch (IllegalAccessException e) {
                continue;
            }
            Element element = new Element("option");
            parentNode.addContent(element);
            element.setAttribute("name", field.getName());
            if (value != null) {
                element.setAttribute("value", value);
            }
        }
    }

    // Copied from DefaultJDOMExternalizer.filterXMLCharacters
    @Nullable
    private static String filterXMLCharacters(String value) {
        if (value != null) {
            StringBuilder builder = null;
            for (int i=0; i<value.length();i++) {
                char c = value.charAt(i);
                if (Verifier.isXMLCharacter(c)) {
                    if (builder != null) {
                        builder.append(c);
                    }
                }
                else {
                    if (builder == null) {
                        builder = new StringBuilder(value.length()+5);
                        builder.append(value, 0, i);
                    }
                }
            }
            if (builder != null) {
                value = builder.toString();
            }
        }
        return value;
    }
}
