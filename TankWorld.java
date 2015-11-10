package tank;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.*;
import java.net.URL;
import java.util.ArrayList; 
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

import javax.swing.*;

import wingman.game.*;
import wingman.*;
import wingman.ui.*;
import wingman.modifiers.*;
import wingman.modifiers.motions.*;
import wingman.modifiers.weapons.*;

public class TankWorld extends GameWorld {

    private Thread thread;
    
    // GameWorld is a singleton class!
    private static final TankWorld game = new TankWorld();
    public static final GameSounds sound = new GameSounds();
    public static final GameClock clock = new GameClock();
    GameMenu menu;
    public TankLevel level;
    
	public static HashMap<String,Image> sprites = GameWorld.sprites;
       
    private BufferedImage bimg, player1view, player2view;
    int score = 0, life = 4;
    Random generator = new Random();
    int sizeX, sizeY;
    Point mapSize;
    
    /*Some ArrayLists to keep track of game things*/
    private ArrayList<Bullet> bullets;
    private ArrayList<PlayerShip> players;
    private ArrayList<InterfaceObject> ui;
    private ArrayList<Ship> powerups;
    
    public static HashMap<String, MotionController> motions = new HashMap<String, MotionController>();

    // is player still playing, did they win, and should we exit
    boolean gameOver, gameWon, gameFinished;
    ImageObserver observer;
        
    // constructors makes sure the game is focusable, then
    // initializes a bunch of ArrayLists
    private TankWorld(){
        this.setFocusable(true);
        background = new ArrayList<BackgroundObject>();  
        players = new ArrayList<PlayerShip>();
        ui = new ArrayList<InterfaceObject>();
        bullets = new ArrayList<Bullet>();
        powerups = new ArrayList<Ship>();
        }
    
    /* This returns a reference to the currently running game*/
    public static TankWorld getInstance(){
    	return game;
    }

    /*Game Initialization*/
    public void init() {
        setBackground(Color.white);
        loadSprites();
         
        gameOver = false;
        observer = this;
        
        level = new TankLevel("Resources/level.txt");
        level.addObserver(this);
        mapSize = new Point(level.w*32,level.h*32);
        GameWorld.setSpeed(new Point(0,0));

        addBackground(new Background(mapSize.x,mapSize.y,GameWorld.getSpeed(), sprites.get("background")));
    
	level.load();
        clock.addObserver(level);
    }
    
    /*Functions for loading image resources*/
    protected void loadSprites(){
	    sprites.put("background", getSprite("Resources/Background.png"));
	    
	    sprites.put("wall", getSprite("Resources/Blue_wall1.png"));
	    sprites.put("wall2", getSprite("Resources/Blue_wall2.png"));
	    
	    sprites.put("bullet", getSprite("Resources/bullet.png"));
	    sprites.put("powerup", getSprite("Resources/powerup.png"));
	    
	    sprites.put("explosion1_1", getSprite("Resources/explosion1_1.png"));
		sprites.put("explosion1_2", getSprite("Resources/explosion1_2.png"));
		sprites.put("explosion1_3", getSprite("Resources/explosion1_3.png"));
		sprites.put("explosion1_4", getSprite("Resources/explosion1_4.png"));
		sprites.put("explosion1_5", getSprite("Resources/explosion1_5.png"));
		sprites.put("explosion1_6", getSprite("Resources/explosion1_6.png"));
	    sprites.put("explosion2_1", getSprite("Resources/explosion2_1.png"));
		sprites.put("explosion2_2", getSprite("Resources/explosion2_2.png"));
		sprites.put("explosion2_3", getSprite("Resources/explosion2_3.png"));
		sprites.put("explosion2_4", getSprite("Resources/explosion2_4.png"));
		sprites.put("explosion2_5", getSprite("Resources/explosion2_5.png"));
		sprites.put("explosion2_6", getSprite("Resources/explosion2_6.png"));
		sprites.put("explosion2_7", getSprite("Resources/explosion2_7.png"));
		
		sprites.put("player1", getSprite("Resources/Tank_blue_basic_strip60.png"));
		sprites.put("player2", getSprite("Resources/Tank_blue_basic_strip60.png"));
    }
    
