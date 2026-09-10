package com.company.biz.service.controller;

import com.company.biz.service.entity.OrderEntity;
import com.company.biz.service.enums.OrderStatus;
import com.company.biz.service.mapper.OrderMapper;
import com.company.security.UserInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 订单归属校验测试：验证 Architect Item 3 的所有权限场景。
 * 测试场景：
 * 1. 订单所有者可以读取自己的订单
 * 2. 非所有者访问他人订单返回 NOT_FOUND（避免泄露资源是否存在）
 * 3. 未认证用户访问返回 UNAUTHORIZED (401)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderOwnershipTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderMapper orderMapper;

    private Long testOrderId;
    private static final Long OWNER_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final String OWNER_USER_NAME = "owner";

    @BeforeEach
    void setUp() {
        orderMapper.delete(null);

        OrderEntity order = new OrderEntity();
        order.setOrderNo("TEST-ORDER-001");
        order.setSkuId(100L);
        order.setQuantity(2);
        order.setAmount(BigDecimal.valueOf(200));
        order.setStatus(OrderStatus.PENDING);
        order.setBuyerId(OWNER_USER_ID);
        order.setBuyerName(OWNER_USER_NAME);
        orderMapper.insert(order);
        testOrderId = order.getId();
    }

    @Test
    @DisplayName("订单所有者可以读取自己的订单")
    void ownerCanReadOwnOrder() throws Exception {
        mockMvc.perform(get("/internal/order/{id}", testOrderId)
                        .header(UserInterceptor.HEADER_USER_ID, OWNER_USER_ID.toString())
                        .header(UserInterceptor.HEADER_USER_NAME, OWNER_USER_NAME)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testOrderId))
                .andExpect(jsonPath("$.data.buyerId").value(OWNER_USER_ID));
    }

    @Test
    @DisplayName("非所有者访问他人订单返回 NOT_FOUND（避免泄露资源存在）")
    void nonOwnerCannotReadOthersOrder() throws Exception {
        mockMvc.perform(get("/internal/order/{id}", testOrderId)
                        .header(UserInterceptor.HEADER_USER_ID, OTHER_USER_ID.toString())
                        .header(UserInterceptor.HEADER_USER_NAME, "other")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(10004));
    }

    @Test
    @DisplayName("未认证用户访问订单返回 UNAUTHORIZED")
    void unauthenticatedUserCannotReadOrder() throws Exception {
        mockMvc.perform(get("/internal/order/{id}", testOrderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    @DisplayName("订单所有者可以查看自己的订单分页列表")
    void ownerCanPageOwnOrders() throws Exception {
        mockMvc.perform(get("/internal/order")
                        .header(UserInterceptor.HEADER_USER_ID, OWNER_USER_ID.toString())
                        .header(UserInterceptor.HEADER_USER_NAME, OWNER_USER_NAME)
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].buyerId").value(OWNER_USER_ID));
    }

    @Test
    @DisplayName("非所有者查看分页列表时看不到他人的订单")
    void nonOwnerCannotSeeOthersOrdersInPage() throws Exception {
        mockMvc.perform(get("/internal/order")
                        .header(UserInterceptor.HEADER_USER_ID, OTHER_USER_ID.toString())
                        .header(UserInterceptor.HEADER_USER_NAME, "other")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records").isEmpty());
    }
}
