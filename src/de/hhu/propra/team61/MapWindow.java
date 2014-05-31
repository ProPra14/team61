package de.hhu.propra.team61;

import de.hhu.propra.team61.IO.GameState;
import de.hhu.propra.team61.IO.JSON.JSONArray;
import de.hhu.propra.team61.IO.JSON.JSONObject;
import de.hhu.propra.team61.IO.Settings;
import de.hhu.propra.team61.IO.TerrainManager;
import de.hhu.propra.team61.Network.Client;
import de.hhu.propra.team61.Network.Networkable;
import de.hhu.propra.team61.Network.Server;
import de.hhu.propra.team61.Objects.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by kegny on 08.05.14.
 * Edited by DiniiAntares on 15.05.14
 * This class is supposed to draw the Array given by "TerrainManager" rendering the Map visible.
 */
public class MapWindow extends Application implements Networkable {
    private ArrayList<Team> teams;
    private Scene drawing;
    private Stage primaryStage;
    private BorderPane root;
    private StackPane centerView;
    private Terrain terrain;
    private Label teamLabel;
    private int currentTeam = 0;
    private int turnCount = 0;
    private int levelCounter = 0;
    private Projectile flyingProjectile = null;
    private Thread moveObjectsThread;
    private Stage stageToClose;
    private int teamquantity;
    private int teamsize;
    private Server server;
    private Client client;
    private Thread serverThread;
    private Thread clientThread;
    private String map;

    @Deprecated
    public MapWindow(String map) {
        try {
            terrain = new Terrain(TerrainManager.load(map));
        } catch (FileNotFoundException e) {
            // TODO do something sensible here
            e.printStackTrace();
        }

        teams = new ArrayList<>();
        for(int i=0; i<2; i++) { // TODO hard coded 2 teams, 2 figures
            ArrayList<Weapon> weapons = new ArrayList<>();
            weapons.add(new Gun("file:resources/weapons/temp1.png", 50, 2));
            weapons.add(new Grenade("file:resources/weapons/temp2.png", 40, 2));
            teams.add(new Team(terrain.getRandomSpawnPoints(2), weapons, Color.WHITE));
        }

        initialize();
    }

    public MapWindow(String map, Stage stageToClose, String file, Client client, Thread clientThread, Server server, Thread serverThread) {
        this.map = map;
        this.client = client;
        this.clientThread = clientThread;
        client.registerMapWindow(this);
        this.server = server;
        this.serverThread = serverThread;
        if(server != null) server.registerCurrentNetworkable(this);

        // TODO code duplication; we have to check what we actually need at the end of the week
        try {
            terrain = new Terrain(TerrainManager.load(map));
        } catch (FileNotFoundException e) {
            // TODO do something sensible here
            e.printStackTrace();
        }

        this.stageToClose = stageToClose;
        JSONObject settings = Settings.getSavedSettings(file);
        this.teamquantity = settings.getInt("numberOfTeams");
        this.teamsize = Integer.parseInt(settings.getString("team-size"));
        teams = new ArrayList<>();
        JSONArray teamsArray = settings.getJSONArray("teams");
        for(int i=0; i<teamsArray.length(); i++) {
            ArrayList<Weapon> weapons = new ArrayList<>();
            weapons.add(new Gun("file:resources/weapons/temp1.png", 50, settings.getInt("weapon1")));
            weapons.add(new Grenade("file:resources/weapons/temp2.png", 40, settings.getInt("weapon2")));
            weapons.add(new Gun("file:resources/weapons/temp3.png", 30, settings.getInt("weapon3")));
            teams.add(new Team(terrain.getRandomSpawnPoints(teamsize), weapons, Color.web(teamsArray.getJSONObject(i).getString("color"))));
        }

        initialize();
    }

