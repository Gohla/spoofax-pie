package mb.spoofax.compiler.spoofaxcore;

import mb.resource.hierarchical.ResourcePath;
import mb.spoofax.compiler.util.ClassKind;
import mb.spoofax.compiler.util.GradleConfiguredBundleDependency;
import mb.spoofax.compiler.util.GradleConfiguredDependency;
import mb.spoofax.compiler.util.GradleDependency;
import mb.spoofax.compiler.util.TemplateCompiler;
import mb.spoofax.compiler.util.TemplateWriter;
import mb.spoofax.compiler.util.TypeInfo;
import org.immutables.value.Value;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Enclosing
public class EclipseProject {
    private final TemplateWriter buildGradleTemplate;
    private final TemplateWriter pluginXmlTemplate;
    private final TemplateWriter manifestTemplate;
    private final TemplateWriter packageInfoTemplate;
    private final TemplateWriter pluginTemplate;
    private final TemplateWriter moduleTemplate;
    private final TemplateWriter componentTemplate;
    private final TemplateWriter identifiersTemplate;
    private final TemplateWriter documentProviderTemplate;
    private final TemplateWriter editorTemplate;
    private final TemplateWriter editorTrackerTemplate;
    private final TemplateWriter natureTemplate;
    private final TemplateWriter addNatureHandlerTemplate;
    private final TemplateWriter removeNatureHandlerTemplate;
    private final TemplateWriter projectBuilderTemplate;
    private final TemplateWriter mainMenuTemplate;
    private final TemplateWriter editorContextMenuTemplate;
    private final TemplateWriter resourceContextMenuTemplate;
    private final TemplateWriter runCommandHandlerTemplate;
    private final TemplateWriter observeHandlerTemplate;
    private final TemplateWriter unobserveHandlerTemplate;

    public EclipseProject(TemplateCompiler templateCompiler) {
        this.buildGradleTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/build.gradle.kts.mustache");
        this.pluginXmlTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/plugin.xml.mustache");
        this.manifestTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/MANIFEST.MF.mustache");
        this.packageInfoTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/package-info.java.mustache");
        this.pluginTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/Plugin.java.mustache");
        this.moduleTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/Module.java.mustache");
        this.componentTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/Component.java.mustache");
        this.identifiersTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/Identifiers.java.mustache");
        this.documentProviderTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/DocumentProvider.java.mustache");
        this.editorTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/Editor.java.mustache");
        this.editorTrackerTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/EditorTracker.java.mustache");
        this.natureTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/Nature.java.mustache");
        this.addNatureHandlerTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/AddNatureHandler.java.mustache");
        this.removeNatureHandlerTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/RemoveNatureHandler.java.mustache");
        this.projectBuilderTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/ProjectBuilder.java.mustache");
        this.mainMenuTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/MainMenu.java.mustache");
        this.editorContextMenuTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/EditorContextMenu.java.mustache");
        this.resourceContextMenuTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/ResourceContextMenu.java.mustache");
        this.runCommandHandlerTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/RunCommandHandler.java.mustache");
        this.observeHandlerTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/ObserveHandler.java.mustache");
        this.unobserveHandlerTemplate = templateCompiler.getOrCompileToWriter("eclipse_project/UnobserveHandler.java.mustache");
    }

