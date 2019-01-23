package data;

import apptemplate.AppTemplate;
import components.AppDataComponent;
import controller.GameError;
import controller.HangmanController;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class GameData implements AppDataComponent {

    public static final  int TOTAL_NUMBER_OF_GUESSES_ALLOWED = 10;
    private static final int TOTAL_NUMBER_OF_STORED_WORDS    = 330622;

    private String         targetWord;
    private boolean        b;
    private Set<Character> goodGuesses;
    private Set<Character> badGuesses;
    private String         guessed = "";
    private int            remainingGuesses;
    public  AppTemplate    appTemplate;
    private int            pos = 0;

    public GameData(AppTemplate appTemplate) {
        this(appTemplate, false);
    }

    public GameData(AppTemplate appTemplate, boolean initiateGame) {
        if (initiateGame) {
            this.appTemplate = appTemplate;
            this.targetWord = setTargetWord();
            this.goodGuesses = new HashSet<>();
            this.badGuesses = new HashSet<>();
            this.remainingGuesses = TOTAL_NUMBER_OF_GUESSES_ALLOWED;
        } else {
            this.appTemplate = appTemplate;
        }
    }

    public void init() {
        this.targetWord = setTargetWord();
        this.goodGuesses = new HashSet<>();
        this.badGuesses = new HashSet<>();
        this.remainingGuesses = TOTAL_NUMBER_OF_GUESSES_ALLOWED;
    }

    public void sethint(boolean b)
    {
        this.b=b;
    }

    public boolean gethint()
    {
        return b;
    }

    @Override
    public void reset() {
        this.targetWord = null;
        this.goodGuesses = new HashSet<>();
        this.badGuesses = new HashSet<>();
        this.remainingGuesses = TOTAL_NUMBER_OF_GUESSES_ALLOWED;
        appTemplate.getWorkspaceComponent().reloadWorkspace();
    }

    public boolean hinter()
    {

        if(countUniqueCharacters()>7){
            return true;
        }
        else return false;
    }

    public int countUniqueCharacters() {
        String word = getTargetWord();
        boolean[] isItThere = new boolean[Character.MAX_VALUE];
        for (int i = 0; i < word.length(); i++) {
            isItThere[word.charAt(i)] = true;
        }

        int count = 0;
        for (int i = 0; i < isItThere.length; i++) {
            if (isItThere[i] == true){
                count++;
            }
        }

        return count;
    }

    public boolean containsIllegals(String toExamine) {
        Pattern pattern = Pattern.compile("[-=)?(*&$!`;\':,./~#@+%{}<>\\[\\]|\"\\_^1234567890]");
        Matcher matcher = pattern.matcher(toExamine);
        return matcher.find();
    }

    public String getTargetWord() {
        return targetWord;
    }

    private String setTargetWord() {
        String s;
        URL wordsResource = getClass().getClassLoader().getResource("words/words.txt");
        assert wordsResource != null;

        int toSkip = new Random().nextInt(TOTAL_NUMBER_OF_STORED_WORDS);
        try (Stream<String> lines = Files.lines(Paths.get(wordsResource.toURI()))) {
            s = lines.skip(toSkip).findFirst().get();
            if(!containsIllegals(s)) {
                return s;
            }
            else{ return setTargetWord();}
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            System.exit(1);
        }

        throw new GameError("Unable to load initial target word.");
    }

    public GameData setTargetWord(String targetWord) {
        this.targetWord = targetWord;
        return this;
    }

    public Set<Character> getGoodGuesses() {
        return goodGuesses;
    }

    public GameData setGoodGuesses(Set<Character> goodGuesses) {
        this.goodGuesses = goodGuesses;
        return this;
    }

    public Set<Character> getBadGuesses() {
        return badGuesses;
    }

    public GameData setBadGuesses(Set<Character> badGuesses) {
        this.badGuesses = badGuesses;
        return this;
    }

    public int getPosi(){return pos;}

    public void setPosi(int i){ pos = i; }

    public int getRemainingGuesses() {
        return remainingGuesses;
    }

    public void setRemainingGuesses(int remainingGuesses) {this.remainingGuesses=remainingGuesses;}

    public void addGoodGuess(char c) {
        goodGuesses.add(c);
    }



    public void addguessed(char c)
    {
        guessed = guessed + c + " ";
    }

    public void p(){guessed = "";}

    public String retguessed()
    {
        return guessed;
    }

    public void addBadGuess(char c) {
        if (!badGuesses.contains(c)) {
            badGuesses.add(c);
            remainingGuesses--;
        }
    }


}
