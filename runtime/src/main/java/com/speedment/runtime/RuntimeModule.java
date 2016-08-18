package com.speedment.runtime;

import com.speedment.common.dagger.Module;
import com.speedment.common.dagger.Provides;
import com.speedment.runtime.component.DbmsHandlerComponent;
import com.speedment.runtime.component.EntityManager;
import com.speedment.runtime.component.InfoComponent;
import com.speedment.runtime.component.ManagerComponent;
import com.speedment.runtime.component.PasswordComponent;
import com.speedment.runtime.component.PrimaryKeyFactoryComponent;
import com.speedment.runtime.component.ProjectComponent;
import com.speedment.runtime.component.StreamSupplierComponent;
import com.speedment.runtime.component.connectionpool.ConnectionPoolComponent;
import com.speedment.runtime.component.resultset.ResultSetMapperComponent;
import com.speedment.runtime.config.parameter.DbmsType;
import com.speedment.runtime.db.StandardDbmsTypes;
import com.speedment.runtime.internal.component.ConnectionPoolComponentImpl;
import com.speedment.runtime.internal.component.DbmsHandlerComponentImpl;
import com.speedment.runtime.internal.component.EntityManagerImpl;
import com.speedment.runtime.internal.component.InfoComponentImpl;
import com.speedment.runtime.internal.component.ManagerComponentImpl;
import com.speedment.runtime.internal.component.NativeStreamSupplierComponentImpl;
import com.speedment.runtime.internal.component.PasswordComponentImpl;
import com.speedment.runtime.internal.component.PrimaryKeyFactoryComponentImpl;
import com.speedment.runtime.internal.component.ProjectComponentImpl;
import com.speedment.runtime.internal.component.ResultSetMapperComponentImpl;
import com.speedment.runtime.internal.config.dbms.StandardDbmsTypesImpl;
import com.speedment.runtime.manager.Manager;
import java.util.Set;
import javax.inject.Singleton;

/**
 * A dependency injection module that specifies providers for all the standard
 * components in Speedment.
 * 
 * @author  Emil Forslund
 * @since   3.0.0
 */
@Module(injects = {
    ConnectionPoolComponent.class,
    DbmsHandlerComponent.class,
    EntityManager.class,
    InfoComponent.class,
    ManagerComponent.class,
    StreamSupplierComponent.class,
    PasswordComponent.class,
    PrimaryKeyFactoryComponent.class,
    ProjectComponent.class,
    ResultSetMapperComponent.class,
    StandardDbmsTypes.class
})
public final class RuntimeModule {

    @Provides @Singleton
    public ConnectionPoolComponent provideConnectionPoolComponent() {
        return new ConnectionPoolComponentImpl();
    }
    
    @Provides @Singleton
    public DbmsHandlerComponent provideDbmsHandlerComponent(Set<DbmsType> dbmsTypes) {
        return new DbmsHandlerComponentImpl(dbmsTypes);
    }
    
    @Provides @Singleton
    public EntityManager provideEntityManager() {
        return new EntityManagerImpl();
    }
    
    @Provides @Singleton
    public InfoComponent provideInfoComponent() {
        return new InfoComponentImpl();
    }
    
    @Provides @Singleton
    public ManagerComponent provideManagerComponent(Set<Manager<?>> managers) {
        return new ManagerComponentImpl(managers);
    }
    
    @Provides @Singleton
    public StreamSupplierComponent provideStreamSupplierComponent() {
        return new NativeStreamSupplierComponentImpl();
    }
    
    @Provides @Singleton
    public PasswordComponent providePasswordComponent() {
        return new PasswordComponentImpl();
    }
    
    @Provides @Singleton
    public PrimaryKeyFactoryComponent providePrimaryKeyFactoryComponent() {
        return new PrimaryKeyFactoryComponentImpl();
    }
    
    @Provides @Singleton
    public ProjectComponent provideProjectComponent() {
        return new ProjectComponentImpl();
    }
    
    @Provides @Singleton
    public ResultSetMapperComponent provideResultSetMapperComponent() {
        return new ResultSetMapperComponentImpl();
    }
    
    @Provides @Singleton
    public StandardDbmsTypes provideStandardDbmsTypes() {
        return new StandardDbmsTypesImpl();
    }
}
