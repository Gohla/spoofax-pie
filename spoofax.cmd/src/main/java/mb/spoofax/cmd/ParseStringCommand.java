package mb.spoofax.cmd;

import mb.common.message.Messages;
import mb.pie.api.None;
import mb.pie.api.PieSession;
import mb.resource.string.StringResource;
import mb.resource.string.StringResourceRegistry;
import mb.spoofax.core.language.AstResult;
import mb.spoofax.core.language.LanguageComponent;
import mb.spoofax.core.language.LanguageInstance;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(description = "Parses a string and shows the AST (if successful) and messages (if any)")
public class ParseStringCommand implements Callable<None> {
    private final StringResourceRegistry stringResourceRegistry;
    private final LanguageComponent languageComponent;

    public ParseStringCommand(StringResourceRegistry stringResourceRegistry, LanguageComponent languageComponent) {
        this.stringResourceRegistry = stringResourceRegistry;
        this.languageComponent = languageComponent;
    }

    @SuppressWarnings("NullableProblems") @Parameters(index = "0", description = "String to parse")
    private String stringToParse;

    @Override public None call() throws Exception {
        final LanguageInstance languageInstance = languageComponent.getLanguageInstance();

        final StringResource stringResource = new StringResource(stringToParse, 0);
        stringResourceRegistry.addResource(stringResource);

        try(final PieSession session = languageComponent.newPieSession()) {
            final AstResult astResult = session.requireTopDown(languageInstance.createAstTask(stringResource.key));
            if(astResult.ast != null) {
                System.out.println(astResult.ast.toString());
            }
            final Messages messages =
                session.requireTopDown(languageInstance.createMessagesTask(stringResource.key));
            System.out.println(messages);
            return None.instance;
        }
    }
}