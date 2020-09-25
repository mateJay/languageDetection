package cn.bluepulse.base.util;

import cn.bluepulse.util.utils.constants.BpLang;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 语种检测
 * https://github.com/optimaize/language-detector
 */
public class LanguageDetection {
    private static final List<LdLocale> LOAD_LANGUAGE;
    private volatile static LanguageDetection instance;
    private final LanguageDetector languageDetector;

    private static Logger logger = LoggerFactory.getLogger(LanguageDetection.class);

    /**
     * 语种识别代码与bpLang之间的转换
     */
    private static final Map<String, Integer> LANG_DETECTION_CONVERT = new HashMap<>();

    static {
        // (根据源码：BuiltInLanguages类添加)
        // 加载的语种。加载的越多，混淆的可能越大
        // todo 如果需要扩充语种库，需要同步更新LANG_DETECTION_CONVERT，更新时需要测试，有可能和fromString不同
        List<LdLocale> languageNames = new ArrayList<>();
        languageNames.add(LdLocale.fromString("zh-CN"));
        languageNames.add(LdLocale.fromString("en"));
        languageNames.add(LdLocale.fromString("ja"));
        languageNames.add(LdLocale.fromString("ko"));
        languageNames.add(LdLocale.fromString("ru"));
        LOAD_LANGUAGE = ImmutableList.copyOf(languageNames);

        LANG_DETECTION_CONVERT.put("zh", BpLang.LANG_ZH);
        LANG_DETECTION_CONVERT.put("en", BpLang.LANG_EN);
        LANG_DETECTION_CONVERT.put("ja", BpLang.LANG_JP);
        LANG_DETECTION_CONVERT.put("ko", BpLang.LANG_KOR);
        LANG_DETECTION_CONVERT.put("ru", BpLang.LANG_RU);

    }

    private LanguageDetection() throws IOException {
        List<LanguageProfile> languageProfiles = new LanguageProfileReader().readBuiltIn(LOAD_LANGUAGE);
        // todo 这里设置最小得分为0.7，如果想要提高准确度，可以修改
        languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                .withProfiles(languageProfiles).minimalConfidence(0.7)
                .build();
    }
    public static LanguageDetection getInstance() {
        if (instance == null) {
            synchronized (LanguageDetection.class) {
                if (instance == null) {
                    try {
                        instance = new LanguageDetection();
                    } catch (IOException e) {
                        logger.error("语种识别初始化读取语种文件IO异常，errMsg:", e);
                        e.printStackTrace();
                    }
                }
            }
        }
        return instance;
    }

    /**
     * 返回识别的语种
     * @param text
     * @return
     */
    public Integer languageDetection(String text) {
        TextObjectFactory textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();

        TextObject textObject = textObjectFactory.forText(text);
        Optional<LdLocale> lang = languageDetector.detect(textObject);
        if (lang.isPresent()) {
            System.out.println(lang.get().getLanguage());
            return LANG_DETECTION_CONVERT.get(lang.get().getLanguage());
        } else {
            return BpLang.LANG_UNKNOWN;
        }
    }

    /**
     * 歌曲识别入excel使用
     * @param text
     * @return
     */
    public String languageDetectionTmp(String text) {
        TextObjectFactory textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();

        TextObject textObject = textObjectFactory.forText(text);
        Optional<LdLocale> lang = languageDetector.detect(textObject);
        if (lang.isPresent()) {
            return lang.get().getLanguage();
        } else {
            return "other";
        }
    }

    /**
     * 批量语种识别
     * @param textArr
     * @return key-语种代码；value-语种下对应的textList
     */
    public Map<Integer, List<String>> batchLanguageDetection(String[] textArr) {
        Map<Integer, List<String>> langTextMap = new HashMap<>();
        for (String text : textArr) {
            Integer lang = languageDetection(text);
            List<String> value = langTextMap.getOrDefault(lang, new ArrayList<>());
            value.add(text);
            langTextMap.put(lang, value);
        }

        return langTextMap;
    }

    public static void main(String[] args) {
        getInstance().languageDetection("Привет");
        getInstance().languageDetection("hello world");
    }
}
