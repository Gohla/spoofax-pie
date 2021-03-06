package mb.spoofax.eclipse;

import dagger.Component;
import mb.pie.dagger.PieModule;
import mb.spoofax.core.platform.ResourceRegistriesModule;
import mb.spoofax.core.platform.LoggerFactoryModule;
import mb.spoofax.core.platform.PlatformComponent;
import mb.spoofax.core.platform.ResourceServiceModule;
import mb.spoofax.eclipse.editor.PartClosedCallback;
import mb.spoofax.eclipse.pie.PieRunner;
import mb.spoofax.eclipse.resource.EclipseResourceRegistryModule;
import mb.spoofax.eclipse.util.ColorShare;
import mb.spoofax.eclipse.util.ResourceUtil;
import mb.spoofax.eclipse.util.StyleUtil;

import javax.inject.Singleton;

@Singleton
@Component(modules = {
    LoggerFactoryModule.class,
    ResourceRegistriesModule.class,
    EclipseResourceRegistryModule.class,
    ResourceServiceModule.class,
    PieModule.class,
    SpoofaxEclipseModule.class
})
public interface SpoofaxEclipseComponent extends PlatformComponent {
    PieRunner getPieRunner();

    ResourceUtil getResourceUtil();

    ColorShare getColorShare();

    StyleUtil getStyleUtils();

    PartClosedCallback partClosedCallback();
}
