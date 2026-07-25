-- UC-65: luu chi tiet danh gia theo tung tieu chi (dung gio, de hieu, tan tam...) duoi dang JSON.
-- rating tong van la trung binh lam tron cua cac tieu chi.
ALTER TABLE reviews
    ADD COLUMN criteria_json TEXT NULL AFTER comment;
