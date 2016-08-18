package com.speedment.runtime;

import com.speedment.common.dagger.Module;
import com.speedment.common.dagger.Provides;
import static java.util.Objects.requireNonNull;
import javax.inject.Singleton;

/**
 * A dependency injection module that holds the application and metadata 
 * implementations and can provide these.
 * 
 * @param <APP>  the application type
 * 
 * @author  Emil Forslund
 * @since   3.0.0
 */
@Module(complete = false, injects = {
    Speedment.class,
    ApplicationMetadata.class
})
public final class ApplicationModule<APP extends Speedment> {
    
    private final Class<APP> application;
    private final Class<? extends ApplicationMetadata> metadata;
    
    public ApplicationModule(
            Class<APP> application, 
            Class<? extends ApplicationMetadata> metadata) {
        
        this.application = requireNonNull(application);
        this.metadata    = requireNonNull(metadata);
    }
    
    @Provides @Singleton
    public Speedment provideApplication() 
    throws InstantiationException, IllegalAccessException {
        return application.newInstance();
    }
    
    @Provides @Singleton
    public ApplicationMetadata provideMetadata() 
    throws InstantiationException, IllegalAccessException {
        return metadata.newInstance();
    }
}