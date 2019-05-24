import java.nio.ByteBuffer;

public class InstructionState {
    public int op;
    public int dst;
    public int src1;
    public int src2;

    public int exec_timer;

    public int issue;
    public int exec;
    public int wb;

    int order = -1;

    public InstructionState() {
        issue = -1;
        exec = -1;
        wb = -1;
    }

    public void printStatus() {
        String t = String.valueOf(order) + " " + Tomasulo.INSKEY[op] + " " + issue + " " + exec + " " + wb;
        String tt = String.valueOf(dst) + " " + String.valueOf(src1) + " " + String.valueOf(src2);
        System.out.println(t + " F:" + tt);
    }

    public int hex2int(String hex) {
        long foo = Long.parseLong(hex, 16);
        if (foo > 0x7fffffffL) {
            foo = -(0x100000000L - foo);
        }
        return (int) foo;
    }

    public void init(String ins, int no) {
        issue = Tomasulo.cur_T;
        exec = -1;
        wb = -1;
        src1 = -1;
        src2 = -1;
        dst = -1;
        order = no;
        String temp[] = ins.split(",");
        if (temp[0].contains("ADD")) {
            op = Tomasulo.OP_ADD;
            String t = temp[1].substring(1);
            dst = Integer.parseInt(t);
            t = temp[2].substring(1);
            src1 = Integer.parseInt(t);
            t = temp[3].substring(1);
            src2 = Integer.parseInt(t);
        } else if (temp[0].contains("SUB")) {
            op = Tomasulo.OP_SUB;
            String t = temp[1].substring(1);
            dst = Integer.parseInt(t);
            t = temp[2].substring(1);
            src1 = Integer.parseInt(t);
            t = temp[3].substring(1);
            src2 = Integer.parseInt(t);
        } else if (temp[0].contains("MUL")) {
            op = Tomasulo.OP_MUL;
            String t = temp[1].substring(1);
            dst = Integer.parseInt(t);
            t = temp[2].substring(1);
            src1 = Integer.parseInt(t);
            t = temp[3].substring(1);
            src2 = Integer.parseInt(t);
        } else if (temp[0].contains("DIV")) {
            op = Tomasulo.OP_DIV;
            String t = temp[1].substring(1);
            dst = Integer.parseInt(t);
            t = temp[2].substring(1);
            src1 = Integer.parseInt(t);
            t = temp[3].substring(1);
            src2 = Integer.parseInt(t);
        } else if (temp[0].contains("LD")) {
            op = Tomasulo.OP_LD;
            String t = temp[1].substring(1);
            dst = Integer.parseInt(t);
            t = temp[2].substring(2);
            src1 = hex2int(t);
        } else if (temp[0].contains("JUMP")) {
            op = Tomasulo.OP_JUMP;
            String t = temp[1].substring(2);
            dst = hex2int(t);
            t = temp[2].substring(1);
            src1 = Integer.parseInt(t);
            t = temp[3].substring(2);
            src2 = hex2int(t);
        }
    }
}