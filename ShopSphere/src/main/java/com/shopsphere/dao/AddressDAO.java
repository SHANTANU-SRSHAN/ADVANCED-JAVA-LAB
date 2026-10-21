package com.shopsphere.dao;

import com.shopsphere.model.Address;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The customer's address book.
 */
public class AddressDAO {

    /** Every address of one customer, default first. */
    public List<Address> findByUser(int userId) throws SQLException {

        String sql = """
                SELECT address_id, user_id, label, full_name, phone,
                       address_line1, address_line2, city, state, pincode,
                       is_default
                FROM user_addresses
                WHERE user_id = ?
                ORDER BY is_default DESC, address_id DESC
                """;

        List<Address> addresses = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    addresses.add(mapAddress(rs));
                }
            }
        }

        return addresses;
    }

    /** One address, but only if it belongs to that customer. */
    public Address findById(int addressId, int userId) throws SQLException {

        String sql = """
                SELECT address_id, user_id, label, full_name, phone,
                       address_line1, address_line2, city, state, pincode,
                       is_default
                FROM user_addresses
                WHERE address_id = ? AND user_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, addressId);
            statement.setInt(2, userId);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapAddress(rs) : null;
            }
        }
    }

    /** The customer's default address, or null. */
    public Address findDefault(int userId) throws SQLException {

        String sql = """
                SELECT address_id, user_id, label, full_name, phone,
                       address_line1, address_line2, city, state, pincode,
                       is_default
                FROM user_addresses
                WHERE user_id = ? AND is_default = 1
                LIMIT 1
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapAddress(rs) : null;
            }
        }
    }

    /** Save a new address. */
    public int insert(int userId, Address address) throws SQLException {

        String sql = """
                INSERT INTO user_addresses
                    (user_id, label, full_name, phone,
                     address_line1, address_line2, city, state, pincode,
                     is_default)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        boolean isFirst = findByUser(userId).isEmpty();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql,
                             java.sql.Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, userId);
            statement.setString(2, nullToEmpty(address.getLabel()));
            statement.setString(3, address.getFullName());
            statement.setString(4, address.getPhone());
            statement.setString(5, address.getAddressLine1());
            statement.setString(6, address.getAddressLine2());
            statement.setString(7, address.getCity());
            statement.setString(8, address.getState());
            statement.setString(9, address.getPincode());
            statement.setInt(10, (isFirst || address.isDefaultAddress())
                    ? 1 : 0);

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        if (address.isDefaultAddress()) {
            makeDefault(address.getUserId(), 0);
        }

        return 0;
    }

    /** Update an existing address. */
    public boolean update(int addressId, int userId, Address address)
            throws SQLException {

        String sql = """
                UPDATE user_addresses
                SET label = ?, full_name = ?, phone = ?,
                    address_line1 = ?, address_line2 = ?,
                    city = ?, state = ?, pincode = ?
                WHERE address_id = ? AND user_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, nullToEmpty(address.getLabel()));
            statement.setString(2, address.getFullName());
            statement.setString(3, address.getPhone());
            statement.setString(4, address.getAddressLine1());
            statement.setString(5, address.getAddressLine2());
            statement.setString(6, address.getCity());
            statement.setString(7, address.getState());
            statement.setString(8, address.getPincode());
            statement.setInt(9, addressId);
            statement.setInt(10, userId);

            return statement.executeUpdate() == 1;
        }
    }

    /** Delete an address. */
    public boolean delete(int addressId, int userId) throws SQLException {

        String sql = """
                DELETE FROM user_addresses
                WHERE address_id = ? AND user_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, addressId);
            statement.setInt(2, userId);

            boolean deleted = statement.executeUpdate() == 1;

            // Never leave the customer without a default address.
            if (deleted) {
                ensureHasDefault(userId);
            }

            return deleted;
        }
    }

    /** Make one address the default. */
    public boolean makeDefault(int userId, int addressId)
            throws SQLException {

        String clear = """
                UPDATE user_addresses SET is_default = 0 WHERE user_id = ?
                """;

        String set = """
                UPDATE user_addresses SET is_default = 1
                WHERE address_id = ? AND user_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement clearStatement =
                     connection.prepareStatement(clear);
             PreparedStatement setStatement =
                     connection.prepareStatement(set)) {

            connection.setAutoCommit(false);

            try {
                clearStatement.setInt(1, userId);
                clearStatement.executeUpdate();

                setStatement.setInt(1, addressId);
                setStatement.setInt(2, userId);

                boolean ok = setStatement.executeUpdate() == 1;

                connection.commit();
                return ok;

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /** If no address is marked default, make the newest one default. */
    private void ensureHasDefault(int userId) throws SQLException {

        List<Address> addresses = findByUser(userId);
        if (addresses.isEmpty()) {
            return;
        }

        for (Address address : addresses) {
            if (address.isDefaultAddress()) {
                return;
            }
        }

        makeDefault(userId, addresses.get(0).getAddressId());
    }

    /** How many addresses the customer has saved. */
    public int countByUser(int userId) throws SQLException {

        String sql = "SELECT COUNT(*) FROM user_addresses WHERE user_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ------------------------------------------------------------

    private Address mapAddress(ResultSet rs) throws SQLException {

        Address address = new Address();

        address.setAddressId(rs.getInt("address_id"));
        address.setUserId(rs.getInt("user_id"));
        address.setLabel(rs.getString("label"));
        address.setFullName(rs.getString("full_name"));
        address.setPhone(rs.getString("phone"));
        address.setAddressLine1(rs.getString("address_line1"));
        address.setAddressLine2(rs.getString("address_line2"));
        address.setCity(rs.getString("city"));
        address.setState(rs.getString("state"));
        address.setPincode(rs.getString("pincode"));
        address.setDefaultAddress(rs.getBoolean("is_default"));

        return address;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