    public Image getSprite(String name) {
        URL url = TankWorld.class.getResource(name);
        Image img = java.awt.Toolkit.getDefaultToolkit().getImage(url);
        try {
            MediaTracker tracker = new MediaTracker(this);
            tracker.addImage(img, 0);
            tracker.waitForID(0);
        } catch (Exception e) {
        }
        return img;
    }
    
    
    /********************************
     * 	These functions GET things	*
     * 		from the game world		*
     ********************************/
    
    public int getFrameNumber(){
    	return clock.getFrame();
    }
    
    public int getTime(){
    	return clock.getTime();
    }
    
    public void removeClockObserver(Observer theObject){
    	clock.deleteObserver(theObject);
    }
    
    public ListIterator<BackgroundObject> getBackgroundObjects(){
    	return background.listIterator();
    }
    
    public ListIterator<PlayerShip> getPlayers(){
    	return players.listIterator();
    }
    
    public ListIterator<Bullet> getBullets(){
    	return bullets.listIterator();
    }
    
    public int countPlayers(){
    	return players.size();
    }
    
    public void setDimensions(int w, int h){
    	this.sizeX = w;
    	this.sizeY = h;
    }
    
    /********************************
     * 	These functions ADD things	*
     * 		to the game world		*
     ********************************/
    
    public void addBullet(Bullet...newObjects){
    	for(Bullet bullet : newObjects){
            bullets.add(bullet);
    	}
    }
    
    public void addPlayer(PlayerShip...newObjects){
    	for(PlayerShip player : newObjects){
            ui.add(new InfoBar(player, Integer.toString(players.size())));
    		players.add(player);
    	}    	
    }
    
    // add background items (islands)
    public void addBackground(BackgroundObject...newObjects){
    	for(BackgroundObject object : newObjects){
            background.add(object);
    	}
    }
    
    // add power ups to the game world
    public void addPowerUp(Ship powerup){
        powerups.add(powerup);
    }
    
    public void addRandomPowerUp(){
    	// rapid fire weapon or pulse weapon
    	if(generator.nextInt(10)%2==0) {
            powerups.add(new PowerUp(generator.nextInt(sizeX), 1, new SimpleWeapon(5)));
        } else {
            powerups.add(new PowerUp(generator.nextInt(sizeX), 1, new PulseWeapon()));
            }
    }
    
    
    public void addClockObserver(Observer theObject){
    	clock.addObserver(theObject);
    }
    
