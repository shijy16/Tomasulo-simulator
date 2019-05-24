public class CalculateStation {
    int id;
    boolean busy;
    String name;

    int result;

    // for calculation
    int op;
    float qj;
    float qk;
    boolean vj_ok;
    boolean vk_ok;
    float vj;
    float vk;

    public CalculateStation(String n, int t) {
        this.init(n, t);
    }

    public void init(String n, int t) {
        busy = false;
        this.name = n;
        this.id = t;
    }
}
