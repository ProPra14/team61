package de.hhu.propra.team61.objects;

import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;

/**
 * A GridPane representing a terrain.
 * The class has methods for getting spawn points, walkability checks and destroying terrain (TODO actually not)
 * Created by markus on 17.05.14.
 */
public class Terrain extends GridPane {
    private static final boolean DEBUG = false;
    private static final boolean GRID_ENABLED = false;

    private static String imgPath = "file:resources/";

    private ArrayList<ArrayList<Character>> terrain;
    private ArrayList<Point2D> spawnPoints;
    private ArrayList<Figure> figures;

    /**
     * @param terrain 2-D-ArrayList containing the terrain to be displayed
     */
    public Terrain(ArrayList<ArrayList<Character>> terrain) {
        load(terrain);
        figures = new ArrayList<>();
    }

    public void load(ArrayList<ArrayList<Character>> terrain) {
        getChildren().clear();

        this.terrain = terrain;

        spawnPoints = new ArrayList<Point2D>();

        //Draw Terrain
        setAlignment(Pos.TOP_LEFT);
        setGridLinesVisible(GRID_ENABLED);

        String img;

        for(int i=0; i < terrain.size(); i++){
            for(int j=0; j < terrain.get(i).size(); j++){
                char terraintype = terrain.get(i).get(j);
                switch(terraintype) {
                    case '_': img = "plain_ground.png";
                        break;
                    case '/': img = "slant_ground_ri.png";
                        break;
                    case '\\': img = "slant_ground_le.png";
                        break;
                    case '|': img = "wall_le.png";
                        break;
                    case 'S': img = "stones.png";
                        break;
                    case 'E': img = "earth.png";
                        break;
                    case 'W': img = "water.png";
                        break;
                    case 'I': img = "ice.png";
                        break;
                    case 'L': img = "lava.png";
                        break;
                    case 'P': // special case: spawn point, add to list and draw sky
                        spawnPoints.add(new Point2D(j, i));
                        terrain.get(i).set(j, ' ');
                    default : img = "sky.png";
                }
                Image image = new Image(imgPath + img);
                ImageView content = new ImageView();
                content.setImage(image);

                add(content, j, i);
                //terrainGrid.setConstraints(content,j,i);
            }
        }
    }

    /**
     * @return the 2-D-ArrayList representing the loaded terrain
     */
    public ArrayList<ArrayList<Character>> toArrayList() {
        return terrain;
    }

    /**
     * get a spawn point and remove it from the list of available spawn points
     * @return a random spawn point, or null if there are no more spawn points
     */
    public Point2D getRandomSpawnPoint() {
        if(spawnPoints.isEmpty()) {
            return null;
        }
        int index = (int) (Math.random()*spawnPoints.size());
        Point2D spawnPoint = spawnPoints.get(index);
        spawnPoints.remove(index);
        System.out.println("TERRAIN: returning spawn point #" + index + " " + spawnPoint + " (" + spawnPoints.size() + " left)");
        return spawnPoint;
    }

    /**
     * @param n
     * @return n random spawn points
     * @see #getRandomSpawnPoint()
     */
    public ArrayList<Point2D> getRandomSpawnPoints(int n) {
        ArrayList<Point2D> spawnPoints = new ArrayList<>();
        for(int i=0; i<n; i++) {
            Point2D sp = getRandomSpawnPoint();
            if(sp != null) spawnPoints.add(sp);
        }
        return spawnPoints;
    }

    /**
     * @param hitRegion of a figure or whatever
     * @param x column of the field
     * @param y row of the field
     * @return true, when hitRegion intersects with the field
     */
    private boolean intersects(Rectangle2D hitRegion, int x, int y) {
        char c;
        try {
            c = terrain.get(y).get(x);
        } catch(IndexOutOfBoundsException e) {
            // this means we "collided" with the end of the terrain, pretend that it is stone
            c = 'S';
        }

        switch(c) {
            case ' ':
                return false;
            case '/':
                Point2D p;
                for(int i=0; i<8; i++) {
                    int px = x*8+i;
                    int py = y*8+8-i;
                    p = new Point2D(px, py);
                    if(hitRegion.contains(p)) {
                        debugLog("diagonal / intersection at " + px + "x" + py + "px");
                        return true;
                    }
                }
                return false;
            case '\\':
                for(int i=0; i<8; i++) {
                    int px = x*8+i;
                    int py = y*8+1+i;
                    p = new Point2D(px, py);
                    if(hitRegion.contains(p)) {
                        debugLog("diagonal / intersection at " + px + "x" + py + "px");
                        return true;
                    }
                }
                return false;
            default:
                Rectangle2D rec = new Rectangle2D(x * 8, y * 8, 8, 8);
                return hitRegion.intersects(rec);
        }
    }

    private static void debugLog(String msg) {
        if(DEBUG) System.out.println(msg);
    }

