package components;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

public abstract class AppWorkspaceComponent implements AppStyleArbiter {

    protected Pane    workspace;          // The workspace that can be customized depending on what the app needs
    protected boolean workspaceActivated; // Denotes whether or not the workspace is activated
    
    /**
     * When called this function puts the workspace into the window, revealing the controls for editing work.
     *
     * @param appPane The pane that contains all the controls in the
     *                entire application, including the file toolbar controls, which
     *                this framework manages, as well as the customly provided workspace,
     *                which would be different for each app.
     */
    public void activateWorkspace(BorderPane appPane) {
        if (!workspaceActivated) {
            appPane.setCenter(workspace);
            workspaceActivated = true;
        }
    }
    
    /**
     * Mutator method for setting the custom workspace.
     *
     * @param initWorkspace The workspace to set as the user interface's workspace.
     */
    public void setWorkspace(Pane initWorkspace) {
        workspace = initWorkspace;
    }
    
    /**
     * Accessor method for getting the workspace.
     *
     * @return The workspace pane for this app.
     */
    public Pane getWorkspace() { return workspace; }
    
    /**
     * This method is defined completely at the concrete implementation level.
     */
    public abstract void reloadWorkspace();
}
