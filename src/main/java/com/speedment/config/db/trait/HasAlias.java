/**
 *
 * Copyright (c) 2006-2016, Speedment, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); You may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.speedment.config.db.trait;

import com.speedment.annotation.Api;
import com.speedment.config.Document;
import com.speedment.internal.util.document.TraitUtil.AbstractTraitView;
import static com.speedment.internal.util.document.TraitUtil.viewOf;
import java.util.Map;
import java.util.Optional;

/**
 * Trait for {@link Document} implementations that implement the 
 * {@link #getAlias()} method. If a {@code Document} implements this trait, it
 * is also expected to implement the {@link HasName} trait.
 * 
 * @author  Emil Forslund
 * @since   2.3
 */
@Api(version = "2.3")
public interface HasAlias extends Document, HasName {
    
    /**
     * The key of the {@code alias} property.
     */
    final String ALIAS = "alias";
    
    /**
     * Returns the alias of the specified document. The alias is an optional
     * string value located under the {@link #ALIAS} key.
     * 
     * @return  the alias or an empty {@code Optional} if none was specified
     */
    default Optional<String> getAlias() {
        return getAsString(ALIAS);
    }
    
    /**
     * Returns the java name of this {@link Document}. If an alias is specified 
     * by {@link #getAlias()}, it will be returned, but if no alias exist the 
     * database name returned by {@link #getName()} will be used.
     * 
     * @return  the java name
     */
    default String getJavaName() {
        return getAlias().orElse(getName());
    }
    
    /**
     * Returns a wrapper of the specified document that implements the 
     * {@link HasAlias} trait. If the specified document already implements the
     * trait, it is returned unwrapped.
     * 
     * @param document  the document to wrap
     * @return          the wrapper
     */
    static HasAlias of(Document document) {
        return viewOf(document, HasAlias.class, HasAliasView::new);
    }
}

/**
 * A wrapper class that makes sure that a given {@link Document} implements the
 * {@link HasAlias} trait.
 * 
 * @author  Emil Forslund
 * @since   2.3
 */
class HasAliasView extends AbstractTraitView implements HasAlias {

    /**
     * Constructs a new alias view of with the specified parent and data.
     * 
     * @param parent         the parent of the wrapped document
     * @param data           the data of the wrapped document
     * @param mainInterface  the main interface of the wrapped document
     */
    HasAliasView(Document parent, Map<String, Object> data, Class<? extends Document> mainInterface) {
        super(parent, data, mainInterface);
    }
}