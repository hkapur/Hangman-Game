package controller;

import java.io.IOException;

public interface FileController {

    void handleNewRequest();

    void handleSaveRequest() throws IOException;

    void handleLoadRequest() throws IOException;

    void handleHintRequest();

    void handleExitRequest();
}
