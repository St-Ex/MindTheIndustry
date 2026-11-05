package mindtheindustry;

import arc.files.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the mod bundles: the canonical EN bundle holds every key the code
 * uses (victory flavor included — never a hardcoded literal), and the locale
 * variants cover the same keys so EN fallback only matters for the long tail.
 */
class BundleTest extends HeadlessMindTheIndustryTest{

    /** Keys referenced from code (bundle.get / bundle.format). */
    private static final String[] USED_KEYS = {
        "mti.victory.title", "mti.victory.flavor", "mti.victory.chat",
        "mti.run.started",
        "mti.choice.title", "mti.choice.relic.title", "mti.choice.take.none", "mti.choice.converted",
        "mti.bosswindow.open", "mti.bosswindow.closed", "mti.bosswindow.cap",
        "mti.bosswindow.title", "mti.bosswindow.ready", "mti.bosswindow.time",
        "mti.trade.title", "mti.trade.offered", "mti.trade.incoming", "mti.trade.rejected", "mti.trade.done", "mti.trade.none", "mti.trade.accept",
        "mti.switch.denied"
    };

    private static Properties load(String name) throws IOException{
        Fi file = new Fi("bundles/" + name);
        assertTrue(file.exists(), "missing bundle file: " + file.absolutePath());
        Properties props = new Properties();
        try(Reader r = new InputStreamReader(file.read(), "UTF-8")){
            props.load(r);
        }
        return props;
    }

    @Test
    void canonicalEnBundleHasEveryUsedKey() throws IOException{
        Properties en = load("bundle.properties");
        for(String key : USED_KEYS){
            assertTrue(en.containsKey(key), "canonical EN bundle missing key: " + key);
            assertFalse(en.getProperty(key).isBlank(), "blank value for key: " + key);
        }
    }

    @Test
    void victoryFlavorIsTheCorporateSatireCopy() throws IOException{
        Properties en = load("bundle.properties");
        String flavor = en.getProperty("mti.victory.flavor");
        assertTrue(flavor.contains("GG"), "GG title in the flavor line");
        assertTrue(flavor.contains("bench time"), "corporate-satire flavor (bench time) present");
        assertEquals("GG", en.getProperty("mti.victory.title"));
    }

    @Test
    void localeVariantsCoverTheSameKeysAsEn() throws IOException{
        Properties en = load("bundle.properties");
        for(String locale : new String[]{"fr", "de", "es", "ja"}){
            Properties props = load("bundle_" + locale + ".properties");
            for(String key : USED_KEYS){
                assertTrue(props.containsKey(key),
                    "bundle_" + locale + " missing key " + key + " (would need EN fallback)");
            }
        }
    }

    @Test
    void frenchVictorySubtitleIsTheOwnersWording() throws IOException{
        Properties fr = load("bundle_fr.properties");
        String flavor = fr.getProperty("mti.victory.flavor");
        assertTrue(flavor.contains("inter-contrat"), "FR flavor keeps the owner's inter-contrat wording");
        assertTrue(flavor.contains("Mission accomplie"));
    }

    @Test
    void localesAdaptTheRegisterInsteadOfLiteralTranslation() throws IOException{
        //the satire must survive in each market's own jargon, not as a literal EN copy
        assertTrue(load("bundle_de.properties").getProperty("mti.victory.flavor").contains("Bench"));
        assertTrue(load("bundle_ja.properties").getProperty("mti.victory.flavor").contains("ベンチ"));
        assertTrue(load("bundle_es.properties").getProperty("mti.victory.flavor").contains("bench"));
    }
}
