package gearth.extensions.parsers;

public class HPoint {
    private int x;
    private int y;
    private double z;

    public HPoint(int x, int y) {
        this(x, y, 0);
    }

    public HPoint(int x, int y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HPoint)) return false;

        HPoint p = (HPoint) o;
        return this.x == p.getX() && this.y == p.getY() && this.z == p.getZ();
    }
    
    @Override
    public String toString() {
        return "(" + this.getX() + "," + this.getY() + "," + this.getZ() + ")";
    }
}
