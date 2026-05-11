package org.example;

import java.io.Serializable;

public class LoginMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    public String username;
    public LoginMessage(String username) { this.username = username; }
}