package mb.tiger.eclipse;

import mb.spoofax.eclipse.command.RunCommandHandler;

public class TigerRunCommandHandler extends RunCommandHandler {
    public TigerRunCommandHandler() {
        super(TigerPlugin.getComponent());
    }
}
