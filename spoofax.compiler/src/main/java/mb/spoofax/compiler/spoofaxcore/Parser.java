package mb.spoofax.compiler.spoofaxcore;

import com.samskivert.mustache.Template;
import mb.resource.ResourceService;
import mb.resource.hierarchical.ResourcePath;
import mb.spoofax.compiler.util.TypeInfo;
import mb.spoofax.compiler.util.ClassKind;
import mb.spoofax.compiler.util.GradleConfiguredDependency;
import mb.spoofax.compiler.util.ResourceWriter;
import mb.spoofax.compiler.util.TemplateCompiler;
import org.immutables.value.Value;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

@Value.Enclosing
public class Parser {
    private final Template tableTemplate;
    private final Template parserTemplate;
    private final Template factoryTemplate;
    private final Template parseTaskDefTemplate;
    private final Template tokenizeTaskDefTemplate;
    private final ResourceService resourceService;
    private final Charset charset;


    private Parser(
        Template tableTemplate,
        Template parserTemplate,
        Template factoryTemplate,
        Template parseTaskDefTemplate,
        Template tokenizeTaskDefTemplate,
        ResourceService resourceService,
        Charset charset
    ) {
        this.tableTemplate = tableTemplate;
        this.parserTemplate = parserTemplate;
        this.factoryTemplate = factoryTemplate;
        this.parseTaskDefTemplate = parseTaskDefTemplate;
        this.tokenizeTaskDefTemplate = tokenizeTaskDefTemplate;
        this.resourceService = resourceService;
        this.charset = charset;
    }

    public static Parser fromClassLoaderResources(ResourceService resourceService, Charset charset) {
        final TemplateCompiler templateCompiler = new TemplateCompiler(Parser.class);
        return new Parser(
            templateCompiler.getOrCompile("parser/ParseTable.java.mustache"),
            templateCompiler.getOrCompile("parser/Parser.java.mustache"),
            templateCompiler.getOrCompile("parser/ParserFactory.java.mustache"),
            templateCompiler.getOrCompile("parser/ParseTaskDef.java.mustache"),
            templateCompiler.getOrCompile("parser/TokenizeTaskDef.java.mustache"),
            resourceService,
            charset
        );
    }


    public LanguageProjectOutput compileLanguageProject(Input input) throws IOException {
        final LanguageProjectOutput output = LanguageProjectOutput.builder().fromInput(input).build();
        if(input.classKind().isManualOnly()) return output; // Nothing to generate: return.

        final ResourcePath genDirectory = input.languageGenDirectory();
        resourceService.getHierarchicalResource(genDirectory).ensureDirectoryExists();

        try(final ResourceWriter writer = new ResourceWriter(resourceService.getHierarchicalResource(input.genTable().file(genDirectory)).createParents(), charset)) {
            tableTemplate.execute(input, writer);
            writer.flush();
        }

        try(final ResourceWriter writer = new ResourceWriter(resourceService.getHierarchicalResource(input.genParser().file(genDirectory)).createParents(), charset)) {
            parserTemplate.execute(input, writer);
            writer.flush();
        }

        try(final ResourceWriter writer = new ResourceWriter(resourceService.getHierarchicalResource(input.genFactory().file(genDirectory)).createParents(), charset)) {
            factoryTemplate.execute(input, writer);
            writer.flush();
        }

        return output;
    }

    public AdapterProjectOutput compileAdapterProject(Input input) throws IOException {
        final AdapterProjectOutput output = AdapterProjectOutput.builder().fromInput(input).build();
        if(input.classKind().isManualOnly()) return output; // Nothing to generate: return.

        final ResourcePath genDirectory = input.adapterGenDirectory();
        resourceService.getHierarchicalResource(genDirectory).ensureDirectoryExists();

        try(final ResourceWriter writer = new ResourceWriter(resourceService.getHierarchicalResource(input.genParseTaskDef().file(genDirectory)).createParents(), charset)) {
            parseTaskDefTemplate.execute(input, writer);
            writer.flush();
        }

        try(final ResourceWriter writer = new ResourceWriter(resourceService.getHierarchicalResource(input.genTokenizeTaskDef().file(genDirectory)).createParents(), charset)) {
            tokenizeTaskDefTemplate.execute(input, writer);
            writer.flush();
        }

        return output;
    }


    @Value.Immutable
    public interface Input extends Serializable {
        class Builder extends ParserData.Input.Builder {}

        static Builder builder() {
            return new Builder();
        }


        Shared shared();


        /// Configuration

        String startSymbol();


        /// Parse table source file (to copy from), and destination file

