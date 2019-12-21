package winstone.entities;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "user")
public class User {

    @XmlElement(name = "username")
    private String username;

    @XmlElement(name = "password")
    private String password;

    @XmlElement(name = "rolename")
    private String roleList;

    public User(String username, String password, String roleList) {
        this.username = username;
        this.password = password;
        this.roleList = roleList;
    }

    public String getPassword() {
        return password;
    }

    public String getRoleList() {
        return roleList;
    }

    public String getUsername() {
        return username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRoleList(String roleList) {
        this.roleList = roleList;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
