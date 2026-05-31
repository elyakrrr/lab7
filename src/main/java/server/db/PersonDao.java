package server.db;

import shared.exceptions.DatabaseException;
import shared.model.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;

public class PersonDao {

    public HashSet<Person> loadAll() throws DatabaseException {
        HashSet<Person> persons = new HashSet<>();
        String sql = "SELECT * FROM persons ORDER BY id";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                persons.add(mapResultSetToPerson(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка загрузки коллекции из БД", e);
        }
        return persons;
    }

    public Integer insert(Person person) throws DatabaseException {
        String sql = "INSERT INTO persons (name, coord_x, coord_y, height, creation_date, birthday, hair_color, nationality, loc_x, loc_y, loc_name, creator) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            fillStatement(pstmt, person);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка вставки Person в БД: " + e.getMessage(), e);
        }
        return null;
    }

    public void updateById(Person person) throws DatabaseException {
        String sql = "UPDATE persons SET name=?, coord_x=?, coord_y=?, height=?, creation_date=?, " +
                "birthday=?, hair_color=?, nationality=?, loc_x=?, loc_y=?, loc_name=?, creator=? " +
                "WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            fillStatement(pstmt, person);
            pstmt.setInt(13, person.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка обновления Person в БД", e);
        }
    }

    public void deleteById(int id) throws DatabaseException {
        String sql = "DELETE FROM persons WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка удаления Person с id=" + id, e);
        }
    }

    public void clearByCreator(String creator) throws DatabaseException {
        String sql = "DELETE FROM persons WHERE creator = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, creator);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка удаления элементов по creator", e);
        }
    }

    public void removeGreater(Person person, String creator) throws DatabaseException {
        String sql = "DELETE FROM persons WHERE height > ? AND creator = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setFloat(1, person.getHeight());
            pstmt.setString(2, creator);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка удаления (remove_greater)", e);
        }
    }

    public void removeLower(Person person, String creator) throws DatabaseException {
        String sql = "DELETE FROM persons WHERE height < ? AND creator = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setFloat(1, person.getHeight());
            pstmt.setString(2, creator);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка удаления (remove_lower)", e);
        }
    }
    private void fillStatement(PreparedStatement pstmt, Person p) throws SQLException {
        pstmt.setString(1, p.getName());
        pstmt.setInt(2, p.getCoordinates().getX());
        pstmt.setInt(3, p.getCoordinates().getY());
        pstmt.setFloat(4, p.getHeight());

        LocalDate creationDate = p.getCreationDate() != null ? p.getCreationDate() : LocalDate.now();
        pstmt.setDate(5, java.sql.Date.valueOf(creationDate));

        pstmt.setDate(6, p.getBirthday() != null ? new java.sql.Date(p.getBirthday().getTime()) : null);
        pstmt.setString(7, p.getHairColor() != null ? p.getHairColor().name() : null);
        pstmt.setString(8, p.getNationality() != null ? p.getNationality().name() : null);

        if (p.getLocation() != null) {
            pstmt.setFloat(9, p.getLocation().getX());
            pstmt.setLong(10, p.getLocation().getY());
            pstmt.setString(11, p.getLocation().getName());
        } else {
            pstmt.setNull(9, Types.FLOAT);
            pstmt.setNull(10, Types.BIGINT);
            pstmt.setNull(11, Types.VARCHAR);
        }
        pstmt.setString(12, p.getCreator());
    }

    private Person mapResultSetToPerson(ResultSet rs) throws SQLException {
        Coordinates coords = new Coordinates(rs.getInt("coord_x"), rs.getInt("coord_y"));

        Location loc = null;
        float locX = rs.getFloat("loc_x");
        if (!rs.wasNull()) {
            long locY = rs.getLong("loc_y");
            String locName = rs.getString("loc_name");
            loc = new Location(locX, locY, locName);
        }

        Date bday = rs.getDate("birthday");
        Color hair = rs.getString("hair_color") != null ? Color.valueOf(rs.getString("hair_color")) : null;
        Country nat = rs.getString("nationality") != null ? Country.valueOf(rs.getString("nationality")) : null;

        Person p = new Person(rs.getString("name"), coords, rs.getFloat("height"), bday, hair, nat, loc);
        p.setId(rs.getInt("id"));
        p.setCreator(rs.getString("creator"));

        java.sql.Date dbCreationDate = rs.getDate("creation_date");
        if (dbCreationDate != null) p.setCreationDate(dbCreationDate.toLocalDate());
        return p;
    }
}