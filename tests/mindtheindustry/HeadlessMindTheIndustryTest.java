package mindtheindustry;

import arc.*;
import arc.backend.headless.*;
import arc.files.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.net.*;
import mindustry.world.*;
import org.junit.jupiter.api.*;

import static mindustry.Vars.*;

/**
 * Base class for all mod tests.
 *
 * Boots Mindustry headless (no UI, no rendering), once per test JVM,
 * following the upstream test pattern
 * (../Mindustry/tests/src/test/java/ApplicationTests.java).
 *
 * All game logic (core/, rewards/, modifiers/, meta/, net/ serialization)
 * must be testable through this class — without launching the game.
 */
public abstract class HeadlessMindTheIndustryTest{

    private static boolean initialized;
    private static Throwable bootError;

    @BeforeAll
    static void bootHeadless(){
        if(initialized){
            // boot already attempted: propagate the same error instead of retrying
            // (a second boot would corrupt global state: content, static classes)
            if(bootError != null){
                throw new IllegalStateException("Failed to boot headless Mindustry", bootError);
            }
            return;
        }
        initialized = true;

        Log.useColors = false;

        try{
            boolean[] began = {false};

            ApplicationCore core = new ApplicationCore(){
                @Override
                public void setup(){
                    Fi testData = new Fi("build/test_data");
                    testData.mkdirs();
                    Core.settings.setDataDirectory(testData);
                    headless = true;
                    net = new Net(null);
                    tree = new FileTree();
                    // assets missing from the test JAR: skip optional internal file reads
                    loadLocales = false;
                    Version.enabled = false;
                    Vars.init();
                    // Vars.init() does not create Schematics (the client launcher does)
                    if(schematics == null) schematics = new Schematics();
                    world = new World(){
                        @Override
                        public float getDarkness(int x, int y){
                            // no dark map borders in tests
                            return 0;
                        }
                    };
                    content.createBaseContent();
                    add(logic = new Logic());
                    add(netServer = new NetServer());
                    content.init();
                }

                @Override
                public void init(){
                    super.init();
                    began[0] = true;
                }
            };

            new HeadlessApplication(core, t -> bootError = t);

            // wait for the boot to finish on the headless thread
            while(!began[0]){
                if(bootError != null){
                    throw new IllegalStateException("Failed to boot headless Mindustry", bootError);
                }
                Thread.sleep(10);
            }
        }catch(Throwable t){
            bootError = t;
            throw new IllegalStateException("Failed to boot headless Mindustry", t);
        }
    }
}
