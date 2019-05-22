package mb.constraint.common;

import mb.common.message.Messages;
import mb.common.message.MessagesBuilder;
import mb.common.message.Severity;
import mb.resource.ResourceKey;
import mb.stratego.common.StrategoException;
import mb.stratego.common.StrategoRuntime;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.library.IOAgent;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ConstraintAnalyzer {
    public static class Result {
        public final @Nullable IStrategoTerm ast;
        public final @Nullable IStrategoTerm analysis;

        public Result(@Nullable IStrategoTerm ast, @Nullable IStrategoTerm analysis) {
            this.ast = ast;
            this.analysis = analysis;
        }

        public Result() {
            this.ast = null;
            this.analysis = null;
        }
    }

    public static class SingleFileResult {
        public final @Nullable IStrategoTerm ast;
        public final @Nullable IStrategoTerm analysis;
        public final Messages messages;

        public SingleFileResult(@Nullable IStrategoTerm ast, @Nullable IStrategoTerm analysis, Messages messages) {
            this.ast = ast;
            this.analysis = analysis;
            this.messages = messages;
        }
    }

    public static class MultiFileResult {
        public final HashMap<ResourceKey, Result> results;
        public final Messages messages;

        public MultiFileResult(HashMap<ResourceKey, Result> results, Messages messages) {
            this.results = results;
            this.messages = messages;
        }
    }


    private final StrategoRuntime strategoRuntime;
    private final ITermFactory termFactory;
    private final String strategyId;


    @Inject public ConstraintAnalyzer(StrategoRuntime strategoRuntime, String strategyId) {
        this.strategoRuntime = strategoRuntime;
        this.termFactory = strategoRuntime.getTermFactory();
        this.strategyId = strategyId;
    }


    public SingleFileResult analyze(ResourceKey resource, IStrategoTerm ast, ConstraintAnalyzerContext context)
        throws ConstraintAnalyzerException {
        final HashMap<ResourceKey, IStrategoTerm> asts = new HashMap<>(1);
        asts.put(resource, ast);
        final MultiFileResult multiFileResult = doAnalyze(null, asts, context);
        final @Nullable Result result = multiFileResult.results.get(resource);
        if(result == null) {
            throw new RuntimeException("BUG: no analysis result was found for resource '" + resource + "'");
        }
        return new SingleFileResult(result.ast, result.analysis, multiFileResult.messages);
    }

    public MultiFileResult analyze(ResourceKey root, HashMap<ResourceKey, IStrategoTerm> asts, ConstraintAnalyzerContext context)
        throws ConstraintAnalyzerException {
        return doAnalyze(root, asts, context);
    }

    private MultiFileResult doAnalyze(
        @Nullable ResourceKey root,
        HashMap<ResourceKey, IStrategoTerm> asts,
        ConstraintAnalyzerContext context
    ) throws ConstraintAnalyzerException {
        /// 1. Compute changeset from given asts and cache.

        final HashMap<ResourceKey, IStrategoTerm> addedOrChangedAsts = new HashMap<>();
        for(Entry<ResourceKey, IStrategoTerm> entry : asts.entrySet()) {
            final ResourceKey resource = entry.getKey();
            final IStrategoTerm ast = entry.getValue();
            addedOrChangedAsts.put(resource, ast);
        }
        final HashSet<ResourceKey> removed = new HashSet<>(context.getResultResources());
        removed.removeAll(addedOrChangedAsts.keySet());

        /// 2. Transform changeset into list of changed terms and expect objects, and remove invalidated units from the context.

        final ArrayList<IStrategoTerm> changeTerms = new ArrayList<>();
        final HashMap<ResourceKey, Expect> expects = new HashMap<>();

        // Root analysis.
        final @Nullable IStrategoTerm rootChange;
        if(root != null) {
            context.registerResource(root);
            final IStrategoTerm ast = mkTuple();
            final IStrategoTerm change;
            final Expect expect;
            final @Nullable Result cachedResult = context.getResult(root);
            if(cachedResult != null) {
                change = mkAppl("Cached", cachedResult.analysis);
                expect = new Update(root);
                context.removeResult(root);
            } else {
                change = mkAppl("Added", ast);
                expect = new Project(root);
            }
            expects.put(root, expect);
            rootChange = mkTuple(mkString(root), change);
        } else {
            rootChange = null;
        }

        // Removed resources.
        for(ResourceKey resource : removed) {
            final @Nullable Result cachedResult = context.getResult(resource);
            if(cachedResult != null) {
                changeTerms.add(mkTuple(mkString(resource), mkAppl("Removed", cachedResult.analysis)));
                context.removeResult(resource);
                context.removeResource(resource);
            }
        }

        // Added and changed resources.
        for(Map.Entry<ResourceKey, IStrategoTerm> entry : addedOrChangedAsts.entrySet()) {
            final ResourceKey resource = entry.getKey();
            context.registerResource(resource);
            final IStrategoTerm ast = entry.getValue();
            final IStrategoTerm change;
            final @Nullable Result cachedResult = context.getResult(resource);
            if(cachedResult != null) {
                change = mkAppl("Changed", ast, cachedResult.analysis);
                context.removeResult(resource);
            } else {
                change = mkAppl("Added", ast);
            }
            expects.put(resource, new Full(resource));
            changeTerms.add(mkTuple(mkString(resource), change));
        }

        // Cached resources.
        if(root != null) {
            for(Map.Entry<ResourceKey, Result> entry : context.getResultEntries()) {
                final ResourceKey resource = entry.getKey();
                context.registerResource(resource);
                final Result cachedResult = entry.getValue();
                if(!addedOrChangedAsts.containsKey(resource)) {
                    final IStrategoTerm change = mkAppl("Cached", cachedResult.analysis);
                    expects.put(resource, new Update(resource));
                    changeTerms.add(mkTuple(mkString(resource), change));
                }
            }
        }

        /// 3. Call analysis, and list results.

        final Map<ResourceKey, IStrategoTerm> resultTerms = new HashMap<>();
        final IStrategoTerm action;
        if(root != null) {
            action = mkAppl("AnalyzeMulti", rootChange, termFactory.makeList(changeTerms));
        } else {
            action = mkAppl("AnalyzeSingle", termFactory.makeList(changeTerms));
        }

        final @Nullable IStrategoTerm allResultsTerm;
        try {
            allResultsTerm = strategoRuntime.invoke(strategyId, action, new IOAgent());
        } catch(StrategoException e) {
            throw new ConstraintAnalyzerException(e);
        }
        if(allResultsTerm == null) {
            throw new ConstraintAnalyzerException("Constraint analysis strategy '" + strategyId + "' failed");
        }

        final @Nullable List<IStrategoTerm> allResultTerms = match(allResultsTerm, "AnalysisResult", 1);
        if(allResultTerms == null || allResultTerms.isEmpty()) {
            throw new RuntimeException("BUG: invalid constraint analysis result, got " + allResultsTerm);
        }

        final IStrategoTerm resultsTerm = allResultTerms.get(0);
        if(!Tools.isTermList(resultsTerm)) {
            throw new RuntimeException("BUG: expected list of results, got: " + resultsTerm);
        }
        for(IStrategoTerm entry : resultsTerm) {
            if(!Tools.isTermTuple(entry) || entry.getSubtermCount() != 2) {
                throw new RuntimeException("BUG: expected tuple result, got " + entry);
            }
            final IStrategoTerm resourceTerm = entry.getSubterm(0);
            if(!Tools.isTermString(resourceTerm)) {
                throw new RuntimeException("BUG: expected resource string as first component, got " + resourceTerm);
            }
            final String resourceString = Tools.asJavaString(resourceTerm);
            final @Nullable ResourceKey resource = context.getResource(resourceString);
            if(resource == null) {
                throw new RuntimeException(
                    "BUG: could not get resource for resource string '" + resourceString + "' in result term " + entry);
            }
            final IStrategoTerm resultTerm = entry.getSubterm(1);
            resultTerms.put(resource, resultTerm);
        }

        /// 4. Process analysis results and collect messages.

        final MessagesBuilder messagesBuilder = new MessagesBuilder();

        // Process result terms, updating the result cache.
        for(Map.Entry<ResourceKey, IStrategoTerm> entry : resultTerms.entrySet()) {
            final ResourceKey resource = entry.getKey();
            final IStrategoTerm resultTerm = entry.getValue();
            if(expects.containsKey(resource)) {
                expects.get(resource).processResultTerm(resultTerm, context, messagesBuilder);
            } else {
                throw new RuntimeException(
                    "BUG: got result '" + resultTerm + "' for resource '" + resource + "' that was not part of the input");
            }
        }

        // Check if all input resources have been covered.
        for(Map.Entry<ResourceKey, IStrategoTerm> entry : addedOrChangedAsts.entrySet()) {
            final ResourceKey resource = entry.getKey();
            if(!resultTerms.containsKey(resource)) {
                expects.get(resource).addFailMessage("Missing analysis result", messagesBuilder);
            }
        }

        /// 5. Build and return result object.

        final HashMap<ResourceKey, Result> results = new HashMap<>(asts.size());
        for(ResourceKey resource : addedOrChangedAsts.keySet()) {
            final @Nullable Result result = context.getResult(resource);
            if(result != null) {
                results.put(resource, result);
            } else {
                results.put(resource, new Result());
            }
        }
        return new MultiFileResult(results, messagesBuilder.build());
    }


    abstract class Expect {
        final ResourceKey resource;

        Expect(ResourceKey resource) {
            this.resource = resource;
        }

        void addResultMessages(IStrategoTerm errors, IStrategoTerm warnings, IStrategoTerm notes, MessagesBuilder messagesBuilder) {
            MessageUtil.addMessagesFromTerm(messagesBuilder, errors, Severity.Error);
            MessageUtil.addMessagesFromTerm(messagesBuilder, warnings, Severity.Warning);
            MessageUtil.addMessagesFromTerm(messagesBuilder, notes, Severity.Info);
        }

        void addFailMessage(String text, MessagesBuilder messagesBuilder) {
            messagesBuilder.addMessage(text, Severity.Error, resource);
        }

        abstract void processResultTerm(IStrategoTerm resultTerm, ConstraintAnalyzerContext context, MessagesBuilder messagesBuilder);
    }

    class Full extends Expect {
        Full(ResourceKey resourceKey) {
            super(resourceKey);
        }

        @Override
        public void processResultTerm(IStrategoTerm resultTerm, ConstraintAnalyzerContext context, MessagesBuilder messagesBuilder) {
            final @Nullable List<IStrategoTerm> results;
            if((results = match(resultTerm, "Full", 5)) != null) {
                final IStrategoTerm ast = results.get(0);
                final IStrategoTerm analysis = results.get(1);
                addResultMessages(results.get(2), results.get(3), results.get(4), messagesBuilder);
                context.updateResult(resource, ast, analysis);
            } else if(match(resultTerm, "Failed", 0) != null) {
                addFailMessage("Analysis failed", messagesBuilder);
                context.removeResult(resource);
            } else {
                addFailMessage("Analysis returned incorrect result", messagesBuilder);
            }
        }
    }

    class Update extends Expect {
        Update(ResourceKey resourceKey) {
            super(resourceKey);
        }

        @Override
        public void processResultTerm(IStrategoTerm resultTerm, ConstraintAnalyzerContext context, MessagesBuilder messagesBuilder) {
            final @Nullable List<IStrategoTerm> results;
            if((results = match(resultTerm, "Update", 4)) != null) {
                final IStrategoTerm analysis = results.get(0);
                addResultMessages(results.get(1), results.get(2), results.get(3), messagesBuilder);
                context.updateResult(resource, analysis);
            } else if(match(resultTerm, "Failed", 0) != null) {
                addFailMessage("Analysis failed", messagesBuilder);
                context.removeResult(resource);
            } else {
                addFailMessage("Analysis returned incorrect result", messagesBuilder);
            }
        }
    }

    class Project extends Expect {
        Project(ResourceKey resourceKey) {
            super(resourceKey);
        }

        @Override
        public void processResultTerm(IStrategoTerm resultTerm, ConstraintAnalyzerContext context, MessagesBuilder messagesBuilder) {
            final @Nullable List<IStrategoTerm> results;
            if((results = match(resultTerm, "Full", 5)) != null) {
                final IStrategoTerm analysis = results.get(1);
                addResultMessages(results.get(2), results.get(3), results.get(4), messagesBuilder);
                context.updateResult(resource, analysis);
            } else if(match(resultTerm, "Failed", 0) != null) {
                addFailMessage("Analysis failed", messagesBuilder);
                context.removeResult(resource);
            } else {
                addFailMessage("Analysis returned incorrect result", messagesBuilder);
            }
        }
    }


    private IStrategoTerm mkAppl(String op, IStrategoTerm... subterms) {
        return termFactory.makeAppl(termFactory.makeConstructor(op, subterms.length), subterms);
    }

    private IStrategoTerm mkTuple(IStrategoTerm... subterms) {
        return termFactory.makeTuple(subterms);
    }

    private IStrategoString mkString(Object obj) {
        return termFactory.makeString(obj.toString());
    }


    private @Nullable List<IStrategoTerm> match(@Nullable IStrategoTerm term, String op, int n) {
        if(term == null || !Tools.isTermAppl(term) || !Tools.hasConstructor((IStrategoAppl) term, op, n)) {
            return null;
        }
        return Arrays.asList(term.getAllSubterms());
    }
}