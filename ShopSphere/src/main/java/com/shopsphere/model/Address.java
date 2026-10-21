package com.shopsphere.model;

/**
 * One of the addresses a customer has saved in their address book.
 */
public class Address {

    private int addressId;
    private int userId;

    private String label = "Home";
    private String fullName = "";
    private String phone = "";
    private String addressLine1 = "";
    private String addressLine2 = "";
    private String city = "";
    private String state = "";
    private String pincode = "";

    private boolean defaultAddress;

    public Address() {
    }

    public int getAddressId() {
        return addressId;
    }

    public void setAddressId(int addressId) {
        this.addressId = addressId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    public void setDefaultAddress(boolean defaultAddress) {
        this.defaultAddress = defaultAddress;
    }

    /** The address as one line — used for the order snapshot. */
    public String toSingleLine() {
        StringBuilder line = new StringBuilder();
        line.append(addressLine1 == null ? "" : addressLine1.trim());

        if (addressLine2 != null && !addressLine2.isBlank()) {
            line.append(", ").append(addressLine2.trim());
        }
        line.append(", ").append(city == null ? "" : city.trim());
        if (state != null && !state.isBlank()) {
            line.append(", ").append(state.trim());
        }
        line.append(" - ").append(pincode == null ? "" : pincode.trim());

        return line.toString();
    }

    /** True when every required field is filled in. */
    public boolean isComplete() {
        return notBlank(fullName) && notBlank(phone)
                && notBlank(addressLine1) && notBlank(city)
                && notBlank(pincode);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
