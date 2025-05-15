package info.sup.proj.backend.model;

import lombok.Data;

@Data
public class UserRegistrationDto {
    public String getEmail() {
        return email;
    }

    public String getCountry() {
        return country;
    }

    public String getPicture() {
        return picture;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public Integer getElo() {
        return elo;
    }

    public Rank getRank() {
        return rank;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public void setRank(Rank rank) {
        this.rank = rank;
    }

    public void setElo(Integer elo) {
        this.elo = elo;
    }

    private String email;
    private String name;
    private String username;
    private String picture;
    private String country;
    private Rank rank;
    private Integer elo;
}