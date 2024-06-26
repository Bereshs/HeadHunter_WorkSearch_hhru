package ru.bereshs.hhworksearch.domain;

import com.github.scribejava.core.model.OAuth2AccessToken;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "keys")
@Data
public class KeyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    private LocalDateTime time;
    private String authorizationCode;
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn;
    private String tokenType;
    private String scope;
    private String clientId;
    private String rowResponse;

    public boolean isValid() {
        if (expiresIn == null || authorizationCode == null) {
            return false;
        }
        LocalDateTime expireTime = time.plusSeconds(expiresIn);
        return LocalDateTime.now()
                .isBefore(expireTime);
    }

    public void set(OAuth2AccessToken token) {
        setAccessToken(token.getAccessToken());
        setRefreshToken(token.getRefreshToken());
        setExpiresIn(token.getExpiresIn());
        setTokenType(token.getTokenType());
        setScope(token.getScope());
        setRowResponse(token.getRawResponse());
    }
}
