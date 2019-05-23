public class InstructionState {
    public int op;
    public int dst;
    public int src1;
    public int src2;

    public int exec_timer;

    public int issue;
    public int exec;
    public int wb;

    public InstructionState() {
        issue = -1;
        exec = -1;
        wb = -1;
    }

    public void init(String ins) {
        issue = -1;
        exec = -1;
        wb = -1;
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
            t = temp[2].substring(1);
            src1 = Integer.parseInt(t);
        } else if (temp[0].contains("JUMP")) {
            op = Tomasulo.OP_JUMP;
            String t = temp[1].substring(1);
            dst = Integer.parseInt(t);
            t = temp[2].substring(1);
            src1 = Integer.parseInt(t);
            t = temp[3].substring(1);
            src2 = Integer.parseInt(t);
        }
    }
}