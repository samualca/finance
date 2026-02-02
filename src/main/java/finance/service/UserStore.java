package finance.service;

import finance.domain.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UserStore {
    private final Map<String, User> users = new HashMap<>();

    public boolean exists(String login) {
        return users.containsKey(login);
    }

    public User get(String login) {
        return users.get(login);
    }

    public void put(User user) {
        users.put(user.getLogin(), user);
    }

    public Collection<User> allUsers() {
        return users.values();
    }

    public void replaceAll(Map<String, User> newUsers) {
        users.clear();
        users.putAll(newUsers);
    }

    public Map<String, User> snapshot() {
        return new HashMap<>(users);
    }
}
