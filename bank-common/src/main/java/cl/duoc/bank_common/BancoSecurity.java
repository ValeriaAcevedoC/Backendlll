package cl.duoc.bank_common;

import java.util.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

public final class BancoSecurity {
  private BancoSecurity() {}

  public static JwtAuthenticationConverter converter() {
    var c = new JwtAuthenticationConverter();
    c.setJwtGrantedAuthoritiesConverter(
        jwt -> {
          Map<String, Object> realm = jwt.getClaim("realm_access");
          if (realm == null || !(realm.get("roles") instanceof Collection<?> roles))
            return List.of();
          return roles.stream()
              .<org.springframework.security.core.GrantedAuthority>map(
                  r -> new SimpleGrantedAuthority("ROLE_" + r))
              .toList();
        });
    return c;
  }
}
