package com.example.WordDocumentsFiller.entities;

import com.example.WordDocumentsFiller.entities.enums.PartyType;
import jakarta.persistence.*;

@Entity
@Table(
        name = "parties",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_party_type_name_address",
                        columnNames = {"party_type", "name", "address", "town", "country"}
                )
        }
)
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "party_type", nullable = false, length = 16)
    private PartyType type;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(length = 128)
    private String town;

    @Column(length = 128)
    private String country;

    public Long getId() {
        return id;
    }

    public PartyType getType() {
        return type;
    }

    public void setType(PartyType type) {
        this.type = type;
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
}
