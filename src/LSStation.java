public class LSStation {
    int op;

    int id;
    int insId = -1;
    boolean busy;
    String name;

    int result;
    int exec_timer;

    // for L/S
    int addr;

    boolean f_ok;
    int q;
    int v;

    public LSStation(String n, int t) {
        this.init(n, t);
    }

    public void init(String n, int t) {
        busy = false;
        this.name = n;
        this.id = t;
        f_ok = false;
        q = -1;
        v = -1;
    }
}
