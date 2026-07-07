-- =====================================================================
-- Bo sung ten chu tai khoan cho phuong thuc thanh toan (payment_methods),
-- phuc vu chuc nang RUT TIEN: nguoi dung luu san tai khoan ngan hang
-- (ngan hang + so tai khoan + ten chu tai khoan) roi chon khi rut.
-- Ghi chu: version 8 da bi nhanh khac dung ("center class support"),
-- nen migration nay danh so V9 de tranh trung.
-- =====================================================================

SET NAMES utf8mb4;

ALTER TABLE payment_methods
    ADD COLUMN account_name VARCHAR(100) NULL AFTER bank_name;