    public Output compile(EclipseProject.Input input) throws IOException {
        final Shared shared = input.shared();

        // Gradle files
        {
            final HashMap<String, Object> map = new HashMap<>();

            final ArrayList<GradleConfiguredDependency> dependencies = new ArrayList<>(input.additionalDependencies());
            dependencies.add(GradleConfiguredDependency.compileOnly(shared.checkerFrameworkQualifiersDep()));
            dependencies.add(GradleConfiguredDependency.annotationProcessor(shared.daggerCompilerDep()));
            map.put("dependencyCodes", dependencies.stream().map(GradleConfiguredDependency::toKotlinCode).collect(Collectors.toCollection(ArrayList::new)));

            final ArrayList<GradleConfiguredBundleDependency> bundleDependencies = new ArrayList<>(input.additionalBundleDependencies());
            bundleDependencies.add(GradleConfiguredBundleDependency.bundle(shared.spoofaxEclipseDep(), true));
            bundleDependencies.add(GradleConfiguredBundleDependency.embeddingBundle(input.eclipseExternaldepsDependency(), true));
            bundleDependencies.add(GradleConfiguredBundleDependency.embeddingBundle(shared.spoofaxEclipseExternaldepsDep(), true));
            map.put("bundleDependencyCodes", bundleDependencies.stream().map(GradleConfiguredBundleDependency::toKotlinCode).collect(Collectors.toCollection(ArrayList::new)));

            buildGradleTemplate.write(input, map, input.buildGradleKtsFile());
        }

        // Eclipse files
        pluginXmlTemplate.write(input, input.pluginXmlFile());
        manifestTemplate.write(input, input.manifestMfFile());

        // Class files
        final ResourcePath classesGenDirectory = input.classesGenDirectory();
        packageInfoTemplate.write(input, input.genPackageInfo().file(classesGenDirectory));
        pluginTemplate.write(input, input.genPlugin().file(classesGenDirectory));
        moduleTemplate.write(input, input.genEclipseModule().file(classesGenDirectory));
        componentTemplate.write(input, input.genEclipseComponent().file(classesGenDirectory));
        identifiersTemplate.write(input, input.genEclipseIdentifiers().file(classesGenDirectory));
        documentProviderTemplate.write(input, input.genDocumentProvider().file(classesGenDirectory));
        editorTemplate.write(input, input.genEditor().file(classesGenDirectory));
        editorTrackerTemplate.write(input, input.genEditorTracker().file(classesGenDirectory));
        natureTemplate.write(input, input.genNature().file(classesGenDirectory));
        addNatureHandlerTemplate.write(input, input.addNatureHandler().file(classesGenDirectory));
        removeNatureHandlerTemplate.write(input, input.removeNatureHandler().file(classesGenDirectory));
        projectBuilderTemplate.write(input, input.genProjectBuilder().file(classesGenDirectory));
        mainMenuTemplate.write(input, input.genMainMenu().file(classesGenDirectory));
        editorContextMenuTemplate.write(input, input.genEditorContextMenu().file(classesGenDirectory));
        resourceContextMenuTemplate.write(input, input.genResourceContextMenu().file(classesGenDirectory));
        runCommandHandlerTemplate.write(input, input.genRunCommandHandler().file(classesGenDirectory));
        observeHandlerTemplate.write(input, input.genObserveHandler().file(classesGenDirectory));
        unobserveHandlerTemplate.write(input, input.genUnobserveHandler().file(classesGenDirectory));

        return Output.builder().fromInput(input).build();
    }


    @Value.Immutable
    public interface Input extends Serializable {
        class Builder extends EclipseProjectData.Input.Builder {}

        static Builder builder() { return new Builder(); }


        Shared shared();

        AdapterProject.Input adapterProject();


        /// Gradle configuration

        GradleDependency eclipseExternaldepsDependency();

        List<GradleConfiguredDependency> additionalDependencies();

        List<GradleConfiguredBundleDependency> additionalBundleDependencies();


        /// Gradle files

        default ResourcePath buildGradleKtsFile() {
            return shared().eclipseProject().baseDirectory().appendRelativePath("build.gradle.kts");
        }


        /// Eclipse configuration

        @Value.Default default String pluginId() { return shared().eclipseProject().coordinate().artifactId(); }

        @Value.Default default String contextId() { return pluginId() + ".context"; }

        @Value.Default default String documentProviderId() { return pluginId() + ".documentprovider"; }

        @Value.Default default String editorId() { return pluginId() + ".editor"; }

        @Value.Default default String natureRelativeId() { return "nature"; }

        @Value.Default default String natureId() { return pluginId() + "." + natureRelativeId(); }

        @Value.Default default String addNatureCommandId() { return natureId() + ".add"; }

        @Value.Default default String removeNatureCommandId() { return natureId() + ".remove"; }

        @Value.Default default String projectBuilderRelativeId() { return "builder"; }

        @Value.Default default String projectBuilderId() { return pluginId() + "." + projectBuilderRelativeId(); }

        @Value.Default default String baseMarkerId() { return pluginId() + ".marker"; }

        @Value.Default default String infoMarkerId() { return baseMarkerId() + ".info"; }

        @Value.Default default String warningMarkerId() { return baseMarkerId() + ".warning"; }

        @Value.Default default String errorMarkerId() { return baseMarkerId() + ".error"; }

        @Value.Default default String observeCommandId() { return pluginId() + ".observe"; }

        @Value.Default default String unobserveCommandId() { return pluginId() + ".unobserve"; }

        @Value.Default default String runCommandId() { return pluginId() + ".runcommand"; }

        @Value.Default default String baseMenuId() { return pluginId() + ".menu"; }

        @Value.Default default String resourceContextMenuId() { return baseMenuId() + ".resource.context"; }

        @Value.Default default String editorContextMenuId() { return baseMenuId() + ".editor.context"; }

        @Value.Default default String mainMenuId() { return baseMenuId() + ".main"; }

        @Value.Default default String mainMenuDynamicId() { return mainMenuId() + ".dynamic"; }


        Optional<ResourcePath> fileIconRelativePath();


        /// Eclipse files

