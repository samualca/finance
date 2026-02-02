package finance.service;

import finance.domain.User;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class FileStorage {
    private final Path file;

    public FileStorage(String filename) {
        this.file = Path.of(filename);
    }

    @SuppressWarnings("unchecked")
    public Map<String, User> loadUsersOrEmpty() {
        if (!Files.exists(file)) {
            return Map.of();
        }

        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            Object obj = in.readObject();
            return (Map<String, User>) obj;
        } catch (Exception e) {
            System.out.println("WARNING: failed to load data file. Starting with empty storage.");
            System.out.println("Reason: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return Map.of();
        }
    }

    public void saveUsers(Map<String, User> users) {
        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);

            try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
                out.writeObject(users);
            }
        } catch (Exception e) {
            System.out.println("ERROR: failed to save data.");
            System.out.println("Reason: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
