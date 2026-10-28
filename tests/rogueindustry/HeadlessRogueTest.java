package rogueindustry;

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
 * Classe de base de tous les tests du mod.
 *
 * Boot Mindustry en mode headless (sans UI ni rendu), une seule fois par JVM de test,
 * en suivant le pattern des tests amont du jeu
 * (../Mindustry/tests/src/test/java/ApplicationTests.java).
 *
 * Toute la logique métier (core/, rewards/, modifiers/, meta/, sérialisation net/)
 * doit être testable via cette classe — sans lancer le jeu.
 */
public abstract class HeadlessRogueTest{

    private static boolean initialized;
    private static Throwable bootError;

    @BeforeAll
    static void bootHeadless(){
        if(initialized){
            // boot déjà tenté: propager la même erreur plutôt que de retenter
            // (un deuxième boot corromprait l'état global: content, classes statiques)
            if(bootError != null){
                throw new IllegalStateException("Échec du boot headless de Mindustry", bootError);
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
                    // assets absents du JAR de test: skip les lectures de fichiers internes optionnels
                    loadLocales = false;
                    Version.enabled = false;
                    Vars.init();
                    // Vars.init() ne crée pas Schematics (c'est le launcher client qui le fait)
                    if(schematics == null) schematics = new Schematics();
                    world = new World(){
                        @Override
                        public float getDarkness(int x, int y){
                            // pas de bords de carte en test
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

            // attendre la fin du boot sur le thread headless
            while(!began[0]){
                if(bootError != null){
                    throw new IllegalStateException("Échec du boot headless de Mindustry", bootError);
                }
                Thread.sleep(10);
            }
        }catch(Throwable t){
            bootError = t;
            throw new IllegalStateException("Échec du boot headless de Mindustry", t);
        }
    }
}