    /**
     * adds direction to oldPosition, but assures that we do not walk/fly through terrain or other figures
     * When canWalkAlongDiagonals is true, the movement continues at slopes in diagonal direction (used for figures);
     * otherwise, the movement is stopped (used for projectiles)
     * @param oldPosition old position of the object
     * @param direction direction vector of the object
     * @param hitRegion a rectangle describing the area where the object can collide with terrain etc.
     * @param canWalkAlongDiagonals when true, the object is moved along diagonal walls
     * @param canWalkThroughFigures when true, the object is able to walk through figures (no CollisionWithFigureException will be thrown) TODO therefore, have a wrapper function which does not throw this exception
     * @param snapToPx when true, the positions returned are rounded to whole px
     * @return new position of the object
     * @throws CollisionWithFigureException thrown when hitting a figure
     * @throws CollisionWithTerrainException thrown when hitting terrain
     */
    public Point2D getPositionForDirection(Point2D oldPosition, Point2D direction, Rectangle2D hitRegion, boolean canWalkAlongDiagonals, boolean canWalkThroughFigures, boolean snapToPx) throws CollisionWithTerrainException, CollisionWithFigureException {
        Point2D newPosition = new Point2D(oldPosition.getX(), oldPosition.getY());
        Point2D normalizedDirection = direction.normalize();

        debugLog("start position: " + oldPosition);
        debugLog("normalized velocity: " + normalizedDirection);

        final int runs = (int)direction.magnitude();

        for(int i=0; i<runs; i++) {
            // move position by 1px
            newPosition = newPosition.add(normalizedDirection);

            // calculate moved hitRegion
            hitRegion = new Rectangle2D(hitRegion.getMinX() + normalizedDirection.getX(), hitRegion.getMinY() + normalizedDirection.getY(), hitRegion.getWidth(), hitRegion.getHeight());

            debugLog("checking new position for collision: " + newPosition + " (" + (i + 1) + "/" + runs + ")" + " " + hitRegion);

            // check if hitRegion intersects with non-walkable terrain
            boolean triedDiagonal = false;
            int tries = 0;
            Point2D diagonalDirection = new Point2D(0, 0);
            do { // while(triedDiagonal && ++tries<2)
                triedDiagonal = false;

                // calculate indices of fields which are touched by hitRegion
                int minY = (int) Math.floor(hitRegion.getMinY() / 8);
                int maxY = (int) Math.ceil(hitRegion.getMaxY() / 8);
                int minX = (int) Math.floor(hitRegion.getMinX() / 8);
                int maxX = (int) Math.ceil(hitRegion.getMaxX() / 8);

                for (int y = minY; y <= maxY && !triedDiagonal; y++) { // TODO recheck necessity of <=
                    for (int x = minX; x <= maxX && !triedDiagonal; x++) {
                        //debugLog(hitRegion + " " + terrain.get(y).get(x) + " field: " + rec);
                        boolean intersects = intersects(hitRegion, x, y);
                        Figure intersectingFigure = null;
                        if (!canWalkThroughFigures && !intersects) {
                            for (Figure figure : figures) {
                                if (hitRegion.intersects(figure.getHitRegion())) {
                                    intersects = true;
                                    intersectingFigure = figure;
                                }
                            }
                        }
                        if (intersects) {
                            try {
                                debugLog("intersection at " + x + " " + y + " with " + terrain.get(y).get(x));
                                if (intersectingFigure != null)
                                    debugLog("intersecting with " + intersectingFigure.getName() + " at " + intersectingFigure.getPosition());
                            } catch (IndexOutOfBoundsException e) {
                                debugLog("intersection at " + x + " " + y + " out of bounds");
                            }
                            if (canWalkAlongDiagonals && tries == 0 && intersectingFigure == null) {
                                diagonalDirection = new Point2D(Math.signum(normalizedDirection.getX())/12, -1.5);
                                Point2D positionOnSlope = newPosition.subtract(normalizedDirection).add(diagonalDirection);
                                hitRegion = new Rectangle2D(hitRegion.getMinX() - normalizedDirection.getX() + diagonalDirection.getX(), hitRegion.getMinY() - normalizedDirection.getY() + diagonalDirection.getY(), hitRegion.getWidth(), hitRegion.getHeight());
                                newPosition = positionOnSlope;
                                triedDiagonal = true;
                                debugLog("trying to walk diagonal along " + diagonalDirection + " to " + newPosition + " " + hitRegion);
                            } else {
                                if (diagonalDirection.magnitude() == 0) { // did not go diagonal
                                    newPosition = newPosition.subtract(normalizedDirection);
                                } else {
                                    newPosition = newPosition.subtract(diagonalDirection);
                                }
                                if (snapToPx) {
                                    newPosition = new Point2D(Math.floor(newPosition.getX()), Math.ceil(newPosition.getY())); // TODO code duplication
                                }
                                if (intersectingFigure == null) {
                                    throw new CollisionWithTerrainException(newPosition);
                                } else {
                                    throw new CollisionWithFigureException(newPosition, intersectingFigure);
                                }
                            }
                        }
                    }
                } // for each field
            } while (triedDiagonal && ++tries < 2);
        }

        if(snapToPx) {
            newPosition = new Point2D(Math.floor(newPosition.getX()), Math.ceil(newPosition.getY())); // TODO code duplication
        }
        return newPosition;
    }

    public void addFigures(ArrayList<Figure> figures) {
        this.figures.addAll(figures);
    }
}