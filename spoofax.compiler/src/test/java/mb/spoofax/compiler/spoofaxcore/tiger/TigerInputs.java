package mb.spoofax.compiler.spoofaxcore.tiger;

import mb.common.util.IOUtil;
import mb.common.util.Preconditions;
import mb.resource.ResourceService;
import mb.resource.hierarchical.HierarchicalResource;
import mb.resource.hierarchical.ResourcePath;
import mb.spoofax.compiler.command.ArgProviderRepr;
import mb.spoofax.compiler.command.CommandDefRepr;
import mb.spoofax.compiler.spoofaxcore.AdapterProject;
import mb.spoofax.compiler.spoofaxcore.ConstraintAnalyzer;
import mb.spoofax.compiler.spoofaxcore.LanguageProject;
import mb.spoofax.compiler.spoofaxcore.Parser;
import mb.spoofax.compiler.spoofaxcore.RootProject;
import mb.spoofax.compiler.spoofaxcore.Shared;
import mb.spoofax.compiler.spoofaxcore.StrategoRuntime;
import mb.spoofax.compiler.spoofaxcore.Styler;
import mb.spoofax.compiler.util.Conversion;
import mb.spoofax.compiler.util.GradleDependency;
import mb.spoofax.compiler.util.TypeInfo;
import mb.spoofax.core.language.command.CommandContextType;
import mb.spoofax.core.language.command.CommandExecutionType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Optional;

public class TigerInputs {
    /// Shared input

    public static Shared.Builder sharedBuilder(ResourcePath baseDirectory) {
        return Shared.builder()
            .name("Tiger")
            .basePackageId("mb.tiger")
            .baseDirectory(baseDirectory)
            /// Metaborg log
            .logApiDep(fromSystemProperty("log.api:classpath"))
            /// Metaborg resource
            .resourceDep(fromSystemProperty("resource:classpath"))
            /// Spoofax-PIE
            .commonDep(fromSystemProperty("common:classpath"))
            .jsglrCommonDep(fromSystemProperty("jsglr.common:classpath"))
            .jsglr1CommonDep(fromSystemProperty("jsglr1.common:classpath"))
            .jsglr2CommonDep(fromSystemProperty("jsglr2.common:classpath"))
            .esvCommonDep(fromSystemProperty("esv.common:classpath"))
            .strategoCommonDep(fromSystemProperty("stratego.common:classpath"))
            .constraintCommonDep(fromSystemProperty("constraint.common:classpath"))
            .nabl2CommonDep(fromSystemProperty("nabl2.common:classpath"))
            .statixCommonDep(fromSystemProperty("statix.common:classpath"))
            .spoofaxCompilerInterfacesDep(fromSystemProperty("spoofax.compiler.interfaces:classpath"))
            .spoofaxCoreDep(fromSystemProperty("spoofax.core:classpath"))
            ;
    }

    private static GradleDependency fromSystemProperty(String key) {
        return GradleDependency.files(Preconditions.checkNotNull(System.getProperty(key)));
    }

    public static Shared shared(ResourcePath baseDirectory) {
        return sharedBuilder(baseDirectory).build();
    }


    /// Parser compiler input

    public static Parser.Input.Builder parserBuilder(Shared shared) {
        return Parser.Input.builder()
            .startSymbol("Module")
            .shared(shared)
            ;
    }

    public static Parser.Input parser(Shared shared) {
        return parserBuilder(shared).build();
    }


    /// Styler compiler input

    public static Styler.Input.Builder stylerBuilder(Shared shared) {
        return Styler.Input.builder()
            .parser(parser(shared))
            .shared(shared)
            ;
    }

    public static Styler.Input styler(Shared shared) {
        return stylerBuilder(shared).build();
    }


    /// Stratego runtime builder compiler input

    public static StrategoRuntime.Input.Builder strategoRuntimeBuilder(Shared shared) {
        return StrategoRuntime.Input.builder()
            .shared(shared)
            .addInteropRegisterersByReflection("org.metaborg.lang.tiger.trans.InteropRegisterer", "org.metaborg.lang.tiger.strategies.InteropRegisterer")
            .addNaBL2Primitives(true)
            .addStatixPrimitives(false)
            .copyJavaStrategyClasses(true)
            ;
    }

