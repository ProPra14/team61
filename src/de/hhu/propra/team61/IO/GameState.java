package de.hhu.propra.team61.IO;

import de.hhu.propra.team61.IO.JSON.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Created by markus on 13.05.14.
 */
public class GameState {

    private static final String SAVE_STATE_FILE = "Afrobob.state.conf";

    /**
     * @param json is saved to SAVE_STATE_FILE
     */
    public static void save(JSONObject json) {
        Json.save(json, SAVE_STATE_FILE);
    }

    /**
     * @return game state from SAVE_STATE_FILE
     */
    public static JSONObject getSavedGameState() {
        return Json.getFromFile(SAVE_STATE_FILE);
    }

}