        @Value.Default default String tableSourceRelPath() {
            return "target/metaborg/sdf.tbl";
        }

        @Value.Default default String tableTargetRelPath() {
            return shared().languageProject().packagePath() + "/" + tableSourceRelPath();
        }


        /// Kinds of classes (generated/extended/manual)

        @Value.Default default ClassKind classKind() {
            return ClassKind.Generated;
        }


        /// Language project classes

        default ResourcePath languageGenDirectory() {
            return shared().languageProject().genSourceSpoofaxJavaDirectory();
        }

        // Parse table

        @Value.Default default TypeInfo genTable() {
            return TypeInfo.of(shared().languagePackage(), shared().classSuffix() + "ParseTable");
        }

        // Parser

        @Value.Default default TypeInfo genParser() {
            return TypeInfo.of(shared().languagePackage(), shared().classSuffix() + "Parser");
        }

        Optional<TypeInfo> manualParser();

        default TypeInfo parser() {
            if(classKind().isManual() && manualParser().isPresent()) {
                return manualParser().get();
            }
            return genParser();
        }

        // Parser factory

        @Value.Default default TypeInfo genFactory() {
            return TypeInfo.of(shared().languagePackage(), shared().classSuffix() + "ParserFactory");
        }

        Optional<TypeInfo> manualFactory();

        default TypeInfo factory() {
            if(classKind().isManual() && manualFactory().isPresent()) {
                return manualFactory().get();
            }
            return genFactory();
        }


        /// Adapter project classes

        default ResourcePath adapterGenDirectory() {
            return shared().adapterProject().genSourceSpoofaxJavaDirectory();
        }

        // Parse task definition

        @Value.Default default TypeInfo genParseTaskDef() {
            return TypeInfo.of(shared().adapterTaskPackage(), shared().classSuffix() + "Parse");
        }

        Optional<TypeInfo> manualParseTaskDef();

        default TypeInfo parseTaskDef() {
            if(classKind().isManual() && manualParseTaskDef().isPresent()) {
                return manualParseTaskDef().get();
            }
            return genParseTaskDef();
        }

        // Tokenize task definition

        @Value.Default default TypeInfo genTokenizeTaskDef() {
            return TypeInfo.of(shared().adapterTaskPackage(), shared().classSuffix() + "Tokenize");
        }

        Optional<TypeInfo> manualTokenizeTaskDef();

        default TypeInfo tokenizeTaskDef() {
            if(classKind().isManual() && manualTokenizeTaskDef().isPresent()) {
                return manualTokenizeTaskDef().get();
            }
            return genTokenizeTaskDef();
        }


        @Value.Check default void check() {
            final ClassKind kind = classKind();
            final boolean manual = kind.isManual();
            if(!manual) return;
            if(!manualParser().isPresent()) {
                throw new IllegalArgumentException("Kind '" + kind + "' indicates that a manual class will be used, but 'manualParser' has not been set");
            }
            if(!manualFactory().isPresent()) {
                throw new IllegalArgumentException("Kind '" + kind + "' indicates that a manual class will be used, but 'manualFactory' has not been set");
            }
            if(!manualParseTaskDef().isPresent()) {
                throw new IllegalArgumentException("Kind '" + kind + "' indicates that a manual class will be used, but 'manualParseTaskDef' has not been set");
            }
            if(!manualTokenizeTaskDef().isPresent()) {
                throw new IllegalArgumentException("Kind '" + kind + "' indicates that a manual class will be used, but 'manualTokenizeTaskDef' has not been set");
            }
        }
    }

    @Value.Immutable
    public interface LanguageProjectOutput extends Serializable {
        class Builder extends ParserData.LanguageProjectOutput.Builder {
            public Builder fromInput(Input input) {
                addDependencies(
                    GradleConfiguredDependency.api(input.shared().jsglrCommonDep()),
                    GradleConfiguredDependency.api(input.shared().jsglr1CommonDep())
                );
                addCopyResources(input.tableSourceRelPath());
                return this;
            }
        }

        static Builder builder() {
            return new Builder();
        }


        List<GradleConfiguredDependency> dependencies();

        List<String> copyResources();
    }

    @Value.Immutable
    public interface AdapterProjectOutput extends Serializable {
        class Builder extends ParserData.AdapterProjectOutput.Builder {
            public Builder fromInput(Input input) {
                addAdditionalTaskDefs(input.parseTaskDef());
                // Do not add tokenize task definition, as it is handled explicitly.
                return this;
            }
        }

        static Builder builder() {
            return new Builder();
        }


        List<GradleConfiguredDependency> dependencies();

        List<TypeInfo> additionalTaskDefs();
    }
}
