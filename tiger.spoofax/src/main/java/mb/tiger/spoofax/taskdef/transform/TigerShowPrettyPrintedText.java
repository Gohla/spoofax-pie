package mb.tiger.spoofax.taskdef.transform;

import mb.common.util.EnumSetView;
import mb.common.util.ListView;
import mb.jsglr1.common.JSGLR1ParseResult;
import mb.pie.api.ExecContext;
import mb.pie.api.Task;
import mb.pie.api.TaskDef;
import mb.resource.ResourceKey;
import mb.spoofax.core.language.transform.*;
import mb.stratego.common.StrategoRuntime;
import mb.stratego.common.StrategoRuntimeBuilder;
import mb.stratego.common.StrategoUtil;
import mb.tiger.spoofax.taskdef.TigerParse;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spoofax.interpreter.library.IOAgent;
import org.spoofax.interpreter.terms.IStrategoTerm;

import javax.inject.Inject;

public class TigerShowPrettyPrintedText implements TaskDef<TransformInput, TransformOutput>, TransformDef {
    private final TigerParse parse;
    private final StrategoRuntimeBuilder strategoRuntimeBuilder;
    private final StrategoRuntime prototypeStrategoRuntime;


    @Inject public TigerShowPrettyPrintedText(
        TigerParse parse,
        StrategoRuntimeBuilder strategoRuntimeBuilder,
        StrategoRuntime prototypeStrategoRuntime
    ) {
        this.parse = parse;
        this.strategoRuntimeBuilder = strategoRuntimeBuilder;
        this.prototypeStrategoRuntime = prototypeStrategoRuntime;
    }


    @Override public String getId() {
        return getClass().getName();
    }

    @Override public TransformOutput exec(ExecContext context, TransformInput input) throws Exception {
        final TransformSubject subject = input.subject;
        final ResourceKey readable = TransformSubjects.getReadable(subject)
            .orElseThrow(() -> new RuntimeException("Cannot show pretty-printed text, subject '" + subject + "' is not a readable subject"));

        final JSGLR1ParseResult parseResult = context.require(parse, readable);
        final IStrategoTerm ast = parseResult.getAst()
            .orElseThrow(() -> new RuntimeException("Cannot show pretty-printed text, parsed AST for '" + input.subject + "' is null"));

        final StrategoRuntime strategoRuntime = strategoRuntimeBuilder.buildFromPrototype(prototypeStrategoRuntime);
        final String strategyId = "pp-Tiger-string";
        final @Nullable IStrategoTerm result = strategoRuntime.invoke(strategyId, ast, new IOAgent());
        if(result == null) {
            throw new RuntimeException("Cannot show pretty-printed text, executing Stratego strategy '" + strategyId + "' failed");
        }

        final String formatted = StrategoUtil.toString(result);
        return new TransformOutput(ListView.of(TransformFeedbacks.openEditorWithText(formatted, "Pretty-printed text for '" + readable + "'", null)));
    }

    @Override public Task<TransformOutput> createTask(TransformInput input) {
        return TaskDef.super.createTask(input);
    }


    @Override public String getDisplayName() {
        return "Show pretty-printed text";
    }

    @Override public EnumSetView<TransformExecutionType> getSupportedExecutionTypes() {
        return EnumSetView.of(TransformExecutionType.ManualOnce, TransformExecutionType.ManualContinuous);
    }

    @Override public EnumSetView<TransformSubjectType> getSupportedSubjectTypes() {
        return EnumSetView.of(TransformSubjectType.Editor);
    }
}
