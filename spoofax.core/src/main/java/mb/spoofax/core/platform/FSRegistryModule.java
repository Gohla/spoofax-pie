package mb.spoofax.core.platform;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import mb.resource.ResourceRegistry;
import mb.resource.fs.FSRegistry;

import javax.inject.Singleton;

@Module
public class FSRegistryModule {
    @Provides @Singleton @IntoSet static ResourceRegistry provideFSRegistry() {
        return new FSRegistry();
    }
}