package com.foodfast.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class SupportIntentClassifier {

    public enum Intent {
        REFUND_OR_PAYMENT,
        PAYMENT_HELP,
        ACCOUNT,
        ORDER_CHANGE,
        FOOD_ISSUE,
        DELIVERY_TIME,
        DELIVERY_FEE,
        ORDER_STATUS,
        VOUCHER,
        RESTAURANT_HOURS,
        REORDER,
        HUMAN_SUPPORT,
        GREETING,
        GENERAL
    }

    private final Map<Intent, String[]> vocabulary = new LinkedHashMap<>();

    public SupportIntentClassifier() {
        vocabulary.put(Intent.REFUND_OR_PAYMENT,
                new String[]{"hoan tien", "refund", "tru tien hai lan", "bi tru tien", "khieu nai thanh toan"});
        vocabulary.put(Intent.PAYMENT_HELP,
                new String[]{"thanh toan", "vnpay", "the ngan hang", "otp", "het han thanh toan", "tra tien"});
        vocabulary.put(Intent.ACCOUNT,
                new String[]{"tai khoan", "mat khau", "dang nhap", "email"});
        vocabulary.put(Intent.ORDER_CHANGE,
                new String[]{"huy don", "doi mon", "them mon", "doi dia chi"});
        vocabulary.put(Intent.FOOD_ISSUE,
                new String[]{"thieu mon", "sai mon", "do an", "chat luong", "nguoi", "hong"});
        vocabulary.put(Intent.DELIVERY_TIME,
                new String[]{"bao lau", "may phut", "thoi gian giao", "eta", "khi nao giao", "khi nao toi", "may gio toi"});
        vocabulary.put(Intent.DELIVERY_FEE,
                new String[]{"phi giao", "phi van chuyen", "shipping fee"});
        vocabulary.put(Intent.ORDER_STATUS,
                new String[]{"trang thai don", "trang thai", "don cua toi", "don hang dau", "tra cuu don",
                        "kiem tra don", "don sao roi", "don toi dau", "don dang dau", "don the nao"});
        vocabulary.put(Intent.VOUCHER,
                new String[]{"ma giam gia", "voucher", "khuyen mai", "giam gia", "ma uu dai"});
        vocabulary.put(Intent.RESTAURANT_HOURS,
                new String[]{"gio mo cua", "may gio dong cua", "nha hang mo", "con nhan don", "dong cua"});
        vocabulary.put(Intent.REORDER,
                new String[]{"dat lai don", "mua lai", "dat lai mon", "don cu"});
        vocabulary.put(Intent.HUMAN_SUPPORT,
                new String[]{"gap nhan vien", "nguoi ho tro", "lien he ho tro", "lien he nhan vien ho tro",
                        "noi chuyen voi nguoi", "tu van vien"});
        vocabulary.put(Intent.GREETING,
                new String[]{"xin chao", "chao ban", "hello", "hi tro ly"});
    }

    public Intent classify(String message) {
        String normalized = normalize(message);
        Intent bestIntent = Intent.GENERAL;
        int bestScore = 0;

        for (Map.Entry<Intent, String[]> entry : vocabulary.entrySet()) {
            int score = 0;
            for (String phrase : entry.getValue()) {
                if (normalized.contains(phrase)) score += phrase.split(" ").length;
            }
            if (score > bestScore) {
                bestScore = score;
                bestIntent = entry.getKey();
            }
        }
        return bestIntent;
    }

    private String normalize(String value) {
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return Pattern.compile("\\p{M}").matcher(decomposed).replaceAll("")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .trim();
    }
}
