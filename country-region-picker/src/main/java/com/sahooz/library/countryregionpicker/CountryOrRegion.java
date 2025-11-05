// java
package com.sahooz.library.countryregionpicker;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public class CountryOrRegion implements PyEntity {
    private static final String TAG = CountryOrRegion.class.getSimpleName();
    public int code;
    public String name, translate, locale, pinyin;
    public int flag;
    private static ArrayList<CountryOrRegion> countryOrRegions = new ArrayList<>();

    // 新增：索引表与初始化控制
    private static final ConcurrentHashMap<String, CountryOrRegion> nameMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CountryOrRegion> codeMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CountryOrRegion> localeMap = new ConcurrentHashMap<>();
    private static final Object LOCK = new Object();
    private static volatile boolean initialized = false;

    public CountryOrRegion(int code, String name, String translate, String pinyin, String locale, int flag) {
        this.code = code;
        this.name = name;
        this.translate = translate;
        this.flag = flag;
        this.locale = locale;
        this.pinyin = pinyin;
    }

    @Override
    public String toString() {
        return "Country{" +
                "code=" + code +
                ", name='" + name + '\'' +
                ", translate='" + translate + '\'' +
                ", locale='" + locale + '\'' +
                ", pinyin='" + pinyin + '\'' +
                ", flag=" + flag +
                '}';
    }

    public static ArrayList<CountryOrRegion> getAll() {
        return new ArrayList<>(countryOrRegions);
    }

    public static CountryOrRegion fromJson(String json) {
        if (TextUtils.isEmpty(json)) return null;
        try {
            JSONObject jo = new JSONObject(json);
            return new CountryOrRegion(
                    jo.optInt("code"),
                    jo.optString("name"),
                    jo.optString("translate"),
                    jo.optString("pinyin"),
                    jo.optString("locale"),
                    jo.optInt("flag")
            );
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String toJson() {
        JSONObject jo = new JSONObject();
        try {
            jo.put("name", name);
            jo.put("translate", translate);
            jo.put("code", code);
            jo.put("flag", flag);
            jo.put("pinyin", pinyin);
            jo.put("locale", locale);
            return jo.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "{}";
    }

    /**
     * 原 load 方法，仍然可被外部手动调用。被调用时会重建索引。
     */
    public static void load(@NonNull Context ctx) throws IOException, JSONException {
        ArrayList<CountryOrRegion> list = new ArrayList<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(ctx.getResources().getAssets().open("code.json")));
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null)
            sb.append(line);
        br.close();
        JSONArray ja = new JSONArray(sb.toString());
        for (int i = 0; i < ja.length(); i++) {
            JSONObject jo = ja.getJSONObject(i);
            int flagRes = 0;
            String translate = "";
            String locale = jo.optString("locale");
            if (!TextUtils.isEmpty(locale)) {
                flagRes = ctx.getResources().getIdentifier("flag_" + locale.toLowerCase(), "drawable", ctx.getPackageName());
                int nameRes = ctx.getResources().getIdentifier("name_" + locale.toLowerCase(), "string", ctx.getPackageName());
                if (nameRes != 0) translate = ctx.getString(nameRes);
            }
            String name = translate;
            Locale defaultLoc = Locale.getDefault();
            boolean inChina = "zh".equalsIgnoreCase(defaultLoc.getLanguage());
            list.add(
                    new CountryOrRegion(
                            jo.optInt("code"),
                            name,
                            translate,
                            inChina ? jo.optString("pinyin") : name,
                            locale,
                            flagRes
                    )
            );
        }

        Collections.sort(list, (o1, o2) -> o1.getPinyin().compareTo(o2.getPinyin()));
        // 替换全局列表并重建索引
        countryOrRegions = list;
        rebuildIndexes();
        initialized = true;
    }

    public static void destroy() {
        countryOrRegions.clear();
        nameMap.clear();
        codeMap.clear();
        localeMap.clear();
        initialized = false;
    }

    @Override
    public int hashCode() {
        return code;
    }

    @NonNull
    @Override
    public String getPinyin() {
        return pinyin;
    }

    // 重建索引表（在 load 后使用）
    private static void rebuildIndexes() {
        nameMap.clear();
        codeMap.clear();
        localeMap.clear();
        for (CountryOrRegion cor : countryOrRegions) {
            if (cor.name != null) nameMap.put(cor.name.toLowerCase(), cor);
            codeMap.put(String.valueOf(cor.code), cor);
            if (cor.locale != null) localeMap.put(cor.locale.toLowerCase(), cor);
        }
    }

    // 确保已初始化，失败时尝试回退填充索引一次（不抛异常）
    private static void ensureLoaded(Context ctx) {
        if (initialized) return;
        synchronized (LOCK) {
            if (initialized) return;
            try {
                load(ctx);
                return;
            } catch (Exception ignored) {
                // 尝试回退：只读取文件并填充索引（不构建完整 list）
                try {
                    InputStream is = ctx.getAssets().open("code.json");
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();
                    JSONArray array = new JSONArray(sb.toString());
                    nameMap.clear();
                    codeMap.clear();
                    localeMap.clear();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject jo = array.getJSONObject(i);
                        int code = jo.optInt("code");
                        String locale = jo.optString("locale");
                        String name = jo.optString("name");
                        String translate = "";
                        int flagRes = 0;
                        if (!TextUtils.isEmpty(locale)) {
                            flagRes = ctx.getResources().getIdentifier("flag_" + locale.toLowerCase(), "drawable", ctx.getPackageName());
                            int nameRes = ctx.getResources().getIdentifier("name_" + locale.toLowerCase(), "string", ctx.getPackageName());
                            if (nameRes != 0) translate = ctx.getString(nameRes);
                        }
                        String displayName = TextUtils.isEmpty(translate) ? name : translate;
                        CountryOrRegion cor = new CountryOrRegion(code, displayName, translate, displayName, locale, flagRes);
                        if (!TextUtils.isEmpty(displayName)) nameMap.put(displayName.toLowerCase(), cor);
                        codeMap.put(String.valueOf(code), cor);
                        if (!TextUtils.isEmpty(locale)) localeMap.put(locale.toLowerCase(), cor);
                    }
                } catch (Exception e) {
                    // 最终失败则保持为空，查询方法将返回默认值
                    e.printStackTrace();
                }
                initialized = true;
            }
        }
    }

    /**
     * 优化后的查询：自动识别 keyWord 类型（数字->code, 两字母或包含字母->locale/name）
     * 优先使用内存索引，已初始化则不会读取 assets 多次。
     */
    public static int getFlagResIdByCountry(Context ctx, String keyWord) {
        if (TextUtils.isEmpty(keyWord)) return 0;
        ensureLoaded(ctx);

        // 优先按 code (纯数字)
        if (TextUtils.isDigitsOnly(keyWord)) {
            CountryOrRegion cor = codeMap.get(keyWord);
            if (cor != null) return cor.flag;
        }

        // 如果看起来像 locale（1-3 字母），先尝试 locale
        String kwLower = keyWord.toLowerCase();
        if (kwLower.length() <= 3) {
            CountryOrRegion cor = localeMap.get(kwLower);
            if (cor != null) return cor.flag;
        }

        // 最后按 name 匹配（大小写不敏感）
        CountryOrRegion cor = nameMap.get(kwLower);
        if (cor != null) return cor.flag;

        // 如果索引为空或未命中，作为兜底仍然尝试读取文件一次（已在 ensureLoaded 中完成），直接返回 0
        return 0;
    }

    /**
     * 兼容旧签名：根据 type 选择匹配策略，内部调用优化后的逻辑
     */
    public static int getFlagResIdByCountry(Context ctx, String keyWord, int type) {
        if (TextUtils.isEmpty(keyWord)) return 0;
        ensureLoaded(ctx);
        if (type == 2) { // code
            CountryOrRegion cor = codeMap.get(keyWord);
            return cor != null ? cor.flag : 0;
        } else if (type == 3) { // locale
            CountryOrRegion cor = localeMap.get(keyWord.toLowerCase());
            return cor != null ? cor.flag : 0;
        } else { // type == 1 or default: name
            CountryOrRegion cor = nameMap.get(keyWord.toLowerCase());
            return cor != null ? cor.flag : 0;
        }
    }

    // 已存在的方法，改为使用索引表以提高性能
    public static String getNameByCodeOrLocale(Context ctx, String keyWord) {
        if (TextUtils.isEmpty(keyWord)) return "";
        ensureLoaded(ctx);
        if (TextUtils.isDigitsOnly(keyWord)) {
            CountryOrRegion cor = codeMap.get(keyWord);
            if (cor != null) return cor.name != null ? cor.name : "";
        } else {
            CountryOrRegion cor = localeMap.get(keyWord.toLowerCase());
            if (cor != null) return cor.name != null ? cor.name : "";
        }
        return "";
    }
}