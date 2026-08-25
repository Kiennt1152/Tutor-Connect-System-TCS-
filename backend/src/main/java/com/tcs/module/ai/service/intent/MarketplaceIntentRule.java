package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.containsAny;

@Component
public class MarketplaceIntentRule implements IntentRule {

    @Override
    public int priority() {
        return 120;
    }

    @Override
    public ClassificationDetail classify(String normalized, String lower) {
        // Global exclusions for Academic Tutoring / Homework & Platform Policies
        boolean isAcademicOrHomework = containsAny(normalized,
                "giai thich", "the nao la", "vi du ve", "vi du", "tai sao", "chung minh",
                "giai phuong trinh", "giai giup", "giai bai", "bai tap", "tinh gia tri", "khai niem",
                "dinh ly", "cong thuc", "ngu phap", "banh pizza", "pizza", "huong dan giai");
        if (isAcademicOrHomework) {
            return null; // Let ConversationSafetyRule or IntentClassifier route to OUT_OF_SCOPE
        }

        boolean isPolicyOrFaq = containsAny(normalized,
                "website minh co", "he thong co", "ben minh co", "tcs co",
                "tieu chuan tuyen chon", "tieu chuan gia su", "kiem duyet", "linh vuc nao duoc",
                "hinh thuc day", "online hay tai nha", "tai nha hay online", "day online khong", "co gia su day online khong", "day online hay", "day truc tuyen khong",
                "hoc thu", "doi gia su", "khong hop thi doi", "doi gia su khac",
                "quy trinh dang ky", "quy trinh tim gia su", "cac buoc tim gia su", "cach thue gia su",
                "muon kiem gia su thi", "muon tim gia su thi", "kiem gia su thi vao dau", "tim gia su thi vao dau", "thue gia su thi vao dau",
                "vao dau", "o dau", "trang nao", "muc nao",
                "thanh toan truc tiep", "chuyen khoan qua", "chuyen tien cho gia su hay trung tam", "thanh toan qua dau",
                "luong trung binh", "hoc phi trung binh", "thu nhap trung binh", "bang gia hoc phi", "khung hoc phi", "gia mot buoi la bao nhieu", "chi phi thue gia su");
        if (isPolicyOrFaq) {
            return null; // Let CatalogFaqIntentRule handle
        }

        // 1. APPLY TO CLASS (TUTOR APPLICATION) - High Priority
        if (containsAny(normalized,
                "ung tuyen nhu the nao", "ung tuyen nhu nao", "cach ung tuyen", "huong dan ung tuyen",
                "lam sao de ung tuyen", "gia su ung tuyen", "ung tuyen nhan lop", "ung tuyen lop day",
                "nop don ung tuyen", "cach nhan lop day", "quy trinh ung tuyen", "nop ho so nhan lop",
                "gia su nop ho so", "nop don", "nop ho so", "cach ung tuyen lop day kem", "ung tuyen lop day kem", "ung tuyen lop")) {
            return new ClassificationDetail(AiDomain.MARKETPLACE, AiSubIntent.APPLY_TO_CLASS, AiIntent.FAQ_SUPPORT, 0.95, "/lop-hoc");
        }

        // 2. CREATE CLASS (POST A REQUEST) - Priority over finding existing tutor
        if (containsAny(normalized,
                "dang bai tim gia su", "tao yeu cau hoc moi", "tao lop tim nguoi day", "dang tin tim gia su", "tao lop tim gia su",
                "tao lop", "dang bai", "tao yeu cau hoc", "tao bai dang", "dang tin", "dang tin tim", "dang bai tim", "tao bai dang tim gia su", "tao yeu cau tim gia su")) {
            return new ClassificationDetail(AiDomain.MARKETPLACE, AiSubIntent.CREATE_CLASS, AiIntent.CREATE_CLASS, 0.95, "/tao-lop");
        }

        // 3. FIND CLASS
        boolean hasTutorKeyword = containsAny(normalized,
                "gia su", "tutor", "thay giao", "co giao", "thay co", "giao vien", "nguoi day", "day kem", "gs", "tim thay", "tim co");

        if (containsAny(normalized,
                "tim lop", "lop hoc dang mo", "lop dang mo", "khoa hoc", "dang ky lop", "danh sach lop", "chon gia su ung tuyen",
                "tim lop toan", "tim lop ly", "tim lop hoa", "tim lop anh", "tim lop van", "tim lop su", "tim lop dia", "tim lop sinh", "tim lop tin", "tim lop tieng",
                "co lop nao", "co lop toan nao", "co lop toan ko", "co lop toan khong", "co lop day tiieng viet khong", "co lop day tieng", "co lop day", "co lop", "tim lop hoc", "lop hoc tieng", "lop day tieng", "lop day toan",
                "find class", "find classes", "find math classes", "open classes", "math classes open", "classes near me", "search classes", "can tim lop")) {
            return new ClassificationDetail(AiDomain.MARKETPLACE, AiSubIntent.FIND_CLASS, AiIntent.FIND_CLASS, 0.9, "/lop-hoc");
        }

        // 4. FIND TUTOR
        boolean hasExclusion = containsAny(normalized,
                "luong", "tra luong", "nhan luong", "tra trong bao lau", "bao lau", "bao gio", "giai ngan", "quy dinh", "bao nhieu phan tram",
                "diem uy tin", "xac minh", "duyet ho so", "cccd", "bang cap", "hoan tien", "tranh chap", "to cao", "ung tuyen", "cach nhan lop",
                "khieu nai", "hop dong", "ky hop dong", "rut tien", "phi san", "phi nen tang", "chuyen khoan rieng", "ngoai san", "bao cao", "lich day", "doi gio");

        boolean hasSearchKeyword = containsAny(normalized,
                "tim", "thue", "can", "kiem", "cho toi", "gioi thieu", "mon", "toan", "ly", "hoa", "anh", "van", "tin", "sinh", "su", "dia",
                "ielts", "toeic", "tieng anh", "tieng nhat", "tieng han", "tieng trung", "ngoai ngu", "giao tiep", "n5", "n4", "n3", "n2", "n1",
                "cap 3", "cap 2", "cap 1", "thay co", "mat goc", "nguoi di lam", "on thi",
                "khu vuc", "cau giay", "dong da", "ba dinh", "ha noi", "hcm", "sai gon", "da nang",
                "hoc phi", "duoi", "khoang", "k/buoi", "vnd", "luyen thi", "find", "looking", "near", "day kem", "tai nha", "1 kem 1", "online");

        if (!hasExclusion && ((hasTutorKeyword && hasSearchKeyword) || containsAny(normalized,
                "tim gia su", "thue gia su", "can gia su", "can thue gia su", "gia su day", "giao vien day", "tim thay", "tim co", "ai re hon",
                "tim gs", "tim gs toan", "tim gs ly", "tim gs hoa", "tim gs anh", "tim thay day toan", "tim co day toan", "co gia su toan ko", "co gia su nao",
                "gia su toan", "gia su ly", "gia su hoa", "gia su anh", "gia su van", "gia su tin", "gia su luyen thi", "gia su tieng", "gia su ielts",
                "co ai day", "ai day ielts", "co ai day ielts", "day ielts", "co ai day toan", "co ai day van", "ai day",
                "find tutor", "math tutor", "tutor near me", "looking for tutor", "need a tutor", "hire tutor", "math tutor near"))) {
            return new ClassificationDetail(AiDomain.MARKETPLACE, AiSubIntent.FIND_TUTOR, AiIntent.FIND_TUTOR, 0.95, "/tim-gia-su");
        }

        return null;
    }
}
