package mb.jsglr1.common;

import mb.common.message.Messages;
import mb.common.token.Token;
import mb.jsglr.common.ResourceKeyAttachment;
import mb.jsglr.common.TokenUtil;
import mb.resource.ResourceKey;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.Disambiguator;
import org.spoofax.jsglr.client.SGLRParseResult;
import org.spoofax.jsglr.client.imploder.ImploderOriginTermFactory;
import org.spoofax.jsglr.client.imploder.NullTokenizer;
import org.spoofax.jsglr.client.imploder.TermTreeFactory;
import org.spoofax.jsglr.client.imploder.TreeBuilder;
import org.spoofax.jsglr.io.SGLR;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.terms.TermFactory;
import org.spoofax.terms.attachments.ParentTermFactory;

import java.util.ArrayList;

public class JSGLR1Parser {
    private final SGLR parser;

    public JSGLR1Parser(JSGLR1ParseTable parseTable) {
        this(parseTable, new ImploderOriginTermFactory(new TermFactory()));
    }

    public JSGLR1Parser(JSGLR1ParseTable parseTable, ITermFactory termFactory) {
        final TermTreeFactory treeFactory = new TermTreeFactory(new ParentTermFactory(termFactory));
        final TreeBuilder treeBuilder = new TreeBuilder(treeFactory);

        this.parser = new SGLR(treeBuilder, parseTable.internalParseTable);
        this.parser.setUseStructureRecovery(true);
        this.parser.setTimeout(5000);
        this.parser.setDisambiguatorTimeout(5000);
        this.parser.setApplyCompletionProd(false);
        this.parser.setNewCompletionMode(false);

        final Disambiguator disambiguator = parser.getDisambiguator();
        disambiguator.setHeuristicFilters(false);
    }

    public JSGLR1ParseResult parse(String text, String startSymbol) throws InterruptedException {
        return parse(text, startSymbol, null);
    }

    public JSGLR1ParseResult parse(String text, String startSymbol, @Nullable ResourceKey resource) throws InterruptedException {
        try {
            final SGLRParseResult result = parser.parse(text, null, startSymbol);
            if(result.output == null) {
                throw new RuntimeException("BUG: parser returned null output even though parsing did not fail");
            }
            if(!(result.output instanceof IStrategoTerm)) {
                throw new RuntimeException("BUG: parser returned an output that is not an instance of IStrategoTerm");
            }
            final IStrategoTerm ast = (IStrategoTerm) result.output;
            if(resource != null) {
                ResourceKeyAttachment.setResourceKey(ast, resource);
            }
            final ArrayList<Token<IStrategoTerm>> tokenStream = TokenUtil.extract(ast);
            final MessagesUtil messagesUtil = new MessagesUtil(true, false, parser.getCollectedErrors());
            messagesUtil.gatherNonFatalErrors(ast);
            final Messages messages = messagesUtil.getMessages();
            final boolean recovered = messages.containsError();
            if(recovered) {
                return JSGLR1ParseResults.recovered(ast, tokenStream, messages);
            } else {
                return JSGLR1ParseResults.success(ast, tokenStream, messages);
            }
        } catch(SGLRException e) {
            final MessagesUtil messagesUtil = new MessagesUtil(true, true, parser.getCollectedErrors());
            messagesUtil.processFatalException(new NullTokenizer(text, null), e);
            final Messages messages = messagesUtil.getMessages();
            return JSGLR1ParseResults.failed(messages);
        }
    }
}
