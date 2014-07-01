package de.hhu.propra.team61.objects;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * An instance of this class represents a quadratic block of the terrain, rendered in an ImageView.
 * By calling {@link #TerrainBlock(char, int, int)}, you create a new ImageView which holds the image
 * matching the given terrain type. The given position information is used for collision handling. The following
 * characters are valid terrain types:
 * <ul>
 * <li>{@code ' '}: sky (transparent)
 * <li>{@code 'P'} (player): spawn point: rendered like sky
<<<<<<< HEAD
 * <li>{@code 'W'} (water): walkable liquid
=======
 * <li>{@code 'W'} (water): walkable liquid // TODO change collision: drop in? Yes please
>>>>>>> items
 * <li>{@code 'L'} (lava): walkable liquid
 * <li>{@code 'S'} (stone): walkable ground with normal friction
 * <li>{@code 's'} (sand): walkable ground with higher friction
 * <li>{@code 'E'} (soil): walkable ground with normal friction
 * <li>{@code 'I'} (ice): walkable ground with lower friction
 * <li>{@code 'i'} (snow): walkable ground with normal friction
 * <li>{@code '/'}, {@code '\'}: walkable slant ground
 * </ul>
 * After creating a block, you should call {@link #setTopNeighbour(TerrainBlock)}, {@link #setRightNeighbour(TerrainBlock)},
 * {@link #setBottomNeighbour(TerrainBlock)}, and {@link #setLeftNeighbour(TerrainBlock)}, since some parts of a block are
 * influenced by the neighbouring blocks (eg. the type of a slant depends on the blocks attached to it).
 * To change the terrain type afterwards, use {@link #setType(char)}. Note that this function does not convert spawn
 * points into sky. So if you want to render a spawn point, create a new block and then call {@code setType('P')}.
 * @see Terrain
 */
public class TerrainBlock extends ImageView {

    /** path to the terrain images */
    private final static String imgPath = "file:resources/terrain/";
    // preloaded terrain images
    /** image looking like earth */
    private final static Image SOIL_IMAGE = new Image(imgPath + "soil.png");
    /** image looking like ice */
    private final static Image ICE_IMAGE = new Image(imgPath + "ice.png");
    /** image looking like lave */
    private final static Image LAVE_IMAGE = new Image(imgPath + "lava.png");
    /** image looking like sand */
    private final static Image SAND_IMAGE = new Image(imgPath + "sand.png");
    /** image looking like sky */
    private final static Image SKY_IMAGE = new Image(imgPath + "sky.png");
    /** image looking like snow */
    private final static Image SNOW_IMAGE = new Image(imgPath + "snow.png");
    /** image looking like a slant from top left to bottom right */
    private final static Image SLANT_LE_IMAGE = new Image(imgPath + "slant_ground_le.png");
    /** image looking like a slant from top left to bottom right, with the other edge filled with water */
    private final static Image SLANT_LE_WATER_IMAGE = new Image(imgPath + "slant_ground_water_le.png");
    /** image looking like a slant from bottom left to top right */
    private final static Image SLANT_RI_IMAGE = new Image(imgPath + "slant_ground_ri.png");
    /** image looking like a slant from bottom left to top right, with the other edge filled with water */
    private final static Image SLANT_RI_WATER_IMAGE = new Image(imgPath + "slant_ground_water_ri.png");
    /** image looking like stone */
    private final static Image STONES_IMAGE = new Image(imgPath + "stones.png");
    /** image looking like water */
    private final static Image WATER_IMAGE = new Image(imgPath + "water.png");
    /** image looking like water with transparent region at the top */
    private final static Image WATER_TOP_IMAGE = new Image(imgPath + "water_top.png");
    /** image indicating a spawn point */
    private final static Image SPAWN_POINT = new Image(imgPath + "spawn.png");

    //Technical Blocks/Special Cases
    /** damage resistance of sky (used to limit range of weapons) */
    private final static double RESISTANCE_OF_SKY = 15;
    /** damage resistance of liquids (liquids cannot be destroyed) */
    private final static double RESISTANCE_OF_LIQUIDS = 99999999;
    //Blocks
    /** damage resistance of soil */
    private final static double RESISTANCE_OF_SOIL = 25; // TODO drop OF_ ?
    /** damage resistance of sand */
    private final static double RESISTANCE_OF_SAND = 20;
    /** damage resistance of snow */
    private final static double RESISTANCE_OF_SNOW = 20;
    /** damage resistance of stone */
    private final static double RESISTANCE_OF_STONE = 35;
    /** damage resistance of ice */
    private final static double RESISTANCE_OF_ICE = 30;
    //Modifiers
    /** factor by which the resistance of a slant differs from the block attached to it */
    final static double MODIFIER_FOR_SLANTS = 0.30;

    /** friction factor for ice */
    private final static double ICE_FRICTION = 2; // TODO rename to FRICTION_ICE
    /** friction factor for soil */
    private final static double SOIL_FRICTION = 1;
    /** friction factor for sand */ // TODO higher value = less friction?
    private final static double SAND_FRICTION = 0.5;
    /** friction factor for liquids */
    private final static double LIQUID_FRICTION = 0.5;

    /** distance in px by which a figure sinks into a liquid */
    private final static int LIQUID_SINK_DISTANCE = Terrain.BLOCK_SIZE/2;

    /** terrain type of this block */
    private char type;
    /** x coordinate (in blocks) of this block in the terrain */
    private int x;
    /** y coordinate (in blocks) of this block in the terrain */
    private int y;

    /** Class for grouping the neighbouring blocks together and performing actions on them. */
    private class NeighbouringBlocks {
        public TerrainBlock left = null;
        public TerrainBlock top = null;
        public TerrainBlock right = null;
        public TerrainBlock bottom = null;

        /** Calls {@link #drawImage()} for all set neighbour blocks */
        public void redrawImages() {
            if(left != null) left.drawImage();
            if(top != null) top.drawImage();
            if(right != null) right.drawImage();
            if(bottom != null) bottom.drawImage();
        }
    }
    /** contains references to the neighbouring blocks */
    private NeighbouringBlocks neighbours = new NeighbouringBlocks();

    /**
     * Creates a new block of the given type for the given position and draws it. Note that spawn points are treated as
     * sky.
     * @param type a terrain type
     * @param x the x coordinate (in blocks) of this block in the terrain
     * @param y the y coordinate (in blocks) of this block in the terrain
     */
    public TerrainBlock(char type, int x, int y) {
        if(type == 'P') type = ' '; // draw spawn points as sky (only on creation - changes later on probably done by level editor)
        this.type = type;
        this.x = x;
        this.y = y;

        drawImage();
    }

    /**
     * Sets the left neighbour of this block and redraws it.
     * @param block a reference to the left neighbour of this block
     */
    public void setLeftNeighbour(TerrainBlock block) {
        neighbours.left = block;
        drawImage();
    }

    /**
     * Sets the top neighbour of this block and redraws it.
     * @param block a reference to the top neighbour of this block
     */
    public void setTopNeighbour(TerrainBlock block) {
        neighbours.top = block;
        drawImage();
    }

    /**
     * Sets the right neighbour of this block and redraws it.
     * @param block a reference to the right neighbour of this block
     */
    public void setRightNeighbour(TerrainBlock block) {
        neighbours.right = block;
        drawImage();
    }

    /**
     * Sets the bottom neighbour of this block and redraws it.
     * @param block a reference to the bottom neighbour of this block
     */
    public void setBottomNeighbour(TerrainBlock block) {
        neighbours.bottom = block;
        drawImage();
    }

    /**
     * Draws the image fitting to the type of this terrain block.
     * The look of the block might be influenced by surrounding blocks. It is save to call this function from non-JavaFX
     * threads.
     */
    private void drawImage() {
        // make calling this save from everywhere (called very often with different names), but only use runLater if necessary (performance impact when starting game/edit)
        if(Platform.isFxApplicationThread()) {
            switch (type) {
                case ' ':
                    this.setImage(SKY_IMAGE);
                    break;
                case 'S':
                    this.setImage(STONES_IMAGE);
                    break;
                case 's':
                    this.setImage(SAND_IMAGE);
                    break;
                case 'E':
                    this.setImage(SOIL_IMAGE);
                    break;
                case 'I':
                    this.setImage(ICE_IMAGE);
                    break;
                case 'i':
                    this.setImage(SNOW_IMAGE);
                    break;
                case '/': // TODO use ice when appropriate
                    this.setImage(SLANT_RI_IMAGE);
                    if(neighbours.right != null && neighbours.right.isSky() && neighbours.bottom != null && neighbours.bottom.isSky()
                            && (neighbours.top == null || !neighbours.top.isSky())) {
                        this.setScaleX(-1); // mirror image when slope is at ceiling
                        this.setScaleY(-1);
                        if(neighbours.right.getType() == 'W' || neighbours.bottom.getType() == 'W') {
                            this.setImage(SLANT_RI_WATER_IMAGE);
                        }
                    } else {
                        this.setScaleX(1);
                        this.setScaleY(1);
                        if(neighbours.left != null) {
                            if(neighbours.left.getType() == 'W') this.setImage(SLANT_RI_WATER_IMAGE);
                        }
                    }
                    break;
                case '\\':
                    this.setImage(SLANT_LE_IMAGE);
                    if(neighbours.left != null && neighbours.left.isSky() && neighbours.bottom != null && neighbours.bottom.isSky()
                            && (neighbours.top == null || !neighbours.top.isSky())) {
                        this.setScaleX(-1);
                        this.setScaleY(-1);
                        if(neighbours.left.getType() == 'W' || neighbours.bottom.getType() == 'W') {
                            this.setImage(SLANT_LE_WATER_IMAGE);
                        }
                    } else {
                        this.setScaleX(1);
                        this.setScaleY(1);
                        if(neighbours.right != null) {
                            if(neighbours.right.getType() == 'W') this.setImage(SLANT_LE_WATER_IMAGE);
                        }
                    }
                    break;
                case 'W':
                    if (neighbours.top == null || neighbours.top.isSky()) {
                        this.setImage(WATER_TOP_IMAGE);
                    } else {
                        this.setImage(WATER_IMAGE);
                    }
                    break;
                case 'L':
                    this.setImage(LAVE_IMAGE);
                    break;
                case 'P': // special case: spawn point to be shown in level editor
                    this.setImage(SPAWN_POINT);
                    break;
                default:
                    this.setImage(SKY_IMAGE);
            }
        } else {
            System.err.println("HERE");
            Platform.runLater(this::drawImage);
        }
    }

    /**
     * Gets the resistance of this block, typically used when calculating the destruction of terrain.
     * @return the resistance of this block
     */
    public double getResistance() {
        switch (type) {
            case ' ': return RESISTANCE_OF_SKY;
            case 'W':
            case 'L': return RESISTANCE_OF_LIQUIDS;
            case '/':
            case '\\': // slants are depending on blocks below // TODO what about slants at the ceiling?
                if(neighbours.bottom != null) {
                    if(neighbours.bottom.isLiquid()) return RESISTANCE_OF_SKY; // do not return nearly infinite resistance for slants on liquids
                    return neighbours.bottom.getResistance() * MODIFIER_FOR_SLANTS;
                } else {
                    return RESISTANCE_OF_SKY; // return an at least somewhat useful information
                }
            case 'S': return RESISTANCE_OF_STONE;
            case 'E': return RESISTANCE_OF_SOIL;
            case 'I': return RESISTANCE_OF_ICE;
            case 'i': return RESISTANCE_OF_SNOW;
            case 's': return RESISTANCE_OF_SAND;
            default: return RESISTANCE_OF_SKY;
        }
    }

    /**
     * Checks if a given collision area intersects this terrain block.
     * @param collisionArea the collision area of a figure or whatever (coordinates in px)
     * @return true, when collisionArea intersects with this block
     */
    public boolean intersects(Rectangle2D collisionArea) {
        switch (type) {
            case ' ':
                return false;
            case '/':
                for (int i = 1; i < Terrain.BLOCK_SIZE; i++) {
                    int px = x * Terrain.BLOCK_SIZE + i;
                    int py = y * Terrain.BLOCK_SIZE + Terrain.BLOCK_SIZE - i;
                    if(collisionArea.contains(px, py)) {
                        Terrain.debugLog("diagonal / intersection at " + px + "x" + py + "px");
                        return true;
                    }
                }
                return false;
            case '\\':
                for(int i = 1; i < Terrain.BLOCK_SIZE; i++) {
                    int px = x * Terrain.BLOCK_SIZE + i;
                    int py = y * Terrain.BLOCK_SIZE + 1 + i;
                    if(collisionArea.contains(px, py)) {
                        Terrain.debugLog("diagonal / intersection at " + px + "x" + py + "px");
                        return true;
                    }
                }
                return false;
            case 'W':
            case 'L':
                return collisionArea.intersects(x * Terrain.BLOCK_SIZE, y * Terrain.BLOCK_SIZE + LIQUID_SINK_DISTANCE, Terrain.BLOCK_SIZE, Terrain.BLOCK_SIZE - LIQUID_SINK_DISTANCE);
            default:
                return collisionArea.intersects(x * Terrain.BLOCK_SIZE, y * Terrain.BLOCK_SIZE, Terrain.BLOCK_SIZE, Terrain.BLOCK_SIZE);
        }
    }

    /**
     * Gets the type of the block.
     * @return the type of this terrain block
     */
    public char getType() {
        return type;
    }

    /**
     * Sets the type of this terrain block and redraws it and its neighbours.
     * @param type the new type of this block
     */
    public void setType(char type) {
        this.type = type;
        drawImage();
        neighbours.redrawImages(); // look of neighbours might change because this block changed
    }

    /**
     * Tells whether the block represents sky.
     * @return returns true if this block represents sky
     */
    public boolean isSky() {
        return type == ' ';
    }

    /**
     * Tells whether the block represents a liquid.
     * @return returns true if this block represents a liquid
     */
    public boolean isLiquid() {
        return (type == 'W' || type == 'L');
    }

    /**
     * Gets the friction of this block, usually used to influence movement speed of figures.
     * @return the friction of this block
     */
    public double getFriction() {
        switch (type) {
            case 's':
                return SAND_FRICTION;
            case 'I':
                return ICE_FRICTION;
            case 'E':
                return SOIL_FRICTION;
            case 'W':
            case 'L':
                return LIQUID_FRICTION;
            default : return 1;
        }
    }
}
