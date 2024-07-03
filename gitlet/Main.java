package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.text.SimpleDateFormat;
import static gitlet.Utils.*;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Saksham Agarwal
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                Repository.operandCheck(args.length, 1);
                Repository.initMethod();
                break;
            case "add":
                Repository.operandCheck(args.length, 2);
                Repository.gitletDirCheck();
                Repository.add(args[1]);
                break;
            case "commit":
                Repository.gitletDirCheck();
                if (args.length < 2 || args[1].replaceAll(" ", "").equals("")) {
                    System.out.println("Please enter a commit message.");
                    System.exit(0);
                }
                Repository.operandCheck(args.length, 2);
                Repository.commit(args[1]);
                break;
            case "rm":
                Repository.operandCheck(args.length, 2);
                Repository.gitletDirCheck();
                Repository.rm(args[1]);
                break;
            case "log":
                Repository.operandCheck(args.length, 1);
                Repository.gitletDirCheck();
                Repository.log();
                break;
            case "global-log":
                Repository.operandCheck(args.length, 1);
                Repository.gitletDirCheck();
                Repository.globalLog();
                break;
            case "find":
                Repository.operandCheck(args.length, 2);
                Repository.gitletDirCheck();
                Repository.find(args[1]);
                break;
            case "status":
                Repository.operandCheck(args.length, 1);
                Repository.gitletDirCheck();
                Repository.status();
                break;
            case "checkout":
                Repository.gitletDirCheck();
                if (args.length == 3 && args[1].equals("--")) {
                    Repository.checkOut(args[2]);
                } else if (args.length == 4 && args[2].equals("--")) {
                    Repository.checkout(args[1], args[3]);
                } else if (args.length == 2) {
                    Repository.checkout(args[1]);
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                break;
            case "branch":
                Repository.operandCheck(args.length, 2);
                Repository.gitletDirCheck();
                Repository.branch(args[1]);
                break;
            case "rm-branch":
                Repository.gitletDirCheck();
                Repository.operandCheck(args.length, 2);
                Repository.rmBranch(args[1]);
                break;
            case "reset":
                Repository.gitletDirCheck();
                Repository.operandCheck(args.length, 2);
                Repository.reset(args[1]);
                break;
            case "merge":
                Repository.gitletDirCheck();
                Repository.operandCheck(args.length, 2);
                Repository.merge(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
                break;
        }
    }
}
