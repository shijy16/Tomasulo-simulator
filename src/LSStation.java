public class LSStation {
    int id;
    boolean busy;
    String name;

    int result;

    // for L/S
    int addr;

    public LSStation(String n, int t) {
        this.init(n, t);
    }

    public void init(String n, int t) {
        busy = false;
        this.name = n;
        this.id = t;
    }
}
