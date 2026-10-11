package cl.duoc.bank_common;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

@Configuration
public class RemoteConfig {
  @Bean
  @Primary
  public RestClient.Builder restClientBuilder() {
    return RestClient.builder();
  }

  @Bean
  @LoadBalanced
  public RestClient.Builder remoteRestClientBuilder() {
    var http =
        java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(2))
            .build();
    var f = new JdkClientHttpRequestFactory(http);
    f.setReadTimeout(java.time.Duration.ofSeconds(5));
    return RestClient.builder()
        .requestFactory(f)
        .requestInterceptor(
            (r, b, e) -> {
              var a = SecurityContextHolder.getContext().getAuthentication();
              if (a instanceof JwtAuthenticationToken t)
                r.getHeaders().setBearerAuth(t.getToken().getTokenValue());
              return e.execute(r, b);
            });
  }
}
