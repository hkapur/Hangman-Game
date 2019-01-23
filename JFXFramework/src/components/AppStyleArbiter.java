package components;

public interface AppStyleArbiter {

    String CLASS_BORDERED_PANE = "bordered_pane";

    enum BUTTON_TYPE {
        NEW, SAVE, LOAD, EXIT;
    }
    
    void initStyle();
}
