package buyhub.users;

import java.sql.Timestamp;

public class ParamUser {
    private String email;
    private String password;
    
    private String name;
    private String maternalSurname;
    private String paternalSurname;
    private Timestamp birthday;
    private Long phoneNumber;
    
    private byte [] photo;

    public String getEmail() {
        if (email == null)
            return null;
        if (email.trim().equals(""))
            return null;
        return email;
    }

    public String getPassword() {
        if (password == null)
            return null;
        if (password.trim().equals(""))
            return null;
        return password;
    }

    public String getName() {
        if (name == null)
            return null;
        if (name.trim().equals(""))
            return null;
        return name;
    }

    public String getMaternalSurname() {
        if (maternalSurname == null)
            return null;
        if (maternalSurname.trim().equals(""))
            return null;
        return maternalSurname;
    }

    public String getPaternalSurname() {
        if (paternalSurname == null)
            return null;
        if (paternalSurname.trim().equals(""))
            return null;
        return paternalSurname;
    }

    public Timestamp getBirthday() {
        return birthday;
    }

    public Long getPhoneNumber() {
        return phoneNumber;
    }

    public byte [] getPhoto() {
        return photo;
    }
}
