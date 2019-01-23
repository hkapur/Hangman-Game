package controller;

import apptemplate.AppTemplate;
import data.GameData;
import gui.Workspace;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import propertymanager.PropertyManager;
import ui.AppMessageDialogSingleton;
import ui.YesNoCancelDialogSingleton;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static settings.AppPropertyType.*;
import static settings.InitializationParameters.APP_WORKDIR_PATH;

public class HangmanController implements FileController {

    public enum GameState {
        UNINITIALIZED,
        INITIALIZED_UNMODIFIED,
        INITIALIZED_MODIFIED,
        ENDED
    }

    private AppTemplate appTemplate; // shared reference to the application
    private GameData gamedata;    // shared reference to the game being played, loaded or saved
    private GameState gamestate;   // the state of the game being shown in the workspace
    private Text[] progress;    // reference to the text area for the word
    private boolean success;     // whether or not player was successful
    private int discovered;  // the number of letters already discovered
    private Button gameButton;  // shared reference to the "start game" button
    private Button hint2;
    private Label remains;     // dynamically updated label that indicates the number of remaining guesses
    private Label str;
    private Path workFile;
    private boolean bool = true;
    private ObservableList<Node> children;
    private ArrayList<Shape> man;
    final int[] left = new int[100];
    private int signal;

    public HangmanController(AppTemplate appTemplate, Button gameButton) {
        this(appTemplate);
        this.gameButton = gameButton;
    }

    public HangmanController(AppTemplate appTemplate) {
        this.appTemplate = appTemplate;
        this.gamestate = GameState.UNINITIALIZED;
    }

    public void enableGameButton() {
        if (gameButton == null) {
            Workspace workspace = (Workspace) appTemplate.getWorkspaceComponent();
            gameButton = workspace.getStartGame();
        }
        gameButton.setDisable(false);
    }

    public void disableGameButton() {
        if (gameButton == null) {
            Workspace workspace = (Workspace) appTemplate.getWorkspaceComponent();
            gameButton = workspace.getStartGame();
        }
        gameButton.setDisable(true);
    }

    public void setGameState(GameState gamestate) {
        this.gamestate = gamestate;
    }

    public GameState getGamestate() {
        return this.gamestate;
    }


    public void start() {
        left[0] = 10;
        gamedata = (GameData) appTemplate.getDataComponent();
        success = false;
        discovered = 0;

        Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();

        gamedata.init();
        setGameState(GameState.INITIALIZED_UNMODIFIED);
        BorderPane bp = gameWorkspace.bp();
        Button hint2 = gameWorkspace.gethint2();
        HBox remainingGuessBox = gameWorkspace.getRemainingGuessBox();
        HBox guessedLetters = (HBox) gameWorkspace.getGameTextsPane().getChildren().get(1);
        HBox guessedalready = (HBox) gameWorkspace.getGameTextsPane().getChildren().get(2);
        HBox hintbox = (HBox) gameWorkspace.getGameTextsPane().getChildren().get(3);
        remains = new Label(Integer.toString(GameData.TOTAL_NUMBER_OF_GUESSES_ALLOWED));
        str = new Label();
        remainingGuessBox.getChildren().addAll(new Label("Remaining Guesses: "), remains);
        guessedalready.getChildren().addAll(new Label("The guessed letters are: "), str);
        if(gamedata.hinter())
        {
            hintbox.getChildren().addAll(hint2);
            hintbox.setAlignment(Pos.CENTER);
        }
        initWordGraphics(guessedLetters);
        play();
    }

