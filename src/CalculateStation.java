public class CalculateStation {
    int id;
    boolean busy;
    String name;

    int result;

    // for calculation
    int op;
    String qj;
    String qk;
    boolean vj_ok;
    boolean vk_ok;
    int vj;
    int vk;

    public CalculateStation(String n, int t) {
        this.init(n, t);
    }

    public void init(String n, int t) {
        busy = false;
        this.name = n;
        this.id = t;
    }
}