    public MapWindow(String map, Stage stageToClose, String file) {
        try {
            terrain = new Terrain(TerrainManager.load(map));
        } catch (FileNotFoundException e) {
            // TODO do something sensible here
            e.printStackTrace();
        }

        this.stageToClose = stageToClose;
        JSONObject settings = Settings.getSavedSettings(file);
        this.teamquantity = settings.getInt("numberOfTeams");
        this.teamsize = Integer.parseInt(settings.getString("team-size"));
        teams = new ArrayList<>();
        JSONArray teamsArray = settings.getJSONArray("teams");
        for(int i=0; i<teamsArray.length(); i++) {
            ArrayList<Weapon> weapons = new ArrayList<>();
            weapons.add(new Gun("file:resources/weapons/temp1.png", 50, settings.getInt("weapon1")));
            weapons.add(new Grenade("file:resources/weapons/temp2.png", 40, settings.getInt("weapon2")));
            weapons.add(new Gun("file:resources/weapons/temp3.png", 30, settings.getInt("weapon3")));
            teams.add(new Team(terrain.getRandomSpawnPoints(teamsize), weapons, Color.web(teamsArray.getJSONObject(i).getString("color"))));
        }

        initialize();
    }

    public MapWindow(JSONObject input, Stage stageToClose) {
        this.terrain = new Terrain(TerrainManager.loadSavedLevel());

        teams = new ArrayList<>();
        JSONArray teamsArray = input.getJSONArray("teams");
        for(int i=0; i<teamsArray.length(); i++) {
            teams.add(new Team(teamsArray.getJSONObject(i)));
        }

        turnCount = input.getInt("turnCount");
        currentTeam = input.getInt("currentTeam");

        this.stageToClose = stageToClose;
        initialize();
    }

    /**
     * creates the stage, so that everything is visible
     */
    private void initialize() {
        primaryStage = new Stage();
        primaryStage.setOnCloseRequest(event -> {
            if(moveObjectsThread != null) moveObjectsThread.interrupt();

            GameState.save(this.toJson());
            TerrainManager.save(terrain.toArrayList());
            System.out.println("MapWindow: saved game state");

            clientThread.interrupt();
            if(serverThread != null) serverThread.interrupt();
            System.out.println("MapWindow threads interrupted");
            client.stop();
            if(server != null) server.stop();
            System.out.println("MapWindow client/server (if any) stopped");

            stageToClose.show();
            primaryStage.close();
        });

        // pane containing terrain, labels at the bottom etc.
        root = new BorderPane();
        // contains the terrain with figures
        centerView = new StackPane();
        centerView.setAlignment(Pos.TOP_LEFT);
        centerView.getChildren().add(terrain);
        root.setCenter(centerView);
        for(Team team: teams) {
            centerView.getChildren().add(team);
            terrain.addFigures(team.getFigures());
        }
        teamLabel = new Label("Team" + currentTeam + "s turn. What will " + teams.get(currentTeam).getCurrentFigure().getName() + " do?");

        root.setBottom(teamLabel);

        drawing = new Scene(root, 1600, 300);
        drawing.setOnKeyPressed(
                keyEvent -> {
                    System.out.println("key pressed: " + keyEvent.getCode());
                    client.sendKeyEvent(keyEvent.getCode());
                }
        );

        primaryStage.setTitle("The Playground");
        primaryStage.setScene(drawing);
        primaryStage.show();

        if(server != null) { // only the server should do calculations
            moveObjectsThread = new Thread(() -> { // TODO move this code to own class
                try {
                    long before = System.currentTimeMillis(), now, sleep;
                    while (true) {
                        if (flyingProjectile != null) {
                            try {
                                final Point2D newPos;
                                newPos = terrain.getPositionForDirection(flyingProjectile.getPosition(), flyingProjectile.getVelocity(), flyingProjectile.getHitRegion(), false, false, false);
                                Platform.runLater(() -> flyingProjectile.setPosition(new Point2D(newPos.getX(), newPos.getY())));
                                server.sendCommand("PROJECTILE_SET_POSITION " + newPos.getX() + " " + newPos.getY());
                            } catch (CollisionWithTerrainException e) {
                                System.out.println("CollisionWithTerrainException, let's destroy something!"); // TODO
                                server.sendCommand("REMOVE_FLYING_PROJECTILE"); // TODO potential race condition (might still be !=null in next iteration)
                                endTurn();
                            } catch (CollisionWithFigureException e) {
                                System.out.println("CollisionWithFigureException, let's harm somebody!");
                                Platform.runLater(() -> {
                                    e.getCollisionPartner().sufferDamage(flyingProjectile.getDamage());
                                    server.sendCommand("SET_HP " + getFigureId(e.getCollisionPartner()) + " " + e.getCollisionPartner().getHp());
                                    server.sendCommand("REMOVE_FLYING_PROJECTILE"); // TODO potential race condition
                                    endTurn();
                                });
                            }
                        }
                        now = System.currentTimeMillis();
                        sleep = Math.max(0, (1000 / 10) - (now - before)); // 10 fps
                        Thread.sleep(sleep);
                        before = System.currentTimeMillis();
                    }
                } catch (InterruptedException e) {
                    System.out.println("moveObjectsThread shut down");
                }
            });
            moveObjectsThread.start();
        }

        stageToClose.close();
    }

