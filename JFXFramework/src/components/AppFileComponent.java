package components;

import java.io.IOException;
import java.nio.file.Path;

public interface AppFileComponent {

    void saveData(AppDataComponent data, Path filePath) throws IOException;

    void loadData(AppDataComponent data, Path filePath) throws IOException;

    void exportData(AppDataComponent data, Path filePath) throws IOException;
}
