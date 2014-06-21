package de.hhu.propra.team61.objects;

import de.hhu.propra.team61.MapWindow;
import de.hhu.propra.team61.io.json.JSONObject;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;

/**
 * Created by kevin on 17.06.14.
 */
public class Crate extends ImageView {
    private static final int NORMED_OBJECT_SIZE = 16;
    private static final int NORMED_BLOCK_SIZE  = 8;
    private static final String IMGSRC = "file:resources/weapons/crate.png";

    private static final int MASS               = 1000;
    private static final int EXPLOSIONPOWER     = 30;
    private static final int DAMAGERESISTANCE   = 10;


        public  static final int JUMP_SPEED = 18 + (int)(MapWindow.GRAVITY.getY() * MASS);
        private static final int FALL_DAMAGE_THRESHOLD = JUMP_SPEED;
        private static final Point2D GRAVEYARD = new Point2D(-1000,-1000);


        /** position of the figure, has to be synced with translateX/Y (introduced to prevent timing issues on JavaFX thread) */
        private Point2D velocity = new Point2D(0,10);
        /** the maximal speed (absolute value) in y direction since last call of resetVelocity, used to limit jump speed */
        private double maxYSpeed = 0;

        private Rectangle2D hitRegion;

    private String content;

    public Crate(int xSize, String contentw){
        setImage(new Image(IMGSRC,NORMED_OBJECT_SIZE,NORMED_OBJECT_SIZE,true,true));
        setTranslateX(Math.random()*xSize*NORMED_BLOCK_SIZE);
        setTranslateY(0);

        // TODO set Content; FILE dingens funktioniert später nicht mehr; musst leider ne liste machen u pflegen
        File f = new File("src/de/hhu/propra/team61/objects/itemtypes/");
        String[] options = f.list();
        System.out.println(options);
        content = options[(int)Math.random()*options.length]; //ToDo remove fileending
        content = content.substring(0,content.indexOf("."));

        content = "PoisonedArrow";

        hitRegion = new Rectangle2D(getTranslateX(), getTranslateY(),NORMED_OBJECT_SIZE,NORMED_OBJECT_SIZE);


        //ToDo add yourself to terrain

        }

    public String getContent() {
        return content;
    }

    public void setPosition(Point2D pos) {
        Platform.runLater(() -> {
            setTranslateX(pos.getX());
            setTranslateY(pos.getY());
        });
        hitRegion = new Rectangle2D(pos.getX(), pos.getY(), hitRegion.getWidth(), hitRegion.getHeight());
    }
    /**
     * @return position in px
     */
    public Point2D getPosition() {
        return new Point2D(getTranslateX(), getTranslateY());
    }
    public Rectangle2D getHitRegion() {
        return hitRegion;
    }

    
    public Point2D getVelocity() {
        return velocity;
    }
    public void resetVelocity() {
        velocity =  new Point2D(0,10);
    }
    public void nullifyVelocity() {
        velocity =  new Point2D(0,0);
    }

    public int getMass() {
        return MASS;
    }

}