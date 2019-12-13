package mb.tiger.spoofax.taskdef.command;

import mb.common.region.Region;
import mb.common.util.EnumSetView;
import mb.common.util.ListView;
import mb.jsglr.common.TermTracer;
import mb.jsglr1.common.JSGLR1ParseResult;
import mb.pie.api.ExecContext;
import mb.pie.api.Task;
import mb.pie.api.TaskDef;
import mb.resource.ResourceKey;
import mb.spoofax.core.language.cli.CliCommand;
import mb.spoofax.core.language.command.CommandContextType;
import mb.spoofax.core.language.command.CommandDef;
import mb.spoofax.core.language.command.CommandExecutionType;
import mb.spoofax.core.language.command.CommandFeedbacks;
import mb.spoofax.core.language.command.CommandInput;
import mb.spoofax.core.language.command.CommandOutput;
import mb.spoofax.core.language.command.arg.ParamDef;
import mb.spoofax.core.language.command.arg.RawArgs;
import mb.spoofax.core.language.command.arg.TextToResourceKeyArgConverter;
import mb.stratego.common.StrategoRuntime;
import mb.stratego.common.StrategoRuntimeBuilder;
import mb.stratego.common.StrategoUtil;
import mb.tiger.spoofax.taskdef.TigerParse;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spoofax.interpreter.library.IOAgent;
import org.spoofax.interpreter.terms.IStrategoTerm;

import javax.inject.Inject;

public class TigerShowDesugaredAst implements TaskDef<CommandInput<TigerShowArgs>, CommandOutput>, CommandDef<TigerShowArgs> {
    private final TigerParse parse;
    private final StrategoRuntimeBuilder strategoRuntimeBuilder;
    private final StrategoRuntime prototypeStrategoRuntime;
    private final TextToResourceKeyArgConverter textToResourceKeyArgConverter;


    @Inject public TigerShowDesugaredAst(
        TigerParse parse,
        StrategoRuntimeBuilder strategoRuntimeBuilder,
        StrategoRuntime prototypeStrategoRuntime,
        TextToResourceKeyArgConverter textToResourceKeyArgConverter
    ) {
        this.parse = parse;
        this.strategoRuntimeBuilder = strategoRuntimeBuilder;
        this.prototypeStrategoRuntime = prototypeStrategoRuntime;
        this.textToResourceKeyArgConverter = textToResourceKeyArgConverter;
    }


    @Override public String getId() {
        return getClass().getName();
    }

    @Override public CommandOutput exec(ExecContext context, CommandInput<TigerShowArgs> input) throws Exception {
        final ResourceKey key = input.args.key;
        final @Nullable Region region = input.args.region;

        final JSGLR1ParseResult parseResult = context.require(parse, key);
        final IStrategoTerm ast = parseResult.getAst()
            .orElseThrow(() -> new RuntimeException("Cannot show desugared AST, parsed AST for '" + key + "' is null"));

        final IStrategoTerm term;
        if(region != null) {
            term = TermTracer.getSmallestTermEncompassingRegion(ast, region);
        } else {
            term = ast;
        }

        final StrategoRuntime strategoRuntime = strategoRuntimeBuilder.buildFromPrototype(prototypeStrategoRuntime);
        final String strategyId = "desugar-all";
        final @Nullable IStrategoTerm result = strategoRuntime.invoke(strategyId, term, new IOAgent());
        if(result == null) {
            throw new RuntimeException("Cannot show desugared AST, executing Stratego strategy '" + strategyId + "' failed");
        }

        final String formatted = StrategoUtil.toString(result);
        return new CommandOutput(ListView.of(CommandFeedbacks.showText(formatted, "Desugared AST for '" + key + "'", null)));
    }

    @Override public Task<CommandOutput> createTask(CommandInput<TigerShowArgs> input) {
        return TaskDef.super.createTask(input);
    }


    @Override public String getDisplayName() {
        return "Show desugared AST";
    }

    @Override public String getDescription() {
        return "Shows the desugared Abstract Syntax Tree of the program.";
    }

    @Override public EnumSetView<CommandExecutionType> getSupportedExecutionTypes() {
        return EnumSetView.of(CommandExecutionType.ManualOnce, CommandExecutionType.ManualContinuous);
    }

    @Override public EnumSetView<CommandContextType> getRequiredContextTypes() {
        return EnumSetView.of(CommandContextType.Resource);
    }

    @Override public ParamDef getParamDef() {
        return TigerShowArgs.getParamDef();
    }

    @Override public TigerShowArgs fromRawArgs(RawArgs rawArgs) {
        return TigerShowArgs.fromRawArgs(rawArgs);
    }

    public CliCommand getCliCommandItem() {
        final String operation = "desugar";
        return CliCommand.of("desugar", "Desugars Tiger sources and shows the desugared AST",
            CliCommand.of("file", "Desugars given Tiger file and shows the desugared AST", this, TigerShowArgs.getFileCliParams(operation)),
            CliCommand.of("text", "Desugars given Tiger text and shows the desugared AST", this, TigerShowArgs.getTextCliParams(operation, textToResourceKeyArgConverter))
        );
    }
}
