package com.speedment.runtime.db;

import com.speedment.runtime.config.parameter.DbmsType;
import java.util.stream.Stream;

/**
 *
 * @author  Emil Forslund
 * @since   3.0.0
 */
public interface StandardDbmsTypes {
    
    /**
     * Stream over all the <em>standard</em> types. This is not a stream
     * over all installed types.
     * 
     * @return  stream of standard dbms types
     */
    Stream<DbmsType> stream();

    /**
     * Return the default dbms type. Observe that there is no guarantee
     * this type is installed.
     * 
     * @return  default type
     */
    DbmsType defaultType();
}