package com.scmcloud.common.web.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 鑷畾锟経serDetails
 *
 * @author Deng
 * createData 2025/10/14 14:52
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityUser implements UserDetails {
    private UUID userId;
    private String username;
    private String password;
    private String realName;
    private UUID deptId;
    private Integer status;
    private Integer accountType;
    private Integer userLevel;
    @Builder.Default
    private Set<String> roles = Collections.emptySet();
    @Builder.Default
    private Set<String> permissions = Collections.emptySet();
    private String twoFactorSecret;

    // 瀹夊叏鐩稿叧瀛楁
    private Boolean twoFactorEnabled;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime passwordExpireTime;
    private Boolean forceChangePassword;
    
    @JsonIgnore
    private Collection<? extends GrantedAuthority> authorities;

    @Override
    @JsonIgnore
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        // 鍚堝苟瑙掕壊鍜屾潈闄愶紝闃插尽绌洪泦锟?
        Set<GrantedAuthority> authorities = (roles != null ? roles : Collections.<String>emptySet()).stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        authorities.addAll((permissions != null ? permissions : Collections.<String>emptySet()).stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet()));

        return authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public @NonNull String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.status != 2; // 2琛ㄧず閿佸畾
    }

    @Override
    public boolean isCredentialsNonExpired() {
        if (passwordExpireTime == null) {
            return true;
        }
        return passwordExpireTime.isAfter(LocalDateTime.now());
    }

    @Override
    public boolean isEnabled() {
        return this.status == 1; // 1琛ㄧず鍚敤
    }
}
