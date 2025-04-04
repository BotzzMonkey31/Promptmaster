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

    private String email;
    private String name;
    private String username;
    private String picture;
    private String country;
    private Rank rank;
    private Integer elo;
}