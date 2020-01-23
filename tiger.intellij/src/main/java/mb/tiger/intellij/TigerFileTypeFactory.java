package mb.tiger.intellij;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;

public class TigerFileTypeFactory extends FileTypeFactory {
    // Instantiated by IntelliJ.
    private TigerFileTypeFactory() {}

    @Override public void createFileTypes(FileTypeConsumer consumer) {
        final TigerFileType fileType = TigerPlugin.getComponent().getFileType();
        consumer.consume(fileType, fileType.getDefaultExtension());
    }
}
