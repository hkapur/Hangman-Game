package components;

public interface AppComponentsBuilder {
    AppDataComponent buildDataComponent() throws Exception;

    AppFileComponent buildFileComponent() throws Exception;

    AppWorkspaceComponent buildWorkspaceComponent() throws Exception;
}
