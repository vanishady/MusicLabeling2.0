package it.polimi.mae.musiclabeling.utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Standalone utility to generate BCrypt hashes for SQL INSERTs.
 *
 * Usage:
 *   Windows: mvnw.cmd exec:java -Dexec.mainClass="it.polimi.mae.musiclabeling.utils.HashPassword" -Dexec.args="mypassword"
 *   Linux:   ./mvnw exec:java -Dexec.mainClass="it.polimi.mae.musiclabeling.utils.HashPassword" -Dexec.args="mypassword"
 *
 * Copy the printed hash into an SQL INSERT:
 *   INSERT INTO users (username, password, is_admin) VALUES ('mario', '<hash>', FALSE);
 */
public class HashPassword {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: HashPassword <password>");
            System.exit(1);
        }
        String hash = BCrypt.hashpw(args[0], BCrypt.gensalt(10));
        System.out.println(hash);
    }
}
