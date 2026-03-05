package com.example.demo;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class Users {
   @Id 
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(unique = true, nullable = false)
    @JsonAlias({"name", "userName", "user_name"})
   private String username;

   @Column(unique = true, nullable = false)
    @JsonAlias({"mail", "emailId", "email_id"})
   private String email;
   
   @Column(nullable = false)
    @JsonAlias({"pass", "pwd"})
   private String password;

   public Users() {}

   public void setUsername(String username) {
       this.username = username;
   }

   public void setEmail(String email) {
       this.email = email;
   }

   public void setPassword(String password) {
       this.password = password;
   }

   public String getPassword() {
       return this.password;
   }
   
   public String getUsername() {
       return this.username;
   }
   
   public String getEmail() {
       return this.email;
   }
   
   public Long getId() {
       return this.id;
   }
}
