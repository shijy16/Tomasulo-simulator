public class InstructionState {
    private String ins;
    // state defines
    public final static int ISSUE = 1001;
    public final static int READY = 1005;
    public final static int EXECUTE = 1002;
    public final static int WB = 1003;
    public final static int FINISHED = 1004;

    public int state;
    public int ready_turn;

    public int op;
    public int dst;
    public int src1;
    public int src2;

    public int exec_timer;

    public int issue;
    public int exec_comp;
    public int wb;

    int order = -1;

    public InstructionState() {
        issue = -1;
        exec_comp = -1;
        wb = -1;
    }

    public void printStatus() {
        String t = String.valueOf(order) + "\t\t" + this.ins + "\tISSUE:" + issue + "\tEXEC_COMP:" + exec_comp + "\tWB:"
                + wb;
        String ttt = "";
        if (state == ISSUE)
            ttt = "ISSUE";
        else if (state == READY)
            ttt = "READY";
        else if (state == EXECUTE)
            ttt = "EXEC";
        else if (state == WB)
            ttt = "WB";
        else if (state == FINISHED)
            ttt = "FINISHED";
        System.out.println(t + "\t" + ttt);
    }

    public int hex2int(String hex) {
        long foo = Long.parseLong(hex, 16);
        if (foo > 0x7fffffffL) {
            foo = -(0x100000000L - foo);
        }
        return (int) foo;
    }

    public void init(String ins, int no) {
        this.ins = ins;
        issue = Tomasulo.cur_T;
        state = ISSUE;
        exec_comp = -1;
        wb = -1;
        src1 = -1;
        src2 = -1;
        dst = -1;
        ready_turn = -1;
        order = no;
        String temp[] = ins.split(",");
        if (temp[0].contains("ADD")) {
            exec_timer = Tomasulo.T_ADD;
            op = Tomasulo.OP_ADD;
            String t = temp[1].substring(1);
            dst = Integer.parseInt(t);
            t = temp[2].substring(1);
            src1 = Integer.parseInt(t);
            t = temp[3].substring(1);
            src2 = Integer.parseInt(t);
        } else if (temp[0].contains("SUB")) {
            exec_timer = Tomasulo.T_SUB;
            op = Tomasulo.OP_SUB;
            String t = temp[1].substring(1);
            dst = Integer.parseInt(t);
            t = temp[2].substring(1);
            src1 = Integer.parseInt(t);
            t = temp[3].substring(1);
            src2 = Integer.parseInt(t);
        } else if (temp[0].contains("MUL")) {
            exec_timer = Tomasulo.T_MUL;
            op = Tomasulo.OP_MUL;
            String t = temp[1].substring(1);
            dst = Integer.parseInt(t);
            t = temp[2].substring(1);
            src1 = Integer.parseInt(t);
            t = temp[3].substring(1);
            src2 = Integer.parseInt(t);
        } else if (temp[0].contains("DIV")) {
            exec_timer = Tomasulo.T_DIV;
            op = Tomasulo.OP_DIV;
            String t = temp[1].substring(1);
            dst = Integer.parseInt(t);
            t = temp[2].substring(1);
            src1 = Integer.parseInt(t);
            t = temp[3].substring(1);
            src2 = Integer.parseInt(t);
        } else if (temp[0].contains("LD")) {
            exec_timer = Tomasulo.T_LD;
            op = Tomasulo.OP_LD;
            String t = temp[1].substring(1);
            dst = Integer.parseInt(t);
            t = temp[2].substring(2);
            src1 = hex2int(t);
            this.ins = "LD," + temp[1] + "," + src1;
        } else if (temp[0].contains("JUMP")) {
            exec_timer = Tomasulo.T_JUMP;
            op = Tomasulo.OP_JUMP;
            String t = temp[1].substring(2);
            dst = hex2int(t);
            t = temp[2].substring(1);
            src1 = Integer.parseInt(t);
            t = temp[3].substring(2);
            src2 = hex2int(t);
            this.ins = "JUMP," + temp[1] + "," + src1 + "," + src2;
        }
    }
}