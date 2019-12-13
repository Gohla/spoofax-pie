package mb.tiger;

import mb.esv.common.ESVStylingRules;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public class TigerStylingRules implements Serializable {
    final ESVStylingRules stylingRules;

    private TigerStylingRules(ESVStylingRules stylingRules) {
        this.stylingRules = stylingRules;
    }

    public static TigerStylingRules fromClassLoaderResources() {
        final String resource = "mb/tiger/target/metaborg/editor.esv.af";
        try(final @Nullable InputStream inputStream = TigerParseTable.class.getClassLoader().getResourceAsStream(resource)) {
            if(inputStream == null) {
                throw new RuntimeException("Cannot create styling rules; cannot find resource '" + resource + "' in classloader resources");
            }
            final ESVStylingRules stylingRules = ESVStylingRules.fromStream(inputStream);
            return new TigerStylingRules(stylingRules);
        } catch(IOException e) {
            throw new RuntimeException("Cannot create styling rules; cannot read styling rules from '" + resource + "' in classloader resources", e);
        }
    }
}
