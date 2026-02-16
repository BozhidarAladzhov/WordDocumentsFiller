package com.example.WordDocumentsFiller.entities;

import jakarta.persistence.*;

@Entity
@Table(
        name = "up_clients",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_up_client_identity",
                        columnNames = {"name", "address", "town", "country", "eori"}
                )
        }
)
public class UpClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 220)
    private String name;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(length = 128)
    private String town;

    @Column(length = 128)
    private String country;

    @Column(length = 128)
    private String eori;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getEori() {
        return eori;
    }

    public void setEori(String eori) {
        this.eori = eori;
    }
}
