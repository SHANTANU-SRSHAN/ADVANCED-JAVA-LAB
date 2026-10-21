package com.shopsphere.servlet;

import com.shopsphere.dao.CouponDAO;
import com.shopsphere.model.Coupon;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin coupon management.
 *
 *   GET  /admin/coupons             → every coupon
 *   POST /admin/coupons action=add  → create a coupon
 *   POST /admin/coupons action=toggle → switch a coupon on or off
 *   POST /admin/coupons action=delete → remove an unused coupon
 */
@WebServlet("/admin/coupons")
public class AdminCouponsServlet extends HttpServlet {

    private final CouponDAO couponDAO = new CouponDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        try {
            List<Coupon> coupons = couponDAO.findAll();

            request.setAttribute("coupons", coupons);

            request.getRequestDispatcher("/admin/coupons.jsp")
                   .forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load the coupon list.", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");
        int couponId = parseInt(request.getParameter("couponId"), -1);

        try {
            if ("add".equals(action)) {

                Coupon coupon = new Coupon();

                coupon.setCode(trim(request.getParameter("code"))
                        .toUpperCase());
                coupon.setDescription(
                        trim(request.getParameter("description")));
                coupon.setDiscountType(
                        "FIXED".equalsIgnoreCase(
                                trim(request.getParameter("discountType")))
                                ? "FIXED" : "PERCENT");
                coupon.setDiscountValue(decimal(
                        request.getParameter("discountValue")));
                coupon.setMinOrderAmount(decimal(
                        request.getParameter("minOrderAmount")));

                String maxDiscount = trim(
                        request.getParameter("maxDiscount"));
                if (!maxDiscount.isEmpty()) {
                    coupon.setMaxDiscount(decimal(maxDiscount));
                }

                String usageLimit = trim(
                        request.getParameter("usageLimit"));
                if (!usageLimit.isEmpty()) {
                    coupon.setUsageLimit((int) decimal(usageLimit)
                            .longValue());
                }

                coupon.setPerUserLimit((int) decimal(
                        request.getParameter("perUserLimit")).longValue());
                coupon.setActive(true);

                String validTo = trim(request.getParameter("validTo"));
                if (!validTo.isEmpty()) {
                    try {
                        coupon.setValidTo(LocalDateTime.parse(
                                validTo + "T00:00:00"));
                    } catch (Exception e) {
                        coupon.setValidTo(
                                LocalDateTime.now().plusYears(1));
                    }
                } else {
                    coupon.setValidTo(LocalDateTime.now().plusYears(1));
                }

                if (coupon.getCode().isEmpty()) {
                    response.sendRedirect(request.getContextPath()
                            + "/admin/coupons?error=The+coupon+needs+a+code.");
                    return;
                }

                boolean created = couponDAO.insert(coupon);

                response.sendRedirect(request.getContextPath()
                        + "/admin/coupons?"
                        + (created ? "created=1"
                        : "error=That+coupon+code+already+exists."));

                return;
            }

            if ("toggle".equals(action) && couponId > 0) {

                Coupon existing = null;
                for (Coupon coupon : couponDAO.findAll()) {
                    if (coupon.getCouponId() == couponId) {
                        existing = coupon;
                        break;
                    }
                }

                if (existing != null) {
                    couponDAO.setActive(couponId, !existing.isActive());
                }

                response.sendRedirect(request.getContextPath()
                        + "/admin/coupons?updated=1");
                return;
            }

            if ("delete".equals(action) && couponId > 0) {

                boolean deleted = couponDAO.delete(couponId);

                response.sendRedirect(request.getContextPath()
                        + "/admin/coupons?"
                        + (deleted ? "deleted=1"
                        : "error=That+coupon+has+already+been+used."));
                return;
            }

            response.sendRedirect(request.getContextPath()
                    + "/admin/coupons");

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to update the coupons.", e);
        }
    }

    private BigDecimal decimal(String value) {
        try {
            return new BigDecimal(trim(value));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
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
}
