package com.tcs.module.finance.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload SePay POST toi webhook khi co bien dong so du tai khoan ngan hang.
 * Tham khao: https://docs.sepay.vn/tich-hop-webhooks.html
 */
@Getter
@Setter
public class SepayWebhookRequest {

    /** ID giao dich tren SePay (dung de chong xu ly trung). */
    private Long id;

    /** Ten ngan hang, vd "MBBank". */
    private String gateway;

    /** So tai khoan nhan tien. */
    private String accountNumber;

    /** Noi dung chuyen khoan — chua referenceCode cua don nap. */
    private String content;

    /** "in" = tien vao, "out" = tien ra. */
    private String transferType;

    /** So tien giao dich. */
    private BigDecimal transferAmount;

    /** Ma tham chieu SMS cua ngan hang (khong phai ma don cua he thong). */
    private String referenceCode;

    /** Mo ta thuan tuy tu SePay (co the trung noi dung). */
    private String description;

    /** Mot so payload dung "code" cho ma noi dung da tach san. */
    @JsonProperty("code")
    private String code;
}