    // this is the main function where game stuff happens!
    // each frame is also drawn here
    public void drawFrame(int w, int h, Graphics2D g2) {
        ListIterator<?> iterator = getBackgroundObjects();
        // iterate through all blocks
        while (iterator.hasNext()) {
            BackgroundObject obj = (BackgroundObject) iterator.next();
            obj.update(w, h);
            obj.draw(g2, this);

            if (obj instanceof BigExplosion || obj instanceof SmallExplosion) {
                if (!obj.show) iterator.remove();
                continue;
            }
            
            // check player-block collisions
            ListIterator<PlayerShip> players = getPlayers();
            while (players.hasNext() && obj.show) {
                Tank player = (Tank) players.next();
                // if the tank collides with an object
                if(obj.collision(player)){
                        // player1 location of the tank
            		Rectangle playerLocation = player.getLocation();                        
                        // player1 location of the object
                        Rectangle location = obj.getLocation();                        
                        // move tank based on pixels given 
                        if(playerLocation.x < location.x){
            			player.move(-3,0);
                        }                         
                        if(playerLocation.x > location.x){
            			player.move(3,0);
                        }                        
                        if(playerLocation.y < location.y){
            			player.move(0,-3);
                        }                         
                        if(playerLocation.y > location.y){
            			player.move(0,3);
                        } 
            	}// end if (obj.collision(player)) 
            }// end while (players.hasNext()) 
        }// end while (iterator.hasNext())

        PlayerShip player1 = players.get(0);
        PlayerShip player2 = players.get(1);
        Rectangle player1Location = player1.getLocation();
        Rectangle player2Location = player2.getLocation();
        
        player1.update(w, h);
        // if player1 collides with the other player
        if (player1.collision(player2)) {            
            if (player1Location.x < player2Location.x) {
                player1.move(-3, 0);
            }        
            if (player1Location.x > player2Location.x) {
                player1.move(3, 0);
            }             
            if (player1Location.y < player2Location.y) {
                player1.move(0, -3);
            }             
            if (player1Location.y > player2Location.y) {
                player1.move(0, 3);
            } 
        } 
        
        player2.update(w, h);
        // if player2 collides with the other player
        if (player2.collision(player1)) {            
            if (player2Location.x < player1Location.x) {
                player2.move(-3, 0);
            }             
            if (player2Location.x > player1Location.x) {
                player2.move(3, 0);
            }            
            if (player2Location.y < player1Location.y) {
                player2.move(0, -3);
            }             
            if (player2Location.y > player1Location.y) {
                player2.move(0, 3);
            } 
        }
        
        if (!gameFinished) {
            ListIterator<Bullet> bullets = this.getBullets();
            while (bullets.hasNext()) {
                Bullet ammo = bullets.next();
                // set up so bullets don't go passed walls
                if (ammo.getX() > w || ammo.getY() > h) {
                    bullets.remove();
                } // end if
                else {
                    iterator = this.getBackgroundObjects();
                    while (iterator.hasNext()) {
                        GameObject other = (GameObject) iterator.next();
                        // if the bullet collides with another object 
                        // remove the bullet & add small explosion
                        if (other.show && other.collision(ammo)) {
                            bullets.remove();
                            addBackground(new SmallExplosion(ammo.getLocationPoint()));
                            break;
                        } // end if
                    } // end while
                } // end else
                ammo.draw(g2, this);
            } // end while
            iterator = getPlayers();
            while (iterator.hasNext()) {
                PlayerShip player = (PlayerShip) iterator.next();

                // check if player is dead
                if (player.isDead()) {
                    gameOver = true;
                    continue;
                } // end if                
                bullets = this.getBullets();
                while (bullets.hasNext()) {
                    Bullet bullet = bullets.next();
                    // decrement player score & increment shooter score
                    // when bullet hits target remove & add small explosion 
                    if (bullet.collision(player) && player.respawnCounter <= 0 && bullet.getOwner() != player) {
                        player.damage(bullet.getStrength());
                        bullet.getOwner().incrementScore(bullet.getStrength());
                        addBackground(new SmallExplosion(bullet.getLocationPoint()));
                        bullets.remove();
                    } // end if
                } // end while
                
                // powerups 
                ListIterator<Ship> powerups = this.powerups.listIterator();
                while (powerups.hasNext()) {
                    Ship powerup = powerups.next();
                    powerup.draw(g2, this);
                    if (powerup.collision(player)) {
                        AbstractWeapon weapon = powerup.getWeapon();
                        player.setWeapon(weapon);
                        powerup.die();
                    } // end if (powerup.collision(player))
                } // end while (powerups.hasNext())
            } // end while (iterator.hasNext())

        player1.draw(g2, this);
        player2.draw(g2, this);
        } 
        // end game stuff
        else {
    		g2.setColor(Color.WHITE);
    		g2.setFont(new Font("Calibri", Font.PLAIN, 24));
        	if(!gameWon){
        		g2.drawImage(sprites.get("gameover"), w/3-50, h/2, null);
        	}
        	else{
        		g2.drawImage(sprites.get("youwon"), sizeX/3, 100, null);
        	}
    		g2.drawString("Score", sizeX/3, 400);
    		int i = 1;
        	for(PlayerShip player : players){
        		g2.drawString(player.getName() + ": " + Integer.toString(player.getScore()), sizeX/3, 375+50*i);
        		i++;
            } // end for
        } // end else
    } // end drawFrame

    public Graphics2D createGraphics2D(int w, int h) {
        Graphics2D g2 = null;
        if (bimg == null || bimg.getWidth() != w || bimg.getHeight() != h) {
            bimg = (BufferedImage) createImage(w, h);
        }
        g2 = bimg.createGraphics();
        g2.setBackground(getBackground());
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2.clearRect(0, 0, w, h);
        return g2;
    }

