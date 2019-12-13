package mb.tiger;

import mb.jsglr1.common.JSGLR1ParseResult;
import mb.stratego.common.StrategoException;
import mb.stratego.common.StrategoRuntime;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.spoofax.interpreter.library.IOAgent;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;

import static org.junit.jupiter.api.Assertions.*;

class TigerStrategoRuntimeBuilderTest {
    private final TigerParser parser = new TigerParserFactory().create();
    private final StrategoRuntime runtime = new TigerStrategoRuntimeBuilderFactory().create().build();

    @Test void parseUnparse() throws InterruptedException, StrategoException {
        final String str = "1 + 2";
        final JSGLR1ParseResult parsed = parser.parse(str, "Module");
        assertTrue(parsed.getAst().isPresent());
        final @Nullable IStrategoTerm unparsedTerm = runtime.invoke("pp-Tiger-string", parsed.getAst().get(), new IOAgent());
        assertNotNull(unparsedTerm);
        final IStrategoString unparsedStringTerm = (IStrategoString)unparsedTerm;
        final String unparsed = unparsedStringTerm.stringValue();
        assertEquals(str, unparsed);
    }
}
