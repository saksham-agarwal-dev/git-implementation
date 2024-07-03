package gitlet;


import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.text.SimpleDateFormat;
import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  Creates and sets up persistence for a commit object.
 *  @author Saksham Agarwal
 */
public class Commit implements Serializable {
    /**
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    private String date;
    private String parentID;
    private TreeMap<String, String> files;
    private String id;
    private String parent2ID;

    public Commit(String message, String parentID) {
        this(message, new Date(), parentID);
    }

    public Commit(String message, int i, String parentID) {
        this(message, new Date(i), parentID);
    }

    public Commit(String message, Date date, String parentID) {
        this (message, dateFormatter(date), parentID);
    }

    public Commit(String message, String date, String parentID) {
        this.message = message;
        this.date = date;
        this.parentID = parentID;
        this.parent2ID = null;
        if (parentID != null) {
            files = readObject(join(Repository.COMMITS, parentID), Commit.class).files;
            trackadd();
            trackRemove();
            this.id = sha1(message, parentID, date, files.values().toString());
        } else {
            files = new TreeMap<>();
            this.id = sha1(message, "", date, "");
        }
        writeObject(join(Repository.COMMITS, id), this);
        writeContents(Repository.HEAD, id);
        writeContents(join(Repository.BRANCHES, readContentsAsString(Repository.CUR_BRANCH)), id());
    }

    public String id() {
        return id;
    }

    public void set2ndparent(String commitID) {
        parent2ID = commitID;
        writeObject(join(Repository.COMMITS, id), this);
    }

    public String get2ndparent() {
        return parent2ID;
    }

    public String getMessage() {
        return message;
    }

    public String getDate() {
        return date;
    }

    public String getParentID() {
        return parentID;
    }

    public TreeMap<String, String> getFiles() {
        return files;
    }

    private void trackadd() {
        List<String> staged = plainFilenamesIn(Repository.STAGING_ADD);
        assert staged != null;
        for (String file: staged) {
            if (files != null) {
                files.remove(file);
            }
            File fileAddress = join(Repository.STAGING_ADD, file);
            String blobSHA = Repository.blobHash(file, Repository.STAGING_ADD);
            File blobAddress = join(Repository.BLOBS, blobSHA);
            writeContents(blobAddress, readContentsAsString(fileAddress));
            fileAddress.delete();
            files.put(file, blobSHA);
        }
    }

    private void trackRemove() {
        for (String file: plainFilenamesIn(Repository.STAGING_REMOVE)) {
            files.remove(file);
            File fileAddress = join(Repository.STAGING_REMOVE, file);
            fileAddress.delete();
        }
    }

    private static String dateFormatter(Date d) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("E MMM dd HH:mm:ss yyy Z");
        return dateFormat.format(d);
    }
}
