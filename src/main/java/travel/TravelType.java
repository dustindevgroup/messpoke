package travel;

public enum TravelType {

    WALK(1, "w", 0.5f),
    RUN(2, "r", 2.1f),
    DRIVE(3, "d", 6f);

    private int id;
    private float speed; // meters per seconds
	private String cmd;

    TravelType(int id, String cmd, float speed) {
        this.id = id;
        this.speed = speed;
        this.cmd = cmd;
    }

    public float getSpeed() {
        return speed;
    }

    public int getId() {
        return id;
    }
    
    public String getCmd() {
		return cmd;
	}
    
    public static TravelType getString( String cmd ) {
    	if ( RUN.cmd.equals(cmd) ) {
    		return RUN;
    	}
    	if ( DRIVE.cmd.equals(cmd) ) {
    		return DRIVE;
    	}
    	
    	return WALK;
    }

	public static TravelType getById(int id) {
        if ( id == TravelType.WALK.id ) {
            return TravelType.WALK;
        }
        return TravelType.WALK;
    }
}
