package org.chop.quarkus.scripting.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusScriptingResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/quarkus-scripting")
                .then()
                .statusCode(200)
                .body(is("Hello quarkus-scripting"));
    }
}
