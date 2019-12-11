package mb.stlcrec;

import mb.common.region.Region;
import mb.common.style.Color;
import mb.common.style.Styling;
import mb.common.style.TokenStyle;
import mb.common.token.TokenTypes;
import mb.jsglr1.common.JSGLR1ParseResult;
import mb.jsglr1.common.JSGLR1ParseTableException;
import mb.log.noop.NoopLoggerFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class STLCRecStylerTest {
    private final STLCRecParser parser = new STLCRecParser(STLCRecParseTable.fromClassLoaderResources());
    private final STLCRecStyler styler =
        new STLCRecStyler(STLCRecStylingRules.fromClassLoaderResources(), new NoopLoggerFactory());

    STLCRecStylerTest() throws IOException, JSGLR1ParseTableException {}

    @Test void style() throws InterruptedException {
        final JSGLR1ParseResult parseOutput = parser.parse("1 + 21", "Start");
        assertTrue(parseOutput.hasSucceeded());
        assertFalse(parseOutput.hasRecovered());
        assertFalse(parseOutput.hasFailed());
        assertTrue(parseOutput.getAst().isPresent());
        assertTrue(parseOutput.getTokens().isPresent());

        final Styling styling = styler.style(parseOutput.getTokens().get());
        final ArrayList<TokenStyle> stylePerToken = styling.getStylePerToken();
        assertEquals(5, stylePerToken.size());

        final TokenStyle ts0 = stylePerToken.get(0);
        assertEquals(TokenTypes.number(), ts0.getToken().getType());
        assertEquals(Region.fromOffsets(0, 1), ts0.getToken().getRegion());
        final Color numberColor = new Color(255, 0, 128, 0);
        assertEquals(numberColor, ts0.getStyle().getColor());
        assertNull(ts0.getStyle().getBackgroundColor());
        assertFalse(ts0.getStyle().getIsBold());
        assertFalse(ts0.getStyle().getIsItalic());
        assertFalse(ts0.getStyle().getIsStrikeout());
        assertFalse(ts0.getStyle().getIsUnderscore());

        final TokenStyle ts1 = stylePerToken.get(1);
        assertEquals(TokenTypes.layout(), ts1.getToken().getType());
        assertEquals(Region.fromOffsets(1, 2), ts1.getToken().getRegion());
        final Color layoutColor = new Color(255, 63, 127, 95);
        assertEquals(layoutColor, ts1.getStyle().getColor());
        assertNull(ts1.getStyle().getBackgroundColor());
        assertFalse(ts1.getStyle().getIsBold());
        assertTrue(ts1.getStyle().getIsItalic());
        assertFalse(ts1.getStyle().getIsStrikeout());
        assertFalse(ts1.getStyle().getIsUnderscore());

        final TokenStyle ts2 = stylePerToken.get(2);
        assertEquals(TokenTypes.operator(), ts2.getToken().getType());
        assertEquals(Region.fromOffsets(2, 3), ts2.getToken().getRegion());
        final Color operatorColor = new Color(255, 0, 0, 128);
        assertEquals(operatorColor, ts2.getStyle().getColor());
        assertNull(ts2.getStyle().getBackgroundColor());
        assertFalse(ts2.getStyle().getIsBold());
        assertFalse(ts2.getStyle().getIsItalic());
        assertFalse(ts2.getStyle().getIsStrikeout());
        assertFalse(ts2.getStyle().getIsUnderscore());

        final TokenStyle ts3 = stylePerToken.get(3);
        assertEquals(TokenTypes.layout(), ts3.getToken().getType());
        assertEquals(Region.fromOffsets(3, 4), ts3.getToken().getRegion());
        assertEquals(layoutColor, ts3.getStyle().getColor());
        assertNull(ts3.getStyle().getBackgroundColor());
        assertFalse(ts3.getStyle().getIsBold());
        assertTrue(ts3.getStyle().getIsItalic());
        assertFalse(ts3.getStyle().getIsStrikeout());
        assertFalse(ts3.getStyle().getIsUnderscore());

        final TokenStyle ts4 = stylePerToken.get(4);
        assertEquals(TokenTypes.number(), ts4.getToken().getType());
        assertEquals(Region.fromOffsets(4, 6), ts4.getToken().getRegion());
        assertEquals(numberColor, ts4.getStyle().getColor());
        assertNull(ts4.getStyle().getBackgroundColor());
        assertFalse(ts4.getStyle().getIsBold());
        assertFalse(ts4.getStyle().getIsItalic());
        assertFalse(ts4.getStyle().getIsStrikeout());
        assertFalse(ts4.getStyle().getIsUnderscore());
    }
}