        default ResourcePath pluginXmlFile() {
            return shared().eclipseProject().genSourceSpoofaxResourcesDirectory().appendRelativePath("plugin.xml");
        }

        default ResourcePath manifestMfFile() {
            return shared().eclipseProject().genSourceSpoofaxResourcesDirectory().appendRelativePath("META-INF/MANIFEST.MF");
        }


        /// Kinds of classes (generated/extended/manual)

        @Value.Default default ClassKind classKind() {
            return ClassKind.Generated;
        }

        default ResourcePath classesGenDirectory() {
            return shared().eclipseProject().genSourceSpoofaxJavaDirectory();
        }


        /// Eclipse project classes

        // package-info

        @Value.Default default TypeInfo genPackageInfo() {
            return TypeInfo.of(shared().eclipseProjectPackage(), "package-info");
        }

        Optional<TypeInfo> manualPackageInfo();

        default TypeInfo packageInfo() {
            if(classKind().isManual() && manualPackageInfo().isPresent()) {
                return manualPackageInfo().get();
            }
            return genPackageInfo();
        }

        // Plugin

        @Value.Default default TypeInfo genPlugin() {
            return TypeInfo.of(shared().eclipseProjectPackage(), shared().defaultClassPrefix() + "Plugin");
        }

        Optional<TypeInfo> manualPlugin();

        default TypeInfo plugin() {
            if(classKind().isManual() && manualPlugin().isPresent()) {
                return manualPlugin().get();
            }
            return genPlugin();
        }

        // Dagger component

        @Value.Default default TypeInfo genEclipseComponent() {
            return TypeInfo.of(shared().eclipseProjectPackage(), shared().defaultClassPrefix() + "EclipseComponent");
        }

        Optional<TypeInfo> manualEclipseComponent();

        default TypeInfo eclipseComponent() {
            if(classKind().isManual() && manualEclipseComponent().isPresent()) {
                return manualEclipseComponent().get();
            }
            return genEclipseComponent();
        }

        default TypeInfo daggerEclipseComponent() {
            return TypeInfo.of(eclipseComponent().packageId(), "Dagger" + eclipseComponent().id());
        }

        // Dagger module

        @Value.Default default TypeInfo genEclipseModule() {
            return TypeInfo.of(shared().eclipseProjectPackage(), shared().defaultClassPrefix() + "EclipseModule");
        }

        Optional<TypeInfo> manualEclipseModule();

        default TypeInfo eclipseModule() {
            if(classKind().isManual() && manualEclipseModule().isPresent()) {
                return manualEclipseModule().get();
            }
            return genEclipseModule();
        }

        // Eclipse Identifiers

        @Value.Default default TypeInfo genEclipseIdentifiers() {
            return TypeInfo.of(shared().eclipseProjectPackage(), shared().defaultClassPrefix() + "EclipseIdentifiers");
        }

        Optional<TypeInfo> manualEclipseIdentifiers();

        default TypeInfo eclipseIdentifiers() {
            if(classKind().isManual() && manualEclipseIdentifiers().isPresent()) {
                return manualEclipseIdentifiers().get();
            }
            return genEclipseIdentifiers();
        }

        // Document provider

        @Value.Default default TypeInfo genDocumentProvider() {
            return TypeInfo.of(shared().eclipseProjectPackage(), shared().defaultClassPrefix() + "DocumentProvider");
        }

        Optional<TypeInfo> manualDocumentProvider();

        default TypeInfo documentProvider() {
            if(classKind().isManual() && manualDocumentProvider().isPresent()) {
                return manualDocumentProvider().get();
            }
            return genDocumentProvider();
        }

        // Editor

        @Value.Default default TypeInfo genEditor() {
            return TypeInfo.of(shared().eclipseProjectPackage(), shared().defaultClassPrefix() + "Editor");
        }

        Optional<TypeInfo> manualEditor();

        default TypeInfo editor() {
            if(classKind().isManual() && manualEditor().isPresent()) {
                return manualEditor().get();
            }
            return genEditor();
        }

        // Editor Tracker

        @Value.Default default TypeInfo genEditorTracker() {
            return TypeInfo.of(shared().eclipseProjectPackage(), shared().defaultClassPrefix() + "EditorTracker");
        }

        Optional<TypeInfo> manualEditorTracker();

        default TypeInfo editorTracker() {
            if(classKind().isManual() && manualEditorTracker().isPresent()) {
                return manualEditorTracker().get();
            }
            return genEditorTracker();
        }

        // Project nature

        @Value.Default default TypeInfo genNature() {
            return TypeInfo.of(shared().eclipseProjectPackage(), shared().defaultClassPrefix() + "Nature");
        }

        Optional<TypeInfo> manualNature();

        default TypeInfo nature() {
            if(classKind().isManual() && manualNature().isPresent()) {
                return manualNature().get();
            }
            return genNature();
        }

