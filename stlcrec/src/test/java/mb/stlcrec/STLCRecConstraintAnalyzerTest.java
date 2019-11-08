package mb.stlcrec;

import mb.common.message.Severity;
import mb.constraint.common.ConstraintAnalyzer;
import mb.constraint.common.ConstraintAnalyzer.MultiFileResult;
import mb.constraint.common.ConstraintAnalyzer.SingleFileResult;
import mb.constraint.common.ConstraintAnalyzerContext;
import mb.constraint.common.ConstraintAnalyzerException;
import mb.jsglr1.common.JSGLR1ParseResult;
import mb.jsglr1.common.JSGLR1ParseTableException;
import mb.log.api.LoggerFactory;
import mb.log.noop.NoopLoggerFactory;
import mb.resource.SimpleResourceKey;
import mb.resource.DefaultResourceService;
import mb.resource.ResourceKey;
import mb.resource.ResourceService;
import mb.resource.fs.FSResourceRegistry;
import mb.resource.url.URLResourceRegistry;
import mb.stratego.common.StrategoIOAgent;
import mb.stratego.common.StrategoRuntime;
import mb.stratego.common.StrategoRuntimeBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.spoofax.interpreter.terms.IStrategoTerm;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class STLCRecConstraintAnalyzerTest {
    private static final String qualifier = "test";

    private final LoggerFactory loggerFactory = new NoopLoggerFactory();
    private final ResourceService resourceService =
        new DefaultResourceService(new FSResourceRegistry(), new URLResourceRegistry());
    private final STLCRecParser parser = new STLCRecParser(STLCRecParseTable.fromClassLoaderResources());
    private final StrategoRuntimeBuilder strategoRuntimeBuilder =
        STLCRecStrategoRuntimeBuilder.fromClassLoaderResources();
    private final StrategoRuntime strategoRuntime =
        STLCRecStatixStrategoRuntimeBuilder.fromClassLoaderResources(strategoRuntimeBuilder).build();
    private final STLCRecConstraintAnalyzer analyzer = new STLCRecConstraintAnalyzer(strategoRuntime);

    STLCRecConstraintAnalyzerTest() throws IOException, JSGLR1ParseTableException {}

    @Test void analyzeSingleErrors() throws InterruptedException, ConstraintAnalyzerException {
        final ResourceKey resource = new SimpleResourceKey(qualifier, "a.stlcrec");
        final JSGLR1ParseResult parsed = parser.parse("1 + nil", "Start", resource);
        assertTrue(parsed.getAst().isPresent());
        final SingleFileResult result = analyzer.analyze(resource, parsed.getAst().get(), new ConstraintAnalyzerContext(),
            new StrategoIOAgent(loggerFactory, resourceService));
        assertNotNull(result.ast);
        assertNotNull(result.analysis);
        assertTrue(result.messages.containsError());
    }

    @Test void analyzeSingleSuccess() throws InterruptedException, ConstraintAnalyzerException {
        final ResourceKey resource = new SimpleResourceKey(qualifier, "b.stlcrec");
        final JSGLR1ParseResult parsed = parser.parse("1 + 2", "Start", resource);
        assertTrue(parsed.getAst().isPresent());
        final SingleFileResult result = analyzer.analyze(resource, parsed.getAst().get(), new ConstraintAnalyzerContext(),
            new StrategoIOAgent(loggerFactory, resourceService));
        assertNotNull(result.ast);
        assertNotNull(result.analysis);
        assertTrue(result.messages.isEmpty());
    }

    @Disabled("Multiple resource test in non-multifile language with Statix does not seem to work")
    @Test void analyzeMultipleErrors() throws InterruptedException, ConstraintAnalyzerException {
        final ResourceKey resource1 = new SimpleResourceKey(qualifier, "a.stlcrec");
        final JSGLR1ParseResult parsed1 = parser.parse("1 + 1", "Start", resource1);
        assertTrue(parsed1.getAst().isPresent());
        final ResourceKey resource2 = new SimpleResourceKey(qualifier, "b.stlcrec");
        final JSGLR1ParseResult parsed2 = parser.parse("1 + 2", "Start", resource2);
        assertTrue(parsed2.getAst().isPresent());
        final ResourceKey resource3 = new SimpleResourceKey(qualifier, "c.stlcrec");
        final JSGLR1ParseResult parsed3 = parser.parse("1 + nil", "Start", resource3);
        assertTrue(parsed3.getAst().isPresent());
        final HashMap<ResourceKey, IStrategoTerm> asts = new HashMap<>();
        asts.put(resource1, parsed1.getAst().get());
        asts.put(resource2, parsed2.getAst().get());
        asts.put(resource3, parsed3.getAst().get());
        final MultiFileResult result = analyzer.analyze(null, asts, new ConstraintAnalyzerContext(),
            new StrategoIOAgent(loggerFactory, resourceService));
        final ConstraintAnalyzer.@Nullable Result result1 = result.getResult(resource1);
        assertNotNull(result1);
        assertNotNull(result1.ast);
        assertNotNull(result1.analysis);
        final ConstraintAnalyzer.@Nullable Result result2 = result.getResult(resource2);
        assertNotNull(result2);
        assertNotNull(result2.ast);
        assertNotNull(result2.analysis);
        final ConstraintAnalyzer.@Nullable Result result3 = result.getResult(resource3);
        assertNotNull(result3);
        assertNotNull(result3.ast);
        assertNotNull(result3.analysis);
        assertEquals(1, result.keyedMessages.size());
        assertTrue(result.keyedMessages.containsError());
        final boolean[] foundCorrectMessage = {false};
        result.keyedMessages.accept((text, exception, severity, resource, region) -> {
            if(resource3.equals(resource) && severity.equals(Severity.Error)) {
                foundCorrectMessage[0] = true;
                return false;
            }
            return true;
        });
        assertTrue(foundCorrectMessage[0]);
    }

    @Disabled("Multiple resource test in non-multifile language with Statix does not seem to work")
    @Test void analyzeMultipleSuccess() throws InterruptedException, ConstraintAnalyzerException {
        final ResourceKey root = new SimpleResourceKey(qualifier, "0");
        final ResourceKey resource1 = new SimpleResourceKey(qualifier, "a.stlcrec");
        final JSGLR1ParseResult parsed1 = parser.parse("1 + 1", "Start", resource1);
        assertTrue(parsed1.getAst().isPresent());
        final ResourceKey resource2 = new SimpleResourceKey(qualifier, "b.stlcrec");
        final JSGLR1ParseResult parsed2 = parser.parse("1 + 2", "Start", resource2);
        assertTrue(parsed2.getAst().isPresent());
        final ResourceKey resource3 = new SimpleResourceKey(qualifier, "c.stlcrec");
        final JSGLR1ParseResult parsed3 = parser.parse("1 + 3", "Start", resource3);
        assertTrue(parsed3.getAst().isPresent());
        final HashMap<ResourceKey, IStrategoTerm> asts = new HashMap<>();
        asts.put(resource1, parsed1.getAst().get());
        asts.put(resource2, parsed2.getAst().get());
        asts.put(resource3, parsed3.getAst().get());
        final MultiFileResult result = analyzer.analyze(root, asts, new ConstraintAnalyzerContext(),
            new StrategoIOAgent(loggerFactory, resourceService));
        final ConstraintAnalyzer.@Nullable Result result1 = result.getResult(resource1);
        assertNotNull(result1);
        assertNotNull(result1.ast);
        assertNotNull(result1.analysis);
        final ConstraintAnalyzer.@Nullable Result result2 = result.getResult(resource2);
        assertNotNull(result2);
        assertNotNull(result2.ast);
        assertNotNull(result2.analysis);
        final ConstraintAnalyzer.@Nullable Result result3 = result.getResult(resource3);
        assertNotNull(result3);
        assertNotNull(result3.ast);
        assertNotNull(result3.analysis);
        assertTrue(result.keyedMessages.isEmpty());
    }
}
