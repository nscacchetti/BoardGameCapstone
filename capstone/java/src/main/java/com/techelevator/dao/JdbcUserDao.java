package com.techelevator.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.techelevator.model.User;

@Component
public class JdbcUserDao implements UserDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int findIdByUsername(String username) {
        if (username == null) throw new IllegalArgumentException("Username cannot be null");

        int userId;
        try {
            userId = jdbcTemplate.queryForObject("select user_id from users where username = ?", int.class, username);
        } catch (EmptyResultDataAccessException e) {
            throw new UsernameNotFoundException("User " + username + " was not found.");
        }

        return userId;
    }

	@Override
	public User getUserById(int userId) {
		String sql = "SELECT * FROM users WHERE user_id = ?";
		SqlRowSet results = jdbcTemplate.queryForRowSet(sql, userId);
		if (results.next()) {
			return mapRowToUser(results);
		} else {
			return null;
		}
	}

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "select * from users";

        SqlRowSet results = jdbcTemplate.queryForRowSet(sql);
        while (results.next()) {
            User user = mapRowToUser(results);
            users.add(user);
        }

        return users;
    }

    @Override
    public User findByUsername(String username) {
        if (username == null) throw new IllegalArgumentException("Username cannot be null");

        for (User user : this.findAll()) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                return user;
            }
        }
        throw new UsernameNotFoundException("User " + username + " was not found.");
    }
//    @TODO fix fuzzy search on frontend to return nothing if input is blank
    @Override
    public List<User> findByUsernameContains(String username) {
//        if (username == null) throw new IllegalArgumentException("Username cannot be null");
        List<User> matchingUsers = new ArrayList<>();
        for (User user : this.findAll()) {
            if (user.getUsername().toLowerCase().contains(username.toLowerCase())) {
                matchingUsers.add(user);
            }
        }
//        if (matchingUsers.size()==0){
//        throw new UsernameNotFoundException("User " + username + " was not found.");}
        return matchingUsers;
    }

    @Override
    public boolean create(String username, String password, String role) {
        String insertUserSql = "insert into users (username,password_hash,role) values (?,?,?)";
        String password_hash = new BCryptPasswordEncoder().encode(password);
        String ssRole = role.toUpperCase().startsWith("ROLE_") ? role.toUpperCase() : "ROLE_" + role.toUpperCase();

        return jdbcTemplate.update(insertUserSql, username, password_hash, ssRole) == 1;
    }

    public List<User> usersByBoardGame(String boardGameId){
        List<User> users = new ArrayList<>();

        String sql = "SELECT users.user_id, users.username, users.password_hash, users.role, users.image FROM users JOIN boardgames ON boardgames.user_id = users.user_id WHERE board_game_id = ?";

        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, boardGameId);

        while(results.next()){
            User user = mapRowToUser(results);
            users.add(user);
        }
        return users;
    }

    public String findUsernameInPosts (int userId){
        String username = null;
        String sql = "SELECT username FROM users WHERE user_id IN (SELECT user_id FROM posts WHERE user_id = ?)";
        SqlRowSet result = jdbcTemplate.queryForRowSet(sql, userId);
        if(result.next()){
            username = result.getString("username");
        }
        return username;
    }

    private User mapRowToUser(SqlRowSet rs) {
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password_hash"));
        user.setAuthorities(Objects.requireNonNull(rs.getString("role")));
        user.setActivated(true);
        user.setImage(rs.getString("image"));
        return user;
    }


}
