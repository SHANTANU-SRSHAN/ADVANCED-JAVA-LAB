package com.shopsphere.servlet;

import com.shopsphere.dao.AddressDAO;
import com.shopsphere.model.Address;
import com.shopsphere.model.User;
import com.shopsphere.util.Csrf;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The customer's address book.
 *
 *   GET  /addresses                    → list of saved addresses
 *   POST /addresses action=add        → save a new address
 *   POST /addresses action=update     → edit an address
 *   POST /addresses action=delete     → remove an address
 *   POST /addresses action=default    → make an address the default
 */
@WebServlet("/addresses")
public class AddressServlet extends HttpServlet {

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9]{10}$");
    private static final Pattern PINCODE_PATTERN =
            Pattern.compile("^[0-9]{6}$");

    private final AddressDAO addressDAO = new AddressDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        try {
            int userId = currentUserId(request);

            request.setAttribute("addresses",
                    addressDAO.findByUser(userId));
            request.setAttribute("csrfToken", Csrf.token(request));

            request.getRequestDispatcher("/addresses.jsp")
                   .forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load your addresses.", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        if (!Csrf.isValid(request)) {
            response.sendRedirect(request.getContextPath()
                    + "/addresses?error=session");
            return;
        }

        String action = request.getParameter("action");
        int userId = currentUserId(request);

        try {
            if ("add".equals(action) || "update".equals(action)) {

                Address address = readAddress(request);

                String error = validate(address);
                if (error != null) {
                    response.sendRedirect(request.getContextPath()
                            + "/addresses?error=" + urlEncode(error));
                    return;
                }

                if ("add".equals(action)) {
                    addressDAO.insert(userId, address);
                } else {
                    int addressId = parseInt(
                            request.getParameter("addressId"), -1);
                    if (addressId > 0) {
                        addressDAO.update(addressId, userId, address);
                    }
                }

                response.sendRedirect(request.getContextPath()
                        + "/addresses?saved=1");

            } else if ("delete".equals(action)) {

                int addressId = parseInt(
                        request.getParameter("addressId"), -1);
                if (addressId > 0) {
                    addressDAO.delete(addressId, userId);
                }

                response.sendRedirect(request.getContextPath()
                        + "/addresses?deleted=1");

            } else if ("default".equals(action)) {

                int addressId = parseInt(
                        request.getParameter("addressId"), -1);
                if (addressId > 0) {
                    addressDAO.makeDefault(userId, addressId);
                }

                response.sendRedirect(request.getContextPath()
                        + "/addresses?isDefault=1");

            } else {
                response.sendRedirect(request.getContextPath()
                        + "/addresses");
            }

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to update your addresses.", e);
        }
    }

    // ------------------------------------------------------------

    private Address readAddress(HttpServletRequest request) {

        Address address = new Address();

        address.setAddressId(parseInt(
                request.getParameter("addressId"), 0));
        address.setLabel(trim(request.getParameter("label")));
        address.setFullName(trim(request.getParameter("fullName")));
        address.setPhone(trim(request.getParameter("phone")));
        address.setAddressLine1(trim(request.getParameter("addressLine1")));
        address.setAddressLine2(trim(request.getParameter("addressLine2")));
        address.setCity(trim(request.getParameter("city")));
        address.setState(trim(request.getParameter("state")));
        address.setPincode(trim(request.getParameter("pincode")));

        return address;
    }

    private String validate(Address address) {

        if (address.getFullName().length() < 2) {
            return "Please enter the recipient's full name.";
        }
        if (!PHONE_PATTERN.matcher(address.getPhone()).matches()) {
            return "Please enter a 10-digit phone number.";
        }
        if (address.getAddressLine1().length() < 5) {
            return "Please enter the street address.";
        }
        if (address.getCity().length() < 2) {
            return "Please enter the city.";
        }
        if (!PINCODE_PATTERN.matcher(address.getPincode()).matches()) {
            return "Please enter a 6-digit pincode.";
        }

        return null;
    }

    private int currentUserId(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        return user.getUserId();
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value,
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "error";
        }
    }
}