    private void end() {
        appTemplate.getGUI().getPrimaryScene().setOnKeyTyped(null);
        gameButton.setDisable(true);
        disablehint2();
        setGameState(GameState.ENDED);
        Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
        appTemplate.getGUI().updateWorkspaceToolbar(gamestate.equals(GameState.INITIALIZED_MODIFIED));
        //appTemplate.getGUI().disableHint();
        Platform.runLater(() -> {
            PropertyManager manager = PropertyManager.getManager();
            AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
            String endMessage = manager.getPropertyValue(success ? GAME_WON_MESSAGE : GAME_LOST_MESSAGE);
            if (!success)
            {
                gameWorkspace.reinitialize2();
                BorderPane bp = gameWorkspace.bp();
                HBox remainingGuessBox = gameWorkspace.getRemainingGuessBox();
                HBox guessedLetters = (HBox) gameWorkspace.getGameTextsPane().getChildren().get(1);
                HBox guessedalready = (HBox) gameWorkspace.getGameTextsPane().getChildren().get(2);
                initialize(bp);
                for(int i=0;i<10;i++){
                    man.get(i).setVisible(true);
                }
                remains = new Label(Integer.toString(0));
                str = new Label();
                remainingGuessBox.getChildren().addAll(new Label("Remaining Guesses: "), remains);
                guessedalready.getChildren().addAll(new Label("The guessed letters are: "), str);
                String guessing = String.valueOf(gamedata.getGoodGuesses() + String.valueOf(gamedata.getBadGuesses()));
                str.setText(guessing);
                no(guessedLetters);
            }
            if (dialog.isShowing())
                dialog.toFront();
            else
                dialog.show(manager.getPropertyValue(GAME_OVER_TITLE), endMessage);
        });
    }
    public void no(HBox guessedLetters){
        Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
        char[] targetword = gamedata.getTargetWord().toCharArray();
        progress = new Text[targetword.length];
        StackPane[] sp = new StackPane[progress.length];
        Rectangle[] r = new Rectangle[progress.length];
        for (int i = 0; i < progress.length; i++) {
            r[i] = new Rectangle();
            sp[i] = new StackPane();
            r[i].setX(50);
            r[i].setY(50);
            r[i].setFill(Paint.valueOf("grey"));
            r[i].setWidth(20);
            r[i].setHeight(20);
            r[i].setStroke(Paint.valueOf("black"));
            progress[i] = new Text(Character.toString(targetword[i]));
            progress[i].setVisible(!gamedata.getGoodGuesses().contains(progress[i].getText().charAt(0)));
            sp[i].getChildren().addAll(r[i], progress[i]);
        }
        guessedLetters.getChildren().addAll(sp);
    }

    private void initWordGraphics(HBox guessedLetters) {
        char[] targetword = gamedata.getTargetWord().toCharArray();
        progress = new Text[targetword.length];
        StackPane[] sp = new StackPane[progress.length];
        Rectangle[] r = new Rectangle[progress.length];
        for (int i = 0; i < progress.length; i++) {
            r[i] = new Rectangle();
            sp[i] = new StackPane();
            r[i].setX(50);
            r[i].setY(50);
            r[i].setFill(Paint.valueOf("lightblue"));
            r[i].setWidth(20);
            r[i].setHeight(20);
            r[i].setStroke(Paint.valueOf("black"));
            progress[i] = new Text(Character.toString(targetword[i]));
            progress[i].setVisible(false);
            sp[i].getChildren().addAll(r[i], progress[i]);
        }
        guessedLetters.getChildren().addAll(sp);
    }