    /**
     * @return the whole state of the window as JSONObject (except terrain, use terrain.toArrayList())
     */
    public JSONObject toJson() {
        JSONObject output = new JSONObject();
        JSONArray teamsArray = new JSONArray();
        for(Team t: teams) {
            teamsArray.put(t.toJson());
        }
        output.put("teams", teamsArray);
        output.put("turnCount", turnCount);
        output.put("currentTeam", currentTeam);
        return output;
    }

    /**
     *
     * @param figure
     * @return
     */
    private String getFigureId(Figure figure) {
        String id = "";
        for(int i=0; i<teams.size(); i++) {
            for(int j=0; j<teams.get(i).getFigures().size(); j++) {
                if(teams.get(i).getFigures().get(j) == figure) {
                    id = i+" "+j;
                }
            }
        }
        return id;
    }

    @Deprecated
    public void cheatMode() {
        try {
            levelCounter++;
            terrain.load(TerrainManager.load(TerrainManager.getAvailableTerrains().get(levelCounter = levelCounter % TerrainManager.getNumberOfAvailableTerrains())));
            // quite bad hack to reload spawn points, but ok as it's a cheat anyway
            for(Team team: teams) {
                centerView.getChildren().remove(team);
            }
            teams.clear();
            for(int i=0; i<teamquantity; i++) { // TODO hard coded 2 teams, 2 figures
                ArrayList<Weapon> weapons = new ArrayList<>();
                weapons.add(new Gun("file:resources/weapons/temp1.png", 50, 2));
                weapons.add(new Grenade("file:resources/weapons/temp2.png", 40, 2));
                Team team = new Team(terrain.getRandomSpawnPoints(teamsize), weapons, Color.WHITE);
                teams.add(team);
                centerView.getChildren().add(team);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    public void endTurn() {
        turnCount++; // TODO timing issue
        server.sendCommand("SET_TURN_COUNT " + turnCount);

        int oldCurrentTeam = currentTeam;
        do {
            currentTeam++;
            if (currentTeam == teams.size()) {
                currentTeam = 0;
            }
            if (currentTeam == oldCurrentTeam) {
                server.sendCommand("GAME_OVER " + currentTeam);
                return;
            }
        } while (teams.get(currentTeam).getNumberOfLivingFigures() == 0);

        server.sendCommand("CURRENT_TEAM_END_ROUND");
        server.sendCommand("TEAM_LABEL_SET_TEXT " + "It's team " + currentTeam + "'s turn");

        System.out.println("Turn " + currentTeam + ", Team " + currentTeam + ", Worm \"" + teams.get(currentTeam).getCurrentFigure().getName() + "\"");
    }

    @Override
    public void start(Stage ostage) {
    }

    @Override
    public void handleOnClient(String command) {
        String[] cmd = command.split(" ");

        switch(cmd[0]) {
            case "CURRENT_TEAM_END_ROUND":
                teams.get(currentTeam).endRound();
                break;
            case "CURRENT_FIGURE_ANGLE_DOWN":
                if(teams.get(currentTeam).getCurrentFigure().getSelectedItem() != null) {
                    teams.get(currentTeam).getCurrentFigure().getSelectedItem().angle_down(teams.get(currentTeam).getCurrentFigure() .getFacing_right());
                }
                break;
            case "CURRENT_FIGURE_ANGLE_UP":
                if (teams.get(currentTeam).getCurrentFigure().getSelectedItem() != null) {
                    teams.get(currentTeam).getCurrentFigure().getSelectedItem().angle_up(teams.get(currentTeam).getCurrentFigure().getFacing_right());
                }
                break;
            case "CURRENT_FIGURE_CHOOSE_WEAPON_1":
                if (teams.get(currentTeam).getCurrentFigure().getSelectedItem() != null) {
                    centerView.getChildren().remove(teams.get(currentTeam).getCurrentFigure().getSelectedItem().getCrosshair());
                    centerView.getChildren().remove(teams.get(currentTeam).getCurrentFigure().getSelectedItem());
                }
                teams.get(currentTeam).getCurrentFigure().setSelectedItem(teams.get(currentTeam).getWeapon(0));
                centerView.getChildren().add(teams.get(currentTeam).getCurrentFigure().getSelectedItem());
                centerView.getChildren().add(teams.get(currentTeam).getCurrentFigure().getSelectedItem().getCrosshair());
                break;
            case "CURRENT_FIGURE_CHOOSE_WEAPON_2":
                if (teams.get(currentTeam).getCurrentFigure().getSelectedItem() != null) {
                    centerView.getChildren().remove(teams.get(currentTeam).getCurrentFigure().getSelectedItem().getCrosshair());
                    centerView.getChildren().remove(teams.get(currentTeam).getCurrentFigure().getSelectedItem());
                }
                teams.get(currentTeam).getCurrentFigure().setSelectedItem(teams.get(currentTeam).getWeapon(1));
                centerView.getChildren().add(teams.get(currentTeam).getCurrentFigure().getSelectedItem());
                centerView.getChildren().add(teams.get(currentTeam).getCurrentFigure().getSelectedItem().getCrosshair());
                break;
            case "CURRENT_FIGURE_CHOOSE_WEAPON_3":
                if(teams.get(currentTeam).getNumberOfWeapons() >= 3) {
                    if (teams.get(currentTeam).getCurrentFigure().getSelectedItem() != null) {
                        centerView.getChildren().remove(teams.get(currentTeam).getCurrentFigure().getSelectedItem().getCrosshair());
                        centerView.getChildren().remove(teams.get(currentTeam).getCurrentFigure().getSelectedItem());
                    }
                    teams.get(currentTeam).getCurrentFigure().setSelectedItem(teams.get(currentTeam).getWeapon(2));
                    centerView.getChildren().add(teams.get(currentTeam).getCurrentFigure().getSelectedItem());
                    centerView.getChildren().add(teams.get(currentTeam).getCurrentFigure().getSelectedItem().getCrosshair());
                }
                break;
            case "CURRENT_FIGURE_FACE_LEFT":
                if(teams.get(currentTeam).getCurrentFigure().getSelectedItem() != null) {
                    teams.get(currentTeam).getCurrentFigure().setFacing_right(false);
                    teams.get(currentTeam).getCurrentFigure().getSelectedItem().angle_draw(teams.get(currentTeam).getCurrentFigure().getFacing_right());
                }
                break;
            case "CURRENT_FIGURE_FACE_RIGHT":
                if (teams.get(currentTeam).getCurrentFigure().getSelectedItem() != null) {
                    teams.get(currentTeam).getCurrentFigure().setFacing_right(true);
                    teams.get(currentTeam).getCurrentFigure().getSelectedItem().angle_draw(teams.get(currentTeam).getCurrentFigure().getFacing_right());
                }
                break;
            case "CURRENT_FIGURE_SET_POSITION":
                Figure f = teams.get(currentTeam).getCurrentFigure();
                f.setPosition(new Point2D(Double.parseDouble(cmd[1]) / 8, Double.parseDouble(cmd[2]) / 8));
                break;
//            case "Number Sign": // TODO really? this is broken and deprecated
//                cheatMode();
//                break;
            case "CURRENT_FIGURE_SHOOT":
                try {
                    Projectile projectile = teams.get(currentTeam).getCurrentFigure().shoot();
                    flyingProjectile = projectile;
                    centerView.getChildren().add(flyingProjectile);
                } catch (NoMunitionException e) {
                    System.out.println("no munition");
                    break;
                }
                centerView.getChildren().remove(teams.get(currentTeam).getCurrentFigure().getSelectedItem().getCrosshair());
                centerView.getChildren().remove(teams.get(currentTeam).getCurrentFigure().getSelectedItem());
                teams.get(currentTeam).getCurrentFigure().setSelectedItem(null);
                break;
            case "GAME_OVER":
                primaryStage.close();
                GameOverWindow gameOverWindow = new GameOverWindow();
                gameOverWindow.showWinner(Integer.parseInt(cmd[1]), stageToClose, map, "SETTINGS_FILE.conf", client, clientThread, server, serverThread);
                break;
            case "PROJECTILE_SET_POSITION": // TODO though server did null check, recheck here (problem when connecting later)
                flyingProjectile.setPosition(new Point2D(Double.parseDouble(cmd[1]), Double.parseDouble(cmd[2])));
                break;
            case "REMOVE_FLYING_PROJECTILE":
                centerView.getChildren().remove(flyingProjectile);
                flyingProjectile = null;
                break;
            case "SET_HP":
                teams.get(Integer.parseInt(cmd[1])).getFigures().get(Integer.parseInt(cmd[2])).setHp(Integer.parseInt(cmd[3]));
            case "SET_TURN_COUNT":
                turnCount = Integer.parseInt(cmd[1]);
                break;
            case "TEAM_LABEL_SET_TEXT":
                StringBuilder builder = new StringBuilder();
                for(int i=2; i<cmd.length; i++) builder.append(cmd[i]);
                teamLabel.setText(builder.toString());
                break;
            default:
                System.out.println("handleKeyEventOnClient: no event for key " + command);
        }
    }

    @Override
    public void handleKeyEventOnServer(String keyCode) {
        Point2D v = null;

        switch(keyCode) {
            case "Space":
                if(teams.get(currentTeam).getCurrentFigure().getSelectedItem() != null) {
                    server.sendCommand("CURRENT_FIGURE_SHOOT");
                }
                break;
            // these codes always result in optical changes only, so nothing to do on server side
            case "Up":
            case "W":
                server.sendCommand("CURRENT_FIGURE_ANGLE_UP");
                break;
            case "Down":
            case "S":
                server.sendCommand("CURRENT_FIGURE_ANGLE_DOWN");
                break;
            case "Left":
            case "A":
                if(teams.get(currentTeam).getCurrentFigure().getSelectedItem() != null) {
                   server.sendCommand("CURRENT_FIGURE_FACE_LEFT");
                   break;
                } else {
                    v = new Point2D(-10, 0);
                }
            case "Right":
            case "D":
                if (teams.get(currentTeam).getCurrentFigure().getSelectedItem() != null) {
                    server.sendCommand("CURRENT_FIGURE_FACE_RIGHT");
                } else {
                    if (v == null) v = new Point2D(+10, 0);
                    Figure f = teams.get(currentTeam).getCurrentFigure();
                    Point2D pos = new Point2D(f.getPosition().getX() * 8, f.getPosition().getY() * 8);
                    Rectangle2D hitRegion = f.getHitRegion();
                    Point2D newPos = null;
                    try {
                        newPos = terrain.getPositionForDirection(pos, v, hitRegion, true, true, true);
                    } catch (CollisionWithTerrainException e) {
                        System.out.println("CollisionWithTerrainException, stopped movement");
                        newPos = e.getLastGoodPosition();
                    } catch (CollisionWithFigureException e) {
                        // figures can walk through each other // TODO really?
                        System.out.println("ERROR How did we get here?");
                    }
                    server.sendCommand("CURRENT_FIGURE_SET_POSITION " + newPos.getX() + " " + newPos.getY());
                }
                break;
            case "1":
                if(teams.get(currentTeam).getNumberOfWeapons() >= 1) {
                    server.sendCommand("CURRENT_FIGURE_CHOOSE_WEAPON_1");
                }
                break;
            case "2":
                if(teams.get(currentTeam).getNumberOfWeapons() >= 2) {
                    server.sendCommand("CURRENT_FIGURE_CHOOSE_WEAPON_2");
                }
                break;
            case "3":
                if(teams.get(currentTeam).getNumberOfWeapons() >= 3) {
                    server.sendCommand("CURRENT_FIGURE_CHOOSE_WEAPON_3");
                }
                break;
            default:
                System.out.println("handleKeyEventOnServer: no event for key " + keyCode);
        }
    }

}