    /* paint each frame */
    public void paint(Graphics g) {
        if(players.size()!=0)
        	clock.tick();
    	Dimension windowSize = getSize();
        Graphics2D g2 = createGraphics2D(mapSize.x, mapSize.y);
        drawFrame(mapSize.x, mapSize.y, g2);
        g2.dispose();
        
        
        int p1x = this.players.get(0).getX() - windowSize.width/4 > 0 ? this.players.get(0).getX() - windowSize.width/4 : 0;
        int p1y = this.players.get(0).getY() - windowSize.height/2 > 0 ? this.players.get(0).getY() - windowSize.height/2 : 0;
        
        if(p1x > mapSize.x-windowSize.width/2){
        	p1x = mapSize.x-windowSize.width/2;
        }
        if(p1y > mapSize.y-windowSize.height){
        	p1y = mapSize.y-windowSize.height;
        }
        
        int p2x = this.players.get(1).getX() - windowSize.width/4 > 0 ? this.players.get(1).getX() - windowSize.width/4 : 0;
        int p2y = this.players.get(1).getY() - windowSize.height/2 > 0 ? this.players.get(1).getY() - windowSize.height/2 : 0;
        
        if(p2x > mapSize.x-windowSize.width/2){
        	p2x = mapSize.x-windowSize.width/2;
        }
        if(p2y > mapSize.y-windowSize.height){
        	p2y = mapSize.y-windowSize.height;
        }
        
        player1view = bimg.getSubimage(p1x, p1y, windowSize.width/2, windowSize.height);
        player2view = bimg.getSubimage(p2x, p2y, windowSize.width/2, windowSize.height);
        g.drawImage(player1view, 0, 0, this);
        g.drawImage(player2view, windowSize.width/2, 0, this);
        g.drawRect(windowSize.width/2-1, 0, 1, windowSize.height);
        g.drawRect(windowSize.width/2-101, 399, 151, 151);
        g.drawImage(bimg, windowSize.width/2-101, 400, 200, 150, observer);
      
        
        // interface stuff
        ListIterator<InterfaceObject> objects = ui.listIterator();
        int offset = 0;
        while(objects.hasNext()){
        	InterfaceObject object = objects.next();
        	object.draw(g, offset, windowSize.height);
        	offset += 500;
        }
    }

    /* start the game thread*/
    public void start() {
        thread = new Thread(this);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /* run the game */
    public void run() {
    	
        Thread me = Thread.currentThread();
        while (thread == me) {
        	this.requestFocusInWindow();
            repaint();
          
          try {
                thread.sleep(23); // pause a little to slow things down
            } catch (InterruptedException e) {
                break;
            }
            
        }
    }
    
    /* End the game, and signal either a win or loss */
    public void endGame(boolean win){
    	this.gameOver = true;
    	this.gameWon = win;
    }
    
    public boolean isGameOver(){
    	return gameOver;
    }
    
    // signal that we can stop entering the game loop
    public void finishGame(){
    	gameFinished = true;
    }
    

    /*I use the 'read' function to have observables act on their observers.
     */
	@Override
	public void update(Observable o, Object arg) {
		AbstractGameModifier modifier = (AbstractGameModifier) o;
		modifier.read(this);
	}
	
	public static void main(String argv[]) {
	    final TankWorld game = TankWorld.getInstance();
	    JFrame f = new JFrame("Non-Scrolling Non-Shooter");
	    f.addWindowListener(new WindowAdapter() {
		    public void windowGainedFocus(WindowEvent e) {
		        game.requestFocusInWindow();
		    }
	    });
	    f.getContentPane().add("Center", game);
	    f.pack();
	    f.setSize(new Dimension(900, 600));
	    game.setDimensions(800, 600);
	    game.init();
	    f.setVisible(true);
	    f.setResizable(false);
	    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            GameWorld.sound.play("Resources/castlemusic.mp3");
	    game.start();
	}
}