    public void play() {
        if(gamedata.hinter())
        {
           // appTemplate.getGUI().enableHint();
            gamedata.sethint(true);
            enablehint2();
        }
        if(!bool){
           // appTemplate.getGUI().disableHint();
            disablehint2();
        }
        System.out.println(gamedata.getTargetWord());
        disableGameButton();
        Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
        BorderPane bp = gameWorkspace.bp();
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                appTemplate.getGUI().updateWorkspaceToolbar(gamestate.equals(GameState.INITIALIZED_MODIFIED));
                appTemplate.getGUI().getPrimaryScene().setOnKeyTyped((KeyEvent event) -> {
                    char guess = event.getCharacter().charAt(0);
                    if (!containsIllegals(String.valueOf(guess))) {
                        if (!alreadyGuessed(guess)) {
                            boolean goodguess = false;
                            for (int i = 0; i < progress.length; i++) {
                                if (gamedata.getTargetWord().charAt(i) == guess) {
                                    progress[i].setVisible(true);
                                    gamedata.addGoodGuess(guess);
                                    goodguess = true;
                                    discovered++;
                                }
                            }
                            if (!goodguess) {
                                gamedata.addBadGuess(guess);
                                initialize(bp);
                                man.get(10 - left[0]).setVisible(true);
                                left[0] = left[0] - 1;
                            }
                            signal = left[0]+1;
                            gamedata.setPosi(signal);
                            Set<Character> goodguesses = gamedata.getGoodGuesses();
                            Set<Character> badguesses = gamedata.getBadGuesses();
                            String guessing = String.valueOf(goodguesses) + String.valueOf(badguesses);
                            str.setText(guessing);
                            success = (discovered == progress.length);
                            remains.setText(Integer.toString(gamedata.getRemainingGuesses()));
                        }
                    }
                    else {
                        AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
                        PropertyManager props = PropertyManager.getManager();
                        dialog.show(props.getPropertyValue(INVALID_TITLE), props.getPropertyValue(INVALID_MESSAGE));
                    }
                    setGameState(GameState.INITIALIZED_MODIFIED);
                });
                if (gamedata.getRemainingGuesses() <= 0 || success)
                    stop();
            }

