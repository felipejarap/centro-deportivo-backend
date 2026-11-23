package cl.duoc.ms_apiGateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"jwt.secret=test-only-signing-key-not-for-production-1234567890",
		"eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false"
})
class MsApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
