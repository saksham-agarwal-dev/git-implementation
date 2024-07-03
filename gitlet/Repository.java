package gitlet;

import java.io.File;
import java.util.TreeMap;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Map;
import static gitlet.Utils.*;


/** Represents a gitlet repository.
 *  does at a high level.
 *
 *  @author Saksham Agarwal
 */
public class Repository {
    /**
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File STAGING_ADD = join(GITLET_DIR, ".staging");
    public static final File STAGING_REMOVE = join(GITLET_DIR, ".remove");
    public static final File BLOBS = join(GITLET_DIR, ".blobs");
    public static final File COMMITS = join(GITLET_DIR, ".commits");
    public static final File HEAD = join(GITLET_DIR, "head");
    public static final File BRANCHES = join(GITLET_DIR, ".branches");
    public static final File CUR_BRANCH = join(GITLET_DIR, "curBranch");

    public static void initMethod() {
        if (GITLET_DIR.exists()) {
            System.out.print("A Gitlet version-control system already ");
            System.out.println("exists in the current directory.");
            System.exit(0);
        }
        GITLET_DIR.mkdir();
        STAGING_ADD.mkdir();
        STAGING_REMOVE.mkdir();
        BLOBS.mkdir();
        COMMITS.mkdir();
        BRANCHES.mkdir();
        writeContents(CUR_BRANCH, "master");
        Commit init = new Commit("initial commit", 0, null);
        writeContents(join(BRANCHES, "master"), init.id());
    }

    public static void add(String file) {
        File stageFile = join(STAGING_ADD, file);
        if (!join(CWD, file).exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        if (join(STAGING_REMOVE, file).isFile()) {
            join(STAGING_REMOVE, file).delete();
        }
        String headLoc = readContentsAsString(HEAD);
        Commit head = readObject(join(COMMITS, headLoc), Commit.class);
        if (head.getFiles() != null && blobHash(file, CWD).equals(head.getFiles().get(file))) {
            if (stageFile.exists()) {
                join(STAGING_ADD, file).delete();
            }
            System.exit(0);
        }
        String contents = readContentsAsString(join(CWD, file));
        writeContents(stageFile, contents);
        if (join(STAGING_REMOVE, file).isFile()) {
            join(STAGING_REMOVE, file).delete();
        }
    }

    public static void commit(String message) {
        if (plainFilenamesIn(STAGING_ADD).isEmpty() && plainFilenamesIn(STAGING_REMOVE).isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        new Commit(message, readContentsAsString(HEAD));
    }

    public static void rm(String file) {
        List<String> stagedAdd = plainFilenamesIn(STAGING_ADD);
        boolean a = false;
        boolean b = false;
        for (String staged: stagedAdd) {
            if (staged.equals(file)) {
                a = true;
                join(STAGING_ADD, staged).delete();
                break;
            }
        }
        Commit head = readObject(join(COMMITS, readContentsAsString(HEAD)), Commit.class);
        TreeMap<String, String> filesTracked = head.getFiles();
        if (filesTracked.containsKey(file)) {
            b = true;
            writeContents(join(STAGING_REMOVE, file), filesTracked.get(file));
            if (join(CWD, file).exists()) {
                join(CWD, file).delete();
            }
        }
        if (!(a || b)) {
            System.out.println("No reason to remove the file.");
        }
    }

    public static void log() {
        Commit head = readObject(join(COMMITS, readContentsAsString(HEAD)), Commit.class);
        while (!head.getMessage().equals("initial commit")) {
            printLog(head);
            head = readObject(join(COMMITS, head.getParentID()), Commit.class);
        }
        printLog(head);
    }

    public static void globalLog() {
        List<String> commits = plainFilenamesIn(COMMITS);
        Commit cur;
        for (String file: commits) {
            cur = readObject(join(COMMITS, file), Commit.class);
            printLog(cur);
        }
    }

    public static void find(String commitMessage) {
        List<String> commits = plainFilenamesIn(COMMITS);
        Commit cur;
        boolean found = false;
        for (String file: commits) {
            cur = readObject(join(COMMITS, file), Commit.class);
            if (cur.getMessage().equals(commitMessage)) {
                found = true;
                System.out.println(cur.id());
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void status() {
        List<String> staged = plainFilenamesIn(STAGING_ADD);
        List<String> branches = plainFilenamesIn(BRANCHES);
        String curBranch = readContentsAsString(CUR_BRANCH);
        List<String> cwdFiles = plainFilenamesIn(CWD);
        File headLoc = join(COMMITS, readContentsAsString(HEAD));
        Commit head = readObject(headLoc, Commit.class);
        TreeMap<String, String> trackedFiles = head.getFiles();

        System.out.println(statusPrint("Branches"));
        for (String branch:branches) {
            if (branch.equals(curBranch)) {
                System.out.print("*");
            }
            System.out.println(branch);
        }
        System.out.println();

        System.out.println(statusPrint("Staged Files"));
        for (String file: staged) {
            System.out.println(file);
        }
        System.out.println();

        List<String> removed = plainFilenamesIn(STAGING_REMOVE);
        System.out.println(statusPrint("Removed Files"));
        for (String file: removed) {
            System.out.println(file);
        }
        System.out.println();

        System.out.println(statusPrint("Modifications Not Staged For Commit"));
        System.out.println();

        System.out.println(statusPrint("Untracked Files"));
        for (String file:cwdFiles) {
            if ((!staged.contains(file) && !trackedFiles.containsKey(file))) {
                System.out.println(file);
            }
        }
        System.out.println();
    }

    public static void checkout(String branchName) {
        String cur = readContentsAsString(CUR_BRANCH);
        if (branchName.equals(cur)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        if (!plainFilenamesIn(BRANCHES).contains(branchName)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        String commitID = readContentsAsString(join(BRANCHES, branchName));
        Commit commit = readObject(join(COMMITS, commitID), Commit.class);
        TreeMap<String, String> commitFiles = commit.getFiles();
        File headLoc = join(COMMITS, readContentsAsString(HEAD));
        Commit head = readObject(headLoc, Commit.class);
        TreeMap<String, String> headFiles = head.getFiles();
        List<String> filesCWD = plainFilenamesIn(CWD);
        for (String file: filesCWD) {
            if (!headFiles.containsKey(file) && commitFiles.containsKey(file)) {
                System.out.print("There is an untracked file in the way; delete it, ");
                System.out.println("or add and commit it first.");
                System.exit(0);
            }
        }
        for (String entry : headFiles.keySet()) {
            join(CWD, entry).delete();
        }
        for (Map.Entry<String, String> entry: commitFiles.entrySet()) {
            File fileDest = join(CWD, entry.getKey());
            File fileLoc = join(BLOBS, entry.getValue());
            writeContents(fileDest, readContentsAsString(fileLoc));
        }
        checkoutBranchHelper(branchName, CUR_BRANCH);
        writeContents(HEAD, commitID);
    }

    public static void checkOut(String fileName) {
        checkout(readContentsAsString(HEAD), fileName);
    }

    public static void checkout(String commitID, String fileName) {
        boolean a = false;
        for (String s : plainFilenamesIn(COMMITS)) {
            if (s.startsWith(commitID)) {
                commitID = s;
                a = true;
                break;
            }
        }
        if (!a) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        File commitLoc = join(COMMITS, commitID);
        Commit commit = readObject(commitLoc, Commit.class);
        String fileBlob = commit.getFiles().get(fileName);
        if (fileBlob == null) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        String fileContents = readContentsAsString(join(BLOBS, fileBlob));
        writeContents(join(CWD, fileName), fileContents);
    }

    public static void branch(String branchName) {
        if (plainFilenamesIn(BRANCHES).contains(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        String pointer = readContentsAsString(HEAD);
        writeContents(join(BRANCHES, branchName), pointer);
    }

    public static void rmBranch(String branchName) {
        if (!plainFilenamesIn(BRANCHES).contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (readContentsAsString(CUR_BRANCH).equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        join(BRANCHES, branchName).delete();
    }

    public static void reset(String commitID) {
        if (!plainFilenamesIn(COMMITS).contains(commitID)) {
            System.out.println("No commit with that ID exists.");
            System.exit(0);
        }
        List<String> cwdFiles = plainFilenamesIn(CWD);
        Commit head = readObject(join(COMMITS, readContentsAsString(HEAD)), Commit.class);
        Commit des = readObject(join(COMMITS, commitID), Commit.class);
        TreeMap<String, String> headFiles = head.getFiles();
        TreeMap<String, String> desFiles = des.getFiles();
        for (String file : cwdFiles) {
            if (!headFiles.containsKey(file) && desFiles.containsKey(file)) {
                System.out.print("There is an untracked file in the way; ");
                System.out.println("delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        for (Map.Entry<String, String> entry : desFiles.entrySet()) {
            String contents = readContentsAsString(join(BLOBS, entry.getValue()));
            writeContents(join(CWD, entry.getKey()), contents);
        }
        for (String entry : headFiles.keySet()) {
            join(CWD, entry).delete();
        }
        checkoutBranchHelper(commitID, HEAD);
        writeContents(join(BRANCHES, readContentsAsString(CUR_BRANCH)), commitID);
    }

    public static void merge(String branchName) {
        mergeFailCases(branchName);
        String commitIdBranch = readContentsAsString(join(BRANCHES, branchName));
        String commitIDHead = readContentsAsString(HEAD);
        String commitIDSplit = splitPoint(commitIdBranch, commitIDHead);
        Commit given = readObject(join(COMMITS, commitIdBranch), Commit.class);
        Commit head = readObject(join(COMMITS, commitIDHead), Commit.class);
        Commit split = readObject(join(COMMITS, commitIDSplit), Commit.class);
        TreeMap<String, String> givenFiles = given.getFiles();
        TreeMap<String, String> headFiles = head.getFiles();
        TreeMap<String, String> splitFiles = split.getFiles();
        boolean conflict = false;
        for (Map.Entry<String, String> entry : splitFiles.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            if (givenFiles.containsKey(key)
                    && !givenFiles.get(key).equals(val)
                    && headFiles.containsKey(key)
                    && headFiles.get(key).equals(val)) {
                String contents = readContentsAsString(join(BLOBS, givenFiles.get(key)));
                writeContents(join(CWD, key), contents);
                writeContents(join(STAGING_ADD, key), contents);
                continue;
            }
            if (!givenFiles.containsKey(key)
                    && headFiles.containsKey(key)
                    && headFiles.get(key).equals(val)) {
                writeContents(join(STAGING_REMOVE, key), readContentsAsString(join(CWD, key)));
                if (join(STAGING_ADD, key).exists()) {
                    join(STAGING_ADD, key).delete();
                }
                if (join(CWD, key).exists()) {
                    join(CWD, key).delete();
                }
                continue;
            }
            if (givenFiles.containsKey(key)
                    && headFiles.containsKey(key)
                    && !givenFiles.get(key).equals(val)
                    && !headFiles.get(key).equals(val)
                    && !headFiles.get(key).equals(givenFiles.get(key))) {
                conflict = true;
                String newCon = mergeConflict(headFiles.get(key), givenFiles.get(key));
                writeContents(join(CWD, key), newCon);
                writeContents(join(STAGING_ADD, key), newCon);
                continue;
            }
            if (!givenFiles.containsKey(key)
                    && headFiles.containsKey(key)
                    && !headFiles.get(key).equals(val)) {
                conflict = true;
                String newCon = mergeConflict(headFiles.get(key), null);
                writeContents(join(CWD, key), newCon);
                writeContents(join(STAGING_ADD, key), newCon);
                continue;
            }
            if (!headFiles.containsKey(key)
                    && givenFiles.containsKey(key)
                    && !givenFiles.get(key).equals(val)) {
                String newCon = mergeConflict(null, givenFiles.get(key));
                conflict = true;
                writeContents(join(CWD, key), newCon);
                writeContents(join(STAGING_ADD, key), newCon);
                continue;
            }
        }
        for (Map.Entry<String, String> entry : givenFiles.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            if (!headFiles.containsKey(key) && !splitFiles.containsKey(key)) {
                String contents = readContentsAsString(join(BLOBS, val));
                writeContents(join(CWD, key), contents);
                writeContents(join(STAGING_ADD, key), contents);
            }
        }
        Commit merged = new Commit("Merged " + branchName
                + " into " + readContentsAsString(CUR_BRANCH) + ".",
                new Date(), commitIDHead);
        merged.set2ndparent(commitIdBranch);
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
        writeContents(HEAD, merged.id());
        writeContents(join(BRANCHES, readContentsAsString(CUR_BRANCH)), merged.id());
    }

    private static String mergeConflict(String a, String b) {
        String s = "<<<<<<< HEAD\n";
        String c = "";
        if (a != null) {
            c = readContentsAsString(join(BLOBS, a));
        }
        s += c;
        s += "=======\n";
        c = "";
        if (b != null) {
            c = readContentsAsString(join(BLOBS, b));
        }
        s += c;
        s += ">>>>>>>\n";
        return s;
    }

    private static void mergeFailCases(String branchName) {
        if (!plainFilenamesIn(BRANCHES).contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        TreeMap<String, String> headFiles = readObject(HEAD, Commit.class).getFiles();
        String commitID = readContentsAsString(join(BRANCHES, branchName));
        TreeMap<String, String> commitFiles = readObject(join(COMMITS, commitID),
                Commit.class).getFiles();
        if (!plainFilenamesIn(STAGING_ADD).isEmpty()
                || !plainFilenamesIn(STAGING_REMOVE).isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (branchName.equals(readContentsAsString(CUR_BRANCH))) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        String commitIdBranch = readContentsAsString(join(BRANCHES, branchName));
        String commitIDHead = readContentsAsString(HEAD);
        String commitIDSplit = splitPoint(commitIdBranch, commitIDHead);
        if (commitIDSplit.equals(commitIdBranch)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (commitIDSplit.equals(commitIDHead)) {
            checkout(branchName);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
    }

    private static String splitPoint(String commit1, String commit2) {
        TreeMap<String, Integer> bfs1 = bfs(commit1);
        TreeMap<String, Integer> bfs2 = bfs(commit2);
        String splitPoint = null;
        int splitRef = 0;
        for (Map.Entry<String, Integer> entry : bfs1.entrySet()) {
            String key = entry.getKey();
            int val = entry.getValue();
            if (bfs2.containsKey(key)
                    && (splitPoint == null || val + bfs2.get(key) < splitRef)) {
                splitPoint = key;
                splitRef = val + bfs2.get(key);
            }
        }
        return splitPoint;
    }

    private static TreeMap<String, Integer> bfs(String commit) {
        Queue<String> fringe = new LinkedList<>();
        TreeMap<String, Integer> marked = new TreeMap<>();
        marked.put(commit, 0);
        fringe.add(commit);
        int disTo = 1;
        while (!fringe.isEmpty()) {
            String v = fringe.remove();
            Commit c = readObject(join(COMMITS, v), Commit.class);
            String firstParent = c.getParentID();
            String secondParent = c.get2ndparent();
            if (firstParent != null && !marked.containsKey(firstParent)) {
                fringe.add(firstParent);
                marked.put(firstParent, disTo);
            }
            if (secondParent != null && !marked.containsKey(secondParent)) {
                fringe.add(secondParent);
                marked.put(secondParent, disTo);
            }
            disTo++;
        }
        return marked;
    }

    private static void checkoutBranchHelper(String commitID, File head2) {
        List<String> staged = plainFilenamesIn(STAGING_ADD);
        for (String s : staged) {
            join(STAGING_ADD, s).delete();
        }
        staged = plainFilenamesIn(STAGING_REMOVE);
        for (String s : staged) {
            join(STAGING_REMOVE, s).delete();
        }
        writeContents(head2, commitID);
    }

    private static String statusPrint(String header) {
        return "=== " + header + " ===";
    }

    private static void printLog(Commit c) {
        System.out.println("===");
        System.out.println("commit " + c.id());
        if (c.get2ndparent() != null) {
            System.out.print("Merge: ");
            System.out.print(c.getParentID().substring(0, 7) + " ");
            System.out.println(c.get2ndparent().substring(0, 7));
        }
        System.out.println("Date: " + c.getDate());
        System.out.println(c.getMessage());
        System.out.println();
    }

    public static void operandCheck(int len, int correct) {
        if (len != correct) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    public static void gitletDirCheck() {
        if (!GITLET_DIR.isDirectory()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    public static String blobHash(String file, File directory) {
        return sha1(file, readContentsAsString(join(directory, file)));
    }
}
