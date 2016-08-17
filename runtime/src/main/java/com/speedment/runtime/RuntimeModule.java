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
import javax.inject.Singleton;

/**
 * A dependency injection module that specifies providers for all the standard
 * components in Speedment.
 * 
 * @author  Emil Forslund
 * @since   3.0.0
 */
@Module
public final class RuntimeModule {
    
    @Provides @Singleton
    public ConnectionPoolComponent provideConnectionPoolComponent() {
        return new ConnectionPoolComponentImpl();
    }
    
    @Provides @Singleton
    public DbmsHandlerComponent provideDbmsHandlerComponent() {
        return new DbmsHandlerComponentImpl();
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
    public ManagerComponent provideManagerComponent() {
        return new ManagerComponentImpl();
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
