package mb.spoofax.compiler.spoofaxcore;

import mb.resource.fs.FSPath;
import mb.spoofax.compiler.spoofaxcore.tiger.TigerInputs;
import mb.spoofax.compiler.util.GradleDependency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

class EclipseProjectTest extends TestBase {
    @Test void testCompilerDefaults(@TempDir Path temporaryDirectoryPath) throws IOException {
        final FSPath baseDirectory = new FSPath(temporaryDirectoryPath);
        final Shared shared = TigerInputs.shared(baseDirectory);

        // Compile language and adapter projects.
        final AdapterProject.Input adapterProjectInput = compileLanguageAndAdapterProject(shared);

        // Compile Eclipse externaldeps project, as Eclipse project depends on it.
        eclipseExternaldepsProjectCompiler.compile(TigerInputs.eclipseExternaldepsProjectBuilder(shared)
            .languageProjectDependency(GradleDependency.project(":" + shared.languageProject().coordinate().artifactId()))
            .adapterProjectDependency(GradleDependency.project(":" + shared.adapterProject().coordinate().artifactId()))
            .build()
        );

        // Compile Eclipse project and test generated files.
        final EclipseProject.Input input = TigerInputs.eclipseProjectBuilder(shared, adapterProjectInput)
            .eclipseExternaldepsDependency(GradleDependency.project(":" + shared.eclipseExternaldepsProject().coordinate().artifactId()))
            .build();
        eclipseProjectCompiler.compile(input);
        fileAssertions.asserts(input.buildGradleKtsFile(), (a) -> a.assertContains("org.metaborg.coronium.bundle"));
        fileAssertions.asserts(input.pluginXmlFile(), (s) -> s.assertAll("plugin.xml", "<plugin>"));
        fileAssertions.asserts(input.manifestMfFile(), (s) -> s.assertAll("MANIFEST.MF", "Export-Package"));
        fileAssertions.scopedExists(input.classesGenDirectory(), (s) -> {
            s.asserts(input.packageInfo(), (a) -> a.assertAll("package-info.java", "@DefaultQualifier(NonNull.class)"));
            s.assertPublicJavaClass(input.genPlugin(), "TigerPlugin");
            s.assertPublicJavaInterface(input.genEclipseComponent(), "TigerEclipseComponent");
            s.assertPublicJavaClass(input.genEclipseModule(), "TigerEclipseModule");
            s.assertPublicJavaClass(input.genEclipseIdentifiers(), "TigerEclipseIdentifiers");
            s.assertPublicJavaClass(input.genEditor(), "TigerEditor");
            s.assertPublicJavaClass(input.genEditorTracker(), "TigerEditorTracker");
            s.assertPublicJavaClass(input.genNature(), "TigerNature");
            s.assertPublicJavaClass(input.genAddNatureHandler(), "TigerAddNatureHandler");
            s.assertPublicJavaClass(input.genRemoveNatureHandler(), "TigerRemoveNatureHandler");
            s.assertPublicJavaClass(input.genProjectBuilder(), "TigerProjectBuilder");
            s.assertPublicJavaClass(input.genMainMenu(), "TigerMainMenu");
            s.assertPublicJavaClass(input.genEditorContextMenu(), "TigerEditorContextMenu");
            s.assertPublicJavaClass(input.genResourceContextMenu(), "TigerResourceContextMenu");
            s.assertPublicJavaClass(input.genRunCommandHandler(), "TigerRunCommandHandler");
            s.assertPublicJavaClass(input.genObserveHandler(), "TigerObserveHandler");
            s.assertPublicJavaClass(input.genUnobserveHandler(), "TigerUnobserveHandler");
        });

        // Compile root project, which links together all projects, and build it.
        final RootProject.Output rootProjectOutput = rootProjectCompiler.compile(TigerInputs.rootProjectBuilder(shared)
            .addIncludedProjects(
                shared.languageProject().coordinate().artifactId(),
                shared.adapterProject().coordinate().artifactId(),
                shared.eclipseExternaldepsProject().coordinate().artifactId(),
                shared.eclipseProject().coordinate().artifactId()
            )
            .build()
        );
        fileAssertions.asserts(rootProjectOutput.baseDirectory(), (a) -> a.assertGradleBuild("buildAll"));
    }
}
