package com.shopease;

import com.jayway.jsonpath.JsonPath;
import com.shopease.enums.OrderStatus;
import com.shopease.service.NotificationService;
import com.shopease.service.OrderExpiryJob;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end happy path on an in-memory H2 database (no PostgreSQL needed to run `mvn test`). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopEaseFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private OrderExpiryJob orderExpiryJob;

    @Test
    void vendorSellsProductAndCustomerBuysIt() throws Exception {
        String suffix = String.valueOf(System.nanoTime());

        // --- vendor onboarding ---
        String vendorToken = registerAndGetToken("vendor" + suffix + "@test.com", "VENDOR");
        mvc.perform(post("/api/stores").header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tech Hub\",\"description\":\"Gadgets\"}"))
                .andExpect(status().isCreated());

        long categoryId = idOf(mvc.perform(post("/api/categories").header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Electronics" + suffix + "\",\"description\":\"Devices\"}"))
                .andExpect(status().isCreated()).andReturn());

        long productId = idOf(mvc.perform(post("/api/products").header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Infinix Hot 40\",\"description\":\"Phone\",\"price\":125000.00,"
                                + "\"stockQuantity\":5,\"sku\":\"SKU-" + suffix + "\",\"categoryId\":" + categoryId
                                + ",\"imageUrls\":[\"https://example.com/a.png\"]}"))
                .andExpect(status().isCreated()).andReturn());

        // --- public browsing + search ---
        mvc.perform(get("/api/products/" + productId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Infinix Hot 40"));
        mvc.perform(get("/api/products/search").param("keyword", "infinix").param("minPrice", "100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // --- customer buys ---
        String customerToken = registerAndGetToken("customer" + suffix + "@test.com", "CUSTOMER");
        mvc.perform(post("/api/cart/items").header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(250000.00));

        long orderId = idOf(mvc.perform(post("/api/orders").header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shippingAddress\":\"12 Ilorin Road, Ogbomoso\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING")).andReturn());

        // stock was reserved
        mvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.stockQuantity").value(3));

        // --- payment (mock gateway) ---
        MvcResult init = mvc.perform(post("/api/payments/initialize").header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + "}"))
                .andExpect(status().isOk()).andReturn();
        String reference = JsonPath.read(init.getResponse().getContentAsString(), "$.reference");

        mvc.perform(get("/api/payments/verify/" + reference).header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.orderStatus").value("PAID"));

        // --- vendor fulfils ---
        mvc.perform(put("/api/orders/" + orderId + "/status").header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PROCESSING"));
        // cannot skip SHIPPED
        mvc.perform(put("/api/orders/" + orderId + "/status").header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isBadRequest());

        // vendor ships; only the CUSTOMER can confirm arrival
        mvc.perform(put("/api/orders/" + orderId + "/status").header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SHIPPED"));
        mvc.perform(put("/api/orders/" + orderId + "/status").header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/orders/" + orderId + "/status").header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELIVERED"));

        // --- in-app notifications (call the service directly so the test does not depend on async timing) ---
        notificationService.notifyOrderStatus(orderId, OrderStatus.PAID);
        notificationService.notifyOrderStatus(orderId, OrderStatus.SHIPPED);
        mvc.perform(get("/api/notifications").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.status=='SHIPPED')]").isNotEmpty());
        mvc.perform(get("/api/notifications").header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.status=='PAID')]").isNotEmpty());
        mvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
        mvc.perform(put("/api/notifications/read-all").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // --- wishlist: love, then unlove (twice is fine) ---
        mvc.perform(post("/api/wishlist").header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":" + productId + "}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/wishlist").header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/wishlist/" + productId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/wishlist").header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/wishlist/" + productId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNoContent());

        // --- customer can now review ---
        mvc.perform(post("/api/products/" + productId + "/reviews").header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5,\"comment\":\"Great\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void unpaidOrdersExpireAndGiveTheirStockBack() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String vendorToken = registerAndGetToken("ev" + suffix + "@test.com", "VENDOR");
        mvc.perform(post("/api/stores").header("Authorization", "Bearer " + vendorToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Expiry " + suffix + "\"}")).andExpect(status().isCreated());
        long categoryId = idOf(mvc.perform(post("/api/categories").header("Authorization", "Bearer " + vendorToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Exp" + suffix + "\"}")).andExpect(status().isCreated()).andReturn());
        long productId = idOf(mvc.perform(post("/api/products").header("Authorization", "Bearer " + vendorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Stock item\",\"price\":10.00,\"stockQuantity\":5,\"sku\":\"EX-" + suffix + "\",\"categoryId\":" + categoryId + "}"))
                .andExpect(status().isCreated()).andReturn());

        String customerToken = registerAndGetToken("ec" + suffix + "@test.com", "CUSTOMER");
        mvc.perform(post("/api/cart/items").header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":" + productId + ",\"quantity\":3}")).andExpect(status().isOk());
        long orderId = idOf(mvc.perform(post("/api/orders").header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"shippingAddress\":\"Somewhere\"}")).andExpect(status().isCreated()).andReturn());
        mvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.stockQuantity").value(2));

        // pretend an hour has passed: the unpaid order is cancelled and its stock returns
        org.junit.jupiter.api.Assertions.assertTrue(orderExpiryJob.expireOlderThan(LocalDateTime.now().plusMinutes(1)) >= 1);
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + customerToken))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.stockQuantity").value(5));
    }

    @Test
    void reviewWithoutPurchaseExplainsWhy() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String vendorToken = registerAndGetToken("rv" + suffix + "@test.com", "VENDOR");
        mvc.perform(post("/api/stores").header("Authorization", "Bearer " + vendorToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Shop " + suffix + "\"}")).andExpect(status().isCreated());
        long categoryId = idOf(mvc.perform(post("/api/categories").header("Authorization", "Bearer " + vendorToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Cat" + suffix + "\"}")).andExpect(status().isCreated()).andReturn());
        long productId = idOf(mvc.perform(post("/api/products").header("Authorization", "Bearer " + vendorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Thing\",\"price\":10.00,\"stockQuantity\":1,\"sku\":\"RV-" + suffix + "\",\"categoryId\":" + categoryId + "}"))
                .andExpect(status().isCreated()).andReturn());
        String customerToken = registerAndGetToken("rc" + suffix + "@test.com", "CUSTOMER");
        mvc.perform(post("/api/products/" + productId + "/reviews").header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only review products you have purchased"));
    }

    @Test
    void securityRulesAreEnforced() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String customerToken = registerAndGetToken("sec" + suffix + "@test.com", "CUSTOMER");

        // no token -> 401
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        // wrong role -> 403
        mvc.perform(post("/api/products").header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        // customers cannot use admin endpoints
        mvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
        // ADMIN cannot be self-registered
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Evil\",\"email\":\"evil" + suffix + "@test.com\","
                                + "\"password\":\"Password@123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());
        // validation errors are reported per field
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"not-an-email\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    void guestCartWorksWithSessionHeaderAndDefaultAdminCanLogin() throws Exception {
        mvc.perform(get("/api/cart").header("X-Session-Id", "guest-" + System.nanoTime()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(get("/api/cart")).andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@shopease.com\",\"password\":\"Admin@12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    // ---------- helpers ----------

    private String registerAndGetToken(String email, String role) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Test User\",\"email\":\"" + email + "\","
                                + "\"password\":\"Password@123\",\"role\":\"" + role + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private long idOf(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