        // Add nature handler

        @Value.Default default TypeInfo genAddNatureHandler() {
            return TypeInfo.of(shared().eclipseProjectPackage(), shared().defaultClassPrefix() + "AddNatureHandler");
        }

        Optional<TypeInfo> manualAddNatureHandler();

        default TypeInfo addNatureHandler() {
            if(classKind().isManual() && manualAddNatureHandler().isPresent()) {
                return manualAddNatureHandler().get();
            }
            return genAddNatureHandler();
        }

        // Remove nature handler

        @Value.Default default TypeInfo genRemoveNatureHandler() {
            return TypeInfo.of(shared().eclipseProjectPackage(), shared().defaultClassPrefix() + "RemoveNatureHandler");
        }

        Optional<TypeInfo> manualRemoveNatureHandler();

        default TypeInfo removeNatureHandler() {
            if(classKind().isManual() && manualRemoveNatureHandler().isPresent()) {
                return manualRemoveNatureHandler().get();
            }
            return genRemoveNatureHandler();
        }

        // Project builder

        @Value.Default default TypeInfo genProjectBuilder() {
            return TypeInfo.of(shared().eclipseProjectPackage(), shared().defaultClassPrefix() + "ProjectBuilder");
        }

        Optional<TypeInfo> manualProjectBuilder();

        default TypeInfo projectBuilder() {
            if(classKind().isManual() && manualProjectBuilder().isPresent()) {
                return manualProjectBuilder().get();
            }
            return genProjectBuilder();
        }

        // Main menu

        @Value.Default default TypeInfo genMainMenu() {
            return TypeInfo.of(shared().eclipseProjectPackage(), shared().defaultClassPrefix() + "MainMenu");
        }

        Optional<TypeInfo> manualMainMenu();

        default TypeInfo mainMenu() {
            if(classKind().isManual() && manualMainMenu().isPresent()) {
                return manualMainMenu().get();
            }
            return genMainMenu();
        }

        // Editor context menu

        @Value.Default default TypeInfo genEditorContextMenu() {
            return TypeInfo.of(shared().eclipseProjectPackage(), shared().defaultClassPrefix() + "EditorContextMenu");
        }

        Optional<TypeInfo> manualEditorContextMenu();

        default TypeInfo editorContextMenu() {
            if(classKind().isManual() && manualEditorContextMenu().isPresent()) {
                return manualEditorContextMenu().get();
            }
            return genEditorContextMenu();
        }

        // Resource context menu

        @Value.Default default TypeInfo genResourceContextMenu() {
            return TypeInfo.of(shared().eclipseProjectPackage(), shared().defaultClassPrefix() + "ResourceContextMenu");
        }

        Optional<TypeInfo> manualResourceContextMenu();

        default TypeInfo resourceContextMenu() {
            if(classKind().isManual() && manualResourceContextMenu().isPresent()) {
                return manualResourceContextMenu().get();
            }
            return genResourceContextMenu();
        }

        // Command handler

        @Value.Default default TypeInfo genRunCommandHandler() {
            return TypeInfo.of(shared().eclipseProjectPackage(), shared().defaultClassPrefix() + "RunCommandHandler");
        }

        Optional<TypeInfo> manualRunCommandHandler();

        default TypeInfo runCommandHandler() {
            if(classKind().isManual() && manualRunCommandHandler().isPresent()) {
                return manualRunCommandHandler().get();
            }
            return genRunCommandHandler();
        }

        // Observe handler

        @Value.Default default TypeInfo genObserveHandler() {
            return TypeInfo.of(shared().eclipseProjectPackage(), shared().defaultClassPrefix() + "ObserveHandler");
        }

        Optional<TypeInfo> manualObserveHandler();

        default TypeInfo observeHandler() {
            if(classKind().isManual() && manualObserveHandler().isPresent()) {
                return manualObserveHandler().get();
            }
            return genObserveHandler();
        }

        // Unobserve handler

        @Value.Default default TypeInfo genUnobserveHandler() {
            return TypeInfo.of(shared().eclipseProjectPackage(), shared().defaultClassPrefix() + "UnobserveHandler");
        }

        Optional<TypeInfo> manualUnobserveHandler();

        default TypeInfo unobserveHandler() {
            if(classKind().isManual() && manualUnobserveHandler().isPresent()) {
                return manualUnobserveHandler().get();
            }
            return genUnobserveHandler();
        }


        // TODO: implement check
    }

    @Value.Immutable
    public interface Output extends Serializable {
        class Builder extends EclipseProjectData.Output.Builder {
            public Builder fromInput(Input input) {
                return this;
            }
        }

        static Builder builder() {
            return new Builder();
        }
    }
}