    public static StrategoRuntime.Input strategoRuntime(Shared shared) {
        return strategoRuntimeBuilder(shared).build();
    }


    /// Constraint analyzer compiler input

    public static ConstraintAnalyzer.Input.Builder constraintAnalyzerBuilder(Shared shared) {
        return ConstraintAnalyzer.Input.builder()
            .shared(shared)
            .parse(parser(shared))
            ;
    }

    public static ConstraintAnalyzer.Input constraintAnalyzer(Shared shared) {
        return constraintAnalyzerBuilder(shared).build();
    }


    /// Language project compiler input

    public static LanguageProject.Input.Builder languageProjectBuilder(Shared shared) {
        return LanguageProject.Input.builder()
            .shared(shared)
            .parser(parser(shared))
            .styler(styler(shared))
            .strategoRuntime(strategoRuntime(shared))
            .constraintAnalyzer(constraintAnalyzer(shared))
            .languageSpecificationDependency(GradleDependency.files(Preconditions.checkNotNull(System.getProperty("org.metaborg.lang.tiger:classpath"))))
            ;
    }

    public static LanguageProject.Input languageProject(Shared shared) {
        return languageProjectBuilder(shared).build();
    }


    /// Adapter project compiler input

    public static AdapterProject.Input.Builder adapterProjectBuilder(Shared shared) {
        final TypeInfo showParsedAst = TypeInfo.of(shared.adapterTaskPackage(), "TigerShowParsedAstTaskDef");
        return AdapterProject.Input.builder()
            .shared(shared)
            .parser(parser(shared))
            .styler(styler(shared))
            .strategoRuntime(strategoRuntime(shared))
            .constraintAnalyzer(constraintAnalyzer(shared))
            .addTaskDefs(showParsedAst)
            .addCommandDefs(CommandDefRepr.builder()
                .type(shared.adapterCommandPackage(), "TigerShowParsedAst")
                .taskDefType(showParsedAst)
                .argType(shared.adapterTaskPackage(), "TigerShowParsedAstTaskDef.Args")
                .displayName("Show parsed AST")
                .addSupportedExecutionTypes(CommandExecutionType.ManualOnce, CommandExecutionType.ManualContinuous)
                .addRequiredContextTypes(CommandContextType.Resource)
                .addParams("resource", TypeInfo.of("mb.resource", "ResourceKey"), true, Optional.empty(), Collections.singletonList(ArgProviderRepr.context()))
                .addParams("region", TypeInfo.of("mb.common.region", "Region"), false, Optional.empty(), Collections.singletonList(ArgProviderRepr.context()))
                .build()
            )
            ;
    }

    public static AdapterProject.Input adapterProject(Shared shared) {
        return adapterProjectBuilder(shared).build();
    }

    public static void copyTaskDefsIntoAdapterProject(AdapterProject.Input input, ResourceService resourceService) throws IOException {
        final ResourcePath srcMainJavaDirectory = input.shared().adapterProject().sourceMainJavaDirectory();
        final String taskPackagePath = Conversion.packageIdToPath(input.shared().adapterTaskPackage());
        final String fileName = "TigerShowParsedAstTaskDef.java";
        try(final @Nullable InputStream inputStream = TigerInputs.class.getResourceAsStream(fileName)) {
            if(inputStream == null) {
                throw new IllegalStateException("Cannot get input stream for resource '" + fileName + "'");
            }
            final HierarchicalResource resource = resourceService.getHierarchicalResource(srcMainJavaDirectory.appendRelativePath(taskPackagePath).appendSegment(fileName)).createParents();
            IOUtil.copy(inputStream, resource.openWrite());
        }
    }


    /// Root project compiler input

    public static RootProject.Input.Builder rootProjectBuilder(Shared shared) {
        return RootProject.Input.builder()
            .shared(shared)
            ;
    }

    public static RootProject.Input rootProject(Shared shared) {
        return rootProjectBuilder(shared).build();
    }
}
