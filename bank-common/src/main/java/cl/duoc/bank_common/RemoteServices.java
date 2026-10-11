package cl.duoc.bank_common;

import org.springframework.context.annotation.*;

@Configuration
@Import({RemoteConfig.class, CuentasClient.class, CanalClient.class, ApiErrors.class})
public class RemoteServices {}
