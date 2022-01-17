package com.springbootangular.backend.model;

import javax.persistence.*;

@Entity
@Table(name = "APPLICATION_USER")
public class User {
    private Integer id;
    private String name;

    public User() {
    }

    public User(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    @Id
    @Column(name = "ID")
    @SequenceGenerator(name = "id_seq", sequenceName = "application_user_id_seq",allocationSize=1)
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="id_seq")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