            @Override
            public void stop() {
                super.stop();
                end();
            }
        };
        timer.start();
    }

    public void initialize(BorderPane bp) {

        man = new ArrayList<Shape>();

        Ellipse head = new Ellipse(100, 150, 25, 25);
        head.setStroke(Color.BLACK);
        head.setFill(Color.TRANSPARENT);
        head.setStrokeWidth(3);
        head.setVisible(false);
        bp.getChildren().addAll(head);
        man.add(head);

        Line tor = new Line(100, 250, 100, 175);
        tor.setStroke(Color.BLACK);
        tor.setStrokeWidth(3);
        tor.setVisible(false);
        bp.getChildren().addAll(tor);
        man.add(tor);

        Line lefthand = new Line(70, 220, 100, 190);
        lefthand.setStroke(Color.BLACK);
        lefthand.setStrokeWidth(3);
        lefthand.setVisible(false);
        bp.getChildren().addAll(lefthand);
        man.add(lefthand);

        Line righthand = new Line(130, 220, 100, 190);
        righthand.setStroke(Color.BLACK);
        righthand.setStrokeWidth(3);
        righthand.setVisible(false);
        bp.getChildren().addAll(righthand);
        man.add(righthand);

        Line leftleg = new Line(70, 275, 100, 250);
        leftleg.setStroke(Color.BLACK);
        leftleg.setStrokeWidth(3);
        leftleg.setVisible(false);
        bp.getChildren().addAll(leftleg);
        man.add(leftleg);

        Line rightleg = new Line(100, 250, 130, 275);
        rightleg.setStroke(Color.BLACK);
        rightleg.setStrokeWidth(3);
        rightleg.setVisible(false);
        bp.getChildren().addAll(rightleg);
        man.add(rightleg);

        Line firstrope = new Line(25, 100, 100, 100);
        firstrope.setStroke(Color.BLACK);
        firstrope.setStrokeWidth(3);
        firstrope.setVisible(false);
        bp.getChildren().addAll(firstrope);
        man.add(firstrope);

        Line secondrope = new Line(25, 100, 25, 300);
        secondrope.setStroke(Color.BLACK);
        secondrope.setStrokeWidth(3);
        secondrope.setVisible(false);
        bp.getChildren().addAll(secondrope);
        man.add(secondrope);

        Line thirdrope = new Line(100, 300, 25, 300);
        thirdrope.setStroke(Color.BLACK);
        thirdrope.setStrokeWidth(3);
        thirdrope.setVisible(false);
        bp.getChildren().addAll(thirdrope);
        man.add(thirdrope);

        Line lastrope = new Line(100, 125, 100, 100);
        lastrope.setStroke(Color.BLACK);
        lastrope.setStrokeWidth(3);
        lastrope.setVisible(false);
        bp.getChildren().addAll(lastrope);
        man.add(lastrope);
    }

    public boolean containsIllegals(String toExamine) {
        Pattern pattern = Pattern.compile("[-=)?(*&$!`;\':,./~#@+%{}<>\\[\\]|\"\\_^1234567890]");
        Matcher matcher = pattern.matcher(toExamine);
        return matcher.find();
    }

    private void restoreGUI() {
        disableGameButton();
        Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
        gameWorkspace.reinitialize();

        HBox guessedLetters = (HBox) gameWorkspace.getGameTextsPane().getChildren().get(1);
        restoreWordGraphics(guessedLetters);

        HBox remainingGuessBox = gameWorkspace.getRemainingGuessBox();
        remains = new Label(Integer.toString(gamedata.getRemainingGuesses()));
        remainingGuessBox.getChildren().addAll(new Label("Remaining Guesses: "), remains);

        HBox guessedalready = (HBox) gameWorkspace.getGameTextsPane().getChildren().get(2);
        str = new Label();
        guessedalready.getChildren().addAll(new Label("The guessed letters are: "), str);

        Button hint2 = gameWorkspace.gethint2();
        HBox hintbox = (HBox) gameWorkspace.getGameTextsPane().getChildren().get(3);
        if(gamedata.hinter()) {
            hintbox.getChildren().addAll(hint2);
            hintbox.setAlignment(Pos.CENTER);
        }

        String g;
        g = gamedata.retguessed();
        str.setText(g);

        gamedata.p();

        BorderPane bp = gameWorkspace.bp();
        left[0] = gamedata.getPosi();
        initialize(bp);
        int j = left[0];
        for(int i = 0;i<10-j+1;i++) {
            man.get(10 - left[0]).setVisible(true);
            left[0]++;
        }
        left[0] = gamedata.getPosi();
        success = false;
        play();
    }

    private void restoreWordGraphics(HBox guessedLetters) {
        discovered = 0;
        char[] targetword = gamedata.getTargetWord().toCharArray();
        progress = new Text[targetword.length];
        StackPane[] sp = new StackPane[progress.length];
        Rectangle[] r = new Rectangle[progress.length];
        for (int i = 0; i < progress.length; i++) {
            r[i] = new Rectangle();
            sp[i] = new StackPane();
            r[i].setX(50);
            r[i].setY(50);
            r[i].setFill(Paint.valueOf("lightblue"));
            r[i].setWidth(20);
            r[i].setHeight(20);
            r[i].setStroke(Paint.valueOf("black"));
            progress[i] = new Text(Character.toString(targetword[i]));
            progress[i].setVisible(gamedata.getGoodGuesses().contains(progress[i].getText().charAt(0)));
            if (progress[i].isVisible())
                discovered++;
            sp[i].getChildren().addAll(r[i], progress[i]);
        }
        guessedLetters.getChildren().addAll(sp);
    }

    private boolean alreadyGuessed(char c) {
        return gamedata.getGoodGuesses().contains(c) || gamedata.getBadGuesses().contains(c);
    }

    @Override
    public void handleNewRequest() {
        AppMessageDialogSingleton messageDialog = AppMessageDialogSingleton.getSingleton();
        PropertyManager propertyManager = PropertyManager.getManager();
        boolean makenew = true;
        if (gamestate.equals(GameState.INITIALIZED_MODIFIED))
            try {
                makenew = promptToSave();
            } catch (IOException e) {
                messageDialog.show(propertyManager.getPropertyValue(NEW_ERROR_TITLE), propertyManager.getPropertyValue(NEW_ERROR_MESSAGE));
            }
        if (makenew) {
            appTemplate.getDataComponent().reset();                // reset the data (should be reflected in GUI)
            appTemplate.getWorkspaceComponent().reloadWorkspace(); // load data into workspace
            ensureActivatedWorkspace();                            // ensure workspace is activated
            workFile = null;                                       // new workspace has never been saved to a file
            ((Workspace) appTemplate.getWorkspaceComponent()).reinitialize();
            enableGameButton();
        }
        if (gamestate.equals(GameState.ENDED)) {
            appTemplate.getGUI().updateWorkspaceToolbar(false);
            Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
            gameWorkspace.reinitialize();
        }

    }

    @Override
    public void handleSaveRequest() throws IOException {
        PropertyManager propertyManager = PropertyManager.getManager();
        if (workFile == null) {
            FileChooser filechooser = new FileChooser();
            Path appDirPath = Paths.get(propertyManager.getPropertyValue(APP_TITLE)).toAbsolutePath();
            Path targetPath = appDirPath.resolve(APP_WORKDIR_PATH.getParameter());
            filechooser.setInitialDirectory(targetPath.toFile());
            filechooser.setTitle(propertyManager.getPropertyValue(SAVE_WORK_TITLE));
            String description = propertyManager.getPropertyValue(WORK_FILE_EXT_DESC);
            String extension = propertyManager.getPropertyValue(WORK_FILE_EXT);
            ExtensionFilter extFilter = new ExtensionFilter(String.format("%s (*.%s)", description, extension),
                    String.format("*.%s", extension));
            filechooser.getExtensionFilters().add(extFilter);
            File selectedFile = filechooser.showSaveDialog(appTemplate.getGUI().getWindow());
            if (selectedFile != null)
                save(selectedFile.toPath());
        } else
            save(workFile);
    }

    @Override
    public void handleLoadRequest() throws IOException {
        boolean load = true;
        if (gamestate.equals(GameState.INITIALIZED_MODIFIED))
            load = promptToSave();
        if (load) {
            PropertyManager propertyManager = PropertyManager.getManager();
            FileChooser filechooser = new FileChooser();
            Path appDirPath = Paths.get(propertyManager.getPropertyValue(APP_TITLE)).toAbsolutePath();
            Path targetPath = appDirPath.resolve(APP_WORKDIR_PATH.getParameter());
            filechooser.setInitialDirectory(targetPath.toFile());
            filechooser.setTitle(propertyManager.getPropertyValue(LOAD_WORK_TITLE));
            String description = propertyManager.getPropertyValue(WORK_FILE_EXT_DESC);
            String extension = propertyManager.getPropertyValue(WORK_FILE_EXT);
            ExtensionFilter extFilter = new ExtensionFilter(String.format("%s (*.%s)", description, extension),
                    String.format("*.%s", extension));
            filechooser.getExtensionFilters().add(extFilter);
            File selectedFile = filechooser.showOpenDialog(appTemplate.getGUI().getWindow());
            if (selectedFile != null && selectedFile.exists())
                load(selectedFile.toPath());
            bool = gamedata.gethint();

            restoreGUI();
            // restores the GUI to reflect the state in which the loaded game was last saved
        }
    }

    @Override
    public void handleExitRequest() {
        try {
            boolean exit = true;
            if (gamestate.equals(GameState.INITIALIZED_MODIFIED))
                exit = promptToSave();
            if (exit)
                System.exit(0);
        } catch (IOException ioe) {
            AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
            PropertyManager props = PropertyManager.getManager();
            dialog.show(props.getPropertyValue(SAVE_ERROR_TITLE), props.getPropertyValue(SAVE_ERROR_MESSAGE));
        }
    }

    private void ensureActivatedWorkspace() {
        appTemplate.getWorkspaceComponent().activateWorkspace(appTemplate.getGUI().getAppPane());
    }

    private boolean promptToSave() throws IOException {
        PropertyManager propertyManager = PropertyManager.getManager();
        YesNoCancelDialogSingleton yesNoCancelDialog = YesNoCancelDialogSingleton.getSingleton();

        yesNoCancelDialog.show(propertyManager.getPropertyValue(SAVE_UNSAVED_WORK_TITLE),
                propertyManager.getPropertyValue(SAVE_UNSAVED_WORK_MESSAGE));

        if (yesNoCancelDialog.getSelection().equals(YesNoCancelDialogSingleton.YES))
            handleSaveRequest();

        return !yesNoCancelDialog.getSelection().equals(YesNoCancelDialogSingleton.CANCEL);
    }

    /**
     * A helper method to save work. It saves the work, marks the current work file as saved, notifies the user, and
     * updates the appropriate controls in the user interface
     *
     * @param target The file to which the work will be saved.
     * @throws IOException
     */
    private void save(Path target) throws IOException {
        appTemplate.getFileComponent().saveData(appTemplate.getDataComponent(), target);
        workFile = target;
        setGameState(GameState.INITIALIZED_UNMODIFIED);
        AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
        PropertyManager props = PropertyManager.getManager();
        dialog.show(props.getPropertyValue(SAVE_COMPLETED_TITLE), props.getPropertyValue(SAVE_COMPLETED_MESSAGE));
    }

    /**
     * A helper method to load saved game data. It loads the game data, notified the user, and then updates the GUI to
     * reflect the correct state of the game.
     *
     * @param source The source data file from which the game is loaded.
     * @throws IOException
     */
    private void load(Path source) throws IOException {
        // load game data
        appTemplate.getFileComponent().loadData(appTemplate.getDataComponent(), source);

        // set the work file as the file from which the game was loaded
        workFile = source;

        // notify the user that load was successful
        AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
        PropertyManager props = PropertyManager.getManager();
        dialog.show(props.getPropertyValue(LOAD_COMPLETED_TITLE), props.getPropertyValue(LOAD_COMPLETED_MESSAGE));

        setGameState(GameState.INITIALIZED_UNMODIFIED);
        Workspace gameworkspace = (Workspace) appTemplate.getWorkspaceComponent();
        ensureActivatedWorkspace();
        gameworkspace.reinitialize();
        gamedata = (GameData) appTemplate.getDataComponent();
    }

    public static char selectAChar(String s) {

        Random random = new Random();
        int index = random.nextInt(s.length());
        return s.charAt(index);
    }

    public void disablehint2() {
        if (hint2 == null) {
            Workspace workspace = (Workspace) appTemplate.getWorkspaceComponent();
            hint2 = workspace.gethint2();
        }
        hint2.setDisable(true);
    }

    public void enablehint2() {
        if (hint2 == null) {
            Workspace workspace = (Workspace) appTemplate.getWorkspaceComponent();
            hint2 = workspace.gethint2();
        }
        hint2.setDisable(false);
    }
    public void handleHintRequest() {
        Workspace gameworkspace = (Workspace) appTemplate.getWorkspaceComponent();
       // appTemplate.getGUI().disableHint();
        gamedata.sethint(false);
        disablehint2();
        BorderPane bp = gameworkspace.bp();
        initialize(bp);
        setGameState(GameState.INITIALIZED_MODIFIED);
        man.get(10-left[0]).setVisible(true);
        String word = gamedata.getTargetWord();
        int j = gamedata.getRemainingGuesses()-1;
        char c;
        String m = String.valueOf(gamedata.getGoodGuesses());
        c = selectAChar(word);
        if (m.contains(String.valueOf(c))) {
            handleHintRequest();
        }
        for (int i = 0; i < progress.length; i++) {
            {
                if (gamedata.getTargetWord().charAt(i) == c) {
                    progress[i].setVisible(true);
                    gamedata.addGoodGuess(c);
                    discovered++;
                }
            }
        }
        success = (discovered == progress.length);
        gamedata.addBadGuess(' ');
        remains.setText(Integer.toString(j));
        gamedata.setRemainingGuesses(j);
        if (gamedata.getRemainingGuesses() == 0) {
            success = false;
            end();
        }
    }
}
